package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class Beacon(
        @Json(name = "mac") val mac: String,
        @Json(name = "icon") val icon: String,
        @Json(name = "text") val text: String,
        @Json(name = "beacon_type") val beaconType: String?
)