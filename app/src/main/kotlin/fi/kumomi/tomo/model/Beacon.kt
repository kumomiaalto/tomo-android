package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class Beacon(
        @Json(name = "name") val name: String,
        @Json(name = "icon") val icon: String?,
        @Json(name = "text") val text: String?,
        @Json(name = "beacon_type") val beaconType: String,
        @Json(name = "latitude") val latitude: String?,
        @Json(name = "longitude") val longitude: String?,
        @Json(name = "time_to_gate") val timeToGate: String?,
        @Json(name = "next_beacon") val nextBeacon: String?,
        @Json(name = "boarding_time") val boardingTime: String?,
        @Json(name = "departure_time") val departureTime: String?,
        @Json(name = "route") val route: String?

)
