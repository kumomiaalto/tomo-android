package fi.kumomi.tomo.flowable

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import fi.kumomi.tomo.model.DevicePosOrientEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe

class DeviceOrientationFlowable {
    companion object {
        fun create(sensorManager: SensorManager): Flowable<DevicePosOrientEvent> {
            var sensorListener : SensorEventListener? = null

            return Flowable.create(FlowableOnSubscribe<DevicePosOrientEvent> { emitter ->
                sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) = emitter.onNext(DevicePosOrientEvent(sensorEvent = event, eventType = DevicePosOrientEvent.ORIENTATION_EVENT))
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                val accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

                sensorManager.registerListener(sensorListener, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL,  SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(sensorListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
            }, BackpressureStrategy.BUFFER).doOnCancel({
                sensorManager.unregisterListener(sensorListener)
            })
        }
    }
}