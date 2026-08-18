package `is`.xyz.mpv

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File

object TvDefaults {
    private const val TAG = "mpv-tv"

    fun ensureDefaults(context: Context, configDir: String) {
        val dir = File(configDir)
        dir.mkdirs()
        File(configDir, "scripts").mkdirs()

        copyDefaultIfMissing(context.assets, "mpv.conf", File(configDir, "mpv.conf"))
        copyDefaultIfMissing(context.assets, "input.conf", File(configDir, "input.conf"))

        // mpv-tv: ensure screenshot-directory points to external dir (not /)
        val confFile = File(configDir, "mpv.conf")
        if (confFile.exists()) {
            val content = confFile.readText()
            if (!content.contains("screenshot-directory=")) {
                confFile.appendText("\nscreenshot-directory=$configDir\n")
                Log.i(TAG, "Added screenshot-directory=$configDir to mpv.conf")
            }
        }
    }

    private fun copyDefaultIfMissing(assets: AssetManager, name: String, target: File) {
        if (target.exists()) {
            Log.v(TAG, "User config exists, skipping default: $name")
            return
        }
        try {
            assets.open(name).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Copied default config: $name (${target.length()} bytes)")
        } catch (e: Exception) {
            Log.w(TAG, "No bundled default for $name: ${e.message}")
        }
    }
}
