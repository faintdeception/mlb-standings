package com.dreampipe.mlbstandings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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
        private val LAST_UPDATE_KEY = longPreferencesKey("last_update")
        private val FAVORITE_TEAM_KEY = stringPreferencesKey("favorite_team")
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
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
    
    suspend fun getStandings(): Result<MLBStandingsResponse> {
        return try {
            // Check if we have cached data from today
            val lastUpdate = context.dataStore.data.map { preferences ->
                preferences[LAST_UPDATE_KEY] ?: 0L
            }.first()
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            if (lastUpdate >= today) {
                // Return cached data
                val cachedJson = context.dataStore.data.map { preferences ->
                    preferences[CACHED_STANDINGS_KEY]
                }.first()
                
                cachedJson?.let {
                    val cachedData = gson.fromJson(it, MLBStandingsResponse::class.java)
                    return Result.success(cachedData)
                }
            }
            
            // Try to fetch fresh data with retry logic
            try {
                val response = retryNetworkCall {
                    mlbApiService.getStandings()
                }
                
                if (response.isSuccessful) {
                    val standings = response.body()!!
                    
                    // Cache the data
                    context.dataStore.edit { preferences ->
                        preferences[CACHED_STANDINGS_KEY] = gson.toJson(standings)
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
        }
    }
    
    suspend fun getFavoriteTeamRecord(): Result<TeamRecord?> {
        val favoriteTeam = getFavoriteTeam()
        return getStandings().map { standings ->
            standings.records.flatMap { it.teamRecords }
                .find { it.team.name == favoriteTeam }
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
        return getStandings().map { standings ->
            val teamDivision = standings.records.find { division ->
                division.teamRecords.any { it.team.name == teamName }
            }
            teamDivision?.teamRecords?.sortedBy { it.divisionRank.toIntOrNull() ?: Int.MAX_VALUE } ?: emptyList()
        }
    }
}
