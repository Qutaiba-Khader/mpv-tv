package `is`.xyz.mpv

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class QuickSettingsPanel(private val context: Context, private val parent: ViewGroup) {
    private val panel: FrameLayout
    private val container: LinearLayout
    var isVisible = false
        private set
    var onRecordToggle: (() -> Unit)? = null // mpv-tv: callback for recording toggle

    init {
        panel = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#99000000"))
            visibility = View.GONE
            isFocusable = true
            isFocusableInTouchMode = true
        }

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val w = dp(320)
            layoutParams = FrameLayout.LayoutParams(w, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#DD1A1A1A"))
                cornerRadius = dp(12).toFloat()
            }
            background = bg
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }

        addTitle("Quick Settings")
        addRow("Speed", arrayOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x")) { v ->
            val speed = v.replace("x", "").toDoubleOrNull() ?: 1.0
            MPVLib.setPropertyDouble("speed", speed)
        }
        addRow("Aspect", arrayOf("16:9", "4:3", "2.35:1", "Auto")) { v ->
            val aspect = if (v == "Auto") "no" else v
            MPVLib.setPropertyString("video-aspect-override", aspect)
        }
        addRow("Deband", arrayOf("ON", "OFF")) { v ->
            MPVLib.setPropertyBoolean("deband", v == "ON")
        }
        addRow("Deinterlace", arrayOf("ON", "OFF")) { v ->
            MPVLib.setPropertyBoolean("deinterlace", v == "ON")
        }

        // mpv-tv: recording toggle accessible from QuickPanel (GTV remote has no RECORD button)
        addRow("Record", arrayOf("Start", "Stop")) { v ->
            onRecordToggle?.invoke()
        }

        panel.addView(container)
        parent.addView(panel)
    }

    private fun addTitle(text: String) {
        container.addView(TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })
    }

    private fun addRow(label: String, options: Array<String>, onSelect: (String) -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        row.addView(TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#AAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(dp(80), LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        val optionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        options.forEach { opt ->
            optionsRow.addView(TextView(context).apply {
                text = opt
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(dp(8), dp(6), dp(8), dp(6))
                isFocusable = true
                isFocusableInTouchMode = true
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#333333"))
                    cornerRadius = dp(6).toFloat()
                }
                background = bg
                setOnClickListener { onSelect(opt) }
                setOnFocusChangeListener { _, hasFocus ->
                    (background as? GradientDrawable)?.setColor(
                        if (hasFocus) Color.parseColor("#555555") else Color.parseColor("#333333")
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(4) }
            })
        }

        row.addView(optionsRow)
        container.addView(row)
    }

    fun toggle() {
        if (isVisible) hide() else show()
    }

    fun show() {
        panel.visibility = View.VISIBLE
        isVisible = true
        highlightCurrentValues()
        container.getChildAt(1)?.requestFocus()
    }

    private fun highlightCurrentValues() {
        try {
            val currentSpeed = MPVLib.getPropertyDouble("speed") ?: 1.0
            val currentAspect = MPVLib.getPropertyString("video-aspect-override") ?: "no"
            val currentDeband = MPVLib.getPropertyBoolean("deband") ?: false
            val currentDeinterlace = MPVLib.getPropertyBoolean("deinterlace") ?: false
            highlightOption(1, "%.1fx".format(currentSpeed).replace(".0x", "x"))
            highlightOption(2, if (currentAspect == "no" || currentAspect == "-1") "Auto" else currentAspect)
            highlightOption(3, if (currentDeband) "ON" else "OFF")
            highlightOption(4, if (currentDeinterlace) "ON" else "OFF")
        } catch (_: Exception) {}
    }

    private fun highlightOption(rowIndex: Int, activeValue: String) {
        val row = container.getChildAt(rowIndex) as? LinearLayout ?: return
        val optionsRow = row.getChildAt(1) as? LinearLayout ?: return
        for (i in 0 until optionsRow.childCount) {
            val tv = optionsRow.getChildAt(i) as? TextView ?: continue
            val bg = tv.background as? GradientDrawable ?: continue
            if (tv.text.toString().equals(activeValue, ignoreCase = true))
                bg.setColor(Color.parseColor("#2196F3"))
            else
                bg.setColor(Color.parseColor("#333333"))
        }
    }

    fun hide() {
        panel.visibility = View.GONE
        isVisible = false
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (!isVisible) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                hide()
                return true
            }
        }
        // consume both ACTION_DOWN and ACTION_UP for non-navigation keys while panel is open
        // but let volume/media keys through
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            event.keyCode == KeyEvent.KEYCODE_MUTE) return false
        return true
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics
    ).toInt()
}
