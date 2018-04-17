package io.proximi.proximiiodemo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import io.proximi.proximiiolibrary.ProximiioAPI;
import io.proximi.proximiiolibrary.ProximiioGeofence;
import io.proximi.proximiiolibrary.ProximiioListener;
import io.proximi.proximiiolibrary.ProximiioOptions;
import io.proximi.proximiiomap.ProximiioMapHelper;
import io.proximi.proximiiomap.ProximiioMapView;

/**
 * Proximiio Demo
 */
public class MainActivity extends AppCompatActivity {
    private ProximiioAPI proximiioAPI;
    private ProximiioMapHelper mapHelper;

    private static final String TAG = "ProximiioDemo";

    public static final String AUTH = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImlzcyI6ImU0ZjNkYmUyLTRlOWQtNDA1My1iYzljLTM5OTU1OWQwOWE1ZCIsInR5cGUiOiJhcHBsaWNhdGlvbiIsImFwcGxpY2F0aW9uX2lkIjoiNjlkNGYzNTMtZjc2NS00MDc4LWE0NDEtYzNhMDhhNzJiYzJiIn0.IY8nfXX35yDHd0JqU6ZqZ3zVClwv1DKLdAkZEk-tn0Y"; // TODO: Replace with your own!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For Android 8+, create a notification channel for notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedPreferences preferences = getSharedPreferences("Proximi.io Map Demo", MODE_PRIVATE);
            if (!preferences.contains("notificationChannel")) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    NotificationChannel channel = new NotificationChannel(BackgroundListener.NOTIFICATION_CHANNEL_ID,
                                                                          BackgroundListener.NOTIFICATION_CHANNEL_NAME,
                                                                          NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(channel);
                    preferences.edit()
                            .putBoolean("notificationChannel", true)
                            .apply();
                }
            }
        }

        ProximiioOptions options = new ProximiioOptions()
                .setNotificationMode(ProximiioOptions.NotificationMode.ENABLED);

        // Create our Proximi.io listener
        proximiioAPI = new ProximiioAPI(TAG, this, options);
        proximiioAPI.setListener(new ProximiioListener() {
            @Override
            public void geofenceEnter(ProximiioGeofence geofence) {
                Log.d(TAG, "Geofence enter: " + geofence.getName());
            }

            @Override
            public void geofenceExit(ProximiioGeofence geofence, @Nullable Long dwellTime) {
                Log.d(TAG, "Geofence exit: " + geofence.getName() + ", dwell time: " + String.valueOf(dwellTime));
            }

            @Override
            public void loginFailed(LoginError loginError) {
                Log.e(TAG, "LoginError! (" + loginError.toString() + ")");
            }
        });
        proximiioAPI.setAuth(AUTH);
        proximiioAPI.setActivity(this);

        // Initialize the map
        ProximiioMapView mapView = findViewById(R.id.map);
        mapHelper = new ProximiioMapHelper.Builder(this, mapView, AUTH, savedInstanceState)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapHelper.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapHelper.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapHelper.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapHelper.onDestroy();
        proximiioAPI.destroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapHelper.onLowMemory();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        proximiioAPI.onActivityResult(requestCode, resultCode, data);
        mapHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        proximiioAPI.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
