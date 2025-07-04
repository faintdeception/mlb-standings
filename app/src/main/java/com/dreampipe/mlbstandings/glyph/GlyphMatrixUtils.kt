package com.dreampipe.mlbstandings.glyph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object GlyphMatrixUtils {
    
    const val MATRIX_SIZE = 25
    
    /**
     * Creates a simple text bitmap for display on the Glyph Matrix
     */
    fun createTextBitmap(text: String, context: Context): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = false
            textAlign = Paint.Align.CENTER
        }
        
        // Clear the canvas
        canvas.drawColor(Color.BLACK)
        
        // Draw text in center
        val centerX = MATRIX_SIZE / 2f
        val centerY = MATRIX_SIZE / 2f + paint.textSize / 3f
        
        canvas.drawText(text, centerX, centerY, paint)
        
        return bitmap
    }
    
    /**
     * Creates a wins-losses display bitmap
     */
    fun createWinLossBitmap(wins: Int, losses: Int, context: Context): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 6f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = false
            textAlign = Paint.Align.CENTER
        }
        
        // Clear the canvas
        canvas.drawColor(Color.BLACK)
        
        // Draw wins on top
        canvas.drawText("W", MATRIX_SIZE / 2f, 8f, paint)
        canvas.drawText(wins.toString(), MATRIX_SIZE / 2f, 15f, paint)
        
        // Draw losses on bottom
        canvas.drawText("L", MATRIX_SIZE / 2f, 18f, paint)
        canvas.drawText(losses.toString(), MATRIX_SIZE / 2f, 25f, paint)
        
        return bitmap
    }
    
    /**
     * Creates a ranking display bitmap
     */
    fun createRankingBitmap(rank: Int, teamAbbrev: String, context: Context): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 6f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = false
            textAlign = Paint.Align.CENTER
        }
        
        // Clear the canvas
        canvas.drawColor(Color.BLACK)
        
        // Draw rank
        canvas.drawText("#$rank", MATRIX_SIZE / 2f, 10f, paint)
        
        // Draw team abbreviation
        paint.textSize = 5f
        canvas.drawText(teamAbbrev, MATRIX_SIZE / 2f, 20f, paint)
        
        return bitmap
    }
    
    /**
     * Creates a simple loading animation bitmap
     */
    fun createLoadingBitmap(frame: Int, context: Context): Bitmap {
        val bitmap = Bitmap.createBitmap(MATRIX_SIZE, MATRIX_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        
        // Clear the canvas
        canvas.drawColor(Color.BLACK)
        
        // Simple spinning dot animation
        val centerX = MATRIX_SIZE / 2f
        val centerY = MATRIX_SIZE / 2f
        val radius = 8f
        val angle = (frame * 30) % 360 // 30 degrees per frame
        
        val x = centerX + radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat()
        val y = centerY + radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
        
        canvas.drawCircle(x, y, 2f, paint)
        
        return bitmap
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
