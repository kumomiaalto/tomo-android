package fi.kumomi.tomo.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.R
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.flowable.ProximiEventsFlowable
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.observable.AirlineTicketObservable
import io.proximi.proximiiolibrary.ProximiioAPI
import io.proximi.proximiiolibrary.ProximiioOptions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_default_screen.*
import org.jetbrains.anko.toast
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.os.Vibrator
import android.util.Log
import fi.kumomi.tomo.model.Beacon
import org.joda.time.DateTime
import org.joda.time.Interval


class DefaultActivity : AppCompatActivity() {
    private val tag = "DefaultActivity"
    private var flightInfoObservableSwitch = PublishSubject.create<Boolean>()
    private var proximiObservableSwitch = PublishSubject.create<Boolean>()
    private var proximiApi: ProximiioAPI? = null
    private var notificationLock = false
    private var mode = "big_notification" //default, small_notification or big_notification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_screen)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val app = applicationContext as TomoApplication
        if (app.ticket != null)
            updateTicketData(app.ticket!!)

        val proximiOptions = ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)
        proximiApi = ProximiioAPI(tag, this, proximiOptions)
        proximiApi?.setAuth(Config.PROXIMI_API_KEY)
        proximiApi?.setActivity(this)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val flightInfoObservable = AirlineTicketObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        val proximiObservable = ProximiEventsFlowable.create(proximiApi)
                .toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        flightInfoObservableSwitch
                .switchMap { if(it) flightInfoObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
                }

        proximiObservableSwitch
                .switchMap { if(it) proximiObservable else Observable.never() }
                .subscribe {
                    if (it.eventType == DevicePosOrientEvent.BEACON_FOUND_EVENT) {
                        Log.i(tag, "Beacon found - ${it.proximiEvent?.beacon?.mac}")
                    }

                    if (!notificationLock && it.eventType == DevicePosOrientEvent.BEACON_FOUND_EVENT &&
                            app.beacons.containsKey(it.proximiEvent?.beacon?.mac)) {
                        var seenBeacon = false

                        if (app.seenBeacons.containsKey(it.proximiEvent?.beacon?.mac)) {
                            val seenTime = app.seenBeacons[it.proximiEvent?.beacon?.mac]
                            val currentTime = DateTime()
                            val interval = Interval(seenTime, currentTime)

                            if (interval.toDuration().standardMinutes < 2) {
                                seenBeacon = true
                            }
                        }

                        if (!seenBeacon) {
                            app.seenBeacons[it.proximiEvent?.beacon?.mac] = DateTime()

                            val beacon = app.beacons[it.proximiEvent?.beacon?.mac]

                            if (beacon!!.beaconType == "notification") { //not needed anymore, TODO: remove
                                mode = "big_notification"
                                setNotificationData(beacon)
                                toggleTicketBoxElements(false)
                                toggleNotificatioBoxElements(true)
                                notificationLock = true
                                vibrator.vibrate(1000)

                                Handler().postDelayed({
                                    notificationLock = false
                                    toggleNotificatioBoxElements(false)
                                    toggleTicketBoxElements(true)
                                }, 10000)
                            }
                            if (beacon!!.beaconType == "big_notification") {
                                mode = "big_notification"
                                setBigNotificationData(beacon)
                                toggleBigNotificationBoxElements(true)
                                notificationLock = true
                                vibrator.vibrate(3000)
                                //TODO: add big sound
                                //TODO: make button blink until being clicked
                                //button listener necessary?
                            }
                            if (beacon!!.beaconType == "small_notification") {
                                mode = "small_notification"
                                setSmallNotificationData(beacon)
                                toggleSmallNotificationBoxElements(true)
                                toggleNeedleViewElements(false)
                                notificationLock = true
                                vibrator.vibrate(3000)
                                //TODO: add small sound
                                //TODO: make button blink until being clicked
                                //button listener necessary?
                            }
                            else if (beacon.beaconType == "direction") { //not needed anymore, TODO: remove
                                setDirectionNotificationData(beacon)
                                toggleNeedleViewElements(false)
                                toggleDirectionNotificationBoxElements(true)
                                notificationLock = true
                                vibrator.vibrate(1000)

                                Handler().postDelayed({
                                    notificationLock = false
                                    toggleDirectionNotificationBoxElements(false)
                                    toggleNeedleViewElements(true)
                                }, 10000)
                            }

                        }
                    }

                    if (it.eventType == DevicePosOrientEvent.GEOFENCE_ENTER_EVENT) {
                        val geofenceMetadata = it.proximiEvent?.geofence?.metadata
                        if (geofenceMetadata != null) {
                            Log.i(tag, "Geofence Enter! Time - ${geofenceMetadata["time"]} --- Tag --- ${geofenceMetadata["tag"]}")
                            timeToGate.text = "${geofenceMetadata["time"]} min to gate"
                        }

                    }
                }
    }

    //fun launchTicketActivity(view: View) {
    //    toast("Launching Ticket Info")
    //    val intent = Intent(this, TicketInfoActivity::class.java)
    //    startActivity(intent)
    //}

    /**
     * Update the interface based on button input
     * If a notification is shown, switch back to default screen
     * Otherwise to ticket info
     */
    fun updateView(view: View) {
        if (mode == "big_notification") {
            notificationLock = false
            toggleBigNotificationBoxElements(false)
            mode = "default"
        } else if (mode == "small_notification") {
            notificationLock = false
            toggleSmallNotificationBoxElements(false)
            toggleNeedleViewElements(true)
            timeline.setImageResource(R.drawable.timeline_blue)
            container.setImageResource(R.drawable.background)
            mode = "default"
        } else {
            val intent = Intent(this, TicketInfoActivity::class.java)
            startActivity(intent)
        }
    }

    //fun buttonBlink() {
    //    button.setBackgroundResource(R.drawable.button2)
    //    Handler().postDelayed({
    //        button.setBackgroundResource(R.drawable.button)
    //    }, 500)
    //}

    override fun onResume() {
        super.onResume()
        toggleTicketBoxElements(true)
        toggleNotificatioBoxElements(false)

        flightInfoObservableSwitch.onNext(true)
        proximiObservableSwitch.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        flightInfoObservableSwitch.onNext(false)
        proximiObservableSwitch.onNext(false)
    }

    private fun updateTicketData(ticket: AirlineTicket) {
        boardingTime.text = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.boardingTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                .toString("HH:mm")
        flightTime.text = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.departureTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                .toString("HH:mm")
        //TODO: calc boardingTime - current time
        //val timeToBoarding =
        timeUntilBoarding.text = "25\nmin" //timeUntilBoarding
        gate.text = ticket.gate
        flightNumber.text = ticket.flightNumber
        //sourceDestination.text = "${ticket.source} → ${ticket.destination}"
        time.text = LocalDateTime().toString("HH:mm")
    }

    private fun toggleTicketBoxElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        //ticketInfoBoxHeader.visibility = viewVisibility
        //sourceDestination.visibility = viewVisibility
        flightNumber.visibility = viewVisibility
        gateText.visibility = viewVisibility
        boardingTimeText.visibility = viewVisibility
        gate.visibility = viewVisibility
        boardingTime.visibility = viewVisibility
        flightTime.visibility = viewVisibility
        timeUntilBoarding.visibility = viewVisibility
        planeIcon.visibility = viewVisibility
    }

    private fun toggleNotificatioBoxElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        //notificationIcon.visibility = viewVisibility
        //notificationText.visibility = viewVisibility
    }

    /**
     * Sets the visibility of visuals for big notification
     */
    private fun toggleBigNotificationBoxElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        bigNotiBackground.visibility = viewVisibility
        bigNotificationText.visibility = viewVisibility
        bigNotificationIcon.visibility = viewVisibility
    }

    /**
     * Sets the visibility of visuals for small notification
     */
    private fun toggleSmallNotificationBoxElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        smallNotificationText.visibility = viewVisibility
        smallNotificationIcon.visibility = viewVisibility
    }

    /**
     * Not needed anymore
     * //TODO: remove
     */
    private fun setNotificationData(beacon: Beacon) {
        //notificationText.text = beacon.text
        //notificationIcon.setImageResource(resources.getIdentifier(beacon.icon, "drawable", packageName))
    }

    /**
     * Sets visual elements for a big notification
     * such as gate change, go to gate reminder etc.
     */
    private fun setBigNotificationData(beacon: Beacon) {
        bigNotiBackground.setImageResource(R.drawable.big_noti_background)
        bigNotificationText.text = beacon.text
        bigNotificationIcon.setImageResource(R.drawable.big_notification)
    }

    /**
     * Sets visual elements for a big notification
     * such as gate change, go to gate reminder etc.
     */
    private fun setSmallNotificationData(beacon: Beacon) {
        container.setImageResource(R.drawable.small_noti_background)
        timeline.setImageResource(R.drawable.timeline_green)
        smallNotificationText.text = beacon.text
        smallNotificationIcon.setImageResource(resources.getIdentifier(beacon.icon, "drawable", packageName))
    }

    /**
     * Not needed anymore
     * //TODO: remove
     */
    private fun setDirectionNotificationData(beacon: Beacon) {
        //directionNotificationText.text = beacon.text
        //directionNotificationIcon.setImageResource(resources.getIdentifier(beacon.icon, "drawable", packageName))
    }

    /**
     * Not needed anymore
     * //TODO: remove
     */
    private fun toggleNeedleViewElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        gateText.visibility = viewVisibility
        gate.visibility = viewVisibility
        needle.visibility = viewVisibility
        timeToGate.visibility = viewVisibility
    }

    /**
     * Not needed anymore
     * //TODO: remove
     */
    private fun toggleDirectionNotificationBoxElements(visible: Boolean) {
        val viewVisibility = getViewVisibility(visible)

        //directionNotification.visibility = viewVisibility
        //directionNotificationText.visibility = viewVisibility
        //directionNotificationIcon.visibility = viewVisibility
    }

    private fun getViewVisibility(visible: Boolean): Int {
        var viewVisibility = View.VISIBLE
        if (!visible)
            viewVisibility = View.INVISIBLE

        return viewVisibility
    }
}
