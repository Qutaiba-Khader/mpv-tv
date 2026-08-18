package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import java.io.File

data class KeyBinding(
    val keyName: String,
    val keyCode: Int,
    val command: String,
    val description: String,
    val isDefault: Boolean = true
)

object KeyMappingManager {
    private const val TAG = "mpv-tv"
    private val bindings = mutableListOf<KeyBinding>()

    private val defaultBindings = listOf(
        KeyBinding("RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT, "seek 10", "Seek forward 10s"),
        KeyBinding("LEFT", KeyEvent.KEYCODE_DPAD_LEFT, "seek -10", "Seek back 10s"),
        KeyBinding("FORWARD", KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "no-osd seek 30", "Seek forward 30s"),
        KeyBinding("REWIND", KeyEvent.KEYCODE_MEDIA_REWIND, "no-osd seek -30", "Seek back 30s"),
        KeyBinding("PLAYPAUSE", KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "cycle pause", "Toggle play/pause"),
        KeyBinding("PLAY", KeyEvent.KEYCODE_MEDIA_PLAY, "set pause no", "Play"),
        KeyBinding("PAUSE", KeyEvent.KEYCODE_MEDIA_PAUSE, "set pause yes", "Pause"),
        KeyBinding("1", KeyEvent.KEYCODE_1, "add audio-delay +0.1", "Audio delay +0.1s"),
        KeyBinding("4", KeyEvent.KEYCODE_4, "add audio-delay -0.1", "Audio delay -0.1s"),
        KeyBinding("7", KeyEvent.KEYCODE_7, "set audio-delay 0; show-text \"Audio delay: 0ms\" 2000", "Reset audio delay"),
        KeyBinding("3", KeyEvent.KEYCODE_3, "cycle-values video-aspect-override 16:9 4:3 2.35:1 no", "Cycle aspect"),
        KeyBinding("2", KeyEvent.KEYCODE_2, "add panscan 0.01", "Panscan in"),
        KeyBinding("5", KeyEvent.KEYCODE_5, "add panscan -0.01", "Panscan out"),
        KeyBinding("8", KeyEvent.KEYCODE_8, "set panscan 0.42; show-text \"Panscan: 0.42\" 1500", "Panscan preset"),
        KeyBinding("0", KeyEvent.KEYCODE_0, "set video-zoom 0; no-osd set panscan 0; no-osd set video-pan-x 0; no-osd set video-pan-y 0; no-osd set video-align-x 0; no-osd set video-align-y 0; show-text \"Zoom/Pan reset\" 1500", "Reset zoom/pan"),
        KeyBinding("6", KeyEvent.KEYCODE_6, "screenshot; show-text \"Screenshot saved\" 2000", "Take screenshot"),
    )

    fun init() {
        bindings.clear()
        bindings.addAll(defaultBindings)
    }

    fun getBindings(): List<KeyBinding> = bindings.toList()

    fun updateBinding(index: Int, keyCode: Int, keyName: String) {
        if (index in bindings.indices) {
            val old = bindings[index]
            bindings[index] = old.copy(keyCode = keyCode, keyName = keyName, isDefault = false)
        }
    }

    fun resetToDefaults() {
        bindings.clear()
        bindings.addAll(defaultBindings)
    }

    fun exportToInputConf(context: Context) {
        if (bindings.isEmpty()) {
            Log.w(TAG, "No bindings to export — call init() first")
            return
        }
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val lines = bindings.map { "${it.keyName} ${it.command}" }
        try {
            val tmp = File(dir, "input.conf.tmp")
            tmp.writeText(lines.joinToString("\n") + "\n")
            tmp.renameTo(File(dir, "input.conf"))
            Log.i(TAG, "Exported ${bindings.size} key bindings to input.conf")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export key bindings: ${e.message}")
        }
    }

    fun androidKeyCodeToMpvName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> "RIGHT"
            KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
            KeyEvent.KEYCODE_DPAD_UP -> "UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
            KeyEvent.KEYCODE_DPAD_CENTER -> "ENTER"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "PLAYPAUSE"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "PLAY"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "PAUSE"
            KeyEvent.KEYCODE_MEDIA_STOP -> "STOP"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "NEXT"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "PREV"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "FORWARD"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "REWIND"
            KeyEvent.KEYCODE_MEDIA_RECORD -> "RECORD"
            KeyEvent.KEYCODE_DEL -> "BS"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> (keyCode - KeyEvent.KEYCODE_0).toString()
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }
    }
}
