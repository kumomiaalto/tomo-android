package com.steerpath.example;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.Toast;

import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.geofence.Geofence;
import com.steerpath.sdk.geofence.GeofencingApi;
import com.steerpath.sdk.location.FusedLocationProviderApi;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaFeatureFactory;
import com.steerpath.sdk.meta.MetaFeatureParser;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;
import com.steerpath.sdk.meta.internal.K;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 * Show prompt when location updates did not occur in 5 seconds and last known location was inside a geofence area.
 */

public class NoBeaconZoneActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener, LocationListener, MetaLoader.LoadListener {

    private SteerpathMap map = null;
    private Handler handler = new Handler();
    private Runnable geofenceCheckRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // http://stackoverflow.com/questions/22926393/why-is-my-oncreateview-method-being-called-twice
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_parent, SteerpathMapFragment.newInstance(), "steerpath-map-fragment").commit();
        }

        geofenceCheckRunnable = new Runnable() {
            @Override
            public void run() {
                List<Geofence> hits = GeofencingApi.getApi(getApplicationContext()).hitTest(FusedLocationProviderApi.Api.get().getLastLocation());
                if (!hits.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "This area has no positioning available!", Toast.LENGTH_LONG).show();
                }
            }
        };

        MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
        getSupportActionBar().setSubtitle(building.getTitle());
    }

    @Override
    public void onStart() {
        super.onStart();
        FusedLocationProviderApi.Api.get().requestLocationUpdates(this);
    }

    @Override
    public void onMapViewReady(final SteerpathMapView mapView) {
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
        //readRemotePOIs();
        readLocalPOIs();
    }

    /**
     * Reads no-beacon zone information from the Steerpath server.
     */
    private void readRemotePOIs() {
        // Tags are specific to your project and must match with your map data.
        // If you are unsure what are your tags, contact support@steerpath.com
        String[] areaTags = {
                "example_no_beacon_area"
        };

        MetaLoader.load(new MetaQuery.Builder(this,
                MetaQuery.DataType.POINTS_OF_INTEREST)
                .withAllTags(areaTags)
                .build(),
                this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        addGeofences(result.getMetaFeatures(), result.getJson());
    }

    private void addGeofences(List<MetaFeature> features, JSONObject featureCollection) {
        final GeofencingApi api = GeofencingApi.getApi(this);
        for (MetaFeature feature : features) {

            // TODO: see below:
            //JSONObject polygon = GeoJsonHelper.getPolygonJson(feature, featureCollection);
            JSONObject polygon = getPolygonJson(feature, featureCollection);

            if (polygon != null) {
                try {
                    api.addGeofence(new Geofence.Builder()
                            .addPolygon(polygon)

                            // optional but helpful
                            .setInfo(feature.getTags().toString())

                            // be sure to add these
                            .setLevelIndex(feature.getFloor())
                            .setRequestId(feature.getId())

                            .build());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // TODO: this getPolygonJson is a copy of GeoJsonUtils.getPolygonJson
    // In local json file, coordinates comes from geometry-node whereas GeoJsonUtils is not prepared for that!
    // GeoJsonUtils.getPolygonJson needs to be fixed in future releases.
    // For now, lets use this one instead:
    private static @Nullable JSONObject getPolygonJson(MetaFeature feature, JSONObject rootJson) {
        if (rootJson.has(K.features)) {
            try {
                JSONArray features = rootJson.getJSONArray(K.features);
                for (int i=0; i<features.length(); i++) {
                    JSONObject tmp = features.getJSONObject(i);
                    if (tmp.has(K.id) && tmp.get(K.id).equals(feature.getId())) {
                        if (tmp.has(K.geometry)) {
                            JSONObject geometry = tmp.getJSONObject(K.geometry);
                            String type = geometry.getString(K.type);
                            if (type.equals(K.polygonType)) {
                                JSONObject result = new JSONObject();
                                result.put(K.type, K.polygonType);
                                JSONArray coordinates = geometry.getJSONArray(K.coordinates);
                                result.put(K.coordinates, coordinates);
                                return result;
                            }
                        } else {
                            JSONObject properties = tmp.getJSONObject(K.properties);
                            JSONObject area = properties.getJSONObject(K.area);
                            String type = area.getString(K.type);
                            if (type.equals(K.polygonType)) {
                                JSONObject result = new JSONObject();
                                result.put(K.type, K.polygonType);
                                JSONArray coordinates = area.getJSONArray(K.coordinates);
                                result.put(K.coordinates, coordinates);
                                return result;
                            }
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Reads local FeatureCollection defining no-beacon zones. This file must be located in your /res/raw - folder.
     */
    private void readLocalPOIs() {
        // NOTE: requires local json file describing areas without beacons!
        //JSONObject featureCollection = Utils.toJSONObject(this, R.raw.zones);
        //addGeofences(getNoBeaconZonesFromAssets(featureCollection), featureCollection);
    }

    private List<MetaFeature> getNoBeaconZonesFromAssets(JSONObject featureCollection) {
        List<MetaFeature> results = new ArrayList<>();
        try {
            JSONArray features = featureCollection.getJSONArray(K.features);
            MetaFeatureParser parser = MetaFeatureFactory.getDefaultParser();
            for (int i=0; i<features.length(); i++) {
                try {
                    JSONObject obj = features.getJSONObject(i);
                    results.add(parser.parseFeature(obj));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return results;
    }

    @Override
    public void onLocationChanged(Location location) {
        handler.removeCallbacks(geofenceCheckRunnable);
        // if location updates was not received in 5 seconds, do geofence check!
        handler.postDelayed(geofenceCheckRunnable, 5000);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(geofenceCheckRunnable);
        FusedLocationProviderApi.Api.get().removeLocationUpdates(this);
        MetaLoader.removeListener(this);
    }
}
