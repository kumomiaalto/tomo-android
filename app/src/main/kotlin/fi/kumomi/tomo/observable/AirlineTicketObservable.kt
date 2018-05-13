package fi.kumomi.tomo.observable

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import fi.kumomi.tomo.Config
import fi.kumomi.tomo.TomoApi
import fi.kumomi.tomo.model.AirlineTicket
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AirlineTicketObservable {
    companion object {
        fun create(): Observable<AirlineTicket> {
            val retrofit = Retrofit.Builder()
                    .baseUrl(Config.TOMO_API_BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()

            val tomoApi = retrofit.create(TomoApi::class.java)

            return tomoApi.getAirlineTicket()
        }
    }

}