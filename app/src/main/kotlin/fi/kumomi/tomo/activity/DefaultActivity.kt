package fi.kumomi.tomo.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.R
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.flowable.DeviceOrientationFlowable
import fi.kumomi.tomo.flowable.ProximiEventsFlowable
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.Beacon
import fi.kumomi.tomo.model.ProximiEvent
import fi.kumomi.tomo.model.ProximiLocation
import fi.kumomi.tomo.observable.AirlineTicketObservable
import fi.kumomi.tomo.observable.NeedleDirectionObservable
import fi.kumomi.tomo.util.RadiansToDegrees
import io.proximi.proximiiolibrary.ProximiioAPI
import io.proximi.proximiiolibrary.ProximiioGeofence
import io.proximi.proximiiolibrary.ProximiioOptions
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_default_screen.*
import org.joda.time.*
import org.joda.time.format.ISODateTimeFormat
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DefaultActivity : AppCompatActivity() {
    private var airlineTicketObservableSwitch = PublishSubject.create<Boolean>()
    private val proximiFlowableSubject = PublishProcessor.create<Boolean>()
    private val needleDirectionObservableSubject = PublishSubject.create<Boolean>()
    private val orientationFlowableSubject = PublishProcessor.create<Boolean>()

    private var proximiApi: ProximiioAPI? = null
    private var notificationLock = false
    private var airlineTicketDataOverride = false
    private var currentLocationFromProximi = false
    private var reachedGate = false

    // Default time and navigation text before we see any navigation beacon
    private var timeToGateText: String? = "30"
    private var navigationText: String? = ""
    private var geofenceBearing: Double = 0F.toDouble()
    private var currentNavigationTextState = "time"
    private var navigationTextToggleHandler: Handler? = null
    private var navigationTextToggleRunnable: Runnable? = null

    private var mode = "default" //default, small_notification or big_notification
    private var bigNotificationMediaPlayer: MediaPlayer? = null
    private var smallNotificationMediaPlayer: MediaPlayer? = null

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
                .repeatWhen { it.delay(18, TimeUnit.MILLISECONDS) }

        airlineTicketObservableSwitch
                .switchMap { if(it) airlineTicketObservable else Observable.never() }
                .subscribe {
                    if (!airlineTicketDataOverride)
                        app.ticket = it
                    updateTicketData(it)
                }

        // Process proximi events - ONLY geofence now
        proximiFlowableSubject
                .switchMap { if(it) proximiFlowable else Flowable.never() }
                .subscribe {

                    if (it.eventType == ProximiEvent.GEOFENCE_ENTER_EVENT) {
                        val geofenceMetadata = it.geofence?.metadata

                        if (geofenceMetadata != null) {
                            Log.i(TAG, "Geofence Enter! Time - ${geofenceMetadata["time"]} --- Tag --- ${geofenceMetadata["text"]} --- Bearing --- ${geofenceMetadata["bearing"]}")

                            processNavigationalGeofence(geofenceMetadata)

                            when (geofenceMetadata["notification_type"] as String) {
                                "security" -> processSecurityGeofence(geofenceMetadata)
                                "big", "small" -> {
                                    if (!notificationLock)
                                        processNotificationGeofence(it.geofence)
                                }
                                else -> Log.i(TAG, "Not a notification beacon")
                            }
                        }
                    }


                    // Updating current position from proximi, and setting a flag that location is coming from proximi
                    // if (it.eventType == ProximiEvent.POSITION_EVENT) updateCurrentPosition(it.location)

                    // All beacons processing
                    // if (it.eventType == ProximiEvent.BEACON_FOUND_EVENT && app.apiBeacons.containsKey(it.beacon?.name)) {
                        // val apiBeacon = app.apiBeacons[it.beacon?.name]

                        // notifications beacon processing - when any new notification type is added, code goes here
                        // if ((apiBeacon?.beaconType == "big_notification" || apiBeacon?.beaconType == "small_notification") &&
                        //        !notificationLock) processNotificationBeacon(apiBeacon)

                        // navigation beacon processing
                        // if (apiBeacon?.beaconType == "navigation") processNavigationBeacon(apiBeacon)

                        // security beacon processing
                        // if (apiBeacon?.beaconType == "security") processSecurityBeacon(apiBeacon)

                        // gate change beacon processing - update gate number and stop polling ticket data
                        // if (apiBeacon?.beaconType == "gate_change") processGateChangeBeacon(apiBeacon)
                    // }
                }

        orientationFlowableSubject
                .switchMap { if(it) orientationFlowable else Flowable.never() }
                .subscribe {
                    if (it.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
                        app.acceleroWindow0.addValue(it.values[0].toDouble())
                        app.acceleroWindow1.addValue(it.values[1].toDouble())
                        app.acceleroWindow2.addValue(it.values[2].toDouble())

                        app.accelerometerReading[0] = app.acceleroWindow0.mean.toFloat()
                        app.accelerometerReading[1] = app.acceleroWindow1.mean.toFloat()
                        app.accelerometerReading[2] = app.acceleroWindow2.mean.toFloat()
                    }

                    if (it.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
                        app.magnetoWindow0.addValue(it.values[0].toDouble())
                        app.magnetoWindow1.addValue(it.values[1].toDouble())
                        app.magnetoWindow2.addValue(it.values[2].toDouble())

                        app.magnetometerReading[0] = app.magnetoWindow0.mean.toFloat()
                        app.magnetometerReading[1] = app.magnetoWindow1.mean.toFloat()
                        app.magnetometerReading[2] = app.magnetoWindow2.mean.toFloat()
                    }

                    //Todo Calculate Azimuth so that it is not affected by pitch and roll.
                    //https://stackoverflow.com/questions/15649684/how-should-i-calculate-azimuth-pitch-orientation-when-my-android-device-isnt

                    SensorManager.getRotationMatrix(app.rotationMatrix, null, app.accelerometerReading, app.magnetometerReading)

                    // In orientation angles calculated difference between magnetic north and device current orientation
                    // is first element
                    SensorManager.getOrientation(app.rotationMatrix, app.orientationAngles)
                    var azimuth = RadiansToDegrees.convert(app.orientationAngles[0].toDouble())
                    azimuth = (azimuth + 360) % 360

//                    val currentLocationObj     = Location("current")
//                    val destinationLocationObj = Location("destination")
//
//                    if (app.currentPosition["lat"] != null) {
//                        currentLocationObj.latitude = app.currentPosition["lat"]!!
//                        currentLocationObj.longitude = app.currentPosition["lon"]!!
//                    } else {
//                        currentLocationObj.latitude = app.bootstrapOrigin["lat"]!!
//                        currentLocationObj.longitude = app.bootstrapOrigin["lon"]!!
//                    }
//
//                    if (app.destinationPosition["lat"] != null) {
//                        destinationLocationObj.latitude = app.destinationPosition["lat"]!!
//                        destinationLocationObj.longitude = app.destinationPosition["lon"]!!
//                    } else {
//                        destinationLocationObj.latitude = app.bootstrapDestination["lat"]!!
//                        destinationLocationObj.longitude = app.bootstrapDestination["lon"]!!
//                    }

//                        val geoField = GeomagneticField(currentLocationObj.latitude.toFloat(),
//                                currentLocationObj.longitude.toFloat(), currentLocationObj.altitude.toFloat(),
//                                System.currentTimeMillis())

//                    Log.i(DefaultActivity.TAG, "Azimuth - $azimuth")

                    // this is angle for a straight between current pos to destination pos w.r.t north pole line from
                    // current position
//                    val bearingTo = currentLocationObj.bearingTo(destinationLocationObj)

//                         if (bearingTo < 0)
//                             bearingTo += 360

//                        if (rotateAngle < 0)
//                            rotateAngle += 360

                    app.rotateAngle = azimuth - geofenceBearing
                }

        needleDirectionObservableSubject
                .switchMap { if(it) needleDirectionObservable else Observable.never() }
                .subscribe {
                    var correctedDirection = it + 90

                    if (correctedDirection > 360) {
                        correctedDirection -= 360
                    }

                    correctedDirection = 360 - correctedDirection

//                    Log.i(TAG, correctedDirection.toString())

                    // Update only on angle difference of more than x from previous angle
                    if (abs(app.prevRotateAngle - correctedDirection) > 5) {
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

                        an.duration = 17
                        an.repeatCount = 0
                        an.fillAfter = true

                        if (!reachedGate)
                            needle.startAnimation(an)
                        else
                            needle.visibility = View.INVISIBLE

                        app.prevRotateAngle = correctedDirection
                    }
                }

        // Code for fade in and fade out toggling navigation text
        navigationTextToggleHandler = Handler()
        navigationTextToggleRunnable = object: Runnable {
            override fun run() {
                val fadeIn = AlphaAnimation(0.0f, 1.0f)
                val fadeOut = AlphaAnimation(1.0f, 0.0f)
                // duration of fade in fade out animation only
                fadeOut.duration = 500
                fadeIn.duration  = 500

                timeToGate.startAnimation(fadeOut)

                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationEnd(animation: Animation?) {
                        if (currentNavigationTextState == "time") {
                            timeToGate.text = navigationText
                            currentNavigationTextState = "text"
                        } else {
                            timeToGate.text = "$timeToGateText min to gate"
                            currentNavigationTextState = "time"
                        }

                        timeToGate.startAnimation(fadeIn)
                    }

                    override fun onAnimationRepeat(animation: Animation?) = Unit
                    override fun onAnimationStart(animation: Animation?) = Unit
                })

                // duration text in on screen before toggling out for other text
                navigationTextToggleHandler?.postDelayed(this, 5000)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        toggleTicketBoxElements(true)
        if (!airlineTicketDataOverride)
            airlineTicketObservableSwitch.onNext(true)
        proximiFlowableSubject.onNext(true)
        orientationFlowableSubject.onNext(true)
        needleDirectionObservableSubject.onNext(true)

        if (!reachedGate)
            navigationTextToggleHandler?.post(navigationTextToggleRunnable)
    }

    override fun onPause() {
        super.onPause()

        if (!airlineTicketDataOverride)
            airlineTicketObservableSwitch.onNext(false)
        proximiFlowableSubject.onNext(false)
        orientationFlowableSubject.onNext(false)
        needleDirectionObservableSubject.onNext(false)
        navigationTextToggleHandler?.removeCallbacks(navigationTextToggleRunnable)
    }

    private fun updateTicketData(ticket: AirlineTicket) {
        val currentTime = LocalDateTime().toDateTime()
        val boardingDateTime = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.boardingTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
        val flightDateTime = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.departureTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
        val timeToBoarding = Minutes.minutesBetween(currentTime, boardingDateTime).minutes

        flightTime.text = flightDateTime.toString("HH:mm")
        boardingTime.text = boardingDateTime.toString("HH:mm")
        timeUntilBoarding.text = "$timeToBoarding\nmin"
        gate.text = ticket.gate
        flightNumber.text = ticket.flightNumber
        time.text = currentTime.toString("HH:mm")
    }

    fun updateView(view: View) {
        bigNotificationMediaPlayer?.stop()
        smallNotificationMediaPlayer?.stop()
        button.clearAnimation()

        when (mode) {
            "big_notification" -> {
                notificationLock = false
                toggleBigNotificationBoxElements(false)
                mode = "default"
            }
            "small_notification" -> {
                notificationLock = false
                toggleSmallNotificationBoxElements(false)
                timeline.setImageResource(R.drawable.timeline_blue)
                container.setImageResource(R.drawable.background)
                mode = "default"
            }
            else -> {
                val intent = Intent(this, TicketInfoActivity::class.java)
                startActivity(intent)
            }
        }
    }

    // Current position derived from proximi sdk
    private fun updateCurrentPosition(proximiLocation: ProximiLocation?) {
        // comment the method body to disable feature
      //  val app = applicationContext as TomoApplication
      //  app.currentPosition["lat"] = proximiLocation?.lat
      //  app.currentPosition["lon"] = proximiLocation?.lon
      //  currentLocationFromProximi = true
      //  Log.i(TAG, "proximi position coming")
    }

    private fun processNotificationBeacon(notificationBeacon: Beacon?) {
        var seenNotificationBeacon = false
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val app = applicationContext as TomoApplication

        if (app.seenBeacons.containsKey(notificationBeacon?.name)) {
            val seenTime = app.seenBeacons[notificationBeacon?.name]
            val currentTime = DateTime()
            val interval = Interval(seenTime, currentTime)

            Log.i(TAG, "This Beacon was last seen at ${seenTime.toString()}")
            Log.i(TAG, "Time since we saw this beacon ${interval.toDuration().standardMinutes}")

            // compares minutes since when beacon was seen.
            // to compare seconds use standardSeconds
            if (interval.toDuration().standardSeconds < 120) {
                seenNotificationBeacon = true
            }
        }

        if (!seenNotificationBeacon) {
            Log.i(TAG, "We have never seen this beacon until now - Storing ref")
            app.seenBeacons[notificationBeacon?.name] = DateTime()

            if (notificationBeacon!!.beaconType == "big_notification") {
                mode = "big_notification"
                notificationLock = true

                // Showing Big Notification after 1 minute from beacon detect
                Handler().postDelayed({
                    setBigNotificationData(notificationBeacon)

                    toggleBigNotificationBoxElements(true)
                    vibrator.vibrate(3000)

                    // Play sound
                    bigNotificationMediaPlayer = MediaPlayer.create(this, R.raw.big_sound)
                    bigNotificationMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    bigNotificationMediaPlayer?.isLooping = true
                    bigNotificationMediaPlayer?.start()

                    // Flash button
                    val animation = AlphaAnimation(1F, 0F)
                    animation.duration = 200
                    animation.interpolator = LinearInterpolator()
                    animation.repeatCount = Animation.INFINITE
                    animation.repeatMode = Animation.REVERSE
                    button.startAnimation(animation)
                }, 60000)
            }

            //TODO Toggle needle and time to gate and gate
            if (notificationBeacon.beaconType == "small_notification") {
                mode = "small_notification"
                setSmallNotificationData(notificationBeacon)
                toggleSmallNotificationBoxElements(true)
                notificationLock = true
                vibrator.vibrate(3000)

                // Play sound
                smallNotificationMediaPlayer = MediaPlayer.create(this, R.raw.small_sound)
                smallNotificationMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                smallNotificationMediaPlayer?.isLooping = true
                smallNotificationMediaPlayer?.start()

                // Flash button
                val animation = AlphaAnimation(1F, 0F)
                animation.duration = 200
                animation.interpolator = LinearInterpolator()
                animation.repeatCount = Animation.INFINITE
                animation.repeatMode = Animation.REVERSE
                button.startAnimation(animation)
            }
        }
    }

    private fun processNotificationGeofence(geofence: ProximiioGeofence?) {
        var seenNotificationGeofence = false
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val app = applicationContext as TomoApplication

        if (app.seenGeofences.containsKey(geofence?.name)) {
            val seenTime = app.seenBeacons[geofence?.name]
            val currentTime = DateTime()
            val interval = Interval(seenTime, currentTime)

            Log.i(TAG, "This geofence was last seen at ${seenTime.toString()}")
            Log.i(TAG, "Time since we saw this geofence ${interval.toDuration().standardMinutes}")

            // compares minutes since when beacon was seen.
            // to compare seconds use standardSeconds
            if (interval.toDuration().standardSeconds < 120) {
                seenNotificationGeofence = true
            }
        }

        if (!seenNotificationGeofence) {
            Log.i(TAG, "We have never seen this geofence until now - Storing ref")
            app.seenGeofences[geofence?.name] = DateTime()

            val geofenceMetadata = geofence?.metadata
            val geofenceNotificationType = geofenceMetadata?.get("notification_type") as String

            when (geofenceNotificationType) {
                "big" -> {
                    mode = "big_notification"
                    notificationLock = true

                    // Showing Big Notification after 1 minute from beacon detect
                    Handler().postDelayed({
                        setBigNotificationDataFromGeofence(geofenceMetadata)

                        toggleBigNotificationBoxElements(true)
                        vibrator.vibrate(3000)

                        // Play sound
                        bigNotificationMediaPlayer = MediaPlayer.create(this, R.raw.big_sound)
                        bigNotificationMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        bigNotificationMediaPlayer?.isLooping = true
                        bigNotificationMediaPlayer?.start()

                        // Flash button
                        val animation = AlphaAnimation(1F, 0F)
                        animation.duration = 200
                        animation.interpolator = LinearInterpolator()
                        animation.repeatCount = Animation.INFINITE
                        animation.repeatMode = Animation.REVERSE
                        button.startAnimation(animation)
                    }, 60000)
                }
                "small" -> {
                    mode = "small_notification"
                    setSmallNotificationDataFromGeofence(geofenceMetadata)
                    toggleSmallNotificationBoxElements(true)
                    notificationLock = true
                    vibrator.vibrate(3000)

                    // Play sound
                    smallNotificationMediaPlayer = MediaPlayer.create(this, R.raw.small_sound)
                    smallNotificationMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    smallNotificationMediaPlayer?.isLooping = true
                    smallNotificationMediaPlayer?.start()

                    // Flash button
                    val animation = AlphaAnimation(1F, 0F)
                    animation.duration = 200
                    animation.interpolator = LinearInterpolator()
                    animation.repeatCount = Animation.INFINITE
                    animation.repeatMode = Animation.REVERSE
                    button.startAnimation(animation)
                }

            }
        }
    }

    private fun processNavigationBeacon(navigationBeacon: Beacon?) {
        val app = applicationContext as TomoApplication

        if (!currentLocationFromProximi) {
            app.currentPosition["lat"] = navigationBeacon?.latitude?.toDouble()
            app.currentPosition["lon"] = navigationBeacon?.longitude?.toDouble()
        }

        val nextBeacon = app.apiBeacons[navigationBeacon?.nextBeacon]

        // When reached gate hide needle show "you have reached"
        if (nextBeacon == null) {
            reachedGate = true
            navigationTextToggleHandler?.removeCallbacks(navigationTextToggleRunnable)
            reachedText.visibility = View.VISIBLE
            needle.visibility = View.INVISIBLE
            timeToGate.visibility = View.INVISIBLE

            app.destinationPosition["lat"] = navigationBeacon?.latitude?.toDouble()
            app.destinationPosition["lon"] = navigationBeacon?.longitude?.toDouble()
        } else {
            app.destinationPosition["lat"] = nextBeacon.latitude?.toDouble()
            app.destinationPosition["lon"] = nextBeacon.longitude?.toDouble()
        }

        timeToGateText = navigationBeacon?.timeToGate
        navigationText = navigationBeacon?.text
    }

    private fun processNavigationalGeofence(geofenceMetadata: JSONObject) {
        if (geofenceMetadata["tag"] == "gate") {
            reachedGate = true
            navigationTextToggleHandler?.removeCallbacks(navigationTextToggleRunnable)
            reachedText.visibility = View.VISIBLE
            needle.visibility = View.INVISIBLE
            timeToGate.visibility = View.INVISIBLE
        } else {
            timeToGateText = geofenceMetadata["time"] as String
            navigationText = geofenceMetadata["text"] as String
            geofenceBearing = (geofenceMetadata["bearing"] as String).toDouble()
        }
    }

    private fun processSecurityBeacon(securityBeacon: Beacon?) {
        airlineTicketDataOverride = true
        airlineTicketObservableSwitch.onNext(false)
        val app = applicationContext as TomoApplication

        app.ticket?.boardingTime  = securityBeacon?.boardingTime
        app.ticket?.departureTime = securityBeacon?.departureTime
        updateTicketData(app.ticket!!)
    }

    private fun processSecurityGeofence(geofenceMetadata: JSONObject) {
        airlineTicketDataOverride = true
        airlineTicketObservableSwitch.onNext(false)
        val app = applicationContext as TomoApplication

        app.ticket?.boardingTime = geofenceMetadata["boarding_time"] as String
        app.ticket?.departureTime = geofenceMetadata["departure_time"] as String
        updateTicketData(app.ticket!!)
    }

    private fun processGateChangeBeacon(gateChangeBeacon: Beacon?) {
        airlineTicketDataOverride = true
        airlineTicketObservableSwitch.onNext(false)
        val app = applicationContext as TomoApplication

        app.ticket?.gate = gateChangeBeacon?.gate
        updateTicketData(app.ticket!!)
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

    private fun setBigNotificationDataFromGeofence(geofenceMetadata: JSONObject) {
        bigNotiBackground.setImageResource(R.drawable.big_noti_background)
        bigNotificationText.text = geofenceMetadata["text"] as String
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

    private fun setSmallNotificationDataFromGeofence(geofenceMetadata: JSONObject) {
        container.setImageResource(R.drawable.small_noti_background)
        timeline.setImageResource(R.drawable.timeline_green)
        smallNotificationText.text = geofenceMetadata["text"] as String
        smallNotificationIcon.setImageResource(resources.getIdentifier(geofenceMetadata["icon"] as String, "drawable", packageName))
    }

    private fun getViewVisibility(visible: Boolean): Int {
        var viewVisibility = View.VISIBLE
        if (!visible)
            viewVisibility = View.INVISIBLE

        return viewVisibility
    }

    companion object {
        private const val TAG = "DefaultActivity"
    }
}
