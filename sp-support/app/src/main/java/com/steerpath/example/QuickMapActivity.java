package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;

/**
 *
 */

public class QuickMapActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        // http://stackoverflow.com/questions/22926393/why-is-my-oncreateview-method-being-called-twice
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_parent, SteerpathMapFragment.newInstance(), "steerpath-map-fragment").commit();
        }
    }

    @Override
    public void onMapViewReady(SteerpathMapView mapView) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap map) {
                // enable positioning
                map.setMyLocationEnabled(true);
            }
        });
    }
}
