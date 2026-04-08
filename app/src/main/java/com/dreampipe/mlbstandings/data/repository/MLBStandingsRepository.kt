package com.dreampipe.mlbstandings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dreampipe.mlbstandings.data.api.MLBApiService
import com.dreampipe.mlbstandings.data.model.MLBStandingsResponse
import com.dreampipe.mlbstandings.data.model.TeamRecord
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mlb_settings")

class MLBStandingsRepository(private val context: Context) {
    
    private val mlbApiService: MLBApiService = Retrofit.Builder()
        .baseUrl("https://statsapi.mlb.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MLBApiService::class.java)
    
    private val gson = Gson()
    
    companion object {
        private val CACHED_STANDINGS_KEY = stringPreferencesKey("cached_standings")
        private val CACHED_SEASON_KEY = stringPreferencesKey("cached_season")
        private val LAST_UPDATE_KEY = longPreferencesKey("last_update")
        private val FAVORITE_TEAM_KEY = stringPreferencesKey("favorite_team")
        private val FAVORITE_TEAM_ID_KEY = intPreferencesKey("favorite_team_id")
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L

        private val TEAM_IDS = mapOf(
            "Arizona Diamondbacks" to 109,
            "Atlanta Braves" to 144,
            "Baltimore Orioles" to 110,
            "Boston Red Sox" to 111,
            "Chicago Cubs" to 112,
            "Chicago White Sox" to 145,
            "Cincinnati Reds" to 113,
            "Cleveland Guardians" to 114,
            "Colorado Rockies" to 115,
            "Detroit Tigers" to 116,
            "Houston Astros" to 117,
            "Kansas City Royals" to 118,
            "Los Angeles Angels" to 108,
            "Los Angeles Dodgers" to 119,
            "Miami Marlins" to 146,
            "Milwaukee Brewers" to 158,
            "Minnesota Twins" to 142,
            "New York Mets" to 121,
            "New York Yankees" to 147,
            "Athletics" to 133,
            "Philadelphia Phillies" to 143,
            "Pittsburgh Pirates" to 134,
            "San Diego Padres" to 135,
            "San Francisco Giants" to 137,
            "Seattle Mariners" to 136,
            "St. Louis Cardinals" to 138,
            "Tampa Bay Rays" to 139,
            "Texas Rangers" to 140,
            "Toronto Blue Jays" to 141,
            "Washington Nationals" to 120
        )
    }
    
    private suspend fun <T> retryNetworkCall(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        delayMs: Long = RETRY_DELAY_MS,
        call: suspend () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                return call()
            } catch (e: java.net.UnknownHostException) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(delayMs * (attempt + 1)) // Exponential backoff
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(delayMs * (attempt + 1))
                }
            } catch (e: Exception) {
                // For non-network errors, don't retry
                throw e
            }
        }
        
        // All retries failed
        throw lastException ?: Exception("Unknown network error")
    }
    
    suspend fun getStandings(forceRefresh: Boolean = false): Result<MLBStandingsResponse> {
        return try {
            val currentSeason = getCurrentSeason()

            // Check if we have cached data from today
            val cachedState = context.dataStore.data.map { preferences ->
                Triple(
                    preferences[LAST_UPDATE_KEY] ?: 0L,
                    preferences[CACHED_SEASON_KEY],
                    preferences[CACHED_STANDINGS_KEY]
                )
            }.first()
            val lastUpdate = cachedState.first
            val cachedSeason = cachedState.second
            val cachedJson = cachedState.third
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            if (!forceRefresh && lastUpdate >= today && cachedSeason == currentSeason) {
                // Return cached data
                cachedJson?.let {
                    val cachedData = gson.fromJson(it, MLBStandingsResponse::class.java)
                    return Result.success(cachedData)
                }
            }
            
            // Try to fetch fresh data with retry logic
            try {
                val response = retryNetworkCall {
                    mlbApiService.getStandings(season = currentSeason)
                }
                
                if (response.isSuccessful) {
                    val standings = response.body()!!
                    
                    // Cache the data
                    context.dataStore.edit { preferences ->
                        preferences[CACHED_STANDINGS_KEY] = gson.toJson(standings)
                        preferences[CACHED_SEASON_KEY] = currentSeason
                        preferences[LAST_UPDATE_KEY] = System.currentTimeMillis()
                    }
                    
                    Result.success(standings)
                } else {
                    // API error - try to fall back to stale cache
                    tryStaleCache() ?: Result.failure(Exception("API_ERR_${response.code()}"))
                }
            } catch (e: java.net.UnknownHostException) {
                // Network error after retries - try stale cache
                tryStaleCache() ?: Result.failure(Exception("NET_ERR"))
            } catch (e: java.net.SocketTimeoutException) {
                // Timeout after retries - try stale cache
                tryStaleCache() ?: Result.failure(Exception("TIMEOUT"))
            } catch (e: Exception) {
                // Other error - try stale cache
                tryStaleCache() ?: Result.failure(Exception("UNK_ERR"))
            }
        } catch (e: Exception) {
            tryStaleCache() ?: Result.failure(Exception("UNK_ERR"))
        }
    }
    
    private suspend fun tryStaleCache(): Result<MLBStandingsResponse>? {
        return try {
            val cachedJson = context.dataStore.data.map { preferences ->
                preferences[CACHED_STANDINGS_KEY]
            }.first()
            
            cachedJson?.let {
                val cachedData = gson.fromJson(it, MLBStandingsResponse::class.java)
                Result.success(cachedData)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getFavoriteTeam(): String {
        return context.dataStore.data.map { preferences ->
            preferences[FAVORITE_TEAM_KEY] ?: "New York Yankees"
        }.first()
    }
    
    suspend fun setFavoriteTeam(teamName: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_TEAM_KEY] = teamName
            TEAM_IDS[teamName]?.let { teamId ->
                preferences[FAVORITE_TEAM_ID_KEY] = teamId
            }
        }
    }

    suspend fun getFavoriteTeamId(): Int? {
        val preferences = context.dataStore.data.first()
        return preferences[FAVORITE_TEAM_ID_KEY] ?: TEAM_IDS[preferences[FAVORITE_TEAM_KEY]]
    }
    
    suspend fun getFavoriteTeamRecord(): Result<TeamRecord?> {
        val favoriteTeam = getFavoriteTeam()
        val favoriteTeamId = getFavoriteTeamId()
        return getStandings().map { standings ->
            standings.records.flatMap { it.teamRecords }
                .find { teamRecord ->
                    favoriteTeamId != null && teamRecord.team.id == favoriteTeamId
                }
                ?: standings.records.flatMap { it.teamRecords }
                    .find { teamRecord -> teamNamesMatch(teamRecord.team.name, favoriteTeam) }
        }
    }
    
    suspend fun getTopTeams(count: Int = 5): Result<List<TeamRecord>> {
        return getStandings().map { standings ->
            standings.records.flatMap { it.teamRecords }
                .sortedBy { it.sportRank.toIntOrNull() ?: Int.MAX_VALUE }
                .take(count)
        }
    }
    
    suspend fun getDivisionStandings(teamName: String): Result<List<TeamRecord>> {
        val favoriteTeamId = getFavoriteTeamId() ?: TEAM_IDS[teamName]
        return getStandings().map { standings ->
            val teamDivision = standings.records.find { division ->
                division.teamRecords.any { teamRecord ->
                    (favoriteTeamId != null && teamRecord.team.id == favoriteTeamId) ||
                        teamNamesMatch(teamRecord.team.name, teamName)
                }
            }
            teamDivision?.teamRecords?.sortedBy { it.divisionRank.toIntOrNull() ?: Int.MAX_VALUE } ?: emptyList()
        }
    }

    private fun getCurrentSeason(): String {
        return Calendar.getInstance().get(Calendar.YEAR).toString()
    }

    private fun teamNamesMatch(apiTeamName: String, favoriteTeamName: String): Boolean {
        val normalizedApiName = normalizeTeamName(apiTeamName)
        val normalizedFavoriteName = normalizeTeamName(favoriteTeamName)

        return normalizedApiName == normalizedFavoriteName ||
            normalizedFavoriteName.endsWith(normalizedApiName) ||
            normalizedApiName.endsWith(normalizedFavoriteName)
    }

    private fun normalizeTeamName(teamName: String): String {
        return teamName
            .lowercase()
            .replace("[^a-z0-9]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
