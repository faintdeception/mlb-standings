package com.dreampipe.mlbstandings.debug

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.dreampipe.mlbstandings.R
import com.dreampipe.mlbstandings.glyph.GlyphMatrixUtils

/**
 * Debug activity to simulate what the Glyph Matrix would look like
 * Shows a 25x25 grid representing the Nothing Phone Glyph display
 */
class GlyphSimulatorActivity : AppCompatActivity() {
    
    private lateinit var gridLayout: GridLayout
    private val pixelViews = Array(25) { Array(25) { ImageView(this) } }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glyph_simulator)
        
        setupGrid()
        setupButtons()
        
        // Test with some sample data
        showSampleDisplay()
    }
    
    private fun setupButtons() {
        findViewById<android.widget.Button>(R.id.btn_test_text).setOnClickListener {
            displayArray(GlyphMatrixUtils.createLoadingArray(0))
        }
        
        findViewById<android.widget.Button>(R.id.btn_test_wins).setOnClickListener {
            testWinLossDisplay(95, 67)
        }
        
        findViewById<android.widget.Button>(R.id.btn_test_rankings).setOnClickListener {
            displayArray(GlyphMatrixUtils.createDivisionArray("AL East", emptyList()))
        }
    }
    
    private fun setupGrid() {
        gridLayout = findViewById(R.id.glyph_grid)
        gridLayout.rowCount = 25
        gridLayout.columnCount = 25
        
        // Create 25x25 grid of ImageViews
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val imageView = ImageView(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 8
                        height = 8
                        setMargins(1, 1, 1, 1)
                    }
                    setBackgroundColor(Color.BLACK)
                }
                pixelViews[row][col] = imageView
                gridLayout.addView(imageView)
            }
        }
    }
    
    private fun showSampleDisplay() {
        // Create a sample array using the loading animation
        displayArray(GlyphMatrixUtils.createLoadingArray(0))
    }
    
    private fun displayArray(array: IntArray) {
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val brightness = array[row * 25 + col]
                
                // Set pixel color based on brightness (simulate LED on/off)
                val color = if (brightness > 500) Color.WHITE else Color.BLACK
                pixelViews[row][col].setBackgroundColor(color)
            }
        }
    }
    
    /**
     * Call this method to test different displays
     */
    fun testWinLossDisplay(wins: Int, losses: Int) {
        displayArray(GlyphMatrixUtils.createLoadingArray(wins % 10))
    }
}
