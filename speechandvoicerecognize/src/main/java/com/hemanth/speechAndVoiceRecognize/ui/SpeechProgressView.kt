package com.hemanth.speechAndVoiceRecognize.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.hemanth.speechAndVoiceRecognize.ui.animator.*

class SpeechProgressView : View {
    private val speechBars: MutableList<SpeechBar> = ArrayList()
    private var paint: Paint? = null
    private var animator: BarParamsAnimator? = null
    private var radius = 0
    private var spacing = 0
    private var rotationRadius = 0
    private var amplitude = 0
    private var density = 0f
    private var isSpeaking = false
    private var animating = false
    private var barColor = -1
    private var barColors: IntArray? = intArrayOf(
        Color.parseColor("#3164d7"),
        Color.parseColor("#d92d29"),
        Color.parseColor("#eeaa10"),
        Color.parseColor("#3164d7"),
        Color.parseColor("#2e9641")
    )
    private var barMaxHeights: IntArray? = intArrayOf(60, 76, 58, 80, 55)

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    /**
     * Starts animating view
     */
    fun play() {
        startIdleInterpolation()
        animating = true
    }

    /**
     * Stops animating view
     */
    fun stop() {
        if (animator != null) {
            animator!!.stop()
            animator = null
        }
        isSpeaking = false
        animating = false
        resetBars()
    }

    /**
     * Set one color to all bars in view
     * @param color bar color
     */
    fun setSingleColor(color: Int) {
        barColor = color
    }

    /**
     * Set different colors to bars in view
     *
     * @param colors - array with size = [.BARS_COUNT]
     */
    fun setColors(colors: IntArray?) {
        if (colors == null) return
        barColors = IntArray(BARS_COUNT)
        if (colors.size < BARS_COUNT) {
            System.arraycopy(colors, 0, barColors, 0, colors.size)
            for (i in colors.size until BARS_COUNT) {
                barColors!![i] = colors[0]
            }
        } else {
            System.arraycopy(colors, 0, barColors, 0, BARS_COUNT)
        }
    }

    /**
     * Set sizes of bars in view
     *
     * @param heights - array with size = [.BARS_COUNT],
     * if not set uses default bars heights
     */
    fun setBarMaxHeightsInDp(heights: IntArray?) {
        if (heights == null) return
        barMaxHeights = IntArray(BARS_COUNT)
        if (heights.size < BARS_COUNT) {
            System.arraycopy(heights, 0, barMaxHeights, 0, heights.size)
            for (i in heights.size until BARS_COUNT) {
                barMaxHeights!![i] = heights[0]
            }
        } else {
            System.arraycopy(heights, 0, barMaxHeights, 0, BARS_COUNT)
        }
    }

    private fun init() {
        paint = Paint()
        paint!!.flags = Paint.ANTI_ALIAS_FLAG
        paint!!.color = Color.GRAY
        density = resources.displayMetrics.density
        radius = (CIRCLE_RADIUS_DP * density).toInt()
        spacing = (CIRCLE_SPACING_DP * density).toInt()
        rotationRadius = (ROTATION_RADIUS_DP * density).toInt()
        amplitude = (IDLE_FLOATING_AMPLITUDE_DP * density).toInt()
        if (density <= MDPI_DENSITY) {
            amplitude *= 2
        }
        startIdleInterpolation()
        animating = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (speechBars.isEmpty()) {
            initBars()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (speechBars.isEmpty()) {
            return
        }
        if (animating) {
            animator!!.animate()
        }
        for (i in speechBars.indices) {
            val bar = speechBars[i]
            if (barColors != null) {
                paint!!.color = barColors!![i]
            } else if (barColor != -1) {
                paint!!.color = barColor
            }
            canvas.drawRoundRect(bar.rect, radius.toFloat(), radius.toFloat(), paint!!)
        }
        if (animating) {
            invalidate()
        }
    }

    private fun initBars() {
        val heights = initBarHeights()
        val firstCirclePosition = measuredWidth / 2 - 2 * spacing - 4 * radius
        for (i in 0 until BARS_COUNT) {
            val x = firstCirclePosition + (2 * radius + spacing) * i
            val bar = SpeechBar(x, measuredHeight / 2, 2 * radius, heights[i], radius)
            speechBars.add(bar)
        }
    }

    private fun initBarHeights(): List<Int> {
        val barHeights: MutableList<Int> = ArrayList()
        if (barMaxHeights == null) {
            for (i in 0 until BARS_COUNT) {
                barHeights.add((DEFAULT_BARS_HEIGHT_DP[i] * density).toInt())
            }
        } else {
            for (i in 0 until BARS_COUNT) {
                barHeights.add((barMaxHeights!![i] * density).toInt())
            }
        }
        return barHeights
    }

    private fun resetBars() {
        for (bar in speechBars) {
            bar!!.x = bar.startX
            bar.y = bar.startY
            bar.height = radius * 2
            bar.update()
        }
    }

    private fun startIdleInterpolation() {
        animator = IdleAnimator(speechBars, amplitude)
        (animator as IdleAnimator).start()
    }

    private fun startRmsInterpolation() {
        resetBars()
        animator = RmsAnimator(speechBars)
        (animator as RmsAnimator).start()
    }

    private fun startTransformInterpolation() {
        resetBars()
        animator = TransformAnimator(speechBars, width / 2, height / 2, rotationRadius)
        (animator as TransformAnimator).start()
        (animator as TransformAnimator).setOnInterpolationFinishedListener(object :
            TransformAnimator.OnInterpolationFinishedListener {
            override fun onFinished() {
                startRotateInterpolation()
            }

        })
    }

    private fun startRotateInterpolation() {
        animator = RotatingAnimator(speechBars, width / 2, height / 2)
        (animator as RotatingAnimator).start()
    }

    fun onBeginningOfSpeech() {
        isSpeaking = true
    }

    fun onRmsChanged(rmsdB: Float) {
        if (animator == null || rmsdB < 1f) {
            return
        }
        if (animator !is RmsAnimator && isSpeaking) {
            startRmsInterpolation()
        }
        if (animator is RmsAnimator) {
            (animator as RmsAnimator).onRmsChanged(rmsdB)
        }
    }

    fun onEndOfSpeech() {
        isSpeaking = false
        startTransformInterpolation()
    }

    fun onResultOrOnError() {
        stop()
        play()
    }

    companion object {
        const val BARS_COUNT = 5
        private const val CIRCLE_RADIUS_DP = 5
        private const val CIRCLE_SPACING_DP = 11
        private const val ROTATION_RADIUS_DP = 25
        private const val IDLE_FLOATING_AMPLITUDE_DP = 3
        private val DEFAULT_BARS_HEIGHT_DP = intArrayOf(60, 46, 70, 54, 64)
        private const val MDPI_DENSITY = 1.5f
    }
}