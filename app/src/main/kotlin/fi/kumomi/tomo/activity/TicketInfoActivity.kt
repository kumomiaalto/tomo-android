package fi.kumomi.tomo.activity

import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import fi.kumomi.tomo.R
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.observable.AirlineTicketObservable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_ticket_info.*
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import java.util.*


class TicketInfoActivity : AppCompatActivity() {
    private val tag = "TicketActivity"
    private var observable: Observable<AirlineTicket>? = null
    private var flightInfoObservableSwitch = PublishSubject.create<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        observable = AirlineTicketObservable().create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        flightInfoObservableSwitch
                .switchMap { if(it) observable else Observable.never() }
                .subscribe {
                    name.text = "${it.firstName} ${it.lastName}"
                    flightTime.text = ISODateTimeFormat.dateTimeParser()
                            .parseDateTime(it.departureTime)
                            .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                            .toString("HH:mm")
                    seat.text = it.seat
                    ticketClass.text = it.ticketClass
                    gate.text = it.gate
                    flightNumber.text = it.flightNumber
                    sourceDestination.text = "${it.source} → ${it.destination}"
                    currentTime.text = LocalDateTime().toString("HH:mm")
                    Log.e(tag, LocalDateTime().toString("HH:mm"))
                }
    }

    override fun onResume() {
        super.onResume()
        flightInfoObservableSwitch.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        flightInfoObservableSwitch.onNext(false)
    }
}
