package fi.kumomi.tomo.model

import com.squareup.moshi.Json
import java.util.*

data class AirlineTicket(
        @Json(name = "first_name") val firstName: String?,
        @Json(name = "last_name") val lastName: String?,
        @Json(name = "airline") val airline: String?,
        @Json(name = "source") val source: String?,
        @Json(name = "destination") val destination: String?,
        @Json(name = "flight_number") val flightNumber: String?,
        @Json(name = "ticket_number") val ticketNumber: String?,
        @Json(name = "seat") val seat: String?,
        @Json(name = "ticket_class") val ticketClass: String?,
        @Json(name = "boarding_time") val boardingTime: String?,
        @Json(name = "departure_time") val departureTime: String?,
        @Json(name = "terminal") val terminal: String?,
        @Json(name = "gate") val gate: String?,
        @Json(name = "gate_location") val gateLocation: String?,
        @Json(name = "security_time") val securityTime: String?,
        @Json(name = "immigration_time") val immigrationTime: String?
)
