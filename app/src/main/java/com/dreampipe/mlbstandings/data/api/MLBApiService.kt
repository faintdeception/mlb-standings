package com.dreampipe.mlbstandings.data.api

import com.dreampipe.mlbstandings.data.model.MLBStandingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MLBApiService {
    @GET("api/v1/standings")
    suspend fun getStandings(
        @Query("leagueId") leagueId: String = "103,104", // AL and NL
        @Query("season") season: String = "2025"
    ): Response<MLBStandingsResponse>
}
