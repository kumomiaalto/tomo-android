package fi.kumomi.tomo.activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.Sensor
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
import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.observable.AirlineTicketObservable
import fi.kumomi.tomo.util.RadiansToDegrees
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
import android.location.Location
import android.support.v7.app.AppCompatDelegate
import android.view.View
import android.widget.ImageView
import org.jetbrains.anko.toast


class TicketInfoActivity : AppCompatActivity() {
    private val tag = "TicketActivity"
    private var flightInfoObservable: Observable<AirlineTicket>? = null
    private var flightInfoObservableSwitch = PublishSubject.create<Boolean>()
    private var proximiApi: ProximiioAPI? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var direction: Double = 0F.toDouble()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_info)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val app = applicationContext as TomoApplication
        if (app.ticket != null)
            updateTicketData(app.ticket!!)

        val proximiOptions = ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)
        proximiApi = ProximiioAPI(tag, this, proximiOptions)
        proximiApi?.setAuth(Config.PROXIMI_API_KEY)
        proximiApi?.setActivity(this)

        flightInfoObservable = AirlineTicketObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .repeatWhen { it.delay(30, TimeUnit.SECONDS) }
                .retryWhen { it.flatMap { Observable.timer(5, TimeUnit.SECONDS) } }

        val posOrientationFlowable = Flowable.merge(
                DeviceOrientationFlowable.create(sensorManager).subscribeOn(Schedulers.io()),
                ProximiEventsFlowable.create(proximiApi).subscribeOn(Schedulers.io())
        )

//        posOrientationFlowable.observeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    val currentPosition = app.proximiPosition
//
//                    if (it.eventType == DevicePosOrientEvent.BEACON_FOUND_EVENT) {
//                        Log.i(tag, "Beacon Found ${it.proximiEvent?.beacon?.mac}")
//                    }
////                    Log.i(tag, "event type ---- ${it.eventType}")
//
//                    if (it.eventType == DevicePosOrientEvent.POSITION_EVENT) {
//                        currentPosition["lat"] = it.proximiEvent?.location?.lat
//                        currentPosition["lng"] = it.proximiEvent?.location?.lon
//                    }
//
//                    if (it.eventType == DevicePosOrientEvent.ORIENTATION_EVENT &&
//                            currentPosition["lat"] != 0F.toDouble() &&
//                            app.startGeofence != null) {
//
//                        if (it.sensorEvent?.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))
//                            System.arraycopy(it.sensorEvent?.values,
//                                    0, accelerometerReading, 0, accelerometerReading.size)
//
//                        if (it.sensorEvent?.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD))
//                            System.arraycopy(it.sensorEvent?.values,
//                                    0, magnetometerReading, 0, magnetometerReading.size)
//
//                        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
//
//                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
//                        var azimuth = RadiansToDegrees.convert(orientationAngles[0].toDouble())
//
//                        val currentLocationObj = Location("current")
//                        currentLocationObj.latitude = currentPosition["lat"]!!
//                        currentLocationObj.longitude = currentPosition["lng"]!!
//
//                        val destinationLocationObj = Location("destination")
//                        destinationLocationObj.latitude = app.startGeofence!!.latlng.lat
//                        destinationLocationObj.longitude = app.startGeofence!!.latlng.lng
//
//                        val geoField = GeomagneticField(currentLocationObj.latitude.toFloat(),
//                                currentLocationObj.longitude.toFloat(), currentLocationObj.altitude.toFloat(),
//                                System.currentTimeMillis())
//
//                        azimuth -= geoField.declination
//
//                        var bearingTo = currentLocationObj.bearingTo(destinationLocationObj)
//                        if (bearingTo < 0)
//                            bearingTo += 360
//
//                        direction = bearingTo - azimuth
//
//                        if (direction < 0)
//                            direction += 360
//
////                        Log.i(tag, direction.toString())
////                        directionAngleText.text = direction.toInt().toString()
////                        rotateImageView(needle, R.drawable.needle, direction)
//                    }
//                }

        flightInfoObservableSwitch
                .switchMap { if(it) flightInfoObservable else Observable.never() }
                .subscribe {
                    app.ticket = it
                    updateTicketData(it)
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
        //toast("Launch Default Activity")
        val intent  = Intent(this, DefaultActivity::class.java)
        startActivity(intent)
    }

    fun updateTicketData(ticket: AirlineTicket) {
        name.text = "${ticket.firstName} ${ticket.lastName}"
        flightTime.text = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.departureTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                .toString("HH:mm")
        seat.text = ticket.seat
        ticketClass.text = ticket.ticketClass
        gate.text = ticket.gate
        boardingTime.text = ISODateTimeFormat.dateTimeParser()
                .parseDateTime(ticket.boardingTime)
                .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                .toString("HH:mm")
        flightNumber.text = ticket.flightNumber
        sourceDestination.text = "${ticket.source} → ${ticket.destination}"
        terminal.text = ticket.terminal
    }
}
