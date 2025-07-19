package com.dreampipe.mlbstandings.glyph

import android.content.Context
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject

object GlyphMatrixUtils {
    
    const val MATRIX_SIZE = 25
    
    /**
     * Creates a frame displaying wins and losses using native Glyph SDK methods
     */
    fun createWinLossFrame(wins: Int, losses: Int, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        // Create separate text objects at different positions
        // Shift right and up to center better in circular display
        val winsObject = GlyphMatrixObject.Builder()
            .setText("W:$wins")
            .setPosition(3, 6)  // Moved from (2, 8) - more right and up
            .build()
        
        val lossesObject = GlyphMatrixObject.Builder()
            .setText("L:$losses")
            .setPosition(3, 14)  // Moved from (2, 16) - more right and up
            .build()
        
        return frameBuilder
            .addTop(winsObject)
            .addMid(lossesObject)
            .build(context)
    }
    
    /**
     * Creates a frame displaying just the favorite team abbreviation
     */
    fun createFavoriteTeamFrame(teamAbbrev: String, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        val teamObject = GlyphMatrixObject.Builder()
            .setText(teamAbbrev)
            .setPosition(8, 12)  // Center position for team abbreviation
            .build()
        
        return frameBuilder
            .addTop(teamObject)
            .build(context)
    }
    
    /**
     * Creates a frame displaying top teams rankings using native SDK
     */
    fun createRankingsFrame(teams: List<String>, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        val maxTeams = minOf(teams.size, 3) // Show top 3 teams
        
        // Create separate text objects for each team at different vertical positions
        for (i in 0 until maxTeams) {
            val displayText = "${i + 1}.${teams[i]}"
            val yPosition = 5 + (i * 6) // Spacing: 5, 11, 17
            
            val teamObject = GlyphMatrixObject.Builder()
                .setText(displayText)
                .setPosition(2, yPosition)
                .build()
            
            // Add to different layers to avoid conflicts
            when (i) {
                0 -> frameBuilder.addTop(teamObject)
                1 -> frameBuilder.addMid(teamObject)
                2 -> frameBuilder.addLow(teamObject)
            }
        }
        
        return frameBuilder.build(context)
    }
    
    /**
     * Creates a frame for division standings using proper text formatting
     * Shows the 3 teams around the favorite team's position
     */
    fun createDivisionFrame(division: String, teams: List<Pair<String, String>>, favoriteTeamAbbrev: String, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        // Find the favorite team's position
        val favoriteIndex = teams.indexOfFirst { it.first == favoriteTeamAbbrev }
        
        // Get 3 teams around the favorite team's position
        val teamsToShow = when {
            favoriteIndex <= 0 -> teams.take(3) // Show top 3 if favorite is 1st or not found
            favoriteIndex >= teams.size - 1 -> teams.takeLast(3) // Show bottom 3 if favorite is last
            else -> teams.subList(favoriteIndex - 1, minOf(favoriteIndex + 2, teams.size)) // Show team above, favorite, and team below
        }
        
        // Create separate team entries at different positions
        for (i in teamsToShow.indices) {
            val (teamAbbrev, record) = teamsToShow[i]
            val actualRank = teams.indexOfFirst { it.first == teamAbbrev } + 1
            val displayText = "$actualRank.$teamAbbrev"
            val yPosition = 6 + (i * 5) // Position below title: 6, 11, 16
            
            val teamObject = GlyphMatrixObject.Builder()
                .setText(displayText)
                .setPosition(1, yPosition)
                .build()
            
            // Add to different layers
            when (i) {
                0 -> frameBuilder.addMid(teamObject)
                1 -> frameBuilder.addLow(teamObject)
                2 -> frameBuilder.addTop(teamObject) // Won't conflict with title due to different position
            }
        }
        
        return frameBuilder.build(context)
    }

