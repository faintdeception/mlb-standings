package com.dreampipe.mlbstandings.glyph

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.dreampipe.mlbstandings.data.repository.MLBStandingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Glyph Matrix SDK imports
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import com.nothing.ketchum.Glyph

class MLBStandingsGlyphToyService : Service() {
    
    private var glyphMatrixManager: GlyphMatrixManager? = null
    private lateinit var repository: MLBStandingsRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var currentDisplayMode = DisplayMode.FAVORITE_TEAM
    private var animationFrame = 0
    private var isLoading = false
    
    enum class DisplayMode {
        FAVORITE_TEAM,  // Shows favorite team abbreviation (first screen)
        TEAM_RECORD,    // Shows favorite team's wins/losses
        DIVISION,       // Shows division standings
        TOP_TEAMS       // Shows top teams in MLB
    }
    
    private val serviceHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val bundle = msg.data
                val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                handleGlyphEvent(event)
                true
            }
            else -> false
        }
    }
    
    private val serviceMessenger = Messenger(serviceHandler)
    
    override fun onCreate() {
        super.onCreate()
        repository = MLBStandingsRepository(this)
        Log.d(TAG, "MLB Standings Glyph Toy Service created")
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound - initializing Glyph Matrix")
        init()
        return serviceMessenger.binder
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Service unbound - cleaning up")
        cleanup()
        return false
    }
    
    private fun init() {
        try {
            // Initialize Glyph Matrix Manager using getInstance pattern
            glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
            glyphMatrixManager?.init(callback)
            glyphMatrixManager?.register("23111") // For Nothing Phone 3
            
            Log.d(TAG, "Glyph Matrix initialized successfully")
            
            // Start displaying data
            displayCurrentMode()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Glyph Matrix: ${e.message}")
        }
    }
    
    private fun cleanup() {
        try {
            serviceScope.coroutineContext[Job]?.cancel()
            glyphMatrixManager?.let {
                it.unInit()
                glyphMatrixManager = null
            }
            Log.d(TAG, "Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
    
    private fun handleGlyphEvent(event: String?) {
        when (event) {
            GlyphToy.EVENT_CHANGE -> {
                // Long press - cycle through display modes
                currentDisplayMode = when (currentDisplayMode) {
                    DisplayMode.FAVORITE_TEAM -> DisplayMode.TEAM_RECORD
                    DisplayMode.TEAM_RECORD -> DisplayMode.DIVISION
                    DisplayMode.DIVISION -> DisplayMode.TOP_TEAMS
                    DisplayMode.TOP_TEAMS -> DisplayMode.FAVORITE_TEAM
                }
                Log.d(TAG, "Display mode changed to: $currentDisplayMode")
                displayCurrentMode()
            }
            GlyphToy.EVENT_AOD -> {
                // Always-on display update (every minute)
                Log.d(TAG, "AOD update triggered")
                displayCurrentMode()
            }
            "action_down" -> {
                // Button pressed down
                Log.d(TAG, "Glyph button pressed down")
            }
            "action_up" -> {
                // Button released
                Log.d(TAG, "Glyph button released")
            }
        }
    }
    
    private fun displayCurrentMode() {
        if (isLoading) return
        
        serviceScope.launch {
            isLoading = true
            try {
                when (currentDisplayMode) {
                    DisplayMode.FAVORITE_TEAM -> displayFavoriteTeam()
                    DisplayMode.TEAM_RECORD -> displayFavoriteTeamRecord()
                    DisplayMode.DIVISION -> displayDivisionStandings()
                    DisplayMode.TOP_TEAMS -> displayTopTeams()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying mode $currentDisplayMode: ${e.message}")
                displayError()
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun displayFavoriteTeam() {
        Log.d(TAG, "Displaying favorite team abbreviation")
        
        try {
            val favoriteTeamName = repository.getFavoriteTeam()
            val teamAbbrev = GlyphMatrixUtils.getTeamAbbreviation(favoriteTeamName)
            
            val frame = GlyphMatrixUtils.createFavoriteTeamFrame(
                teamAbbrev,
                this@MLBStandingsGlyphToyService
            )
            displayFrame(frame)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to get team abbreviation: ${error.message}")
            displayError("CFG")
        }
    }
    
    private suspend fun displayFavoriteTeamRecord() {
        Log.d(TAG, "Displaying favorite team record")
        
        /* Temporarily hardcode triple-digit values for testing
        val frame = GlyphMatrixUtils.createWinLossFrame(
            101, // Test wins 
            110, // Test losses
            this@MLBStandingsGlyphToyService
        )
        displayFrame(frame)
        */
        // Original code - commented out for testing
        val result = repository.getFavoriteTeamRecord()
        result.fold(
            onSuccess = { teamRecord ->
                if (teamRecord != null) {
                    val frame = GlyphMatrixUtils.createWinLossFrame(
                        teamRecord.wins, 
                        teamRecord.losses,
                        this@MLBStandingsGlyphToyService
                    )
                    displayFrame(frame)
                } else {
                    displayError("NFD")
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get team record: ${error.message}")
                val errorCode = when {
                    error.message?.contains("NET_ERR") == true -> "NET"
                    error.message?.contains("TIMEOUT") == true -> "TMO"
                    error.message?.contains("API_ERR") == true -> "API"
                    else -> "ERR"
                }
                displayError(errorCode)
            }
        )        
    }
    
    private suspend fun displayTopTeams() {
        Log.d(TAG, "Displaying top teams")
        
        val result = repository.getTopTeams(5)
        result.fold(
            onSuccess = { topTeams ->
                // Get all team abbreviations
                val teamAbbrevs = topTeams.map { team ->
                    GlyphMatrixUtils.getTeamAbbreviation(team.team.name)
                }
                
                // Display all teams at once using rankings frame
                val frame = GlyphMatrixUtils.createRankingsFrame(teamAbbrevs, this@MLBStandingsGlyphToyService)
                displayFrame(frame)
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get top teams: ${error.message}")
                val errorCode = when {
                    error.message?.contains("NET_ERR") == true -> "NET"
                    error.message?.contains("TIMEOUT") == true -> "TMO"
                    error.message?.contains("API_ERR") == true -> "API"
                    else -> "ERR"
                }
                displayError(errorCode)
            }
        )
    }
    
    private suspend fun displayDivisionStandings() {
        Log.d(TAG, "Displaying division standings")
        
        val favoriteTeam = repository.getFavoriteTeam()
        val result = repository.getDivisionStandings(favoriteTeam)
        
        result.fold(
            onSuccess = { divisionTeams ->
                if (divisionTeams.isNotEmpty()) {
                    // Get division name from first team's league/division info
                    val divisionName = "AL E" // Simplified for now
                    
                    // Prepare teams with records
                    val teamsWithRecords = divisionTeams.map { team ->
                        val abbrev = GlyphMatrixUtils.getTeamAbbreviation(team.team.name)
                        val record = "${team.wins}-${team.losses}"
                        abbrev to record
                    }
                    
                    // Get favorite team abbreviation
                    val favoriteTeamAbbrev = GlyphMatrixUtils.getTeamAbbreviation(favoriteTeam)
                    
                    // Use the new frame-based approach for division standings
                    val frame = GlyphMatrixUtils.createDivisionFrame(divisionName, teamsWithRecords, favoriteTeamAbbrev, this@MLBStandingsGlyphToyService)
                    displayFrame(frame)
                    Log.d(TAG, "Division $divisionName teams: $teamsWithRecords")
                } else {
                    displayError("NOD")
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get division standings: ${error.message}")
                val errorCode = when {
                    error.message?.contains("NET_ERR") == true -> "NET"
                    error.message?.contains("TIMEOUT") == true -> "TMO"
                    error.message?.contains("API_ERR") == true -> "API"
                    else -> "ERR"
                }
                displayError(errorCode)
            }
        )
    }
    
    private fun displayFrame(frame: GlyphMatrixFrame) {
        try {
            glyphMatrixManager?.setMatrixFrame(frame.render())
            Log.d(TAG, "Displaying frame on Glyph Matrix")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display frame: ${e.message}")
        }
    }
    
    private fun displayError(message: String = "ERR") {
        val frame = GlyphMatrixUtils.createErrorFrame(message, this@MLBStandingsGlyphToyService)
        displayFrame(frame)
        Log.e(TAG, "Displaying error: $message")
    }
    
    private fun displayLoading() {
        serviceScope.launch {
            repeat(10) { frameCount ->
                if (!isLoading) return@repeat
                val array = GlyphMatrixUtils.createLoadingArray(frameCount)
                glyphMatrixManager?.setMatrixFrame(array)
                delay(200)
            }
        }
    }
    
    // Glyph Matrix Manager callback
    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: android.content.ComponentName?) {
            Log.d(TAG, "Glyph Matrix service connected: $name")
        }
        
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            Log.d(TAG, "Glyph Matrix service disconnected: $name")
        }
    }
    
    companion object {
        private const val TAG = "MLBStandingsGlyphToy"
    }
}
