package com.dreampipe.mlbstandings

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        setContentView(R.layout.activity_main)
        
        repository = MLBStandingsRepository(this)
        
        initializeViews()
        setupTeamSpinner()
        setupClickListeners()
        
        // Load current settings
        loadCurrentSettings()
    }
    
    private fun initializeViews() {
        teamSpinner = findViewById(R.id.teamSpinner)
        lastUpdateText = findViewById(R.id.lastUpdateText)
        refreshButton = findViewById(R.id.refreshButton)
        testButton = findViewById(R.id.testButton)
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
                refreshButton.text = "Refreshing..."
                
                val result = repository.getStandings()
                result.fold(
                    onSuccess = { standings ->
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
                refreshButton.text = "Refresh Data"
            }
        }
    }
    
    private fun testGlyphDisplay() {
        lifecycleScope.launch {
            try {
                testButton.isEnabled = false
                testButton.text = "Testing..."
                
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
                testButton.text = "Test Glyph Display"
            }
        }
    }
    
    private fun updateLastUpdateText() {
        // This would show when data was last updated
        val currentTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        lastUpdateText.text = "Last checked: $currentTime"
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}
