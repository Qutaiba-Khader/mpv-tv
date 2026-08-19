package `is`.xyz.mpv

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val status = mutableListOf<String>()

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
        }

        // first-run prompts (sequential, status shows AFTER all dialogs)
        val prefs = getSharedPreferences("mpvtv", MODE_PRIVATE)
        val showBridge = BridgeInstaller.shouldShowFirstRunPrompt(this)
        val showPreset = !prefs.getBoolean("preset_picked", false)

        if (showBridge) {
            BridgeInstaller.showBridgeDialog(this, isFirstRun = true, onDismiss = {
                if (showPreset) {
                    showPresetPicker(prefs, onDone = { showStatus() })
                } else {
                    showStatus()
                }
            })
        } else if (showPreset) {
            showPresetPicker(prefs, onDone = { showStatus() })
        } else {
            showStatus()
        }
    }

    private fun showPresetPicker(prefs: SharedPreferences, onDone: () -> Unit) {
        if (isFinishing || isDestroyed) return
        val profile = DeviceProfileManager.detect(this)
        val presets = PresetManager.loadBundledPresets(this)
        val recommended = presets.firstOrNull { it.id == profile.recommendedPreset }
        if (recommended != null) {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Recommended Preset")
                .setMessage("For ${profile.model}: ${recommended.name}\n\n${recommended.description}")
                .setPositiveButton("Apply") { _, _ ->
                    PresetManager.applyPreset(this, recommended)
                    prefs.edit().putBoolean("preset_picked", true).apply()
                    status.add("Preset: ${recommended.name}")
                }
                .setNegativeButton("Skip") { _, _ ->
                    prefs.edit().putBoolean("preset_picked", true).apply()
                    status.add("Preset: skipped")
                }
                .create()
            dialog.setOnDismissListener { onDone() }
            dialog.show()
        } else {
            prefs.edit().putBoolean("preset_picked", true).apply()
            onDone()
        }
    }

    private fun showStatus() {
        if (isFinishing || isDestroyed) return
        val msg = status.joinToString(" · ")
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
    }
}
