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
import android.widget.ImageView
import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.observable.NeedleDirectionObservable
import fi.kumomi.tomo.util.RadiansToDegrees
import io.reactivex.processors.PublishProcessor
import org.jetbrains.anko.toast
import kotlin.math.abs


class TicketInfoActivity : AppCompatActivity() {
    private var flightInfoObservableSubject = PublishSubject.create<Boolean>()
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

        val flightInfoObservable = AirlineTicketObservable.create()
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

        val needleDirectionObservable = NeedleDirectionObservable.create(applicationContext as TomoApplication)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.MILLISECONDS) }

        proximiFlowableSubject
                .switchMap { if(it) proximiEventsFlowable else Flowable.never() }
                .subscribe {
                    val currentPosition = app.proximiPosition

                    if (it.eventType == DevicePosOrientEvent.POSITION_EVENT) {
                        currentPosition["lat"] = it.proximiEvent?.location?.lat
                        currentPosition["lng"] = it.proximiEvent?.location?.lon
                    }
                }

        // Needle direction stream processing code
        orientationFlowableSubject
                .switchMap { if(it) orientationFlowable else Flowable.never() }
                .subscribe {
                    if (app.startGeofence != null) {
                        if (it.sensorEvent?.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
                            System.arraycopy(it.sensorEvent?.values,
                                    0, app.accelerometerReading, 0, app.accelerometerReading.size)

                        if (it.sensorEvent?.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD))
                            System.arraycopy(it.sensorEvent?.values,
                                    0, app.magnetometerReading, 0, app.magnetometerReading.size)

                        SensorManager.getRotationMatrix(app.rotationMatrix, null, app.accelerometerReading, app.magnetometerReading)

                        // In orientation angles calculated difference between magnetic north and device current orientation
                        // is first element
                        SensorManager.getOrientation(app.rotationMatrix, app.orientationAngles)
                        var azimuth = RadiansToDegrees.convert(app.orientationAngles[0].toDouble())

                        val currentLocationObj = Location("current")
                        currentLocationObj.latitude = app.proximiPosition["lat"]!!
                        currentLocationObj.longitude = app.proximiPosition["lng"]!!

                        val destinationLocationObj = Location("destination")
                        destinationLocationObj.latitude = 90.0
                        destinationLocationObj.longitude = 0.0

                        val geoField = GeomagneticField(currentLocationObj.latitude.toFloat(),
                                currentLocationObj.longitude.toFloat(), currentLocationObj.altitude.toFloat(),
                                System.currentTimeMillis())

                        // Adjusts azimuth to have true north pole reference
                        azimuth -= geoField.declination

                        Log.i(TAG, "Azimuth - $azimuth")

                        // this is angle for a straight between current pos to destination pos w.r.t north pole line from
                        // current position
                        var bearingTo = currentLocationObj.bearingTo(destinationLocationObj)
                        if (bearingTo < 0)
                            bearingTo += 360

                        var rotateAngle = bearingTo - azimuth
                        if (rotateAngle < 0)
                            rotateAngle += 360

                        app.rotateAngleMovingWindow.addValue(rotateAngle)
                        app.rotateAngle = app.rotateAngleMovingWindow.mean
                    }
                }

        flightInfoObservableSubject
                .switchMap { if(it) flightInfoObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
                }

        needleDirectionObservableSubject
                .switchMap { if(it) needleDirectionObservable else Observable.never() }
                .subscribe {
                    var correctedDirection = it - 90
                    if (correctedDirection < 0) {
                        correctedDirection = 360 - correctedDirection
                    }

                    Log.i(TAG, correctedDirection.toString())
                    rotationAngleText.text = correctedDirection.toString()

                    if (abs(app.prevRotateAngle - correctedDirection) > 3) {
                        rotateImageView(needle, R.drawable.needle, correctedDirection)
                        app.prevRotateAngle = correctedDirection
                    }
                }
    }

    override fun onResume() {
        super.onResume()
        flightInfoObservableSubject.onNext(true)
        proximiFlowableSubject.onNext(true)
        orientationFlowableSubject.onNext(true)
        needleDirectionObservableSubject.onNext(true)
    }

    override fun onPause() {
        super.onPause()
        flightInfoObservableSubject.onNext(false)
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
        //BitmapDrawable bmd = new BitmapDrawable( rotatedBitmap );

        //imageView.setImageBitmap( rotatedBitmap );
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
