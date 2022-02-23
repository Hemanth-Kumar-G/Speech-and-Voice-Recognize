package com.hemanth.speechAndVoiceRecognize.ui

import android.graphics.RectF

class SpeechBar(var x: Int, var y: Int, var height: Int, maxHeight: Int, val radius: Int) {
    val maxHeight: Int = maxHeight
    val startX: Int = x
    val startY: Int = y
    val rect: RectF = RectF(
        (x - radius).toFloat(),
        (y - height / 2).toFloat(),
        (x + radius).toFloat(),
        (y + height / 2).toFloat()
    )

    fun update() {
        rect[(x - radius).toFloat(), (
                y - height / 2).toFloat(), (
                x + radius).toFloat()] = (
                y + height / 2).toFloat()
    }

}