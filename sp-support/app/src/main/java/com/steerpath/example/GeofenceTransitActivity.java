package com.steerpath.example;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.steerpath.sdk.geofence.Geofence;
import com.steerpath.sdk.geofence.GeofencingApi;
import com.steerpath.sdk.location.FusedLocationProviderApi;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathAnnotation;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;
import com.steerpath.sdk.utils.GeoJsonHelper;
import com.steerpath.sdk.utils.internal.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;
import static com.steerpath.sdk.meta.MetaQuery.DataType.POINTS_OF_INTEREST;

/**
 *
 */

public class GeofenceTransitActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener, MetaLoader.LoadListener {

    private SteerpathMap map = null;
    private SteerpathMapView mapView = null;
    private View progressBar = null;

    private List<SteerpathAnnotation> anns = new ArrayList<>();
    private MetaFeature selectedPOI = null;

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
                if (selectedPOI != null) {
                    Location location = FusedLocationProviderApi.Api.get().getUserLocation();
                    if (location != null) {
                        showTransitionWarningOrNavigate(selectedPOI, location);
                    } else {
                        Toast.makeText(this, "Unknown location!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Select destination first", Toast.LENGTH_SHORT).show();
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

                // Geofences actually needs to be registered only once. This example does it each time when entering Activity.
                // You can move this for example in your Application class, just wait for SteerpathClient.StartListener() to be called.
                registerGeofenceAreas();
            }
        });
    }


    private void registerGeofenceAreas() {
        // Tags are specific to your project and must match with your map data.
        // If you are unsure what are your tags, contanct support@steerpath.com
        String[] transitAreaTags = {
                "example_transit_area_1", "example_transit_area_2", "example_transit_area_3"
        };

        MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
        MetaQuery query = new MetaQuery.Builder(this, POINTS_OF_INTEREST).building(building.getId()).withAnyTags(transitAreaTags).build();
        MetaLoader.load(query, this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        final GeofencingApi api = GeofencingApi.getApi(this);
        for (MetaFeature poi : result.getMetaFeatures()) {
            JSONObject polygon = GeoJsonHelper.getPolygonJson(poi, result.getJson());
            if (polygon != null) {
                try {
                    api.addGeofence(new Geofence.Builder()
                            .addPolygon(polygon)

                            // optional but helpful
                            .setInfo(poi.getTags().toString())

                            // be sure to add these
                            .setLevelIndex(poi.getFloor())
                            .setRequestId(poi.getId())

                            .build());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        setupOnMapClickListener();
    }

    private void setupOnMapClickListener() {
        mapView.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {

                MetaFeature feature = mapView.getRenderedMetaFeature(point);
                if (feature != null) {
                    // allow only one POI at the time
                    map.removeAnnotations(anns);
                    anns.clear();

                    SteerpathAnnotationOptions option = AnnotationOptionsFactory.createAnnotationOptions(mapView.getContext(), feature);
                    anns.add(map.addAnnotation(option));
                    selectedPOI = feature;
                }
            }
        });
    }

    private void clearAllAnnotations() {
        map.removeAnnotations(anns);
        anns.clear();
        mapView.stopNavigation();
        selectedPOI = null;
    }

    private void showTransitionWarningOrNavigate(MetaFeature feature, Location location) {
        boolean isUserInTransitSide = isUserLocationInAnyTransitArea(location);
        boolean isDestinationInTransitSide = isDestinationInAnyTransitArea(feature);

        String msg = null;
        if (isUserInTransitSide && isDestinationInTransitSide) {
            // both in transit side

        } else if (!isUserInTransitSide && !isDestinationInTransitSide) {
            // both in public side

        } else if (!isUserInTransitSide && isDestinationInTransitSide) {
            msg = "You need to get through Immigration to reach your destination";

        } else {
            msg = "Destination is in public area and you are in transit side. Continue anyway?";
        }

        if (msg != null) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            showRoutePreview(selectedPOI);
        }
    }

    private boolean isUserLocationInAnyTransitArea(Location location) {
        GeofencingApi api = GeofencingApi.getApi(this);
        List<Geofence> hits = api.hitTest(location);
        // You may use Geofence.getInfo() or Geofence.getRequestId() to determine in which transit area user is precisely.
        // Here we just resolve if user is in any transit area.
        return hits.size() > 0 ? true : false;
    }

    private boolean isDestinationInAnyTransitArea(MetaFeature destination) {
        GeofencingApi api = GeofencingApi.getApi(this);
        List<Geofence> hits = api.hitTest(Utils.toLocation(destination));
        return hits.size() > 0 ? true : false;
    }

    private void showRoutePreview(MetaFeature destination) {
        RoutePlan plan = new RoutePlan.Builder()
                .destination(destination)
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

            }

            @Override
            public void onDestinationReached() {

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
        MetaLoader.removeListener(this);
    }

    private void stopNavigation() {
        if (mapView != null) {
            mapView.stopNavigation();
        }
    }
}
