package fi.kumomi.tomo.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import fi.kumomi.tomo.R
import org.jetbrains.anko.toast

class NavigationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val action = event?.action
        val keyCode = event?.keyCode

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    toast("Launching Default")
                    val intent = Intent(this, DefaultActivity::class.java)
                    startActivity(intent)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    toast("Launching Default")
                    val intent = Intent(this, DefaultActivity::class.java)
                    startActivity(intent)
                }
                return true
            }
            else -> {
                return super.dispatchKeyEvent(event)
            }
        }
    }
}
