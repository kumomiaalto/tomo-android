package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.steerpath.example.utils.EidUpdaterHelper;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;

/**
 *
 */

public class EidUpdaterActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

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
    public void onMapViewReady(final SteerpathMapView mapView) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap map) {
                // enable positioning
                map.setMyLocationEnabled(true);

                interceptLocateMeButtonClicks(mapView, map);
            }
        });
    }

    private void interceptLocateMeButtonClicks(final SteerpathMapView mapView, final SteerpathMap map) {
        // NOTE: SDK internally handles permissions, disabled Bluetooth configuration
        // etc before click events are delivered to the application.
        mapView.setLocateMeButtonListener(new SteerpathMapView.LocateMeButtonListener() {
            @Override
            public boolean onClick(SteerpathMap.MapMode mapMode) {

                // TODO: in future, you may ask a 'app status code' from the SteerpathClient. Until then, status needs to be resolved manually:
                if (map.getUserLocation() == null) {
                    // location not available! Can't go to "Follow Me" mode and can't start navigation!
                    if (EidUpdaterHelper.hasValidEids()) {
                        // We have EIDs for all buildings, but no location at this moment for some reason.
                        Toast.makeText(mapView.getContext(), "No location yet!", Toast.LENGTH_LONG).show();

                    } else {
                        // Some building does not have EID package and thus positioning does not work. To obtain EID, user must
                        // have proper internet access for EidUpdaterHelper.updateAllEids() to finish download tasks.
                        Toast.makeText(mapView.getContext(), "You need internet access before positioning can be started!", Toast.LENGTH_LONG).show();
                    }

                    // NOTE: if map is initially in MapMode.NORMAL mode, this prevents map mode to be changed. Use map.setMapMode(); to change initial MapMode.
                    return true; // consume event
                }

                // false: calls internally map.toggleMapMode()
                return false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // EID updates can be spammed. Unnecessary network operation not started.
        // See also ExampleApplication!
        EidUpdaterHelper.updateAllEids(this);
    }
}
