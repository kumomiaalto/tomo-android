package com.steerpath.example;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.steerpath.example.utils.AnnotationOptionsFactory;
import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.directions.DirectionsException;
import com.steerpath.sdk.directions.DirectionsResponse;
import com.steerpath.sdk.directions.RouteListener;
import com.steerpath.sdk.directions.RoutePlan;
import com.steerpath.sdk.directions.RouteStep;
import com.steerpath.sdk.directions.RouteTrackerProgress;
import com.steerpath.sdk.directions.Waypoint;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathAnnotation;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 *
 */

public class WaypointsActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

    private SteerpathMap map = null;
    private SteerpathMapView mapView = null;
    private View progressBar = null;

    private List<SteerpathAnnotation> anns = new ArrayList<>();
    private List<Waypoint> waypoints = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_w_progressbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progressBar = findViewById(R.id.steerpath_map_progressbar);

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
        inflater.inflate(R.menu.simple_navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_all_annotations:
                clearAllAnnotations();
                return true;

            case R.id.get_directions:
                if (!waypoints.isEmpty()) {
                    showRoutePreview();
                } else {
                    Toast.makeText(this, "Select few waypoints  first", Toast.LENGTH_SHORT).show();
                }
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

                setupOnMapClickListenerForMultiWaypointNavigation();
            }
        });
    }

    private void setupOnMapClickListenerForMultiWaypointNavigation() {
        mapView.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {

                MetaFeature feature = mapView.getRenderedMetaFeature(point);
                if (feature != null) {
                    SteerpathAnnotationOptions option = AnnotationOptionsFactory.createAnnotationOptions(mapView.getContext(), feature);
                    anns.add(map.addAnnotation(option));
                    waypoints.add(new Waypoint(feature));
                }
            }
        });
    }

    private void clearAllAnnotations() {
        map.removeAnnotations(anns);
        anns.clear();
        mapView.stopNavigation();
        waypoints.clear();
    }

    private void showRoutePreview() {
        RoutePlan plan = new RoutePlan.Builder()
                .waypoints(waypoints)

                // destination can be set explicitly. If not set, last Waypoint will be treated as destination.
                //.destination(destination)

                // change how much map will be zoomed out on preview mode
                // depending on number of waypoints, you may want to zoom preview little bit more or less
                .previewCameraPadding(350)

                .build();

        progressBar.setVisibility(VISIBLE);
        mapView.previewRoute(plan, new RouteListener() {

            @Override
            public void onDirections(DirectionsResponse directions) {
                progressBar.setVisibility(GONE);
            }

            @Override
            public void onProgress(RouteTrackerProgress progress) {

            }

            @Override
            public void onStepEntered(RouteStep step) {

            }

            @Override
            public void onWaypointReached(Waypoint waypoint) {
                // OPTIONS:
                // 1. You may show WaypointViewHolder when Waypoint has been reached and let user
                // continue navigation manually:
                mapView.showWaypointViewHolder(waypoint);

                // 2. You may continue to next Waypoint automatically:
//                int index = plan.waypoints.indexOf(waypoint);
//                if (index < plan.waypoints.size()-1) {
//                    index++;
//                    Waypoint next = plan.waypoints.get(index);
//                    mapView.navigateTo(next);
//                }
            }

            @Override
            public void onDestinationReached() {
                // OPTIONAL:
                //stopNavigation();
            }

            @Override
            public void onNavigationStopped() {

            }

            @Override
            public void onError(DirectionsException error) {
                progressBar.setVisibility(GONE);
            }
        });

        // NOTE: SDK will switch automatically to NORMAL mode. This is to prevent camera wandering around.
        // FOLLOW_USER mode may interrupt preview route camera positioning.
        //map.setMapMode(SteerpathMap.MapMode.FOLLOW_USER);
    }


    @Override
    public void onStop() {
        super.onStop();
        // if navigation is not stopped, positioning thread stays alive when app is backgrounded
        stopNavigation();
    }

    private void stopNavigation() {
        if (mapView != null) {
            mapView.stopNavigation();
        }
    }
}
