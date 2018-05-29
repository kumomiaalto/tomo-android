package fi.kumomi.tomo

import android.support.multidex.MultiDexApplication
import android.util.Log
import fi.kumomi.tomo.model.AirlineTicket
import fi.kumomi.tomo.model.Beacon
import fi.kumomi.tomo.model.Geofence
import fi.kumomi.tomo.observable.BeaconRoutesObservable
import fi.kumomi.tomo.observable.BeaconsObservable
import fi.kumomi.tomo.observable.GeofencesObservable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * All of global app level data is stored here
 * App boots up from here, all the code in this class executes when app boots
 */
class TomoApplication : MultiDexApplication() {
    val apiBeacons: HashMap<String, Beacon> = HashMap()
    val apiBeaconRoutes: HashMap<String, List<String>> = HashMap()
    var currentBeaconRoute: List<String>? = null
    var currentIndexInBeaconRoute = 0
    val seenBeacons: HashMap<String?, DateTime> = HashMap()
    var ticket: AirlineTicket? = null
    val currentPosition: HashMap<String, Double?> = hashMapOf("lat" to null, "lon" to null)
    val destinationPosition: HashMap<String, Double?> = hashMapOf("lat" to null, "lon" to null)

    val accelerometerReading = FloatArray(3)
    val magnetometerReading = FloatArray(3)

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    var rotateAngle: Double = 0F.toDouble()
    var prevRotateAngle: Double = 0F.toDouble()

    val acceleroWindow0 = DescriptiveStatistics(10)
    val acceleroWindow1 = DescriptiveStatistics(10)
    val acceleroWindow2 = DescriptiveStatistics(10)

    val magnetoWindow0 = DescriptiveStatistics(10)
    val magnetoWindow1 = DescriptiveStatistics(10)
    val magnetoWindow2 = DescriptiveStatistics(10)

    // Dummy start and end locations
    val bootstrapOrigin: HashMap<String, Double> = hashMapOf("lat" to 0F.toDouble(), "lon" to 0F.toDouble())
    val bootstrapDestination: HashMap<String, Double> = hashMapOf("lat" to 90F.toDouble(), "lon" to 0F.toDouble())

    override fun onCreate() {
        super.onCreate()

        // Load beacons from Tomo Web API
        BeaconsObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .retryWhen { it.flatMap { Observable.timer(2, TimeUnit.SECONDS) } }
                .subscribe {
                    for (beacon in it) {
                        apiBeacons[beacon.name] = beacon
                    }
                }

        // Load beacon routes from Tomo Web API
        BeaconRoutesObservable.create()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .retryWhen { it.flatMap { Observable.timer(2, TimeUnit.SECONDS) } }
                .subscribe {
                    for (beaconRoute in it) {
                        apiBeaconRoutes[beaconRoute.name] = beaconRoute.beacons
                    }

                    currentBeaconRoute = apiBeaconRoutes["default"]
                }
    }

    companion object {
        private const val TAG = "TomoApplication"
    }
}
