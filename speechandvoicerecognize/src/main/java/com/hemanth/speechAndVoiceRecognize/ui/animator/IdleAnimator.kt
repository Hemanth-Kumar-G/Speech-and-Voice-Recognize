package com.hemanth.speechAndVoiceRecognize.ui.animator

import com.hemanth.speechAndVoiceRecognize.ui.SpeechBar
import kotlin.math.sin

class IdleAnimator(private val bars: List<SpeechBar>, private val floatingAmplitude: Int) :
    BarParamsAnimator {
    private var startTimestamp: Long = 0
    private var isPlaying = false
    override fun start() {
        isPlaying = true
        startTimestamp = System.currentTimeMillis()
    }

    override fun stop() {
        isPlaying = false
    }

    override fun animate() {
        if (isPlaying) {
            update(bars)
        }
    }

    fun update(bars: List<SpeechBar>) {
        val currTimestamp = System.currentTimeMillis()
        if (currTimestamp - startTimestamp > IDLE_DURATION) {
            startTimestamp += IDLE_DURATION
        }
        val delta = currTimestamp - startTimestamp
        for ((i, bar) in bars.withIndex()) {
            updateCirclePosition(bar, delta, i)
        }
    }

    private fun updateCirclePosition(bar: SpeechBar, delta: Long, num: Int) {
        val angle = delta.toFloat() / IDLE_DURATION * 360f + 120f * num
        val y =
            (sin(Math.toRadians(angle.toDouble())) * floatingAmplitude).toInt() + bar.startY
        bar.y = y
        bar.update()
    }

    companion object {
        private const val IDLE_DURATION: Long = 1500
    }
}