package `is`.xyz.mpv

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val status = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate savedInstanceState=${savedInstanceState != null}")

        supportActionBar?.setTitle(R.string.mpv_activity)

        if (savedInstanceState == null) {
            Log.d(TAG, "First create — running startup checks")
            runStartupChecks()

            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, MainScreenFragment())
                commit()
            }
        }
    }

    private fun runStartupChecks() {
        Log.d(TAG, "runStartupChecks START")

        // storage check
        val extDir = getExternalFilesDir(null)
        Log.d(TAG, "extDir=$extDir canWrite=${extDir?.canWrite()}")
        if (extDir != null && extDir.canWrite()) {
            TvDefaults.ensureDefaults(this, extDir.path)
            MpvTvConfig.load(extDir.path)
            WatchHistoryManager.init(this)
            UrlHistoryManager.init(this)
            status.add("Storage: OK")
        } else {
            status.add("Storage: internal")
        }

        // shizuku check
        val shizukuInstalled = ShizukuHelper.isShizukuInstalled(this)
        Log.d(TAG, "Shizuku installed=$shizukuInstalled")
        if (shizukuInstalled) {
            val available = ShizukuHelper.isShizukuAvailable()
            status.add("Shizuku: ${if (available) "ready" else "off"}")
        }

        // first-run prompts
        val prefs = getSharedPreferences("mpvtv", MODE_PRIVATE)
        val showBridge = BridgeInstaller.shouldShowFirstRunPrompt(this)
        val showPreset = !prefs.getBoolean("preset_picked", false)
        Log.d(TAG, "showBridge=$showBridge showPreset=$showPreset")

        if (showBridge) {
            Log.d(TAG, "Showing bridge dialog")
            BridgeInstaller.showBridgeDialog(this, isFirstRun = true, onDismiss = {
                Log.d(TAG, "Bridge dismissed, showPreset=$showPreset")
                if (showPreset) {
                    showPresetPicker(prefs, onDone = {
                        Log.d(TAG, "Preset done, calling showStatus")
                        showStatus()
                    })
                } else {
                    showStatus()
                }
            })
        } else if (showPreset) {
            Log.d(TAG, "Showing preset picker (no bridge)")
            showPresetPicker(prefs, onDone = {
                Log.d(TAG, "Preset done, calling showStatus")
                showStatus()
            })
        } else {
            Log.d(TAG, "No dialogs needed, showing status directly")
            showStatus()
        }
        Log.d(TAG, "runStartupChecks END (dialogs are async)")
    }

    private fun showPresetPicker(prefs: SharedPreferences, onDone: () -> Unit) {
        Log.d(TAG, "showPresetPicker START")
        if (isFinishing || isDestroyed) {
            Log.d(TAG, "showPresetPicker SKIP — activity finishing/destroyed")
            return
        }
        val profile = DeviceProfileManager.detect(this)
        val presets = PresetManager.loadBundledPresets(this)
        val recommended = presets.firstOrNull { it.id == profile.recommendedPreset }
        Log.d(TAG, "presets=${presets.size} recommended=${recommended?.name} profile=${profile.model}")
        if (recommended != null) {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Recommended Preset")
                .setMessage("For ${profile.model}: ${recommended.name}\n\n${recommended.description}")
                .setPositiveButton("Apply") { _, _ ->
                    Log.d(TAG, "Preset APPLIED: ${recommended.name}")
                    PresetManager.applyPreset(this, recommended)
                    prefs.edit().putBoolean("preset_picked", true).apply()
                    status.add("Preset: ${recommended.name}")
                }
                .setNegativeButton("Skip") { _, _ ->
                    Log.d(TAG, "Preset SKIPPED")
                    prefs.edit().putBoolean("preset_picked", true).apply()
                    status.add("Preset: skipped")
                }
                .create()
            dialog.setOnDismissListener {
                Log.d(TAG, "Preset dialog dismissed")
                onDone()
            }
            dialog.show()
            Log.d(TAG, "Preset dialog shown")
        } else {
            Log.d(TAG, "No recommended preset found, skipping")
            prefs.edit().putBoolean("preset_picked", true).apply()
            onDone()
        }
    }

    private fun showStatus() {
        Log.d(TAG, "showStatus: ${status.joinToString(", ")}")
        if (isFinishing || isDestroyed) return
        val msg = status.joinToString(" · ")
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "mpv-tv-startup"
    }
}
