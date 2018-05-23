package fi.kumomi.tomo.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import android.widget.ImageView.ScaleType
import android.graphics.drawable.BitmapDrawable
import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.location.Location
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import fi.kumomi.tomo.model.ProximiEvent
import fi.kumomi.tomo.observable.NeedleDirectionObservable
import fi.kumomi.tomo.util.RadiansToDegrees
import io.reactivex.processors.PublishProcessor
import org.jetbrains.anko.toast
import kotlin.math.abs

class TicketInfoActivity : AppCompatActivity() {
    private var airlineTicketObservableSubject = PublishSubject.create<Boolean>()
    private var needleDirectionObservableSubject = PublishSubject.create<Boolean>()
    private var proximiFlowableSubject = PublishProcessor.create<Boolean>()
    private var orientationFlowableSubject = PublishProcessor.create<Boolean>()
    private var proximiApi: ProximiioAPI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val app = applicationContext as TomoApplication

        // Load ticket data from a global variable in app
        if (app.ticket != null)
            updateTicketData(app.ticket!!)

        // Initial Proximi API
        val proximiOptions = ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)
        proximiApi = ProximiioAPI(TAG, this, proximiOptions)
        proximiApi?.setAuth(Config.PROXIMI_API_KEY)
        proximiApi?.setActivity(this)

        val airlineTicketObservable = AirlineTicketObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        val proximiEventsFlowable = ProximiEventsFlowable.create(proximiApi)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())

        val orientationFlowable = DeviceOrientationFlowable.create(sensorManager)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())

        // Polling needle direction which is being updated in background thread at close to 60Hz
        val needleDirectionObservable = NeedleDirectionObservable.create(applicationContext as TomoApplication)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(40, TimeUnit.MILLISECONDS) }

        // update current position and destination position based on navigation beacon seen
        proximiFlowableSubject
                .switchMap { if(it) proximiEventsFlowable else Flowable.never() }
                .subscribe {
                    val currentPosition = app.currentPosition
                    val destinationPosition = app.destinationPosition

                    if (it.eventType == ProximiEvent.BEACON_FOUND_EVENT &&
                            app.beacons.containsKey(it.beacon?.name) &&
                            app.beacons[it.beacon?.name]?.beaconType == "navigation") {

                        val beacon = app.beacons[it.beacon?.name]
                        currentPosition["lat"]  = beacon?.latitude?.toDouble()
                        currentPosition["long"] = beacon?.longitude?.toDouble()

                        // TODO: Need to handle case when next_beacon is null - that is at Gate
                        val nextBeacon = app.beacons[beacon?.nextBeacon]
                        destinationPosition["lat"]  = nextBeacon?.latitude?.toDouble()
                        destinationPosition["long"] = nextBeacon?.longitude?.toDouble()
                    }
                }

        // Needle direction stream processing
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

                        Log.i(TAG, "Azimuth - $azimuth")

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

        airlineTicketObservableSubject
                .switchMap { if(it) airlineTicketObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
                }

        needleDirectionObservableSubject
                .switchMap { if(it) needleDirectionObservable else Observable.never() }
                .subscribe {
                    var correctedDirection = it + 90

                    if (correctedDirection > 360) {
                        correctedDirection -= 360
                    }

                    correctedDirection = 360 - correctedDirection

                    Log.i(TAG, correctedDirection.toString())
                    rotationAngleText.text = correctedDirection.toString()

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

    override fun onResume() {
        super.onResume()
        airlineTicketObservableSubject.onNext(true)
        proximiFlowableSubject.onNext(true)
        orientationFlowableSubject.onNext(true)
        needleDirectionObservableSubject.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        airlineTicketObservableSubject.onNext(false)
        proximiFlowableSubject.onNext(false)
        orientationFlowableSubject.onNext(false)
        needleDirectionObservableSubject.onNext(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        proximiApi?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        proximiApi?.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        imageView.scaleType = ScaleType.CENTER
    }

    fun launchDefaultActivity(view: View) {
        toast("Launch Default Activity")
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
        flightNumber.text = ticket.flightNumber
        sourceDestination.text = "${ticket.source} → ${ticket.destination}"
        time.text = LocalDateTime().toString("HH:mm")
    }

    companion object {
        private const val TAG = "TicketInfoActivity"
    }
}
