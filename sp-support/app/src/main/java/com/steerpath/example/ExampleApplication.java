package com.steerpath.example;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.StrictMode;

import com.github.anrwatchdog.ANRError;
import com.github.anrwatchdog.ANRWatchDog;
import com.squareup.leakcanary.LeakCanary;
import com.steerpath.example.utils.EidUpdaterHelper;
import com.steerpath.sdk.common.SteerpathClient;
import com.steerpath.sdk.common.internal.DeveloperOptions;
import com.steerpath.sdk.telemetry.TelemetryConfig;
import com.steerpath.sdk.telemetry.TelemetryService;
import com.steerpath.sdk.utils.internal.Monitor;

/**
 * Steerpath Indoor Positioning SDK is derived from Mapbox SDK.
 *
 * TROUBLESHOOT: https://s3-eu-west-1.amazonaws.com/steerpath/android/documentation/latest/javadoc/reference/packages.html
 */

public class ExampleApplication extends Application {

    public static final String BROADCAST_SDK_READY = "com.steerpath.broadcast.SDK_READY";

    // Intent configuration options for Activities
    public static final String EXTRAS_HELPER_BUILDING = "com.steerpath.dev.helper.building";
    public static final String EXTRAS_HELPER_NEXT_ACTIVITY = "com.steerpath.dev.helper.next.activity";

    @Override
    public void onCreate() {
        super.onCreate();

        // Memory Leaks
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        // ANRs
        new ANRWatchDog()
                /*.setIgnoreDebugger(true)
                .setANRListener(new ANRWatchDog.ANRListener() {
                    @Override
                    public void onAppNotResponding(ANRError error) {
                        // Handle the error. For example, log it to HockeyApp:
                        // ExceptionHandler.saveException(error, new CrashManager());
                        error.printStackTrace();
                    }
                })*/.start();

        // Other badness
        enableStrictMode();

        /**
         * OPTIONAL: Steerpath Telemetry collects user location and beacon data and sends them to backend for further processing.
         * This tool is for very large venues and it is recommended to consider more light-weight solutions before enabling
         * Steerpath Telemetry.
         *
         * For required tokens, contact support@steerpath.com
         */
        TelemetryConfig telemetry = new TelemetryConfig.Builder(this)
                .accessToken("YOUR_TELEMETRY_ACCESS_TOKEN")
                .baseUrl("YOUR_TELEMETRY_URL")
                .build();

        SteerpathClient.StartConfig config =  new SteerpathClient.StartConfig.Builder()
                // MANDATORY:
                .name("STEERPATH OFFICE")
                .apiKey("OUR API_KEY HERE")

                // OPTIONAL:
                // 1. OfflineBundle contains metadata, style, positioning, routing and vector tile data. Makes map features usable with bad
                // network conditions or without network at all.
                // For obtaining OfflineBundle, contact support@steerpath.com
                // .sff file must be located in /assets -folder
                // If your setup contains many large buildings and you have low end device, first initial start() may take awhile.
                // Subsequent calls are much faster.
                //.installOfflineBundle("sp_offline_data_20170703T055713Z.sff")

                // 2. Enables Steerpath Telemetry
                //.telemetry(telemetry)

                // 3. Enables some developer options. PLEASE DISABLE DEVELOPER OPTIONS IN PRODUCTION!
                // This will add "Monitor"-button above "LocateMe"-button as a visual reminder developer options are in use
                // Use logcat filter "Monitor", for example: adb logcat *:S Monitor:V
                .developerOptions(DeveloperOptions.getDefaultOptions())
                //.developerOptions(DeveloperOptions.DISABLED)

                .build();

        // NOTE: start() will initialize things in background AsyncTask. This is because installing OfflineBundle is potentially time consuming operation
        // and it shouldn't be done in the main thread. For this reason, app should wait onStarted() callback to be invoked before starting using its features.
        SteerpathClient.getInstance().start(this, config, new SteerpathClient.StartListener() {
            @Override
            public void onStarted() {
                // Proceed with your Splash Screen or Main Screen.
                // Don't let user to access MapActivity before everything is ready.
                notifyReady();

                // OPTIONAL: if your setup has eid-beacons, you should trigger EID package downloads as soon as app starts.
                // See also EidUpdaterActivity
                //EidUpdaterHelper.updateAllEids(ExampleApplication.this);
            }
        });

        // If you need to start Telemetry manually, be sure not to call SteerpathClient.StartConfig.Builder.telemetry()
        //delayTelemetryStart();
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyFlashScreen()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

    private void notifyReady() {
        Intent intent = new Intent();
        intent.setAction(BROADCAST_SDK_READY);
        sendBroadcast(intent);
    }
    
    /**
     * If Telemetry needs to be started after {@link SteerpathClient#start(Context, SteerpathClient.StartConfig, SteerpathClient.StartListener)}.
     * Example Scenario: if user id or other property is downloaded from the backend and start of Telemetry must be delayed until response has been received.
     */
    private void delayTelemetryStart() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TelemetryConfig telemetry = new TelemetryConfig.Builder(getApplicationContext())
                        // omitted other params..
                        .userId("RECEIVED_ID")
                        .build();

                TelemetryService.getInstance().start(getApplicationContext(), telemetry);
            }
        }, 5000);
    }
}
