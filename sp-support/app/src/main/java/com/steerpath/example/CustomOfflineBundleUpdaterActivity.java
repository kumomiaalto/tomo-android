package com.steerpath.example;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.steerpath.sdk.common.OfflineBundle;
import com.steerpath.sdk.common.OfflineUpdater;
import com.steerpath.sdk.common.SteerpathClient;

/**
 * NOTE: SteerpathClient can load OfflineBundle automatically for you during app start-up. You don't necessarily need to implement
 * UI for OfflineBundle Updater. See ExampleApplication.onCreate()
 */

public class CustomOfflineBundleUpdaterActivity extends AppCompatActivity {

    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_bundle_updater);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Working...");
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(100);
    }

    public void onUninstallOfflineBundleBtnClick(View view) {
        OfflineBundle.uninstall(this);
    }

    public void onInstallOfflineBundleBtnClick(View view) {
        dialog.setProgress(0);
        dialog.setCancelable(false); // don't allow MapActivity before OfflineBundle is ready
        dialog.show();

        final OfflineUpdater updater = SteerpathClient.getOfflineUpdater();

        // install OfflineBundle from /assets and then call onUpdated(). After that, starts silently downloading updates
        // from the Steerpath server. These updates will be installed next time app is launched, assuming download was successful.
        OfflineBundle offlineBundle = OfflineBundle.create(this, "sp_offline_data_20170703T055713Z.sff");
        updater.installAndUpdate(this, offlineBundle, new OfflineUpdater.OfflineUpdateListener() {
            @Override
            public void onProgress(int percent) {
                dialog.setProgress(percent);
            }

            @Override
            public void onMapDataInstalled() {

            }

            @Override
            public void onUpdated(OfflineBundle bundle) {
                dialog.hide();
            }

            @Override
            public void onError(String msg) {
                dialog.hide();
                Toast.makeText(CustomOfflineBundleUpdaterActivity.this, "Failed: " + msg, Toast.LENGTH_LONG).show();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                updater.cancel();
            }
        });
    }
}
