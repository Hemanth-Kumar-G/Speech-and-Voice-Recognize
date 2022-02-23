package com.hemanth.speechAndVoiceRecognize.ui.animator

import com.hemanth.speechAndVoiceRecognize.ui.SpeechBar

class RmsAnimator(speechBars: List<SpeechBar?>) : BarParamsAnimator {
    private val barAnimators: MutableList<BarRmsAnimator>
    override fun start() {
        for (barAnimator in barAnimators) {
            barAnimator.start()
        }
    }

    override fun stop() {
        for (barAnimator in barAnimators) {
            barAnimator.stop()
        }
    }

    override fun animate() {
        for (barAnimator in barAnimators) {
            barAnimator.animate()
        }
    }

    fun onRmsChanged(rmsDB: Float) {
        for (barAnimator in barAnimators) {
            barAnimator.onRmsChanged(rmsDB)
        }
    }

    init {
        barAnimators = ArrayList()
        for (bar in speechBars) {
            barAnimators.add(BarRmsAnimator(bar!!))
        }
    }
}