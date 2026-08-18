package `is`.xyz.mpv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class BridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = Intent(intent).apply {
            setPackage("com.qutaiba.mpvtv")
            component = null
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(target, 1)
        } catch (e: Exception) {
            Log.e("mpv-bridge", "mpv-tv not installed", e)
            Toast.makeText(this, "mpv-tv is not installed", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setResult(resultCode, data)
        finish()
    }
}
