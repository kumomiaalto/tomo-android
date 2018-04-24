package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.common.SteerpathClient;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;


/**
 * Well, term "legacy" here is bit exaggerative. SteerpathMapView is valid way to implement map, but doing so
 * requires more work for you.
 *
 * SteerpathMapFragment is a wrapper for SteerpathMapView and implements following:
 * - Mapbox lifecycle methods
 * - sets default map style url for you
 * - implements permission dialog (LocateMe -button)
 */

public class LegacyMapViewActivity extends AppCompatActivity {

    private SteerpathMapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_legacy_mapview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
        getSupportActionBar().setSubtitle(building.getTitle());
        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        mapView = (SteerpathMapView) findViewById(R.id.mapview);

        // mandatory call. Mapbox MapView initializer.
        mapView.onCreate(savedInstanceState);

        // TODO: this is mandatory at this point!
        mapView.setStyleURL(SteerpathClient.getInstance().getMapStyleUrl());

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(SteerpathMap map) {
                map.setMyLocationEnabled(true);
                MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Add the Mapbox lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
