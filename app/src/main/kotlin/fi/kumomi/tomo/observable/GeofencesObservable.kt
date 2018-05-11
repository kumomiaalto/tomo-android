package fi.kumomi.tomo.observable

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.util.ProximiApi
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GeofencesObservable {
//    fun create(): Observable<Geofence> {
//        val retrofit = Retrofit.Builder()
//                .baseUrl(Config.PROXIMI_API_BASE_URL)
//                .addConverterFactory(MoshiConverterFactory.create())
//                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                .build()
//
//        val proximiApi = retrofit.create(ProximiApi::class.java)
//
////        return proximiApi.getGeofences()
//    }
}