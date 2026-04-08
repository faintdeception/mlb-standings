package com.dreampipe.mlbstandings.glyph

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.dreampipe.mlbstandings.data.repository.MLBStandingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Glyph Matrix SDK imports
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphToy
import com.nothing.ketchum.Glyph

class MLBStandingsGlyphToyService : Service() {
    
    private var glyphMatrixManager: GlyphMatrixManager? = null
    private lateinit var repository: MLBStandingsRepository
    private var serviceJob: Job? = null
    private lateinit var serviceScope: CoroutineScope
    
    private var currentDisplayMode = DisplayMode.FAVORITE_TEAM
    private var isLoading = false
    
    enum class DisplayMode {
        FAVORITE_TEAM,  // Shows favorite team abbreviation (first screen)
        TEAM_RECORD,    // Shows favorite team's wins/losses
        DIVISION,       // Shows division standings
        TOP_TEAMS       // Shows top 3 teams in MLB
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
        ensureServiceScope()
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
            ensureServiceScope()

            // Initialize Glyph Matrix Manager using getInstance pattern
            glyphMatrixManager = GlyphMatrixManager.getInstance(applicationContext)
            glyphMatrixManager?.init(callback)
            
            Log.d(TAG, "Glyph Matrix initialization requested")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Glyph Matrix: ${e.message}")
        }
    }
    
    private fun cleanup() {
        try {
            serviceJob?.cancel()
            serviceJob = null
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
        
        val result = repository.getTopTeams(3)
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
            onSuccess = { divisionStandings ->
                if (divisionStandings != null && divisionStandings.teamRecords.isNotEmpty()) {
                    val divisionName = formatDivisionName(divisionStandings.divisionName)
                    
                    // Prepare team abbreviations in division rank order.
                    val divisionTeams = divisionStandings.teamRecords.map { team ->
                        GlyphMatrixUtils.getTeamAbbreviation(team.team.name)
                    }
                    
                    // Get favorite team abbreviation
                    val favoriteTeamAbbrev = GlyphMatrixUtils.getTeamAbbreviation(favoriteTeam)
                    
                    val frame = GlyphMatrixUtils.createDivisionFrame(
                        divisionTeams,
                        favoriteTeamAbbrev,
                        this@MLBStandingsGlyphToyService
                    )
                    displayFrame(frame)
                    Log.d(TAG, "Division $divisionName teams: $divisionTeams")
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

    private fun ensureServiceScope() {
        if (!::serviceScope.isInitialized || serviceJob?.isActive != true) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.Main + serviceJob!!)
        }
    }

    private fun formatDivisionName(divisionName: String): String {
        val normalized = divisionName.lowercase()
        return when {
            normalized.contains("american") && normalized.contains("east") -> "AL E"
            normalized.contains("american") && normalized.contains("central") -> "AL C"
            normalized.contains("american") && normalized.contains("west") -> "AL W"
            normalized.contains("national") && normalized.contains("east") -> "NL E"
            normalized.contains("national") && normalized.contains("central") -> "NL C"
            normalized.contains("national") && normalized.contains("west") -> "NL W"
            else -> divisionName.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.take(1).uppercase() }
                .take(4)
        }
    }
    
    // Glyph Matrix Manager callback
    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: android.content.ComponentName?) {
            val targetDevice = resolveTargetDevice()
            glyphMatrixManager?.register(targetDevice)
            Log.d(TAG, "Glyph Matrix registered for target=$targetDevice matrix=${GlyphMatrixUtils.getMatrixSize()}")
            displayCurrentMode()
            Log.d(TAG, "Glyph Matrix service connected: $name")
        }
        
        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            Log.d(TAG, "Glyph Matrix service disconnected: $name")
        }
    }
    
    companion object {
        private const val TAG = "MLBStandingsGlyphToy"
    }

    private fun resolveTargetDevice(): String {
        return when {
            Common.is25111p() -> Glyph.DEVICE_25111p
            Common.is23112() -> Glyph.DEVICE_23112
            else -> Glyph.DEVICE_23112
        }
    }
}
