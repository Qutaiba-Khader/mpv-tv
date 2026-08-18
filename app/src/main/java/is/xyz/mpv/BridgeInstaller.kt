package `is`.xyz.mpv

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object BridgeInstaller {
    private const val TAG = "mpv-tv"
    private const val BRIDGE_PACKAGE = "is.xyz.mpv"
    private const val BRIDGE_META_KEY = "mpvtv.bridge"
    private const val PREF_KEY = "bridge_prompt_shown"

    fun isBridgeInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(BRIDGE_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }
    }

    fun isStockMpvInstalled(context: Context): Boolean {
        if (!isBridgeInstalled(context)) return false
        return try {
            val info = context.packageManager.getApplicationInfo(
                BRIDGE_PACKAGE, PackageManager.GET_META_DATA)
            val isBridge = info.metaData?.getBoolean(BRIDGE_META_KEY, false) ?: false
            !isBridge
        } catch (_: Exception) { false }
    }

    fun shouldShowFirstRunPrompt(context: Context): Boolean {
        val prefs = context.getSharedPreferences("mpvtv", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_KEY, false)) return false
        // show if: bridge not installed (no is.xyz.mpv at all), OR stock mpv installed (not our bridge)
        return !isBridgeInstalled(context) || isStockMpvInstalled(context)
    }

    fun markPromptShown(context: Context) {
        context.getSharedPreferences("mpvtv", Context.MODE_PRIVATE)
            .edit().putBoolean(PREF_KEY, true).apply()
    }

    fun showBridgeDialog(activity: Activity, isFirstRun: Boolean = false) {
        if (activity.isFinishing || activity.isDestroyed) return
        val installed = isBridgeInstalled(activity)
        val isStock = isStockMpvInstalled(activity)

        val message = when {
            isStock -> "The original mpv app (is.xyz.mpv) is installed. " +
                "To make apps that target the original mpv open mpv-tv instead, " +
                "uninstall the original first, then install the bridge APK."
            installed -> "Bridge APK is already installed. Apps targeting is.xyz.mpv " +
                "will automatically open in mpv-tv."
            else -> "Other apps (like StreamRecorder) may try to open videos with " +
                "the original mpv app (is.xyz.mpv). Installing the bridge APK makes " +
                "those apps open mpv-tv instead.\n\n" +
                "This is optional — mpv-tv works fine without it."
        }

        val builder = AlertDialog.Builder(activity)
            .setTitle("Bridge APK")
            .setMessage(message)

        if (!installed && !isStock) {
            builder.setPositiveButton("Install Bridge") { _, _ ->
                if (canInstallPackages(activity)) {
                    installBridgeFromAssets(activity)
                } else {
                    requestInstallPermission(activity)
                }
            }
        }

        if (isFirstRun) {
            builder.setNegativeButton("Skip") { d, _ ->
                markPromptShown(activity)
                d.dismiss()
            }
        } else {
            builder.setNegativeButton("Close", null)
        }

        if (installed && !isStock) {
            builder.setNeutralButton("Uninstall Bridge") { _, _ ->
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$BRIDGE_PACKAGE")
                }
                activity.startActivity(intent)
            }
        }

        builder.show()
    }

    private fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    private fun requestInstallPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("To install the bridge APK, allow mpv-tv to install unknown apps in Settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun installBridgeFromAssets(activity: Activity) {
        try {
            val bridgeApk = File(activity.cacheDir, "mpv-tv-bridge.apk")
            activity.assets.open("mpv-tv-bridge.apk").use { input ->
                bridgeApk.outputStream().use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(activity,
                "${activity.packageName}.fileprovider", bridgeApk)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            Log.i(TAG, "Launched bridge APK installer")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install bridge: ${e.message}")
            AlertDialog.Builder(activity)
                .setTitle("Bridge APK")
                .setMessage("Bridge APK not bundled in this build. Download it from the mpv-tv GitHub releases page.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
