package `is`.xyz.mpv

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView

class RecordingOverlay(context: Context, parent: ViewGroup) {
    private val indicator: TextView

    init {
        indicator = TextView(context).apply {
            text = "● REC 00:00:00"
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.MONOSPACE
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            visibility = android.view.View.GONE

            val config = MpvTvConfig
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                if (config.recordIndicatorPosition == "top-left")
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                else
                    addRule(RelativeLayout.ALIGN_PARENT_END)
            }
        }
        parent.addView(indicator)
    }

    fun show() {
        indicator.visibility = android.view.View.VISIBLE
        indicator.text = formatRec(0)
    }

    fun hide() {
        indicator.visibility = android.view.View.GONE
    }

    fun updateTime(elapsedSeconds: Long) {
        indicator.text = formatRec(elapsedSeconds)
    }

    private fun formatRec(s: Long): String {
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val prefix = if (MpvTvConfig.recordIndicatorStyle == "emoji") "🔴" else "●"
        return "$prefix REC %02d:%02d:%02d".format(h, m, sec)
    }

    private fun dp(context: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(),
            context.resources.displayMetrics).toInt()
}
