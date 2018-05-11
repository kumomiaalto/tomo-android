package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class LatLng(
        @Json(name = "lat") val lat: String,
        @Json(name = "lng") val lng: String
)