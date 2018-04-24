package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 *
 */

public class MapActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // http://stackoverflow.com/questions/22926393/why-is-my-oncreateview-method-being-called-twice
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_parent, SteerpathMapFragment.newInstance(), "steerpath-map-fragment").commit();
        }

        MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
        getSupportActionBar().setSubtitle(building.getTitle());
    }

    @Override
    public void onMapViewReady(SteerpathMapView mapView) {

        mapView.showMonitorButton(true);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap map) {
                // enable positioning
                map.setMyLocationEnabled(true);

                MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);

                //MapHelper.setCameraBounds(map);
            }
        });
    }
}
