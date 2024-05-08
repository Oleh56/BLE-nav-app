package com.example.BLE_nav_app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class IndoorMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val beaconPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val userPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    // Room dimensions
    private val roomLength = 5.53f // meters
    private val roomWidth = 3f     // meters

    // IndoorMapView dimensions in pixels
    private val mapViewWidthPx = 900f // pixels
    private val mapViewHeightPx = 1080f // pixels

    // Calculate scale factors
    private val widthScaleFactor = mapViewWidthPx / roomLength
    private val heightScaleFactor = mapViewHeightPx / roomWidth

    val beaconWidth = 5.13f // meters (40 cm)
    val beaconLength = 2.3f // meters (70 cm)

    val mappedBeaconX = beaconWidth * widthScaleFactor
    val mappedBeaconY = beaconLength * heightScaleFactor


    private val beaconPositions = mutableListOf<Pair<Float, Float>>()
    private var userPosition: Pair<Float, Float>? = null

    fun setBeaconPositions(positions: List<Pair<Float, Float>>) {
        beaconPositions.clear()
        beaconPositions.addAll(positions)
        invalidate()
    }

    fun setUserPosition(position: Pair<Float, Float>) {
        userPosition = position
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw beacons
        for (position in beaconPositions) {
            canvas.drawCircle(position.first, position.second, 40f, beaconPaint)
        }

        // Draw user position
        userPosition?.let { position ->
            canvas.drawCircle(position.first, position.second, 40f, userPaint)
        }
    }
}