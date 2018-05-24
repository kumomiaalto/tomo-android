package fi.kumomi.tomo.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.R
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.flowable.DeviceOrientationFlowable
import fi.kumomi.tomo.flowable.ProximiEventsFlowable
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.Beacon
import fi.kumomi.tomo.model.ProximiEvent
import fi.kumomi.tomo.observable.AirlineTicketObservable
import fi.kumomi.tomo.observable.NeedleDirectionObservable
import fi.kumomi.tomo.util.RadiansToDegrees
import io.proximi.proximiiolibrary.ProximiioAPI
import io.proximi.proximiiolibrary.ProximiioOptions
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_default_screen.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Interval
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import java.util.*
import java.util.concurrent.TimeUnit


class DefaultActivity : AppCompatActivity() {
    private var airlineTicketObservableSwitch = PublishSubject.create<Boolean>()
    // All proximi events come to proximiFlowableSwitch
    private val proximiFlowableSubject = PublishProcessor.create<Boolean>()
    private val needleDirectionObservableSubject = PublishSubject.create<Boolean>()
    private val orientationFlowableSubject = PublishProcessor.create<Boolean>()
    private var proximiApi: ProximiioAPI? = null
    private var notificationLock = false
    private var mode = "big_notification" //default, small_notification or big_notification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_default_screen)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val app = applicationContext as TomoApplication
        if (app.ticket != null)
            updateTicketData(app.ticket!!)

