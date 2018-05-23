package fi.kumomi.tomo.model

import io.proximi.proximiiolibrary.ProximiioBLEDevice
import io.proximi.proximiiolibrary.ProximiioGeofence

data class ProximiEvent(val geofence: ProximiioGeofence? = null, val beacon: ProximiioBLEDevice? = null, val location: ProximiLocation? = null, val dwellTime: Long? = null, val eventType: String) {
    companion object {
        const val GEOFENCE_ENTER_EVENT = "geofenceEnterEvent"
        const val GEOFENCE_EXIT_EVENT = "geofenceExitEvent"
        const val POSITION_EVENT = "positionEvent"
        const val BEACON_FOUND_EVENT = "beaconFoundEvent"
    }
}