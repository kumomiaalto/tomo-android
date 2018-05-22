package fi.kumomi.tomo

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.util.Log

import org.json.JSONException
import org.json.JSONObject

import io.proximi.proximiiolibrary.ProximiioAPI

class BackgroundListener : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != null) {
            when (intent.action) {
                ProximiioAPI.ACTION_POSITION -> Log.d(tag, "Position: " + intent.getDoubleExtra(ProximiioAPI.EXTRA_LAT, 0.0) + ", " + intent.getDoubleExtra(ProximiioAPI.EXTRA_LON, 0.0))
            // Please note that remote notifications through Proximi.io is purely for demo purposes.
            // If you are looking for a remote notification system to use, please look into a system
            // that's built to handle notifications specifically;
            // they offer far more features, are easier to use, and are more robust for that.
            // Some examples include:
            //     - Firebase Cloud Messaging (https://firebase.google.com/docs/cloud-messaging/)
            //     - OneSignal (https://onesignal.com/)
                ProximiioAPI.ACTION_OUTPUT -> {
                    var json: JSONObject? = null
                    try {
                        json = JSONObject(intent.getStringExtra(ProximiioAPI.EXTRA_JSON))
                    } catch (e: JSONException) {
                        // Not a push
                    } catch (e: NullPointerException) {
                    }

                    if (json != null) {
                        var title: String? = null
                        try {
                            if (!json.isNull("type") && !json.isNull("title")) {
                                if (json.getString("type") == "push") {
                                    title = json.getString("title")
                                }
                            }
                        } catch (e: JSONException) {
                            // Not a push
                        }

                        if (title != null) {
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            if (notificationManager != null) {
                                val intent2 = Intent(context, MainActivity::class.java)
                                intent2.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

                                val contentIntent = PendingIntent.getActivity(context, 0, intent2, PendingIntent.FLAG_CANCEL_CURRENT)

                                val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                                        .setContentIntent(contentIntent)
                                        .setSmallIcon(R.drawable.notification)
                                        .setContentTitle(title)

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    notificationBuilder.priority = Notification.PRIORITY_HIGH
                                }

                                val notification = notificationBuilder.build()

                                notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
                                notificationManager.notify(1, notification)
                            }
                        }
                    }
                }
                Intent.ACTION_BOOT_COMPLETED -> {
                    Log.d(tag, "Phone booted!")
                    val proximiioAPI = ProximiioAPI("BackgroundReceiver", context, null)
                    proximiioAPI.setAuth(MainActivity.AUTH)
                    proximiioAPI.destroy()
                }
            }
        }
    }

    companion object {
        private val tag = "BackgroundListener"

        const val NOTIFICATION_CHANNEL_ID = "io.proximi.proximiiodemo"
        const val NOTIFICATION_CHANNEL_NAME = "Proximi.io Demo Notifications"
    }
}
