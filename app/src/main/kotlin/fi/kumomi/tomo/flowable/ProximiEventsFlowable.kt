package fi.kumomi.tomo.flowable

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
        fun create(proximiioAPI: ProximiioAPI?): Flowable<ProximiEvent> {
            return Flowable.create({ emitter ->
                proximiioAPI?.setListener(object : ProximiioListener() {
                    override fun geofenceEnter(geofence: ProximiioGeofence?) {
                        val proximiEvent = ProximiEvent(geofence = geofence, eventType = ProximiEvent.GEOFENCE_ENTER_EVENT)
                        emitter.onNext(proximiEvent)
                    }

                    override fun geofenceExit(geofence: ProximiioGeofence?, dwellTime: Long?) {
                        val proximiEvent = ProximiEvent(geofence = geofence, dwellTime = dwellTime, eventType = ProximiEvent.GEOFENCE_EXIT_EVENT)
                        emitter.onNext(proximiEvent)
                    }
                    //gets users position
                    override fun position(lat: Double, lon: Double, accuracy: Double) {
                        val proximiEvent = ProximiEvent(location = ProximiLocation(lat = lat, lon = lon, accuracy = accuracy), eventType = ProximiEvent.POSITION_EVENT)
                        emitter.onNext(proximiEvent)
                    }
                    //when beacons is found
                    override fun foundDevice(device: ProximiioBLEDevice?, registered: Boolean) {
                        if (device?.proximity == ProximiioBLEDevice.Proximity.NEAR) {
                            val proximiEvent = ProximiEvent(beacon = device, eventType = ProximiEvent.BEACON_FOUND_EVENT)
                            emitter.onNext(proximiEvent)
                        }
                    }
                })
            }, BackpressureStrategy.BUFFER)
        }
    }
}