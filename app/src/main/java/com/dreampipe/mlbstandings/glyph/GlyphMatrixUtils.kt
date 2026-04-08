package com.dreampipe.mlbstandings.glyph

import android.content.Context
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import kotlin.math.roundToInt

object GlyphMatrixUtils {
    
    private const val DEFAULT_MATRIX_SIZE = 25
    private const val COMPACT_MATRIX_SIZE = 13

    fun getMatrixSize(): Int {
        val reportedSize = Common.getDeviceMatrixLength()
        return reportedSize.takeIf { it > 0 } ?: DEFAULT_MATRIX_SIZE
    }

    private fun isCompactMatrix(): Boolean = getMatrixSize() <= COMPACT_MATRIX_SIZE

    private fun scaleCoordinate(value: Int): Int {
        val matrixSize = getMatrixSize()
        if (matrixSize == DEFAULT_MATRIX_SIZE) {
            return value
        }

        val scaled = (value.toFloat() / DEFAULT_MATRIX_SIZE) * matrixSize
        return scaled.roundToInt().coerceIn(0, matrixSize - 1)
    }

    private fun compactCenterX(text: String): Int {
        val matrixSize = getMatrixSize()
        val estimatedWidth = (text.length * 2).coerceAtLeast(1)
        return ((matrixSize - estimatedWidth) / 2).coerceAtLeast(0)
    }
    
    /**
     * Creates a frame displaying wins and losses using native Glyph SDK methods
     */
    fun createWinLossFrame(wins: Int, losses: Int, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()

        if (isCompactMatrix()) {
            val winsText = "W$wins"
            val lossesText = "L$losses"

            val winsObject = GlyphMatrixObject.Builder()
                .setText(winsText)
                .setPosition(compactCenterX(winsText), scaleCoordinate(5))
                .build()

            val lossesObject = GlyphMatrixObject.Builder()
                .setText(lossesText)
                .setPosition(compactCenterX(lossesText), scaleCoordinate(14))
                .build()

            return frameBuilder
                .addTop(winsObject)
                .addMid(lossesObject)
                .build(context)
        }
        
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
        val xPosition = if (isCompactMatrix()) compactCenterX(teamAbbrev) else 8
        val yPosition = if (isCompactMatrix()) scaleCoordinate(12) else 12
        
        val teamObject = GlyphMatrixObject.Builder()
            .setText(teamAbbrev)
            .setPosition(xPosition, yPosition)
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

        if (isCompactMatrix()) {
            val leader = teams.firstOrNull()?.let { "1$it" } ?: "N/A"
            val leaderObject = GlyphMatrixObject.Builder()
                .setText(leader)
                .setPosition(compactCenterX(leader), scaleCoordinate(12))
                .build()

            return frameBuilder
                .addTop(leaderObject)
                .build(context)
        }
        
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
     * Creates a frame for division standings using proper text formatting.
     * Shows the 3 division teams around the favorite team's position.
     */
    fun createDivisionFrame(teams: List<String>, favoriteTeamAbbrev: String, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        
        // Find the favorite team's position
        val favoriteIndex = teams.indexOfFirst { it == favoriteTeamAbbrev }

        if (isCompactMatrix()) {
            val compactText = if (favoriteIndex >= 0) {
                "${favoriteIndex + 1}$favoriteTeamAbbrev"
            } else {
                favoriteTeamAbbrev
            }

            val compactObject = GlyphMatrixObject.Builder()
                .setText(compactText)
                .setPosition(compactCenterX(compactText), scaleCoordinate(12))
                .build()

            return frameBuilder
                .addTop(compactObject)
                .build(context)
        }
        
        // Get 3 teams around the favorite team's position
        val teamsToShow = when {
            favoriteIndex <= 0 -> teams.take(3) // Show top 3 if favorite is 1st or not found
            favoriteIndex >= teams.size - 1 -> teams.takeLast(3) // Show bottom 3 if favorite is last
            else -> teams.subList(favoriteIndex - 1, minOf(favoriteIndex + 2, teams.size)) // Show team above, favorite, and team below
        }
        
        // Create separate team entries at different positions
        for (i in teamsToShow.indices) {
            val teamAbbrev = teamsToShow[i]
            val actualRank = teams.indexOfFirst { it == teamAbbrev } + 1
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
     * Creates a simple debug-only placeholder array for division mode previews.
     */
    @Suppress("UNUSED_PARAMETER")
    fun createDivisionArray(division: String, teams: List<Pair<String, String>>): IntArray {
        // For now, create a simple pattern to indicate division mode
        // This could be enhanced later with proper text rendering
        val matrixSize = getMatrixSize()
        val array = IntArray(matrixSize * matrixSize) { 0 }
        
        // Create a simple border pattern
        for (i in 0 until matrixSize) {
            array[i] = 1000 // Top border
            array[(matrixSize - 1) * matrixSize + i] = 1000 // Bottom border
            array[i * matrixSize] = 1000 // Left border
            array[i * matrixSize + (matrixSize - 1)] = 1000 // Right border
        }
        
        // Add some dots to indicate content
        for (i in 1..4) {
            val row = scaleCoordinate(5 + (i * 3))
            val col = scaleCoordinate(5)
            if (row < matrixSize && col < matrixSize) {
                array[row * matrixSize + col] = 2000
                if (col + 2 < matrixSize) {
                    array[row * matrixSize + col + 2] = 1500
                }
            }
        }
        
        return array
    }
    
    /**
     * Creates a simple loading animation array
     */
    fun createLoadingArray(frame: Int): IntArray {
        val matrixSize = getMatrixSize()
        val array = IntArray(matrixSize * matrixSize) { 0 }
        
        // Create a spinning dot pattern
        val centerX = matrixSize / 2
        val centerY = matrixSize / 2
        val radius = (matrixSize / 3).coerceAtLeast(2)
        val angle = (frame * 30) % 360
        
        // Calculate dot positions
        val radians = Math.toRadians(angle.toDouble())
        val x = (centerX + radius * Math.cos(radians)).toInt()
        val y = (centerY + radius * Math.sin(radians)).toInt()
        
        // Draw loading indicator
        if (x in 0 until matrixSize && y in 0 until matrixSize) {
            array[y * matrixSize + x] = 4095 // Max brightness
            // Add surrounding dots for better visibility
            if (x > 0) array[y * matrixSize + (x - 1)] = 2000
            if (x < matrixSize - 1) array[y * matrixSize + (x + 1)] = 2000
            if (y > 0) array[(y - 1) * matrixSize + x] = 2000
            if (y < matrixSize - 1) array[(y + 1) * matrixSize + x] = 2000
        }
        
        return array
    }
    
    /**
     * Creates an error display frame
     */
    fun createErrorFrame(message: String, context: Context): GlyphMatrixFrame {
        val frameBuilder = GlyphMatrixFrame.Builder()
        val displayMessage = if (isCompactMatrix()) message.take(3) else "!$message"
        val xPosition = if (isCompactMatrix()) compactCenterX(displayMessage) else 2
        val yPosition = if (isCompactMatrix()) scaleCoordinate(12) else 12
        
        val errorObject = GlyphMatrixObject.Builder()
            .setText(displayMessage)
            .setPosition(xPosition, yPosition)
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
