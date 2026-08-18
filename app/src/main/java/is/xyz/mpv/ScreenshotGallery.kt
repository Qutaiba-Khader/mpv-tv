package `is`.xyz.mpv

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.File

data class ScreenshotEntry(
    val file: File,
    val name: String,
    val size: Long,
    val lastModified: Long
)

object ScreenshotGallery {
    private const val TAG = "mpv-tv"

    fun getScreenshots(context: Context): List<ScreenshotEntry> {
        val dirs = listOf(
            context.getExternalFilesDir(null),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )
        val shots = mutableListOf<ScreenshotEntry>()
        dirs.filterNotNull().forEach { dir ->
            dir.listFiles()?.filter { it.extension in listOf("jpg", "png", "webp") }?.forEach { f ->
                shots.add(ScreenshotEntry(f, f.name, f.length(), f.lastModified()))
            }
        }
        shots.sortByDescending { it.lastModified }
        Log.i(TAG, "Found ${shots.size} screenshots")
        return shots
    }

    fun deleteScreenshot(entry: ScreenshotEntry): Boolean {
        return entry.file.delete()
    }

    fun getPreviewDimensions(file: File): Pair<Int, Int>? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return if (opts.outWidth > 0) Pair(opts.outWidth, opts.outHeight) else null
    }
}
