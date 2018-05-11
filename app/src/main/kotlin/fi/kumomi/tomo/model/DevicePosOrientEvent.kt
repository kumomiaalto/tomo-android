package fi.kumomi.tomo.model

import android.hardware.SensorEvent

data class DevicePosOrientEvent(val proximiEvent: ProximiEvent? = null, val orientationSensorEvent: SensorEvent? = null, val eventType: String)