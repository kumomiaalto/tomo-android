package fi.kumomi.tomo.util

import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.Beacon
import io.reactivex.Observable
import retrofit2.http.GET


interface TomoApi {
    @GET("/ticket/show")
    fun getAirlineTicket(): Observable<AirlineTicket>

    @GET("/beacons/show")
    fun getBeacons(): Observable<List<Beacon>>
}