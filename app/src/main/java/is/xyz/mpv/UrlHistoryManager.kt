package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.File

object UrlHistoryManager {
    private const val TAG = "mpv-tv"
    private const val FILENAME = "url_history.json"
    private const val MAX_ENTRIES = 50
    private val history = mutableListOf<String>()
    private var historyFile: File? = null

    fun init(context: Context) {
        historyFile = File(context.getExternalFilesDir(null) ?: context.filesDir, FILENAME)
        load()
    }

    fun addUrl(url: String) {
        if (url.isBlank()) return
        history.remove(url)
        history.add(0, url)
        while (history.size > MAX_ENTRIES) history.removeLast()
        save()
    }

    fun getHistory(): List<String> = history.toList()

    fun removeUrl(url: String) {
        history.remove(url)
        save()
    }

    fun clear() {
        history.clear()
        save()
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
            history.clear()
            for (i in 0 until arr.length()) history.add(arr.getString(i))
            Log.i(TAG, "Loaded ${history.size} URL history entries")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load URL history: ${e.message}")
        }
    }

    private fun save() {
        val file = historyFile ?: return
        try {
            val arr = JSONArray()
            history.forEach { arr.put(it) }
            file.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save URL history: ${e.message}")
        }
    }
}
