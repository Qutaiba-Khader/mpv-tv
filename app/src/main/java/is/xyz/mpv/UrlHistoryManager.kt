package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.util.Collections

object UrlHistoryManager {
    private const val TAG = "mpv-tv"
    private const val FILENAME = "url_history.json"
    private const val MAX_ENTRIES = 50
    private val history = Collections.synchronizedList(mutableListOf<String>())
    private var historyFile: File? = null

    fun init(context: Context) {
        historyFile = File(context.getExternalFilesDir(null) ?: context.filesDir, FILENAME)
        load()
    }

    fun addUrl(url: String) {
        if (url.isBlank()) return
        synchronized(history) {
            history.remove(url)
            history.add(0, url)
            while (history.size > MAX_ENTRIES) history.removeLast()
        }
        saveAsync()
    }

    fun getHistory(): List<String> = synchronized(history) { history.toList() }

    fun removeUrl(url: String) {
        synchronized(history) { history.remove(url) }
        saveAsync()
    }

    fun clear() {
        synchronized(history) { history.clear() }
        saveAsync()
    }

    fun parseM3u(content: String): List<Pair<String, String>> {
        val entries = mutableListOf<Pair<String, String>>()
        var currentTitle = ""
        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentTitle = trimmed.substringAfter(",").trim()
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                entries.add(Pair(currentTitle.ifEmpty { trimmed }, trimmed))
                currentTitle = ""
            }
        }
        return entries
    }

    fun isM3u(url: String): Boolean {
        val path = url.lowercase().substringBefore("?").substringBefore("#")
        return path.endsWith(".m3u") || path.endsWith(".m3u8")
    }

    private fun load() {
        val file = historyFile ?: return
        if (!file.exists()) return
        try {
            val arr = JSONArray(file.readText())
            val loaded = mutableListOf<String>()
            for (i in 0 until arr.length()) loaded.add(arr.getString(i))
            synchronized(history) {
                history.clear()
                history.addAll(loaded)
            }
            Log.i(TAG, "Loaded ${loaded.size} URL history entries")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load URL history: ${e.message}")
        }
    }

    private fun saveAsync() {
        Thread {
            val file = historyFile ?: return@Thread
            try {
                val arr = JSONArray()
                synchronized(history) { history.forEach { arr.put(it) } }
                val tmp = File(file.parent, "${file.name}.tmp")
                tmp.writeText(arr.toString(2))
                tmp.renameTo(file)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save URL history: ${e.message}")
            }
        }.start()
    }
}
