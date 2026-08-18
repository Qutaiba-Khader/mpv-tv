package `is`.xyz.mpv

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setTitle(R.string.mpv_activity)

        if (savedInstanceState == null) {
            runStartupChecks()

            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, MainScreenFragment())
                commit()
            }
        }
    }

    private fun runStartupChecks() {
        val status = mutableListOf<String>()

        // storage check
        val extDir = getExternalFilesDir(null)
        if (extDir != null && extDir.canWrite()) {
            TvDefaults.ensureDefaults(this, extDir.path)
            MpvTvConfig.load(extDir.path)
            WatchHistoryManager.init(this)
            UrlHistoryManager.init(this)
            status.add("Storage: OK")
            MpvTvLog.i("mpv-tv", "Config: ${extDir.absolutePath}")
        } else {
            status.add("Storage: internal only")
            MpvTvLog.w("mpv-tv", "External files dir not writable")
        }

        // shizuku check
        if (ShizukuHelper.isShizukuInstalled(this)) {
            val available = ShizukuHelper.isShizukuAvailable()
            status.add("Shizuku: ${if (available) "ready" else "not running"}")
            MpvTvLog.i("mpv-tv", "Shizuku ${if (available) "available" else "not running"}")
        }

        // show status in action bar subtitle (visible but not intrusive)
        supportActionBar?.subtitle = status.joinToString(" · ")

        // clear subtitle after 5 seconds
        window.decorView.postDelayed({
            if (!isFinishing && !isDestroyed) supportActionBar?.subtitle = null
        }, 5000)

        // first-run prompts
        val prefs = getSharedPreferences("mpvtv", MODE_PRIVATE)
        val showBridge = BridgeInstaller.shouldShowFirstRunPrompt(this)
        val showPreset = !prefs.getBoolean("preset_picked", false)

        if (showBridge) {
            BridgeInstaller.showBridgeDialog(this, isFirstRun = true, onDismiss = {
                if (showPreset) showPresetPicker(prefs)
            })
        } else if (showPreset) {
            showPresetPicker(prefs)
        }
    }

    private fun showPresetPicker(prefs: SharedPreferences) {
        if (isFinishing || isDestroyed) return
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
        } else {
            prefs.edit().putBoolean("preset_picked", true).apply()
        }
    }
}
