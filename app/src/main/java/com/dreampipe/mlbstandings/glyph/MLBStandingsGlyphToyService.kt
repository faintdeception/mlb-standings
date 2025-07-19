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
    
    private var currentDisplayMode = DisplayMode.TEAM_RECORD
    private var animationFrame = 0
    private var isLoading = false
    
    enum class DisplayMode {
        TEAM_RECORD,    // Shows favorite team's wins/losses
        TOP_TEAMS,      // Shows top 5 MLB teams
        DIVISION        // Shows division standings
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
                    DisplayMode.TEAM_RECORD -> DisplayMode.TOP_TEAMS
                    DisplayMode.TOP_TEAMS -> DisplayMode.DIVISION
                    DisplayMode.DIVISION -> DisplayMode.TEAM_RECORD
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
                    DisplayMode.TEAM_RECORD -> displayFavoriteTeamRecord()
                    DisplayMode.TOP_TEAMS -> displayTopTeams()
                    DisplayMode.DIVISION -> displayDivisionStandings()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying mode $currentDisplayMode: ${e.message}")
                displayError()
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun displayFavoriteTeamRecord() {
        Log.d(TAG, "Displaying favorite team record")
        
        // Temporarily hardcode triple-digit values for testing
        val bitmap = GlyphMatrixUtils.createWinLossBitmap(
            101, // Test wins 
            110, // Test losses
            this@MLBStandingsGlyphToyService
        )
        displayBitmap(bitmap)
        
        /* Original code - commented out for testing
        val result = repository.getFavoriteTeamRecord()
        result.fold(
            onSuccess = { teamRecord ->
                if (teamRecord != null) {
                    val bitmap = GlyphMatrixUtils.createWinLossBitmap(
                        teamRecord.wins, 
                        teamRecord.losses, 
                        this@MLBStandingsGlyphToyService
                    )
                    displayBitmap(bitmap)
                } else {
                    displayError("Team not found")
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get team record: ${error.message}")
                displayError("Network error")
            }
        )
        */
    }
    
    private suspend fun displayTopTeams() {
        Log.d(TAG, "Displaying top teams")
        
        val result = repository.getTopTeams(5)
        result.fold(
            onSuccess = { topTeams ->
                // Cycle through top 5 teams
                for ((index, team) in topTeams.withIndex()) {
                    val abbrev = GlyphMatrixUtils.getTeamAbbreviation(team.team.name)
                    val rank = team.sportRank.toIntOrNull() ?: (index + 1)
                    val bitmap = GlyphMatrixUtils.createRankingBitmap(
                        rank, 
                        abbrev, 
                        this@MLBStandingsGlyphToyService
                    )
                    displayBitmap(bitmap)
                    delay(2000) // Show each team for 2 seconds
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get top teams: ${error.message}")
                displayError("Network error")
            }
        )
    }
    
    private suspend fun displayDivisionStandings() {
        Log.d(TAG, "Displaying division standings")
        
        val favoriteTeam = repository.getFavoriteTeam()
        val result = repository.getDivisionStandings(favoriteTeam)
        
        result.fold(
            onSuccess = { divisionTeams ->
                // Cycle through division teams
                for ((index, team) in divisionTeams.withIndex()) {
                    val abbrev = GlyphMatrixUtils.getTeamAbbreviation(team.team.name)
                    val rank = team.divisionRank.toIntOrNull() ?: (index + 1)
                    val bitmap = GlyphMatrixUtils.createRankingBitmap(
                        rank, 
                        abbrev, 
                        this@MLBStandingsGlyphToyService
                    )
                    displayBitmap(bitmap)
                    delay(2000) // Show each team for 2 seconds
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to get division standings: ${error.message}")
                displayError("Network error")
            }
        )
    }
    
    private fun displayBitmap(bitmap: android.graphics.Bitmap) {
        try {
            // Convert bitmap to Glyph Matrix format and display
            val frameBuilder = GlyphMatrixFrame.Builder()
            val matrixObject = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setBrightness(255)
                .build()
            
            val frame = frameBuilder.addTop(matrixObject).build(this)
            glyphMatrixManager?.setMatrixFrame(frame.render())
            
            Log.d(TAG, "Displaying bitmap on real Glyph Matrix")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display bitmap: ${e.message}")
        }
    }
    
    private fun displayError(message: String = "Error") {
        val bitmap = GlyphMatrixUtils.createTextBitmap("ERR", this)
        displayBitmap(bitmap)
        Log.e(TAG, "Displaying error: $message")
    }
    
    private fun displayLoading() {
        serviceScope.launch {
            repeat(10) { frame ->
                if (!isLoading) return@repeat
                val bitmap = GlyphMatrixUtils.createLoadingBitmap(frame, this@MLBStandingsGlyphToyService)
                displayBitmap(bitmap)
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
