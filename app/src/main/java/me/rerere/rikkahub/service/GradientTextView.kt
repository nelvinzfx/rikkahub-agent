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

    // Bright RGB palette — red → green → blue → (loops back to red). Last entry
    // duplicates the first so TileMode.REPEAT wraps the gradient with no visible
    // seam at the window edge.
    private val colors = intArrayOf(
        0xFFFF1744.toInt(),  // vivid red
        0xFF00E676.toInt(),  // vivid green
        0xFF2979FF.toInt(),  // vivid blue
        0xFFFF1744.toInt(),  // duplicate for seamless wrap
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
        // CRITICAL for a seamless infinite loop: the gradient's repeat period is 2*w,
        // so the shift must travel exactly that distance per cycle. Previously it only
        // travelled w (= half a period), which meant phase=1 landed at a gradient offset
        // that did NOT match phase=0 — so RESTART jumped the offset back and the visible
        // colours snapped, reading as a stutter/kink at the loop seam. With 2*w the
        // offset at phase=1 is identical to phase=0, so the restart is invisible.
        val shift = phase * 2 * w
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
