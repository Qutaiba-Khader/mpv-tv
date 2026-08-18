package `is`.xyz.mpv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

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
    private val presets = Collections.synchronizedList(mutableListOf<Preset>())

    fun fetchPresets(callback: (List<Preset>) -> Unit) {
        Thread {
            try {
                val conn = URL(INDEX_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val arr = JSONArray(json)
                val fetched = mutableListOf<Preset>()
                for (i in 0 until arr.length()) {
                    fetched.add(parsePreset(arr.getJSONObject(i)))
                }
                synchronized(presets) {
                    presets.clear()
                    presets.addAll(fetched)
                }
                Log.i(TAG, "Fetched ${fetched.size} presets")
                callback(fetched.toList())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch presets: ${e.message}")
                callback(emptyList())
            }
        }.start()
    }

    fun loadBundledPresets(context: Context): List<Preset> {
        try {
            val json = context.assets.open("presets/index.json").use {
                it.bufferedReader().readText()
            }
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
        try {
            val conf = File(dir, "mpv.conf")
            val input = File(dir, "input.conf")
            if (conf.exists()) conf.copyTo(File(dir, "mpv.conf.bak"), overwrite = true)
            if (input.exists()) input.copyTo(File(dir, "input.conf.bak"), overwrite = true)
            val confTmp = File(dir, "mpv.conf.tmp")
            val inputTmp = File(dir, "input.conf.tmp")
            confTmp.writeText(preset.mpvConf)
            inputTmp.writeText(preset.inputConf)
            confTmp.renameTo(conf)
            inputTmp.renameTo(input)
            MpvTvConfig.load(dir.path)
            Log.i(TAG, "Applied preset: ${preset.name} (backup saved as .bak)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply preset ${preset.name}: ${e.message}")
        }
    }

    fun getCurrentPresetId(context: Context): String? {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val confFile = File(dir, "mpv.conf")
        val currentConf = if (confFile.exists()) confFile.readText() else ""
        return synchronized(presets) {
            presets.firstOrNull { p -> p.mpvConf.trim() == currentConf.trim() }?.id
        }
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
