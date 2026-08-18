package `is`.xyz.mpv

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import org.json.JSONObject
import java.io.File

data class DeviceProfile(
    val model: String,
    val soc: String,
    val gpu: String,
    val cpuArch: String,
    val cpuCores: Int,
    val ramMb: Long,
    val displayWidth: Int,
    val displayHeight: Int,
    val refreshRate: Float,
    val androidVersion: String,
    val apiLevel: Int,
    val warnings: List<String>,
    val recommendedPreset: String?
)

object DeviceProfileManager {
    private const val TAG = "mpv-tv"

    fun detect(context: Context): DeviceProfile {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(dm)
        @Suppress("DEPRECATION")
        val refreshRate = wm?.defaultDisplay?.refreshRate ?: 60f

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val soc = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL
                  else Build.HARDWARE

        val warnings = mutableListOf<String>()
        val recommendedPreset: String?

        if (Build.MODEL.contains("Google TV Streamer", ignoreCase = true)) {
            warnings.add("vo=gpu may lag on 4K HDR content")
            warnings.add("mediacodec-copy causes green screen — use mediacodec only")
            recommendedPreset = "android-tv-4k"
        } else if (Build.MODEL.contains("Shield", ignoreCase = true)) {
            recommendedPreset = "android-tv-4k"
        } else {
            recommendedPreset = "android-tv-1080p"
        }

        return DeviceProfile(
            model = Build.MODEL,
            soc = soc,
            gpu = getGpuName(),
            cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            ramMb = memInfo.totalMem / (1024 * 1024),
            displayWidth = dm.widthPixels,
            displayHeight = dm.heightPixels,
            refreshRate = refreshRate,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            warnings = warnings,
            recommendedPreset = recommendedPreset
        )
    }

    fun detectEnhanced(context: Context): Map<String, String> {
        val extra = mutableMapOf<String, String>()
        ShizukuHelper.executeCommand("getprop ro.board.platform")?.let {
            extra["platform"] = it
        }
        return extra
    }

    fun toJson(profile: DeviceProfile): JSONObject {
        return JSONObject().apply {
            put("model", profile.model)
            put("soc", profile.soc)
            put("gpu", profile.gpu)
            put("cpu_arch", profile.cpuArch)
            put("cpu_cores", profile.cpuCores)
            put("ram_mb", profile.ramMb)
            put("display", "${profile.displayWidth}x${profile.displayHeight}@${profile.refreshRate}")
            put("android", "${profile.androidVersion} (API ${profile.apiLevel})")
            put("recommended_preset", profile.recommendedPreset)
        }
    }

    fun saveProfile(context: Context, profile: DeviceProfile) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        try {
            File(dir, "device_profile.json").writeText(toJson(profile).toString(2))
            Log.i(TAG, "Saved device profile for ${profile.model}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save device profile: ${e.message}")
        }
    }

    private fun getGpuName(): String {
        return try {
            GLES20.glGetString(GLES20.GL_RENDERER) ?: "unknown"
        } catch (_: Exception) { "unknown" }
    }
}
