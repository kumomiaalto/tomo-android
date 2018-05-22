package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class Geofence(
        @Json(name = "name") val name: String?,
        @Json(name = "id") val id: String?,
        @Json(name = "area") val latlng: LatLng,
        @Json(name = "radius") val radius: String
)