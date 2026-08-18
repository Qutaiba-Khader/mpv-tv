package `is`.xyz.mpv

import android.os.Bundle
import android.util.Log
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
            Log.i("mpv-tv", "Config: ${extDir.absolutePath}")
        } else {
            Log.w("mpv-tv", "External files dir not writable, using internal")
        }

        if (ShizukuHelper.isShizukuInstalled(this)) {
            Log.i("mpv-tv", "Shizuku ${if (ShizukuHelper.isShizukuAvailable()) "available" else "not running"}")
        }

        // first-run prompts (sequential — only one dialog at a time)
        val prefs = getSharedPreferences("mpvtv", MODE_PRIVATE)
        val showBridge = BridgeInstaller.shouldShowFirstRunPrompt(this)
        val showPreset = !prefs.getBoolean("preset_picked", false)

        if (showBridge) {
            // bridge prompt first, preset after dismiss
            BridgeInstaller.showBridgeDialog(this, isFirstRun = true)
            // preset will show on next launch (bridge dialog sets prompt_shown)
        } else if (showPreset) {
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
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        prefs.edit().putBoolean("preset_picked", true).apply()
                    }
                    .show()
            }
        }
    }
}
