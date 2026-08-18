package `is`.xyz.mpv

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setTitle(R.string.mpv_activity)

        // mpv-tv: startup checks (visible to user from launcher)
        if (savedInstanceState == null) {
            runStartupChecks()

            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, MainScreenFragment())
                commit()
            }
        }
    }

    // mpv-tv: all startup verification runs here (launcher entry point)
    private fun runStartupChecks() {
        val extDir = getExternalFilesDir(null)
        if (extDir != null && extDir.canWrite()) {
            TvDefaults.ensureDefaults(this, extDir.path)
            MpvTvConfig.load(extDir.path)
            WatchHistoryManager.init(this)
            UrlHistoryManager.init(this)
            Toast.makeText(this, "mpv-tv ready", Toast.LENGTH_SHORT).show()
            Log.i("mpv-tv", "Config: ${extDir.absolutePath}")
        } else {
            Toast.makeText(this, "mpv-tv: external storage unavailable", Toast.LENGTH_LONG).show()
            Log.w("mpv-tv", "External files dir not writable")
        }

        if (ShizukuHelper.isShizukuInstalled(this)) {
            Log.i("mpv-tv", "Shizuku ${if (ShizukuHelper.isShizukuAvailable()) "available" else "not running"}")
        }

        // first-run prompts
        val prefs = getSharedPreferences("mpvtv", MODE_PRIVATE)
        if (BridgeInstaller.shouldShowFirstRunPrompt(this)) {
            BridgeInstaller.showBridgeDialog(this, isFirstRun = true)
        }
        if (!prefs.getBoolean("preset_picked", false)) {
            val profile = DeviceProfileManager.detect(this)
            val presets = PresetManager.loadBundledPresets(this)
            val recommended = presets.firstOrNull { it.id == profile.recommendedPreset }
            if (recommended != null) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Recommended Preset")
                    .setMessage("For ${profile.model}: ${recommended.name}\n\n${recommended.description}")
                    .setPositiveButton("Apply") { _, _ ->
                        PresetManager.applyPreset(this, recommended)
                        prefs.edit().putBoolean("preset_picked", true).apply()
                        Toast.makeText(this, "Applied: ${recommended.name}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        prefs.edit().putBoolean("preset_picked", true).apply()
                    }
                    .show()
            }
        }
    }
}
