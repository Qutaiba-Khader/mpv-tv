package `is`.xyz.mpv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordManager(private val context: Context, private val overlay: RecordingOverlay?) {
    private companion object {
        const val TAG = "mpv-tv"
    }

    @Volatile var isRecording = false
        private set
    private var recordingFile: String? = null
    @Volatile private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsed = maxOf(0L, (System.currentTimeMillis() - startTime) / 1000)
                overlay?.updateTime(elapsed)
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun toggle(): String {
        return if (isRecording) stop() else start()
    }

    fun start(): String {
        val dir = context.getExternalFilesDir("recordings")
            ?: File(context.filesDir, "recordings")
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "rec_$ts.mkv")
        recordingFile = file.absolutePath
        startTime = System.currentTimeMillis()

        try {
            MPVLib.setOptionString("stream-record", file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            return "error"
        }
        isRecording = true
        overlay?.show()
        handler.post(timerRunnable)
        Log.i(TAG, "Recording started: ${file.absolutePath}")
        return file.name
    }

    fun stop(): String {
        isRecording = false
        handler.removeCallbacks(timerRunnable)
        try {
            MPVLib.setOptionString("stream-record", "")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording: ${e.message}")
        }
        overlay?.hide()
        val name = recordingFile?.let { File(it).name } ?: "unknown"
        Log.i(TAG, "Recording stopped: $recordingFile")
        recordingFile = null
        return name
    }

    fun destroy() {
        if (isRecording) stop()
        handler.removeCallbacksAndMessages(null)
    }
}
