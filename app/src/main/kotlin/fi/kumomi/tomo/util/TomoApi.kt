package fi.kumomi.tomo

import fi.kumomi.tomo.model.AirlineTicket
import io.reactivex.Observable
import retrofit2.http.GET


interface TomoApi {
    @GET("/ticket/show")
    fun getAirlineTicket(): Observable<AirlineTicket>
}