        val proximiOptions = ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)
        proximiApi = ProximiioAPI(TAG, this, proximiOptions)
        proximiApi?.setAuth(Config.PROXIMI_API_KEY)
        proximiApi?.setActivity(this)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        //makes call to API every 30s
        val airlineTicketObservable = AirlineTicketObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        val proximiFlowable = ProximiEventsFlowable.create(proximiApi)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

        val orientationFlowable = DeviceOrientationFlowable.create(sensorManager)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())

        val needleDirectionObservable = NeedleDirectionObservable.create(applicationContext as TomoApplication)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(40, TimeUnit.MILLISECONDS) }

        airlineTicketObservableSwitch
                .switchMap { if(it) airlineTicketObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
                }

        // Process proximi events - geofence,  seeing beacons
        proximiFlowableSubject
                .switchMap { if(it) proximiFlowable else Flowable.never() }
                .subscribe {

                    // Logic to filter beacons for notification and showing the notification
                    if (!notificationLock && it.eventType == ProximiEvent.BEACON_FOUND_EVENT &&
                            app.beacons.containsKey(it.beacon?.name) &&
                            app.beacons[it.beacon?.name]?.beaconType != "navigation") {

                        var seenBeacon = false

                        if (app.seenBeacons.containsKey(it.beacon?.name)) {
                            val seenTime = app.seenBeacons[it.beacon?.name]
                            val currentTime = DateTime()
                            val interval = Interval(seenTime, currentTime)

                            Log.i(TAG, "This Beacon was last seen at ${seenTime.toString()}")
                            Log.i(TAG, "Time since we saw this beacon ${interval.toDuration().standardMinutes}")

                            // compares minutes since when beacon was seen.
                            // to compare seconds use standardSeconds
                            if (interval.toDuration().standardSeconds < 120) {
                                seenBeacon = true
                            }
                        }

                        if (!seenBeacon) {
                            Log.i(TAG, "We have never seen this beacon until now - Storing ref")
                            app.seenBeacons[it.beacon?.name] = DateTime()

                            val beacon = app.beacons[it.beacon?.name]

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
                        }
                    }

                    // Logic to filter navigation beacons and setting origin and destination
                    if (it.eventType == ProximiEvent.BEACON_FOUND_EVENT &&
                            app.beacons.containsKey(it.beacon?.name) &&
                            app.beacons[it.beacon?.name]?.beaconType == "navigation") {

                        val currentPosition = app.currentPosition
                        val destinationPosition = app.destinationPosition
                        val beacon = app.beacons[it.beacon?.name]

                        currentPosition["lat"]  = beacon?.latitude?.toDouble()
                        currentPosition["long"] = beacon?.longitude?.toDouble()

                        // TODO: Need to handle case when next_beacon is null - that is at Gate
                        val nextBeacon = app.beacons[beacon?.nextBeacon]
                        destinationPosition["lat"]  = nextBeacon?.latitude?.toDouble()
                        destinationPosition["long"] = nextBeacon?.longitude?.toDouble()
                    }

                    if (it.eventType == ProximiEvent.GEOFENCE_ENTER_EVENT) {
                        val geofenceMetadata = it.geofence?.metadata
                        if (geofenceMetadata != null) {
                            Log.i(TAG, "Geofence Enter! Time - ${geofenceMetadata["time"]} --- Tag --- ${geofenceMetadata["tag"]}")
                            timeToGate.text = "${geofenceMetadata["time"]} min to gate"
                        }

                    }
                }

        orientationFlowableSubject
                .switchMap { if(it) orientationFlowable else Flowable.never() }
                .subscribe {
                    if (app.startGeofence != null) {
                        if (it.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
                            app.accelerometerReading[0] = app.lowPassAlpha * app.accelerometerReading[0] + (1 - app.lowPassAlpha) * it.values[0]
                            app.accelerometerReading[1] = app.lowPassAlpha * app.accelerometerReading[1] + (1 - app.lowPassAlpha) * it.values[1]
                            app.accelerometerReading[2] = app.lowPassAlpha * app.accelerometerReading[2] + (1 - app.lowPassAlpha) * it.values[2]
                        }

                        if (it.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
                            app.magnetometerReading[0] = app.lowPassAlpha * app.magnetometerReading[0] + (1 - app.lowPassAlpha) * it.values[0]
                            app.magnetometerReading[1] = app.lowPassAlpha * app.magnetometerReading[1] + (1 - app.lowPassAlpha) * it.values[1]
                            app.magnetometerReading[2] = app.lowPassAlpha * app.magnetometerReading[2] + (1 - app.lowPassAlpha) * it.values[2]
                        }

                        SensorManager.getRotationMatrix(app.rotationMatrix, null, app.accelerometerReading, app.magnetometerReading)

                        // In orientation angles calculated difference between magnetic north and device current orientation
                        // is first element
                        SensorManager.getOrientation(app.rotationMatrix, app.orientationAngles)
                        var azimuth = RadiansToDegrees.convert(app.orientationAngles[0].toDouble())
                        azimuth = (azimuth + 360) % 360

                        val currentLocationObj     = Location("current")
                        val destinationLocationObj = Location("destination")

                        if (app.currentPosition["lat"] != null) {
                            currentLocationObj.latitude = app.currentPosition["lat"]!!
                            currentLocationObj.longitude = app.currentPosition["long"]!!

                            destinationLocationObj.latitude = app.destinationPosition["lat"]!!
                            destinationLocationObj.longitude = app.destinationPosition["long"]!!
                        } else {
                            currentLocationObj.latitude = app.bootstrapOrigin["lat"]!!
                            currentLocationObj.longitude = app.bootstrapOrigin["long"]!!

                            destinationLocationObj.latitude = app.bootstrapDestination["lat"]!!
                            destinationLocationObj.longitude = app.bootstrapDestination["long"]!!
                        }

//                        val geoField = GeomagneticField(currentLocationObj.latitude.toFloat(),
//                                currentLocationObj.longitude.toFloat(), currentLocationObj.altitude.toFloat(),
//                                System.currentTimeMillis())

                        Log.i(DefaultActivity.TAG, "Azimuth - $azimuth")

                        // this is angle for a straight between current pos to destination pos w.r.t north pole line from
                        // current position
                        val bearingTo = currentLocationObj.bearingTo(destinationLocationObj)

//                         if (bearingTo < 0)
//                             bearingTo += 360

//                        if (rotateAngle < 0)
//                            rotateAngle += 360

                        app.rotateAngle = azimuth - bearingTo
                    }
                }

        needleDirectionObservableSubject
                .switchMap { if(it) needleDirectionObservable else Observable.never() }
                .subscribe {
                    var correctedDirection = it + 90

                    if (correctedDirection > 360) {
                        correctedDirection -= 360
                    }

                    correctedDirection = 360 - correctedDirection

                    Log.i(DefaultActivity.TAG, correctedDirection.toString())

                    // Handling animation to not jump at 360 to 0 angle boundary
                    val an: RotateAnimation = if (app.prevRotateAngle > 330 && correctedDirection < 30) {
                        RotateAnimation((app.prevRotateAngle - 360).toFloat(), correctedDirection.toFloat(),
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f)
                    } else if (app.prevRotateAngle < 30 && correctedDirection > 330){
                        RotateAnimation(app.prevRotateAngle.toFloat(), (correctedDirection - 360).toFloat(),
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f)
                    } else {
                        RotateAnimation(app.prevRotateAngle.toFloat(), correctedDirection.toFloat(),
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f)
                    }

                    an.duration = 50
                    an.repeatCount = 0
                    an.fillAfter = true

                    needle.startAnimation(an)
                    app.prevRotateAngle = correctedDirection
                }
    }

    fun launchTicketActivity(view: View) {
        val intent = Intent(this, TicketInfoActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        toggleTicketBoxElements(true)
        toggleNotificatioBoxElements(false)

        airlineTicketObservableSwitch.onNext(true)
        proximiFlowableSubject.onNext(true)
        orientationFlowableSubject.onNext(true)
        needleDirectionObservableSubject.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        airlineTicketObservableSwitch.onNext(false)
        proximiFlowableSubject.onNext(false)
        orientationFlowableSubject.onNext(false)
        needleDirectionObservableSubject.onNext(false)
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

    private fun rotateImageView(imageView: ImageView, drawable: Int, rotate: Double) {
        var rotate = rotate

        // Get the width/height of the drawable
        val bitmapOrg = BitmapFactory.decodeResource(resources, drawable)
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        val width = bitmapOrg.width
        val height = bitmapOrg.height

        // Initialize a new Matrix
        val matrix = Matrix()

        // Decide on how much to rotate
        rotate %= 360

        // Actually rotate the image
        matrix.postRotate(rotate.toFloat(), width.toFloat(), height.toFloat())

        // recreate the new Bitmap via a couple conditions
        val rotatedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true)

        imageView.setImageDrawable(BitmapDrawable(resources, rotatedBitmap))
        imageView.scaleType = ImageView.ScaleType.CENTER
    }

    companion object {
        private const val TAG = "DefaultActivity"
    }
}
