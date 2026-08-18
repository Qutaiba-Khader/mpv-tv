package `is`.xyz.mpv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout

class MiniSeekBar(context: Context, parent: ViewGroup) : View(context) {
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bufferPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    @Volatile private var progress = 0f
    @Volatile private var bufferProgress = 0f
    private var mode = "always"
    private var hideRunnable: Runnable? = null

    init {
        val config = MpvTvConfig
        val heightDp = maxOf(1, config.seekbarHeight)
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), context.resources.displayMetrics
        ).toInt()

        layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, heightPx
        ).apply { addRule(RelativeLayout.ALIGN_PARENT_BOTTOM) }

        alpha = config.seekbarOpacity.coerceIn(0f, 1f)
        mode = config.seekbarMode

        progressPaint.color = parseColorSafe(config.seekbarColor, "#FF4444")
        bufferPaint.color = parseColorSafe(config.seekbarBufferColor, "#666666")
        bgPaint.color = Color.parseColor("#33FFFFFF")

        visibility = if (config.seekbarEnabled) VISIBLE else GONE
        parent.addView(this)
    }

    fun updateProgress(position: Double, duration: Double, bufferEnd: Double) {
        if (duration <= 0) return
        progress = (position / duration).toFloat().coerceIn(0f, 1f)
        bufferProgress = (bufferEnd / duration).toFloat().coerceIn(0f, 1f)
        postInvalidate()
    }

    fun onSeekStart() {
        hideRunnable?.let { removeCallbacks(it) }
        if (mode == "on-seek") visibility = VISIBLE
    }

    fun onSeekEnd() {
        if (mode == "on-seek") {
            hideRunnable?.let { removeCallbacks(it) }
            hideRunnable = Runnable { visibility = GONE }
            postDelayed(hideRunnable, 2000)
        }
    }

    fun cleanup() {
        hideRunnable?.let { removeCallbacks(it) }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawRect(0f, 0f, w * bufferProgress, h, bufferPaint)
        canvas.drawRect(0f, 0f, w * progress, h, progressPaint)
    }

    private fun parseColorSafe(color: String, fallback: String): Int {
        return try { Color.parseColor(color) }
        catch (_: Exception) { Color.parseColor(fallback) }
    }
}
