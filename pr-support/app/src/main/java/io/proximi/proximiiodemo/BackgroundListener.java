package io.proximi.proximiiodemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.proximi.proximiiolibrary.ProximiioAPI;

public class BackgroundListener extends BroadcastReceiver {
    private static final String TAG = "BackgroundListener";

    public static final String NOTIFICATION_CHANNEL_ID = "io.proximi.proximiiodemo";
    public static final String NOTIFICATION_CHANNEL_NAME = "Proximi.io Demo Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ProximiioAPI.ACTION_POSITION:
                    Log.d(TAG, "Position: " + intent.getDoubleExtra(ProximiioAPI.EXTRA_LAT, 0) + ", " + intent.getDoubleExtra(ProximiioAPI.EXTRA_LON, 0));
                    break;
                // Please note that remote notifications through Proximi.io is purely for demo purposes.
                // If you are looking for a remote notification system to use, please look into a system
                // that's built to handle notifications specifically;
                // they offer far more features, are easier to use, and are more robust for that.
                // Some examples include:
                //     - Firebase Cloud Messaging (https://firebase.google.com/docs/cloud-messaging/)
                //     - OneSignal (https://onesignal.com/)
                case ProximiioAPI.ACTION_OUTPUT:
                    JSONObject json = null;
                    try {
                        json = new JSONObject(intent.getStringExtra(ProximiioAPI.EXTRA_JSON));
                    }
                    catch (JSONException | NullPointerException e) {
                        // Not a push
                    }

                    if (json != null) {
                        String title = null;
                        try {
                            if (!json.isNull("type") && !json.isNull("title")) {
                                if (json.getString("type").equals("push")) {
                                    title = json.getString("title");
                                }
                            }
                        }
                        catch (JSONException e) {
                            // Not a push
                        }

                        if (title != null) {
                            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            if (notificationManager != null) {
                                Intent intent2 = new Intent(context, MainActivity.class);
                                intent2.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent2, PendingIntent.FLAG_CANCEL_CURRENT);

                                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                                        .setContentIntent(contentIntent)
                                        .setSmallIcon(R.drawable.notification)
                                        .setContentTitle(title);

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                    notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
                                }

                                Notification notification = notificationBuilder.build();

                                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                notificationManager.notify(1, notification);
                            }
                        }
                    }
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    Log.d(TAG, "Phone booted!");
                    ProximiioAPI proximiioAPI = new ProximiioAPI("BackgroundReceiver", context, null);
                    proximiioAPI.setAuth(MainActivity.AUTH);
                    proximiioAPI.destroy();
                    break;
            }
        }
    }
}
