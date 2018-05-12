package fi.kumomi.tomo

import android.support.multidex.MultiDexApplication
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.observable.GeofencesObservable
import io.reactivex.schedulers.Schedulers

class TomoApplication : MultiDexApplication() {
    var geofences: List<Geofence>? = null
    val proximiPosition: HashMap<String, Double?> = hashMapOf("lat" to 0F.toDouble(), "lng" to 0F.toDouble())

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