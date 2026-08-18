package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class WatchHistoryEntry(
    val url: String,
    val title: String,
    val position: Long,
    val duration: Long,
    val timestamp: String
)

object WatchHistoryManager {
    private const val TAG = "mpv-tv"
    private const val FILENAME = "watch_history.json"
    private val entries = Collections.synchronizedList(mutableListOf<WatchHistoryEntry>())
    private var historyFile: File? = null

    fun init(context: Context) {
        historyFile = File(context.getExternalFilesDir(null) ?: context.filesDir, FILENAME)
        load()
    }

    fun addEntry(url: String, title: String, position: Long, duration: Long) {
        if (!MpvTvConfig.historyEnabled || url.isBlank()) return
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        synchronized(entries) {
            entries.removeAll { it.url == url }
            entries.add(0, WatchHistoryEntry(url, title, position, duration, ts))
            while (entries.size > maxOf(1, MpvTvConfig.historyMax)) entries.removeLast()
        }
        saveAsync()
    }

    fun getEntries(): List<WatchHistoryEntry> = synchronized(entries) { entries.toList() }

    fun removeEntry(url: String) {
        synchronized(entries) { entries.removeAll { it.url == url } }
        saveAsync()
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
        saveAsync()
    }

    private fun load() {
        val file = historyFile ?: return
        if (!file.exists()) return
        try {
            val arr = JSONArray(file.readText())
            val loaded = mutableListOf<WatchHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                loaded.add(WatchHistoryEntry(
                    o.getString("url"),
                    o.optString("title", ""),
                    o.optLong("position", 0),
                    o.optLong("duration", 0),
                    o.optString("timestamp", "")
                ))
            }
            synchronized(entries) {
                entries.clear()
                entries.addAll(loaded)
            }
            Log.i(TAG, "Loaded ${loaded.size} history entries")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load history: ${e.message}")
        }
    }

    private fun saveAsync() {
        Thread {
            val file = historyFile ?: return@Thread
            try {
                val arr = JSONArray()
                synchronized(entries) {
                    entries.forEach { e ->
                        arr.put(JSONObject().apply {
                            put("url", e.url)
                            put("title", e.title)
                            put("position", e.position)
                            put("duration", e.duration)
                            put("timestamp", e.timestamp)
                        })
                    }
                }
                val tmp = File(file.parent, "${file.name}.tmp")
                tmp.writeText(arr.toString(2))
                tmp.renameTo(file)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save history: ${e.message}")
            }
        }.start()
    }
}
