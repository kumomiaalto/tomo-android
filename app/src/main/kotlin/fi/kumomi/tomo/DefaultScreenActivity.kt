package fi.kumomi.tomo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_default_screen.*
import android.content.Intent



class DefaultScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_screen)
        setFlightInfo()
    }

    private fun setFlightInfo() {
        gate.text = "G14"
        boarding.text = "11:10"
        timeToGate.text = "35"
        freetime.text = "25 min"
    }

    private fun setNavButton() {
        navButton.setOnClickListener {
            startActivity(Intent(this@DefaultScreenActivity, MainActivity::class.java))
        }
    }
}
