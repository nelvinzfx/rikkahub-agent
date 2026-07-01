package me.rerere.rikkahub.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.TextView

/**
 * TextView whose text is painted with an animated 3-color [LinearGradient] that
 * shifts horizontally each frame — a "flowing water" rainbow effect. Only the
 * shader moves; layout/text are unchanged, so it composes cleanly inside any
 * container and stops animating when detached.
 *
 * Used by [AgentOverlay] to colour just the word "working" in the
 * "The agent is working" pill.
 */
class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : TextView(context, attrs, defStyleAttr) {

    // Three-colour flowing palette: violet → cyan → pink → (loops back to violet).
    // The trailing duplicate of index 0 keeps the seam invisible when TileMode.REPEAT
    // wraps the gradient window — without it, the wrap jumps from pink straight to
    // violet and looks like a flicker.
    private val colors = intArrayOf(
        0xFF7B61FF.toInt(),
        0xFF00E5FF.toInt(),
        0xFFFF61C7.toInt(),
        0xFF7B61FF.toInt(),
    )

    private var phase = 0f

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = CYCLE_MS
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat().coerceAtLeast(1f)
        // Shift the gradient window one full width per cycle. Width = 2*w so all
        // three colours are visible across the text at any phase, and REPEAT tiles
        // it seamlessly so the seam never lands inside the visible text region.
        val shift = phase * w
        paint.shader = LinearGradient(
            shift, 0f,
            shift + 2 * w, 0f,
            colors,
            null,
            Shader.TileMode.REPEAT,
        )
        super.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    companion object {
        private const val CYCLE_MS = 1200L
    }
}
