package `is`.xyz.mpv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PresetPickerFragment : Fragment() {
    private var recyclerView: RecyclerView? = null
    private val presets = mutableListOf<Preset>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            setPadding(32, 16, 32, 16)
        }
        loadPresets()
        return recyclerView!!
    }

    private fun loadPresets() {
        val bundled = PresetManager.loadBundledPresets(requireContext())
        if (bundled.isNotEmpty()) {
            presets.clear()
            presets.addAll(bundled)
            updateAdapter()
        }
        PresetManager.fetchPresets { fetched ->
            activity?.runOnUiThread {
                if (fetched.isNotEmpty()) {
                    presets.clear()
                    presets.addAll(fetched)
                    updateAdapter()
                }
            }
        }
    }

    private fun updateAdapter() {
        val currentId = PresetManager.getCurrentPresetId(requireContext())
        recyclerView?.adapter = PresetAdapter(presets, currentId) { preset ->
            PresetManager.applyPreset(requireContext(), preset)
            Toast.makeText(requireContext(), "Applied: ${preset.name}", Toast.LENGTH_SHORT).show()
            updateAdapter()
        }
    }

    private class PresetAdapter(
        private val presets: List<Preset>,
        private val currentId: String?,
        private val onSelect: (Preset) -> Unit
    ) : RecyclerView.Adapter<PresetAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewWithTag("title")
            val desc: TextView = view.findViewWithTag("desc")
            val tags: TextView = view.findViewWithTag("tags")
            val active: TextView = view.findViewWithTag("active")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val card = androidx.cardview.widget.CardView(ctx).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                radius = 12f
                cardElevation = 4f
                setContentPadding(24, 16, 24, 16)
                isFocusable = true
                isFocusableInTouchMode = true
            }
            val col = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            col.addView(TextView(ctx).apply { tag = "title"; textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD) })
            col.addView(TextView(ctx).apply { tag = "desc"; textSize = 14f; setPadding(0, 4, 0, 4) })
            col.addView(TextView(ctx).apply { tag = "tags"; textSize = 12f })
            col.addView(TextView(ctx).apply { tag = "active"; textSize = 12f; setTextColor(android.graphics.Color.GREEN) })
            card.addView(col)
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = presets[position]
            holder.title.text = p.name
            holder.desc.text = p.description
            holder.tags.text = p.tags.joinToString(", ")
            holder.active.text = if (p.id == currentId) "Active" else ""
            holder.view.setOnClickListener { onSelect(p) }
            holder.view.setOnFocusChangeListener { v, hasFocus ->
                (v as? androidx.cardview.widget.CardView)?.cardElevation = if (hasFocus) 12f else 4f
            }
        }

        override fun getItemCount() = presets.size
    }
}
