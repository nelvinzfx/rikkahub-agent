package me.rerere.rikkahub.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Lightweight top-of-screen pill that shows while a generation turn is active so the
 * user always knows when the agent is driving the UI. Uses TYPE_APPLICATION_OVERLAY
 * with FLAG_NOT_TOUCHABLE so it never blocks user gestures. No-ops silently if
 * SYSTEM_ALERT_WINDOW has not been granted — overlay is purely informational.
 */
object AgentOverlay {
    private const val TAG = "AgentOverlay"

    @Volatile private var view: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Split-point of the pill text. Everything before SPLIT_WORD is rendered as plain
    // bold white; SPLIT_WORD itself is rendered via [GradientTextView] so only that
    // segment carries the animated 3-colour flowing gradient. Keep this in sync with
    // the default text passed to [show].
    private const val SPLIT_WORD = "working"

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun show(context: Context, text: String = "The agent is working") {
        val app = context.applicationContext
        if (!canShow(app)) {
            Log.d(TAG, "show: SYSTEM_ALERT_WINDOW not granted, no-op")
            return
        }
        mainHandler.post { showInternal(app, text) }
    }

    fun hide(context: Context) {
        val app = context.applicationContext
        mainHandler.post { hideInternal(app) }
    }

    @SuppressLint("RtlHardcoded")
    private fun showInternal(app: Context, text: String) {
        val existing = view
        if (existing != null) {
            // Overlay already on screen; no text mutation needed — the pill text is
            // static ("The agent is working") and the gradient is driven by its own
            // animator, so re-calling show() is a no-op.
            return
        }
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val density = app.resources.displayMetrics.density
        val pad = (12 * density).toInt()
        val padV = (6 * density).toInt()
        val textSize = 12f

        // Split the pill text into a plain prefix and the gradient-painted SPLIT_WORD,
        // falling back to a single TextView if the word isn't present (defensive — keeps
        // callers that pass custom text working without crashing).
        val splitIdx = text.lastIndexOf(SPLIT_WORD)
        val prefix = if (splitIdx >= 0) text.substring(0, splitIdx) else text
        val working = if (splitIdx >= 0) SPLIT_WORD else null

        val container = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(pad, padV, pad, padV)
            background = GradientDrawable().apply {
                cornerRadius = 100f
                setColor(0xCC202020.toInt())
            }
        }
        if (prefix.isNotEmpty()) {
            container.addView(TextView(app).apply {
                this.text = prefix
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                includeFontPadding = false
            })
        }
        if (working != null) {
            container.addView(GradientTextView(app).apply {
                this.text = working
                setTypeface(typeface, Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                includeFontPadding = false
            })
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Far enough below the status bar / camera punch-hole that the pill never
            // overlaps the system clock or notch on edge-to-edge devices.
            y = (64 * app.resources.displayMetrics.density).toInt()
        }
        try {
            wm.addView(container, params)
            view = container
        } catch (t: Throwable) {
            Log.w(TAG, "addView failed", t)
        }
    }

    private fun hideInternal(app: Context) {
        val v = view ?: return
        view = null
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        try {
            wm.removeViewImmediate(v)
        } catch (t: Throwable) {
            Log.w(TAG, "removeView failed", t)
        }
    }
}
