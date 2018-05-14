package fi.kumomi.tomo.model

import io.proximi.proximiiolibrary.ProximiioBLEDevice
import io.proximi.proximiiolibrary.ProximiioGeofence

data class ProximiEvent(val geofence: ProximiioGeofence? = null, val beacon: ProximiioBLEDevice? = null, val location: ProximiLocation? = null, val dwellTime: Long? = null)