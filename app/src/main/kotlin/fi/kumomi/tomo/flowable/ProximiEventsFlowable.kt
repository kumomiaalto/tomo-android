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
                        val proximiEvent = ProximiEvent(event_type = ProximiEvent.GEOFENCE_ENTER_EVENT, geofence = geofence)
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = "proximi"))
                    }

                    override fun geofenceExit(geofence: ProximiioGeofence?, dwellTime: Long?) {
                        val proximiEvent = ProximiEvent(event_type = ProximiEvent.GEOFENCE_EXIT_EVENT, geofence = geofence, dwellTime = dwellTime)
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = "proximi"))
                    }

                    override fun position(lat: Double, lon: Double, accuracy: Double) {
                        val proximiEvent = ProximiEvent(event_type = ProximiEvent.POSITION_EVENT,
                                location = ProximiLocation(lat = lat, lon = lon, accuracy = accuracy))
                        emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = "proximi"))
                    }

                    override fun foundDevice(device: ProximiioBLEDevice?, registered: Boolean) {
                        if (registered) {
                            val proximiEvent = ProximiEvent(event_type = ProximiEvent.BEACON_FOUND_EVENT,
                                    beacon = device)
                            emitter.onNext(DevicePosOrientEvent(proximiEvent = proximiEvent, eventType = "proximi"))
                        }
                    }
                })
            }, BackpressureStrategy.BUFFER)
        }
    }
}