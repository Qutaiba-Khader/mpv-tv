package `is`.xyz.mpv

import android.util.Log
import java.io.File

object MpvTvLog {
    fun i(tag: String, msg: String) { if (MpvTvConfig.debugLogs) android.util.Log.i(tag, msg) }
    fun w(tag: String, msg: String) { if (MpvTvConfig.debugLogs) android.util.Log.w(tag, msg) }
    fun e(tag: String, msg: String) { android.util.Log.e(tag, msg) } // errors always log
    fun e(tag: String, msg: String, t: Throwable) { android.util.Log.e(tag, msg, t) }
}

object MpvTvConfig {
    private const val TAG = "mpv-tv"
    private val options = mutableMapOf<String, String>()

    fun load(configDir: String) {
        options.clear()
        val file = File(configDir, "mpv.conf")
        if (!file.exists()) return
        try {
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("#") || trimmed.isEmpty()) return@forEach
                if (trimmed.startsWith("mpvtv-")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        val value = parts[1].trim().replace(Regex("\\s+#.*$"), "")
                        options[parts[0].trim()] = value
                    }
                }
            }
            Log.i(TAG, "Loaded ${options.size} mpvtv-* options")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse mpvtv config: ${e.message}")
        }
    }

    fun getString(key: String, default: String = ""): String = options.getOrDefault(key, default)
    fun getBool(key: String, default: Boolean = false): Boolean {
        val v = options[key] ?: return default
        return v == "yes" || v == "true" || v == "1"
    }
    fun getInt(key: String, default: Int = 0): Int = options[key]?.toIntOrNull() ?: default
    fun getFloat(key: String, default: Float = 0f): Float = options[key]?.toFloatOrNull() ?: default

    val seekbarEnabled get() = getBool("mpvtv-seekbar", true)
    val seekbarHeight get() = getInt("mpvtv-seekbar-height", 3)
    val seekbarOpacity get() = getFloat("mpvtv-seekbar-opacity", 0.4f)
    val seekbarColor get() = getString("mpvtv-seekbar-color", "#FF4444")
    val seekbarBufferColor get() = getString("mpvtv-seekbar-buffer-color", "#666666")
    val seekbarMode get() = getString("mpvtv-seekbar-mode", "always")
    val recordIndicatorPosition get() = getString("mpvtv-record-position", "top-right")
    val recordIndicatorStyle get() = getString("mpvtv-record-style", "text")
    val quickPanelEnabled get() = getBool("mpvtv-quickpanel", true)
    val historyEnabled get() = getBool("mpvtv-history", true)
    val historyMax get() = getInt("mpvtv-history-max", 100)
    val longPressSeek get() = getInt("mpvtv-longpress-seek", 60)
    val debugLogs get() = getBool("mpvtv-debug", false)
}
