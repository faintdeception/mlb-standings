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
            
            // Fetch fresh data
            val response = mlbApiService.getStandings()
            if (response.isSuccessful) {
                val standings = response.body()!!
                
                // Cache the data
                context.dataStore.edit { preferences ->
                    preferences[CACHED_STANDINGS_KEY] = gson.toJson(standings)
                    preferences[LAST_UPDATE_KEY] = System.currentTimeMillis()
                }
                
                Result.success(standings)
            } else {
                Result.failure(Exception("Failed to fetch standings: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
