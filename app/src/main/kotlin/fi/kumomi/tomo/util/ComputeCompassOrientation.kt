package fi.kumomi.tomo.util

import android.content.Context
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.model.Geofence

class ComputeCompassOrientation {
    companion object {
        fun compute(ctx: Context, orientationEvent: DevicePosOrientEvent) {
            val app = ctx.applicationContext as TomoApplication
            var pointerGeofence: Geofence? = null
            if (app.geofences != null) {
                val iterator = app.geofences!!.listIterator()
                for (geofence in iterator) {
                    if (geofence.name == "lobby_stairs") {
                        pointerGeofence = geofence
                    }
                }
            }


        }
    }
}