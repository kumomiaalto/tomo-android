package fi.kumomi.tomo.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.R
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.flowable.DeviceOrientationFlowable
import fi.kumomi.tomo.flowable.ProximiEventsFlowable
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.observable.AirlineTicketObservable
import io.proximi.proximiiolibrary.ProximiioAPI
import io.proximi.proximiiolibrary.ProximiioOptions
import io.reactivex.Flowable
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
    private var flightInfoObservable: Observable<AirlineTicket>? = null
    private var flightInfoObservableSwitch = PublishSubject.create<Boolean>()
    private var proximiApi: ProximiioAPI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val proximiOptions = ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)
        proximiApi = ProximiioAPI(tag, this, proximiOptions)
        proximiApi?.setAuth(Config.PROXIMI_API_KEY)
        proximiApi?.setActivity(this)

        flightInfoObservable = AirlineTicketObservable().create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(5, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        val posOrientationFlowable = Flowable.merge(
                DeviceOrientationFlowable.create(sensorManager).subscribeOn(Schedulers.io()),
                ProximiEventsFlowable.create(proximiApi).subscribeOn(Schedulers.io())
        )

        posOrientationFlowable.observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                }

        flightInfoObservableSwitch
                .switchMap { if(it) flightInfoObservable else Observable.never() }
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

                    val app = applicationContext as TomoApplication
                    val geofences = app.geofences
                }
    }

    override fun onResume() {
        super.onResume()
        flightInfoObservableSwitch.onNext(true)
        // TODO Need to add switches for posorientflowable
    }

    override fun onPause() {
        super.onPause()
        flightInfoObservableSwitch.onNext(false)
        // TODO Need to add switches for posorientflowable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        proximiApi?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        proximiApi?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
