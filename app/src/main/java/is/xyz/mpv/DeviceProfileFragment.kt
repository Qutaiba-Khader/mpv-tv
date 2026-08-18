package `is`.xyz.mpv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class DeviceProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val profile = DeviceProfileManager.detect(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        fun addRow(label: String, value: String) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            row.addView(TextView(ctx).apply {
                text = label; textSize = 14f; minWidth = 200
                setTextColor(android.graphics.Color.GRAY)
            })
            row.addView(TextView(ctx).apply {
                text = value; textSize = 14f
            })
            root.addView(row)
        }

        root.addView(TextView(ctx).apply {
            text = "Device Profile"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        })

        addRow("Model", profile.model)
        addRow("SoC", profile.soc)
        addRow("GPU", profile.gpu)
        addRow("CPU", "${profile.cpuArch} (${profile.cpuCores} cores)")
        addRow("RAM", "${profile.ramMb} MB")
        addRow("Display", "${profile.displayWidth}x${profile.displayHeight}")
        addRow("Refresh", "${profile.refreshRate} Hz")
        addRow("Android", "${profile.androidVersion} (API ${profile.apiLevel})")

        if (profile.warnings.isNotEmpty()) {
            root.addView(TextView(ctx).apply {
                text = "Warnings"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 24, 0, 8)
            })
            profile.warnings.forEach { warning ->
                root.addView(TextView(ctx).apply {
                    text = "⚠ $warning"
                    textSize = 14f
                    setTextColor(android.graphics.Color.parseColor("#FF8800"))
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        if (ShizukuHelper.isShizukuInstalled(ctx)) {
            root.addView(Button(ctx).apply {
                text = "Enhanced Detection (Shizuku)"
                isFocusable = true
                setOnClickListener {
                    val audio = ShizukuHelper.getAudioCapabilities()
                    val display = ShizukuHelper.getDisplayInfo()
                    val decoders = ShizukuHelper.getAvailableDecoders()
                    val msg = buildString {
                        append("Audio: ${audio.entries.joinToString(", ") { "${it.key}=${it.value}" }}\n")
                        append("Display: ${display.entries.joinToString(", ") { "${it.key}=${it.value}" }}\n")
                        append("Decoders: ${decoders.take(10).joinToString(", ")}")
                        if (decoders.size > 10) append(" (+${decoders.size - 10} more)")
                    }
                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                }
            })
        }

        if (profile.recommendedPreset != null) {
            root.addView(Button(ctx).apply {
                text = "Apply Recommended: ${profile.recommendedPreset}"
                isFocusable = true
                setOnClickListener {
                    PresetManager.fetchPresets { presets ->
                        val preset = presets.firstOrNull { it.id == profile.recommendedPreset }
                        activity?.runOnUiThread {
                            if (preset != null) {
                                PresetManager.applyPreset(ctx, preset)
                                Toast.makeText(ctx, "Applied: ${preset.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(ctx, "Preset not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        }

        root.addView(Button(ctx).apply {
            text = "Save Profile to File"
            isFocusable = true
            setOnClickListener {
                DeviceProfileManager.saveProfile(ctx, profile)
                Toast.makeText(ctx, "Saved device_profile.json", Toast.LENGTH_SHORT).show()
            }
        })

        return root
    }
}
