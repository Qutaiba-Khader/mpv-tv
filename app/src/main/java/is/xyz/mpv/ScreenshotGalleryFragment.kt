package `is`.xyz.mpv

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScreenshotGalleryFragment : Fragment() {
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        root.addView(TextView(ctx).apply {
            text = "Screenshots"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        })

        recyclerView = RecyclerView(ctx).apply {
            layoutManager = GridLayoutManager(ctx, 3)
        }
        root.addView(recyclerView)
        updateList()

        return root
    }

    private fun updateList() {
        val ctx = context ?: return
        val shots = ScreenshotGallery.getScreenshots(ctx)
        if (shots.isEmpty()) {
            recyclerView?.adapter = null
            return
        }
        recyclerView?.adapter = GalleryAdapter(shots) { entry ->
            val c = context ?: return@GalleryAdapter
            val deleted = ScreenshotGallery.deleteScreenshot(entry)
            if (deleted) {
                Toast.makeText(c, "Deleted: ${entry.name}", Toast.LENGTH_SHORT).show()
                updateList()
            }
        }
    }

    private class GalleryAdapter(
        private val shots: List<ScreenshotEntry>,
        private val onDelete: (ScreenshotEntry) -> Unit
    ) : RecyclerView.Adapter<GalleryAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewWithTag("img")
            val name: TextView = view.findViewWithTag("name")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
                isFocusable = true
                isFocusableInTouchMode = true
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(ImageView(ctx).apply {
                tag = "img"
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 200
                )
            })
            card.addView(TextView(ctx).apply {
                tag = "name"; textSize = 11f; maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            return VH(card)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = shots[position]
            holder.name.text = s.name
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bmp = BitmapFactory.decodeFile(s.file.absolutePath, opts)
                holder.image.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.image.setImageDrawable(null)
            }
            holder.view.setOnLongClickListener { onDelete(s); true }
        }

        override fun getItemCount() = shots.size
    }
}
