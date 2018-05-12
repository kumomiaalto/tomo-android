package fi.kumomi.tomo

import android.support.multidex.MultiDexApplication
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.observable.GeofencesObservable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class TomoApplication : MultiDexApplication() {
    var geofences: List<Geofence>? = null
    var startGeofence: Geofence? = null
    val proximiPosition: HashMap<String, Double?> = hashMapOf("lat" to 0F.toDouble(), "lng" to 0F.toDouble())

    override fun onCreate() {
        super.onCreate()

        GeofencesObservable().create()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .retryWhen { it.flatMap { Observable.timer(2, TimeUnit.SECONDS) } }
                .subscribe {
                    geofences = it
                    val iterator = geofences!!.listIterator()
                    for (geofence in iterator) {
                        if (geofence.name == "Lobby") {
                            startGeofence = geofence
                        }
                    }
                }
    }
}