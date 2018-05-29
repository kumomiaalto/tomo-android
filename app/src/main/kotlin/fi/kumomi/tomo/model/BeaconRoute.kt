package fi.kumomi.tomo.model

import com.squareup.moshi.Json

data class BeaconRoute(
        @Json(name = "name") val name: String,
        @Json(name = "beacons") val beacons: List<String>
)