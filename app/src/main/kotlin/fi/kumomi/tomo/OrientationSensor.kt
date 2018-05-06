package fi.kumomi.tomo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager



class OrientationSensor : SensorEventListener {
    private val TAG = "KumomiTomoOrientationSensor"

    private var sensorManager: SensorManager? = null
    private var gsensor: Sensor? = null
    private var msensor: Sensor? = null

    constructor(context: Context) {
        this.sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.gsensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        this.msensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    interface OrientationListener {
        fun onNewAzimuth(azimuth: Float)
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}