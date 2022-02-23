package com.hemanth.speechAndVoiceRecognize.ui.animator

import android.graphics.Point
import com.hemanth.speechAndVoiceRecognize.ui.SpeechBar
import com.hemanth.speechAndVoiceRecognize.ui.SpeechProgressView

class TransformAnimator(
    private val bars: List<SpeechBar>,
    private val centerX: Int,
    private val centerY: Int,
    private val radius: Int
) : BarParamsAnimator {
    private var startTimestamp: Long = 0
    private var isPlaying = false
    private var listener: OnInterpolationFinishedListener? = null
    private val finalPositions: MutableList<Point> = ArrayList()
    override fun start() {
        isPlaying = true
        startTimestamp = System.currentTimeMillis()
        initFinalPositions()
    }

    override fun stop() {
        isPlaying = false
        listener?.onFinished()
    }

    override fun animate() {
        if (!isPlaying) return
        val currTimestamp = System.currentTimeMillis()
        var delta = currTimestamp - startTimestamp
        if (delta > DURATION) {
            delta = DURATION
        }
        for (i in bars.indices) {
            val bar = bars[i]
            val x =
                bar.startX + ((finalPositions[i].x - bar.startX) * (delta.toFloat() / DURATION)).toInt()
            val y =
                bar.startY + ((finalPositions[i].y - bar.startY) * (delta.toFloat() / DURATION)).toInt()
            bar.x = x
            bar.y = y
            bar.update()
        }
        if (delta == DURATION) {
            stop()
        }
    }

    private fun initFinalPositions() {
        val startPoint = Point()
        startPoint.x = centerX
        startPoint.y = centerY - radius
        for (i in 0 until SpeechProgressView.BARS_COUNT) {
            val point = Point(startPoint)
            rotate(360.0 / SpeechProgressView.BARS_COUNT * i, point)
            finalPositions.add(point)
        }
    }

    /**
     * X = x0 + (x - x0) * cos(a) - (y - y0) * sin(a);
     * Y = y0 + (y - y0) * cos(a) + (x - x0) * sin(a);
     */
    private fun rotate(degrees: Double, point: Point) {
        val angle = Math.toRadians(degrees)
        val x = centerX + ((point.x - centerX) * Math.cos(angle) -
                (point.y - centerY) * Math.sin(angle)).toInt()
        val y = centerY + ((point.x - centerX) * Math.sin(angle) +
                (point.y - centerY) * Math.cos(angle)).toInt()
        point.x = x
        point.y = y
    }

    fun setOnInterpolationFinishedListener(listener: OnInterpolationFinishedListener?) {
        this.listener = listener
    }

    interface OnInterpolationFinishedListener {
        fun onFinished()
    }

    companion object {
        private const val DURATION: Long = 300
    }
}