    /**
     * Creates a frame for division standings - simplified to just use raw array for now
     */
    fun createDivisionArray(division: String, teams: List<Pair<String, String>>): IntArray {
        // For now, create a simple pattern to indicate division mode
        // This could be enhanced later with proper text rendering
        val array = IntArray(MATRIX_SIZE * MATRIX_SIZE) { 0 }
        
        // Create a simple border pattern
        for (i in 0 until MATRIX_SIZE) {
            array[i] = 1000 // Top border
            array[(MATRIX_SIZE - 1) * MATRIX_SIZE + i] = 1000 // Bottom border
            array[i * MATRIX_SIZE] = 1000 // Left border  
            array[i * MATRIX_SIZE + (MATRIX_SIZE - 1)] = 1000 // Right border
        }
        
        // Add some dots to indicate content
        for (i in 1..4) {
            val row = 5 + (i * 3)
            val col = 5
            if (row < MATRIX_SIZE && col < MATRIX_SIZE) {
                array[row * MATRIX_SIZE + col] = 2000
                array[row * MATRIX_SIZE + col + 2] = 1500
            }
        }
        
        return array
    }
    
    /**
     * Creates a simple loading animation array
     */
    fun createLoadingArray(frame: Int): IntArray {
        val array = IntArray(MATRIX_SIZE * MATRIX_SIZE) { 0 }
        
        // Create a spinning dot pattern
        val centerX = MATRIX_SIZE / 2
        val centerY = MATRIX_SIZE / 2
        val radius = 8
        val angle = (frame * 30) % 360
        
        // Calculate dot positions
        val radians = Math.toRadians(angle.toDouble())
        val x = (centerX + radius * Math.cos(radians)).toInt()
        val y = (centerY + radius * Math.sin(radians)).toInt()
        
        // Draw loading indicator
        if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
            array[y * MATRIX_SIZE + x] = 4095 // Max brightness
            // Add surrounding dots for better visibility
            if (x > 0) array[y * MATRIX_SIZE + (x - 1)] = 2000
            if (x < MATRIX_SIZE - 1) array[y * MATRIX_SIZE + (x + 1)] = 2000
            if (y > 0) array[(y - 1) * MATRIX_SIZE + x] = 2000
            if (y < MATRIX_SIZE - 1) array[(y + 1) * MATRIX_SIZE + x] = 2000
        }
        
        return array
    }
    
    /**
     * Creates an error display frame
     */
    fun createErrorFrame(message: String, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        val errorObject = GlyphMatrixObject.Builder()
            .setText("ERR:$message")
            .setPosition(2, 12) // Center position
            .build()
        
        return frameBuilder
            .addTop(errorObject)
            .build(context)
    }
    
    /**
     * Converts team name to abbreviation
     */
    fun getTeamAbbreviation(teamName: String): String {
        return when {
            teamName.contains("Yankees") -> "NYY"
            teamName.contains("Blue Jays") -> "TOR"
            teamName.contains("Rays") -> "TB"
            teamName.contains("Red Sox") -> "BOS"
            teamName.contains("Orioles") -> "BAL"
            teamName.contains("Tigers") -> "DET"
            teamName.contains("Guardians") -> "CLE"
            teamName.contains("Twins") -> "MIN"
            teamName.contains("Royals") -> "KC"
            teamName.contains("White Sox") -> "CWS"
            teamName.contains("Astros") -> "HOU"
            teamName.contains("Mariners") -> "SEA"
            teamName.contains("Angels") -> "LAA"
            teamName.contains("Rangers") -> "TEX"
            teamName.contains("Athletics") -> "OAK"
            teamName.contains("Phillies") -> "PHI"
            teamName.contains("Mets") -> "NYM"
            teamName.contains("Braves") -> "ATL"
            teamName.contains("Marlins") -> "MIA"
            teamName.contains("Nationals") -> "WSH"
            teamName.contains("Cubs") -> "CHC"
            teamName.contains("Brewers") -> "MIL"
            teamName.contains("Cardinals") -> "STL"
            teamName.contains("Reds") -> "CIN"
            teamName.contains("Pirates") -> "PIT"
            teamName.contains("Dodgers") -> "LAD"
            teamName.contains("Padres") -> "SD"
            teamName.contains("Giants") -> "SF"
            teamName.contains("Diamondbacks") -> "ARI"
            teamName.contains("Rockies") -> "COL"
            else -> teamName.take(3).uppercase()
        }
    }
}
