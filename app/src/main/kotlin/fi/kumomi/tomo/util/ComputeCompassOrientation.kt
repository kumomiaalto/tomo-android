package fi.kumomi.tomo.util

import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.util.Log
import fi.kumomi.tomo.TomoApplication
import fi.kumomi.tomo.model.DevicePosOrientEvent
import fi.kumomi.tomo.model.Geofence

class ComputeCompassOrientation {
    companion object {
        fun compute(ctx: Context, orientationEvent: DevicePosOrientEvent): Float {
            val app = ctx.applicationContext as TomoApplication
            val currentPosition = app.proximiPosition
            var pointerGeofence: Geofence? = null
            var direction = 0F

            val mAccelerometerReading = FloatArray(3)
            val mMagnetometerReading = FloatArray(3)

            val mRotationMatrix = FloatArray(9)
            val mOrientationAngles = FloatArray(3)


            if (orientationEvent.eventType == DevicePosOrientEvent.POSITION_EVENT) {
                currentPosition["lat"] = orientationEvent.proximiEvent?.location?.lat
                currentPosition["lng"] = orientationEvent.proximiEvent?.location?.lon
            }

            if (app.geofences != null) {
                val iterator = app.geofences!!.listIterator()
                for (geofence in iterator) {
                    if (geofence.name == "lobby_stairs") {
                        pointerGeofence = geofence
                    }
                }
            }

            if (orientationEvent.eventType == DevicePosOrientEvent.ORIENTATION_EVENT && pointerGeofence != null) {
                var azimuth = orientationEvent.sensorEvent!!.values[0]
                Log.i("Compute", azimuth.toString())
                val baseAzimuth = azimuth

                val currentLocationObj = Location("current")
                currentLocationObj.latitude = currentPosition["lat"]!!
                currentLocationObj.longitude = currentPosition["lng"]!!

                val destinationLocationObj = Location("destination")
                destinationLocationObj.latitude = pointerGeofence.latlng.lat
                destinationLocationObj.longitude = pointerGeofence.latlng.lng

                val geoField = GeomagneticField(currentLocationObj.latitude.toFloat(), currentLocationObj.longitude.toFloat(), currentLocationObj.altitude.toFloat(), System.currentTimeMillis())
                azimuth -= geoField.declination

                var bearingTo = currentLocationObj.bearingTo(destinationLocationObj)
                if (bearingTo < 0)
                    bearingTo += 360

                direction = bearingTo + azimuth

                if (direction < 0)
                   direction += 360
            }

            return direction
        }
    }
}