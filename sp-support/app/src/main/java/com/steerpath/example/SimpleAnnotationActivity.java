package com.steerpath.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.steerpath.example.utils.AnnotationOptionsFactory;
import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathAnnotation;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;

import java.util.ArrayList;
import java.util.List;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 *
 */

public class SimpleAnnotationActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

    private SteerpathMap map = null;
    private SteerpathMapView mapView = null;
    private List<SteerpathAnnotation> anns = new ArrayList<>();

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_annotations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all_annotations:
                clearAllAnnotations();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapViewReady(SteerpathMapView mapView) {
        this.mapView = mapView;
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap steerpathMap) {
                map = steerpathMap;
                // enable positioning
                map.setMyLocationEnabled(true);

                MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);

                setupOnMapClickListener();
            }
        });
    }

    private void setupOnMapClickListener() {
        mapView.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {
                MetaFeature feature = mapView.getRenderedMetaFeature(point);
                if (feature != null) {
                    SteerpathAnnotationOptions option = AnnotationOptionsFactory.createAnnotationOptions(mapView.getContext(), feature);
                    anns.add(map.addAnnotation(option));
                }
            }
        });
    }

    private void clearAllAnnotations() {
        map.removeAnnotations(anns);
        anns.clear();
    }
}
