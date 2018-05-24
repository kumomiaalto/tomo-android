package fi.kumomi.tomo.flowable

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe

class DeviceOrientationFlowable {
    companion object {
        fun create(sensorManager: SensorManager): Flowable<SensorEvent> {
            var sensorListener : SensorEventListener? = null

            return Flowable.create(FlowableOnSubscribe<SensorEvent> { emitter ->
                sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) = emitter.onNext(event)
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                val accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

                sensorManager.registerListener(sensorListener, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL,  SensorManager.SENSOR_DELAY_GAME)
                sensorManager.registerListener(sensorListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_GAME)
            }, BackpressureStrategy.BUFFER).doOnCancel({
                sensorManager.unregisterListener(sensorListener)
            })
        }
    }
}
