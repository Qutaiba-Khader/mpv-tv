package `is`.xyz.mpv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

class WatchHistoryFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        root.addView(TextView(ctx).apply {
            text = "Watch History"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        })

        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        btnRow.addView(Button(ctx).apply {
            text = "Clear All"
            isFocusable = true
            setOnClickListener {
                WatchHistoryManager.clear()
                updateList()
                Toast.makeText(ctx, "History cleared", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(btnRow)

        recyclerView = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
        }
        root.addView(recyclerView)
        updateList()

        return root
    }

    private fun updateList() {
        val entries = WatchHistoryManager.getEntries()
        if (entries.isEmpty()) {
            recyclerView?.adapter = null
            return
        }
        recyclerView?.adapter = HistoryAdapter(
            entries,
            onPlay = { entry ->
                val ctx = context ?: return@HistoryAdapter
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(entry.url), "video/*")
                        setPackage(ctx.packageName)
                        putExtra("position", minOf(entry.position * 1000, Int.MAX_VALUE.toLong()).toInt())
                        putExtra("title", entry.title)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            onRemove = { entry ->
                WatchHistoryManager.removeEntry(entry.url)
                updateList()
            }
        )
    }

    private class HistoryAdapter(
        private val entries: List<WatchHistoryEntry>,
        private val onPlay: (WatchHistoryEntry) -> Unit,
        private val onRemove: (WatchHistoryEntry) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewWithTag("title")
            val info: TextView = view.findViewWithTag("info")
            val time: TextView = view.findViewWithTag("time")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val card = androidx.cardview.widget.CardView(ctx).apply {
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
                radius = 8f
                cardElevation = 4f
                setContentPadding(20, 12, 20, 12)
                isFocusable = true
                isFocusableInTouchMode = true
            }
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(ctx).apply { tag = "title"; textSize = 16f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
            col.addView(TextView(ctx).apply { tag = "info"; textSize = 13f; setTextColor(android.graphics.Color.GRAY) })
            col.addView(TextView(ctx).apply { tag = "time"; textSize = 12f; setTextColor(android.graphics.Color.GRAY) })
            card.addView(col)
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = entries[position]
            holder.title.text = e.title.ifEmpty { e.url }
            val pos = formatTime(e.position)
            val dur = formatTime(e.duration)
            holder.info.text = "$pos / $dur"
            holder.time.text = e.timestamp
            holder.view.setOnClickListener { onPlay(e) }
            holder.view.setOnLongClickListener { onRemove(e); true }
            holder.view.setOnFocusChangeListener { v, hasFocus ->
                (v as? androidx.cardview.widget.CardView)?.cardElevation = if (hasFocus) 12f else 4f
            }
        }

        override fun getItemCount() = entries.size

        private fun formatTime(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }
    }
}
