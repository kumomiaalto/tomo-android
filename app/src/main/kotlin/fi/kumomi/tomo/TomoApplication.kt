package fi.kumomi.tomo

import android.support.multidex.MultiDexApplication
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.observable.GeofencesObservable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TomoApplication : MultiDexApplication() {
    var geofences: List<Geofence>? = null

    override fun onCreate() {
        super.onCreate()

        GeofencesObservable().create()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .subscribe {
                    geofences = it
                }
    }
}