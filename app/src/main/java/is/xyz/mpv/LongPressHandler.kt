package `is`.xyz.mpv

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

class LongPressHandler(
    private val onLongPressSeek: (Int) -> Unit,
    private val onQuickPanel: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var longPressKey = 0
    private var longPressTriggered = false
    private val longPressDelay = 500L

    private val longPressRunnable = Runnable {
        longPressTriggered = true
        when (longPressKey) {
            KeyEvent.KEYCODE_DPAD_LEFT -> onLongPressSeek(-MpvTvConfig.longPressSeek)
            KeyEvent.KEYCODE_DPAD_RIGHT -> onLongPressSeek(MpvTvConfig.longPressSeek)
            KeyEvent.KEYCODE_DPAD_UP -> onQuickPanel()
        }
    }

    fun onKeyDown(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> {
                longPressKey = event.keyCode
                longPressTriggered = false
                handler.postDelayed(longPressRunnable, longPressDelay)
                return false
            }
        }
        return false
    }

    fun onKeyUp(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP -> {
                handler.removeCallbacks(longPressRunnable)
                if (longPressTriggered) {
                    longPressTriggered = false
                    return true
                }
            }
        }
        return false
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }
}
