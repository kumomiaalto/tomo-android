package fi.kumomi.tomo.model

import android.hardware.SensorEvent

data class DevicePosOrientEvent(val proximiEvent: ProximiEvent? = null, val sensorEvent: SensorEvent? = null, val eventType: String) {
    companion object {
        const val ORIENTATION_EVENT = "orientation"
        const val GEOFENCE_ENTER_EVENT = "geofenceEnterEvent"
        const val GEOFENCE_EXIT_EVENT = "geofenceExitEvent"
        const val POSITION_EVENT = "positionEvent"
        const val BEACON_FOUND_EVENT = "beaconFoundEvent"
    }
}