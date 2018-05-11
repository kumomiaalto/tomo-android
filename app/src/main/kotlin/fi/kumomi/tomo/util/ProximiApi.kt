package fi.kumomi.tomo.util

import fi.kumomi.tomo.model.Geofence
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Header

interface ProximiApi {
    @GET("/core/geofences")
    fun getGeofences(@Header("Authorization")credentials: String): Observable<List<Geofence>>
}