package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.location.FusedLocationSource;
import com.steerpath.sdk.location.LocationRequest;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 *
 */

public class CustomLocationSourceActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

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
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap map) {

                // call setLocationSource() before setMyLocationEnabled()
                map.setLocationSource(new FusedLocationSource(CustomLocationSourceActivity.this, createLocationRequestWithGpsEnabled()));

                // enable positioning
                map.setMyLocationEnabled(true);

                MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);
            }
        });
    }

    /**
     * By default, SDK has disabled GPS. With LocationRequest, you may also define parameters such as how accurate or how frequently positioning is collected.
     * @return
     */
    private static LocationRequest createLocationRequestWithGpsEnabled() {
        LocationRequest request = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // GPS threshold determines the minimum accuracy that GPS must have in order for automatic bluetooth to GPS switch to happen.
        request.setGpsThreshold(80); // using some ridiculously high GPS threshold for testing purposes
        return request;
    }
}
