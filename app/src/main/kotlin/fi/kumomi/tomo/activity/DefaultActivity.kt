package fi.kumomi.tomo.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import fi.kumomi.tomo.R
import org.jetbrains.anko.toast

class DefaultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val action = event?.action
        val keyCode = event?.keyCode

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    toast("Launching Navigation")
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    toast("Launching Navigation")
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                }
                return true
            }
            else -> {
                return super.dispatchKeyEvent(event)
            }
        }
    }

    fun launchTicketActivity(view: View) {
        toast("Launching Ticket Info")
        val intent = Intent(this, TicketInfoActivity::class.java)
        startActivity(intent)
    }
}
