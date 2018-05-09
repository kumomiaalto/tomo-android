package fi.kumomi.tomo.flowable

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe

class DeviceOrientationFlowable(private val sensors: Array<Sensor>, private val sensorManager: SensorManager) {
    fun create(): Flowable<SensorEvent> {
        var sensorListener : SensorEventListener? = null

        return Flowable.create(FlowableOnSubscribe<SensorEvent> { emitter ->
            sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) = emitter.onNext(event)
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensors.forEach { sensor ->
                sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_UI)
            }

        }, BackpressureStrategy.BUFFER).doOnCancel({
            sensorManager.unregisterListener(sensorListener)
        })
    }
}