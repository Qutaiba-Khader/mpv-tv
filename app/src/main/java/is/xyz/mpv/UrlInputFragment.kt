package `is`.xyz.mpv

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class UrlInputFragment : Fragment() {
    private var urlInput: EditText? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        UrlHistoryManager.init(ctx)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        root.addView(TextView(ctx).apply {
            text = "Open URL"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        })

        urlInput = EditText(ctx).apply {
            hint = "Enter URL or paste from clipboard"
            isSingleLine = true
            isFocusable = true
            isFocusableInTouchMode = true
        }
        root.addView(urlInput)

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        btnRow.addView(Button(ctx).apply {
            text = "Play"
            isFocusable = true
            setOnClickListener { playUrl(urlInput?.text?.toString() ?: "") }
        })
        btnRow.addView(Button(ctx).apply {
            text = "Paste"
            isFocusable = true
            setOnClickListener {
                val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val text = clip?.primaryClip?.getItemAt(0)?.text?.toString()
                if (text != null) urlInput?.setText(text)
            }
        })
        btnRow.addView(Button(ctx).apply {
            text = "Clear History"
            isFocusable = true
            setOnClickListener {
                UrlHistoryManager.clear()
                updateHistory()
            }
        })
        root.addView(btnRow)

        root.addView(TextView(ctx).apply {
            text = "Recent URLs"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        })

        recyclerView = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
        }
        root.addView(recyclerView)
        updateHistory()

        return root
    }

    private fun playUrl(url: String) {
        val ctx = context ?: return
        if (url.isBlank()) {
            Toast.makeText(ctx, "Enter a URL", Toast.LENGTH_SHORT).show()
            return
        }
        UrlHistoryManager.addUrl(url)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                setPackage(ctx.packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHistory() {
        recyclerView?.adapter = HistoryAdapter(UrlHistoryManager.getHistory()) { url ->
            urlInput?.setText(url)
        }
    }

    private class HistoryAdapter(
        private val urls: List<String>,
        private val onSelect: (String) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewWithTag("url")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                tag = "url"
                textSize = 14f
                setPadding(16, 12, 16, 12)
                isFocusable = true
                isFocusableInTouchMode = true
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.text.text = urls[position]
            holder.view.setOnClickListener { onSelect(urls[position]) }
        }

        override fun getItemCount() = urls.size
    }
}
