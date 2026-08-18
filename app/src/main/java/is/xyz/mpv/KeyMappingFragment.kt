package `is`.xyz.mpv

import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class KeyMappingFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        KeyMappingManager.init()
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        updateAdapter()

        val btnRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        btnRow.addView(Button(requireContext()).apply {
            text = "Reset Defaults"
            setOnClickListener {
                KeyMappingManager.resetToDefaults()
                updateAdapter()
            }
        })
        btnRow.addView(Button(requireContext()).apply {
            text = "Export to input.conf"
            setOnClickListener {
                KeyMappingManager.exportToInputConf(requireContext())
                Toast.makeText(requireContext(), "Exported to input.conf", Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(recyclerView)
        root.addView(btnRow)
        return root
    }

    private fun updateAdapter() {
        recyclerView?.adapter = BindingAdapter(KeyMappingManager.getBindings()) { index ->
            showKeyCaptureDialog(index)
        }
    }

    private fun showKeyCaptureDialog(index: Int) {
        val binding = KeyMappingManager.getBindings()[index]
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Press a button for: ${binding.description}")
            .setMessage("Current: ${binding.keyName}\n\nPress any button on your remote...")
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode != KeyEvent.KEYCODE_BACK) {
                val name = KeyMappingManager.androidKeyCodeToMpvName(keyCode)
                KeyMappingManager.updateBinding(index, keyCode, name)
                updateAdapter()
                dialog.dismiss()
                true
            } else false
        }
        dialog.show()
    }

    private class BindingAdapter(
        private val bindings: List<KeyBinding>,
        private val onRemap: (Int) -> Unit
    ) : RecyclerView.Adapter<BindingAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val key: TextView = view.findViewWithTag("key")
            val action: TextView = view.findViewWithTag("action")
            val desc: TextView = view.findViewWithTag("desc")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 12, 16, 12)
                isFocusable = true
                isFocusableInTouchMode = true
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
            row.addView(TextView(ctx).apply {
                tag = "key"; textSize = 16f; minWidth = 120
                setTypeface(null, android.graphics.Typeface.BOLD)
            })
            row.addView(TextView(ctx).apply {
                tag = "desc"; textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(ctx).apply {
                tag = "action"; textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            })
            return VH(row)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val b = bindings[position]
            holder.key.text = b.keyName
            holder.desc.text = b.description
            holder.action.text = if (b.isDefault) "" else "(custom)"
            holder.view.setOnClickListener { onRemap(position) }
        }

        override fun getItemCount() = bindings.size
    }
}
