package com.dreampipe.mlbstandings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dreampipe.mlbstandings.data.repository.MLBStandingsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var repository: MLBStandingsRepository
    private lateinit var teamSpinner: Spinner
    private lateinit var lastUpdateText: TextView
    private lateinit var refreshButton: Button
    private lateinit var testButton: Button
    private lateinit var activateToyButton: Button
    private lateinit var mainContent: LinearLayout
    
    private val mlbTeams = arrayOf(
        "Arizona Diamondbacks",
        "Atlanta Braves", 
        "Baltimore Orioles",
        "Boston Red Sox",
        "Chicago Cubs",
        "Chicago White Sox",
        "Cincinnati Reds",
        "Cleveland Guardians",
        "Colorado Rockies",
        "Detroit Tigers",
        "Houston Astros",
        "Kansas City Royals",
        "Los Angeles Angels",
        "Los Angeles Dodgers",
        "Miami Marlins",
        "Milwaukee Brewers",
        "Minnesota Twins",
        "New York Mets",
        "New York Yankees",
        "Athletics",
        "Philadelphia Phillies",
        "Pittsburgh Pirates",
        "San Diego Padres",
        "San Francisco Giants",
        "Seattle Mariners",
        "St. Louis Cardinals",
        "Tampa Bay Rays",
        "Texas Rangers",
        "Toronto Blue Jays",
        "Washington Nationals"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        
        repository = MLBStandingsRepository(this)
        
        initializeViews()
        setupWindowInsets()
        setupTeamSpinner()
        setupClickListeners()
        
        // Load current settings
        loadCurrentSettings()
    }
    
    private fun initializeViews() {
        mainContent = findViewById(R.id.mainContent)
        teamSpinner = findViewById(R.id.teamSpinner)
        lastUpdateText = findViewById(R.id.lastUpdateText)
        refreshButton = findViewById(R.id.refreshButton)
        testButton = findViewById(R.id.testButton)
        activateToyButton = findViewById(R.id.activateToyButton)
        activateToyButton.isEnabled = canOpenGlyphToysManager()
    }

    private fun setupWindowInsets() {
        val baseHorizontalPadding = 24.dpToPx()
        val baseTopPadding = 24.dpToPx()
        val baseBottomPadding = 24.dpToPx()

        ViewCompat.setOnApplyWindowInsetsListener(mainContent) { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutoutInsets = insets.displayCutout

            val leftInset = maxOf(systemBarInsets.left, displayCutoutInsets?.safeInsetLeft ?: 0)
            val topInset = maxOf(systemBarInsets.top, displayCutoutInsets?.safeInsetTop ?: 0)
            val rightInset = maxOf(systemBarInsets.right, displayCutoutInsets?.safeInsetRight ?: 0)
            val bottomInset = maxOf(systemBarInsets.bottom, displayCutoutInsets?.safeInsetBottom ?: 0)

            view.setPadding(
                baseHorizontalPadding + leftInset,
                baseTopPadding + topInset,
                baseHorizontalPadding + rightInset,
                baseBottomPadding + bottomInset
            )

            insets
        }

        ViewCompat.requestApplyInsets(mainContent)
    }
    
    private fun setupTeamSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mlbTeams)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        teamSpinner.adapter = adapter
    }
    
    private fun setupClickListeners() {
        refreshButton.setOnClickListener {
            refreshData()
        }
        
        testButton.setOnClickListener {
            testGlyphDisplay()
        }

        activateToyButton.setOnClickListener {
            openGlyphToysManager()
        }
        
        // Save team selection when changed
        teamSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedTeam = mlbTeams[position]
                lifecycleScope.launch {
                    repository.setFavoriteTeam(selectedTeam)
                    Log.d(TAG, "Favorite team set to: $selectedTeam")
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }
    
    private fun loadCurrentSettings() {
        lifecycleScope.launch {
            try {
                // Load favorite team
                val favoriteTeam = repository.getFavoriteTeam()
                val teamIndex = mlbTeams.indexOf(favoriteTeam)
                if (teamIndex >= 0) {
                    teamSpinner.setSelection(teamIndex)
                }
                
                // Show last update time
                updateLastUpdateText()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading settings: ${e.message}")
            }
        }
    }
    
    private fun refreshData() {
        lifecycleScope.launch {
            try {
                refreshButton.isEnabled = false
                refreshButton.text = getString(R.string.refreshing)
                
                val result = repository.getStandings(forceRefresh = true)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully refreshed standings data")
                        updateLastUpdateText()
                        showToast("Data refreshed successfully!")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to refresh data: ${error.message}")
                        showToast("Failed to refresh data: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing data: ${e.message}")
                showToast("Error: ${e.message}")
            } finally {
                refreshButton.isEnabled = true
                refreshButton.text = getString(R.string.refresh_data)
            }
        }
    }
    
    private fun testGlyphDisplay() {
        lifecycleScope.launch {
            try {
                testButton.isEnabled = false
                testButton.text = getString(R.string.testing)
                
                // Test favorite team display
                val result = repository.getFavoriteTeamRecord()
                result.fold(
                    onSuccess = { teamRecord ->
                        if (teamRecord != null) {
                            Log.d(TAG, "Test: ${teamRecord.team.name} - ${teamRecord.wins}W ${teamRecord.losses}L")
                            showToast("Test successful! Check Glyph Matrix.")
                        } else {
                            showToast("Team not found in standings")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Test failed: ${error.message}")
                        showToast("Test failed: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Test error: ${e.message}")
                showToast("Test error: ${e.message}")
            } finally {
                testButton.isEnabled = true
                testButton.text = getString(R.string.test_glyph_display)
            }
        }
    }
    
    private fun updateLastUpdateText() {
        // This would show when data was last updated
        val currentTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        lastUpdateText.text = "Last checked: $currentTime"
    }

    private fun openGlyphToysManager() {
        if (!canOpenGlyphToysManager()) {
            showToast(getString(R.string.activate_toy_unavailable))
            return
        }

        try {
            startActivity(createGlyphToysManagerIntent())
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "Glyph Toys manager not available: ${error.message}")
            showToast(getString(R.string.activate_toy_unavailable))
        }
    }

    private fun canOpenGlyphToysManager(): Boolean {
        return createGlyphToysManagerIntent().resolveActivity(packageManager) != null
    }

    private fun createGlyphToysManagerIntent(): Intent {
        return Intent().apply {
            component = ComponentName(
                GLYPH_TOYS_MANAGER_PACKAGE,
                GLYPH_TOYS_MANAGER_ACTIVITY
            )
        }
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    companion object {
        private const val TAG = "MainActivity"
        private const val GLYPH_TOYS_MANAGER_PACKAGE = "com.nothing.thirdparty"
        private const val GLYPH_TOYS_MANAGER_ACTIVITY =
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
    }
}
