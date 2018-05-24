package fi.kumomi.tomo.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import fi.kumomi.tomo.R
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.observable.AirlineTicketObservable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_ticket_info.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TicketInfoActivity : AppCompatActivity() {
    private var airlineTicketObservableSubject = PublishSubject.create<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = applicationContext as TomoApplication

        // Load ticket data from a global variable in app
        if (app.ticket != null)
            updateTicketData(app.ticket!!)

        val airlineTicketObservable = AirlineTicketObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        airlineTicketObservableSubject
                .switchMap { if(it) airlineTicketObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
                }
    }

    override fun onResume() {
        super.onResume()
        airlineTicketObservableSubject.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        airlineTicketObservableSubject.onNext(false)
    }

    fun launchDefaultActivity(view: View) {
        val intent  = Intent(this, DefaultActivity::class.java)
        startActivity(intent)
    }

    private fun updateTicketData(ticket: AirlineTicket) {
        name.text = "${ticket.firstName} ${ticket.lastName}"
        flightTime.text = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.departureTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                .toString("HH:mm")
        seat.text = ticket.seat
        ticketClass.text = ticket.ticketClass
        gate.text = ticket.gate
        navigationGate.text = "Gate " + ticket.gate
        flightNumber.text = ticket.flightNumber
        sourceDestination.text = "${ticket.source} → ${ticket.destination}"
        time.text = LocalDateTime().toString("HH:mm")
    }

    companion object {
        private const val TAG = "TicketInfoActivity"
    }
}
