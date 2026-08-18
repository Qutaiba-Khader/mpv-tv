package `is`.xyz.mpv

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object ShizukuHelper {
    private const val TAG = "mpv-tv"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("pingBinder")
            method.invoke(null) as Boolean
        } catch (_: Exception) { false }
    }

    fun executeCommand(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.w(TAG, "Shell command failed: ${e.message}")
            null
        }
    }

    fun getAudioCapabilities(): Map<String, String> {
        val caps = mutableMapOf<String, String>()
        executeCommand("dumpsys audio")?.let { output ->
            if (output.contains("ENCODING_AC3")) caps["ac3"] = "supported"
            if (output.contains("ENCODING_E_AC3")) caps["eac3"] = "supported"
            if (output.contains("ENCODING_DTS")) caps["dts"] = "supported"
            if (output.contains("ENCODING_DOLBY_TRUEHD")) caps["truehd"] = "supported"
            val arcMatch = Regex("ARC.*?supported", RegexOption.IGNORE_CASE).find(output)
            caps["arc_type"] = if (output.contains("eARC", ignoreCase = true)) "eARC" else "ARC"
        }
        return caps
    }

    fun getDisplayInfo(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        executeCommand("dumpsys display")?.let { output ->
            Regex("mPhysicalDisplayInfo=.*?(\\d+)\\s*x\\s*(\\d+)").find(output)?.let {
                info["physical_resolution"] = "${it.groupValues[1]}x${it.groupValues[2]}"
            }
            if (output.contains("HDR_TYPE_HDR10")) info["hdr10"] = "supported"
            if (output.contains("HDR_TYPE_HLG")) info["hlg"] = "supported"
            if (output.contains("HDR_TYPE_DOLBY_VISION")) info["dolby_vision"] = "supported"
        }
        return info
    }

    fun getAvailableDecoders(): List<String> {
        val decoders = mutableListOf<String>()
        executeCommand("dumpsys media.codec")?.let { output ->
            Regex("name: (\\S+)").findAll(output).forEach {
                val name = it.groupValues[1]
                if (name.startsWith("OMX.") || name.startsWith("c2."))
                    decoders.add(name)
            }
        }
        return decoders
    }
}
