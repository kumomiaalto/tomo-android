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
    val beacons: HashMap<String, Beacon> = HashMap()
    val seenBeacons: HashMap<String?, DateTime> = HashMap()
    var startGeofence: Geofence? = null
    var ticket: AirlineTicket? = null
    val proximiPosition: HashMap<String, Double> = hashMapOf("lat" to 0F.toDouble(), "lng" to 0F.toDouble())

    val accelerometerReading = FloatArray(3)
    val magnetometerReading = FloatArray(3)

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)
    var direction: Double = 0F.toDouble()
    var prevDirection: Double = 0F.toDouble()

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