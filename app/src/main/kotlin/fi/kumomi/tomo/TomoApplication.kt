package fi.kumomi.tomo

import android.support.multidex.MultiDexApplication
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.Beacon
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.observable.BeaconsObservable
import fi.kumomi.tomo.observable.GeofencesObservable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

class TomoApplication : MultiDexApplication() {
    var geofences: List<Geofence>? = null
    var beacons: HashMap<String, Beacon> = HashMap()
    val seenBeacons: HashMap<String?, DateTime> = HashMap()
    var startGeofence: Geofence? = null
    var ticket: AirlineTicket? = null
    val proximiPosition: HashMap<String, Double?> = hashMapOf("lat" to 0F.toDouble(), "lng" to 0F.toDouble())

    override fun onCreate() {
        super.onCreate()

        GeofencesObservable.create()
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

        BeaconsObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .retryWhen { it.flatMap { Observable.timer(2, TimeUnit.SECONDS) } }
                .subscribe {
                    for (beacon in it) {
                        beacons[beacon.mac] = beacon
                    }
                }
    }
}