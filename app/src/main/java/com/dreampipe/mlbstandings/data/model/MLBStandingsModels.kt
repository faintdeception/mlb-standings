package com.dreampipe.mlbstandings.data.model

import com.google.gson.annotations.SerializedName

data class MLBStandingsResponse(
    @SerializedName("records")
    val records: List<DivisionRecord>
)

data class DivisionRecord(
    @SerializedName("standingsType")
    val standingsType: String,
    @SerializedName("league")
    val league: League,
    @SerializedName("division")
    val division: Division,
    @SerializedName("teamRecords")
    val teamRecords: List<TeamRecord>
)

data class League(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String? = null
)

data class Division(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String? = null
)

data class TeamRecord(
    @SerializedName("team")
    val team: Team,
    @SerializedName("wins")
    val wins: Int,
    @SerializedName("losses")
    val losses: Int,
    @SerializedName("winningPercentage")
    val winningPercentage: String,
    @SerializedName("gamesBack")
    val gamesBack: String,
    @SerializedName("divisionRank")
    val divisionRank: String,
    @SerializedName("leagueRank")
    val leagueRank: String,
    @SerializedName("sportRank")
    val sportRank: String,
    @SerializedName("streak")
    val streak: Streak? = null,
    @SerializedName("divisionLeader")
    val divisionLeader: Boolean = false,
    @SerializedName("wildCardLeader")
    val wildCardLeader: Boolean = false
)

data class Team(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)

data class Streak(
    @SerializedName("streakType")
    val streakType: String,
    @SerializedName("streakNumber")
    val streakNumber: Int,
    @SerializedName("streakCode")
    val streakCode: String
)
