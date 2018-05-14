package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class LatLng(
        @Json(name = "lat") val lat: Double,
        @Json(name = "lng") val lng: Double
)