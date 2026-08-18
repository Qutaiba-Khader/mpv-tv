package `is`.xyz.mpv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class MiniSeekBar(context: Context, parent: ViewGroup) : View(context) {
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bufferPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var bufferProgress = 0f
    private var mode = "always"

    init {
        val config = MpvTvConfig
        val heightDp = config.seekbarHeight
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, heightDp.toFloat(), context.resources.displayMetrics
        ).toInt()

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, heightPx
        ).apply { gravity = Gravity.BOTTOM }

        alpha = config.seekbarOpacity
        mode = config.seekbarMode

        try { progressPaint.color = Color.parseColor(config.seekbarColor) }
        catch (_: Exception) { progressPaint.color = Color.parseColor("#FF4444") }
        try { bufferPaint.color = Color.parseColor(config.seekbarBufferColor) }
        catch (_: Exception) { bufferPaint.color = Color.parseColor("#666666") }
        bgPaint.color = Color.parseColor("#33FFFFFF")

        visibility = if (config.seekbarEnabled) VISIBLE else GONE
        parent.addView(this)
    }

    fun updateProgress(position: Double, duration: Double, bufferEnd: Double) {
        if (duration <= 0) return
        progress = (position / duration).toFloat().coerceIn(0f, 1f)
        bufferProgress = (bufferEnd / duration).toFloat().coerceIn(0f, 1f)
        invalidate()
    }

    fun onSeekStart() {
        if (mode == "on-seek") visibility = VISIBLE
    }

    fun onSeekEnd() {
        if (mode == "on-seek") postDelayed({ visibility = GONE }, 2000)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawRect(0f, 0f, w * bufferProgress, h, bufferPaint)
        canvas.drawRect(0f, 0f, w * progress, h, progressPaint)
    }
}
