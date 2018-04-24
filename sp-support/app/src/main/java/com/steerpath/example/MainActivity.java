package com.steerpath.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.steerpath.sdk.common.SteerpathClient;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_NEXT_ACTIVITY;

/**
 *
 */

public class MainActivity extends AppCompatActivity {

    private View contentView;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentView = findViewById(R.id.main_activity_content);
        loadingView = findViewById(R.id.main_activity_loading);

        if (SteerpathClient.getInstance().isStarted()) {
            contentView.setVisibility(VISIBLE);
        } else {
            contentView.setVisibility(GONE);
        }

        BroadcastReceiver br = new SDKReadyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ExampleApplication.BROADCAST_SDK_READY);
        registerReceiver(br, filter);
    }

    public class SDKReadyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(this); // no need to keep this anymore
            contentView.setVisibility(VISIBLE);
            loadingView.setVisibility(GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.launch_about:
                launchAboutActivity();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void launchAboutActivity() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    public void onQuickMapBtnClick(View view) {
        Intent intent = new Intent(this, QuickMapActivity.class);
        startActivity(intent);
    }

    public void onTomoBtnClick(View view) {
        Intent intent = new Intent(this, DefaultScreenActivity.class);
        startActivity(intent);
    }

    public void onChooseBuildingBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, MapActivity.class);
        startActivity(intent);
    }

    public void onQueryRenderedFeaturesBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, SimpleAnnotationActivity.class);
        startActivity(intent);
    }

    public void onSimpleNavigationBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, SimpleNavigationActivity.class);
        startActivity(intent);
    }

    public void onGeofenceTransitBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, GeofenceTransitActivity.class);
        startActivity(intent);
    }

    public void onGeofenceBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, GeofenceActivity.class);
        startActivity(intent);
    }

    public void onCustomDirectionsBadgeBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, CustomDirectionsUIActivity.class);
        startActivity(intent);
    }

    public void onCustomRouteBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, CustomRouteActivity.class);
        startActivity(intent);
    }

    public void onFakeLocationBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, FakeLocationActivity.class);
        startActivity(intent);
    }

    public void onEIDBtnClick(View view) {
        Intent intent = new Intent(this, EidUpdaterActivity.class);
        startActivity(intent);
    }

    public void onPositioningBtnClick(View view) {
        Intent intent = new Intent(this, PositioningActivity.class);
        startActivity(intent);
    }

    public void onCustomLocationSourceBtnClick(View view) {
        Intent intent = new Intent(this, CustomLocationSourceActivity.class);
        startActivity(intent);
    }

    public void onMetaExampleBtnClick(View view) {
        Intent intent = new Intent(this, MetaExampleActivity.class);
        startActivity(intent);
    }

    public void onCustomOfflineBundleUpdaterBtnClick(View view) {
        Intent intent = new Intent(this, CustomOfflineBundleUpdaterActivity.class);
        startActivity(intent);
    }

    public void onDirectionsApiBtnClick(View view) {
        Intent intent = new Intent(this, DirectionsApiActivity.class);
        startActivity(intent);
    }

    public void onWaypointsBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, WaypointsActivity.class);
        startActivity(intent);
    }

    public void onCustomWaypointUIBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, CustomWaypointUIActivity.class);
        startActivity(intent);
    }

    public void onNoBeaconZoneBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, NoBeaconZoneActivity.class);
        startActivity(intent);
    }

    public void onSteerpathMapViewBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, LegacyMapViewActivity.class);
        startActivity(intent);
    }

    public void onSymbolLayerPOIBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, SymbolLayerPOIActivity.class);
        startActivity(intent);
    }

    public void onFillExtrusion3DBtnClick(View view) {
        Intent intent = new Intent(this, BuildingChooserActivity.class);
        intent.putExtra(EXTRAS_HELPER_NEXT_ACTIVITY, FillExtrusion3DActivity.class);
        startActivity(intent);
    }
}
