package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val mpvConf: String,
    val inputConf: String,
    val version: Int
)

object PresetManager {
    private const val TAG = "mpv-tv"
    private const val INDEX_URL = "https://qutaiba-khader.github.io/mpv-tv/presets/index.json"
    private val presets = mutableListOf<Preset>()

    fun fetchPresets(callback: (List<Preset>) -> Unit) {
        Thread {
            try {
                val json = URL(INDEX_URL).readText()
                val arr = JSONArray(json)
                presets.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    presets.add(parsePreset(o))
                }
                Log.i(TAG, "Fetched ${presets.size} presets")
                callback(presets.toList())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch presets: ${e.message}")
                callback(emptyList())
            }
        }.start()
    }

    fun loadBundledPresets(context: Context): List<Preset> {
        try {
            val json = context.assets.open("presets/index.json").bufferedReader().readText()
            val arr = JSONArray(json)
            val bundled = mutableListOf<Preset>()
            for (i in 0 until arr.length()) bundled.add(parsePreset(arr.getJSONObject(i)))
            return bundled
        } catch (e: Exception) {
            Log.w(TAG, "No bundled presets: ${e.message}")
            return emptyList()
        }
    }

    fun applyPreset(context: Context, preset: Preset) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        File(dir, "mpv.conf").writeText(preset.mpvConf)
        File(dir, "input.conf").writeText(preset.inputConf)
        Log.i(TAG, "Applied preset: ${preset.name}")
    }

    fun getCurrentPresetId(context: Context): String? {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val currentConf = File(dir, "mpv.conf").let { if (it.exists()) it.readText() else "" }
        return presets.firstOrNull { it.mpvConf.trim() == currentConf.trim() }?.id
    }

    private fun parsePreset(o: JSONObject): Preset {
        val tags = mutableListOf<String>()
        o.optJSONArray("tags")?.let { arr ->
            for (i in 0 until arr.length()) tags.add(arr.getString(i))
        }
        return Preset(
            id = o.getString("id"),
            name = o.getString("name"),
            description = o.optString("description", ""),
            tags = tags,
            mpvConf = o.optString("mpv_conf", ""),
            inputConf = o.optString("input_conf", ""),
            version = o.optInt("version", 1)
        )
    }
}
