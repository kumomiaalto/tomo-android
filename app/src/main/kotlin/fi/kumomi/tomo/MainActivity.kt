package fi.kumomi.tomo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import io.proximi.proximiiolibrary.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private var proximiioAPI: ProximiioAPI? = null

    private val tag = "KumomiTomo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // For Android 8+, create a notification channel for notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val preferences = getSharedPreferences("Proximi.io Map Demo", Context.MODE_PRIVATE)
            if (!preferences.contains("notificationChannel")) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                if (notificationManager != null) {
                    val channel = NotificationChannel(BackgroundListener.NOTIFICATION_CHANNEL_ID,
                            BackgroundListener.NOTIFICATION_CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                    preferences.edit()
                            .putBoolean("notificationChannel", true)
                            .apply()
                }
            }
        }

        val options = ProximiioOptions().setNotificationMode(ProximiioOptions.NotificationMode.DISABLED)

        // Create our Proximi.io listener
        proximiioAPI = ProximiioAPI(tag, this, options)
        proximiioAPI?.setListener(object : ProximiioListener() {
            override fun geofenceEnter(geofence: ProximiioGeofence?) {
                Log.d(tag, "Geofence enter: " + geofence?.name)
                val geofenceData = "Geofence enter: ${geofence?.name}, ${geofence?.lat}, ${geofence?.lon}"
                navigationText.text = geofenceData
            }

            override fun geofenceExit(geofence: ProximiioGeofence?, dwellTime: Long?) {
                Log.d(tag, "Geofence exit: " + geofence?.name + ", dwell time: " + dwellTime.toString())
                val geofenceData = "Geofence exit: ${geofence?.name}, dwell time: ${dwellTime?.toString()}"
                navigationText.text = geofenceData
            }

            override fun loginFailed(loginError: ProximiioListener.LoginError?) {
                Log.e(tag, "LoginError! (" + loginError?.toString() + ")")
            }

            override fun position(lat: Double, lon: Double, accuracy: Double) {
                Log.e(tag, "$lat -- $lon")
            }

            override fun foundDevice(device: ProximiioBLEDevice?, registered: Boolean) {
                Log.e(tag, "${device?.mac}")
            }
        })

        proximiioAPI?.setAuth(AUTH)
        proximiioAPI?.setActivity(this)

        doAsync {
            val geofenceData = getGeofences()

            uiThread {
                val allGeofences = JSONArray(geofenceData)
                val geofence1 = allGeofences.getJSONObject(0).getJSONObject("area")
                val geofence2 = allGeofences.getJSONObject(1).getJSONObject("area")

                geofenceBearing1.text = geofence1.getString("lat")
                geofenceBearing2.text = geofence2.getString("lat")

//                GeomagneticField geoField;
//
//                private final LocationListener locationListener = new LocationListener() {
//                    public void onLocationChanged(Location location) {
//                        geoField = new GeomagneticField(
//                                Double.valueOf(location.getLatitude()).floatValue(),
//                        Double.valueOf(location.getLongitude()).floatValue(),
//                        Double.valueOf(location.getAltitude()).floatValue(),
//                        System.currentTimeMillis()
//                        );
//                        ...
//                    }
//                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        proximiioAPI?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        proximiioAPI?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val AUTH = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImlzcyI6ImU0ZjNkYmUyLTRlOWQtNDA1My1iYzljLTM5OTU1OWQwOWE1ZCIsInR5cGUiOiJhcHBsaWNhdGlvbiIsImFwcGxpY2F0aW9uX2lkIjoiNjlkNGYzNTMtZjc2NS00MDc4LWE0NDEtYzNhMDhhNzJiYzJiIn0.IY8nfXX35yDHd0JqU6ZqZ3zVClwv1DKLdAkZEk-tn0Y" // TODO: Replace with your own!
    }

    private fun getGeofences(): String? {
        val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

        val geofenceUrl = "http://api.proximi.fi/core/geofences"
        val request = Request.Builder()
                .addHeader("Authorization", "Bearer ${AUTH}")
                .url(geofenceUrl)
                .build()

        val response = client.newCall(request).execute()
        return response?.body()?.string()


//        [
//            {
//                "address": "Shaftesbury Avenue 4" ,
//                "area": {
//                "lat": 48.584404113908526 ,
//                "lng": 17.826119363307953
//            } ,
//                "createdAt": "2016-02-29T10:52:42.219Z" ,
//                "department_id": "ec320cac-d0fc-4a61-a46a-2d279c71c7e0" ,
//                "id": "af3e98dc-f6bc-4f48-b6b0-dff102f20133" ,
//                "name": "test geofence" ,
//                "organization_id": "2fd91f35-5243-4226-b182-e138d34825f5" ,
//                "place_id": "abb40118-446a-4540-bee2-ea723c26cbb7" ,
//                "radius": 8.38458105877176 ,
//                "updatedAt": "2016-02-29T10:52:42.219Z"
//            }
//        ]
    }


}