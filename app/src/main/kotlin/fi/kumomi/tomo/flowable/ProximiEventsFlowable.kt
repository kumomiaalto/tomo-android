package fi.kumomi.tomo.flowable

import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.model.ProximiEvent
import fi.kumomi.tomo.model.ProximiLocation
import io.proximi.proximiiolibrary.ProximiioAPI
import io.proximi.proximiiolibrary.ProximiioBLEDevice
import io.proximi.proximiiolibrary.ProximiioGeofence
import io.proximi.proximiiolibrary.ProximiioListener
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

class ProximiEventsFlowable {
    companion object {
        fun create(proximiioAPI: ProximiioAPI?): Flowable<DevicePosOrientEvent> {
            return Flowable.create({ emitter ->
                proximiioAPI?.setListener(object : ProximiioListener() {
                    override fun geofenceEnter(geofence: ProximiioGeofence?) {
                        val proximiEvent = ProximiEvent(geofence = geofence)
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = DevicePosOrientEvent.GEOFENCE_ENTER_EVENT))
                    }

                    override fun geofenceExit(geofence: ProximiioGeofence?, dwellTime: Long?) {
                        val proximiEvent = ProximiEvent(geofence = geofence, dwellTime = dwellTime)
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = DevicePosOrientEvent.GEOFENCE_EXIT_EVENT))
                    }

                    override fun position(lat: Double, lon: Double, accuracy: Double) {
                        val proximiEvent = ProximiEvent(location = ProximiLocation(lat = lat, lon = lon, accuracy = accuracy))
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = DevicePosOrientEvent.POSITION_EVENT))
                    }

                    override fun foundDevice(device: ProximiioBLEDevice?, registered: Boolean) {
                        if (true) {
                            val proximiEvent = ProximiEvent(beacon = device)
                            emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = DevicePosOrientEvent.BEACON_FOUND_EVENT))
                        }
                    }
                })
            }, BackpressureStrategy.BUFFER)
        }
    }
}