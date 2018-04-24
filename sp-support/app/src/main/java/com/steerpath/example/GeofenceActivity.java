package com.steerpath.example;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.steerpath.example.utils.AnnotationOptionsFactory;
import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.geofence.Geofence;
import com.steerpath.sdk.geofence.GeofenceListener;
import com.steerpath.sdk.geofence.GeofencingApi;
import com.steerpath.sdk.geofence.GeofencingEvent;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathAnnotation;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
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
import com.steerpath.sdk.utils.GeoJsonHelper;
import com.steerpath.sdk.utils.internal.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;


/**
 * To add a geofence area, go to "add mode", set markers on the map (clockwise or counter-clockwise) and click "done" -button.
 */

public class GeofenceActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener, GeofenceListener, MetaLoader.LoadListener {

    private static final String TAG = GeofenceActivity.class.getSimpleName();

    private SteerpathMapView mapView = null;
    private SteerpathMap map = null;
    private List<SteerpathAnnotation> tmpMarkers = new ArrayList<>();
    private List<LatLng> latLngList = new ArrayList<>();

    private MenuItem addMenuItem;
    private MenuItem doneMenuItem;
    private MenuItem cancelMenuItem;

    private boolean useFakeLocation = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_w_progressbar);
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
        inflater.inflate(R.menu.geofence_activity, menu);

        addMenuItem = menu.findItem(R.id.add_geofence);
        doneMenuItem = menu.findItem(R.id.add_geofence_done);
        cancelMenuItem = menu.findItem(R.id.add_geofence_cancel);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_geofence:
                setAddMode(true);
                setMapClickListenerForAddingPoints();
                return true;

            case R.id.add_geofence_done:
                createCustomGeofence();
                setAddMode(false);
                return true;

            case R.id.add_geofence_cancel:
                setAddMode(false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setAddMode(boolean isInAddMode) {
        addMenuItem.setVisible(!isInAddMode);
        doneMenuItem.setVisible(isInAddMode);
        cancelMenuItem.setVisible(isInAddMode);

        if (isInAddMode) {
            getSupportActionBar().setSubtitle("Add markers to define area");
        } else {
            MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
            getSupportActionBar().setSubtitle(building.getTitle());

            latLngList.clear();
            map.removeAnnotations(tmpMarkers);
            tmpMarkers.clear();

            if (useFakeLocation) {
                mapView.setupClickForFakeLocation();
            } else {
                map.setOnMapClickListener(null);
            }
        }
    }

    @Override
    public void onMapViewReady(final SteerpathMapView mapView) {
        this.mapView = mapView;
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap steerpathMap) {
                map = steerpathMap;
                // enable positioning

                if (useFakeLocation) {
                    mapView.setupClickForFakeLocation();
                } else {
                    map.setMyLocationEnabled(true);
                }

                MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);

                // Demonstrates couple of ways to set geofence. Basically, you just need GeoJson from somewhere:

                // 1. GeoJson from Steerpath Meta
                //setRemoteGeofences();

                // 2. GeoJson from local file. NOTE: be sure to modify R.raw.geofence_template first!
                //setLocalGeofences();
            }
        });
    }

    /**
     * Reads no-beacon zone information from the Steerpath server. You need to know correct tags before use.
     */
    private void setRemoteGeofences() {
        // Tags are specific to your project and must match with your map data.
        // If you are unsure what are your tags, contact support@steerpath.com
        String[] areaTags = {
                "example_geofence_area"
        };

        MetaQuery query = new MetaQuery.Builder(this, MetaQuery.DataType.POINTS_OF_INTEREST).withAllTags(areaTags).build();
        MetaLoader.load(query, this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        if (!result.hasError()) {
            addGeofences(result.getMetaFeatures(), result.getJson());

            // TODO: move camera automatically to some meaningful position
        }
    }

    private void addGeofences(List<MetaFeature> features, JSONObject featureCollection) {
        for (MetaFeature feature : features) {
            JSONObject polygon = GeoJsonHelper.getPolygonJson(feature, featureCollection);
            SteerpathAnnotationOptions opts = AnnotationOptionsFactory.createGeofenceOptions(this, getFeatureJson(feature, featureCollection));
            map.addAnnotation(opts);
            addGeofence(polygon, feature.getFloor(), feature.getId(), feature.getTitle());
        }
    }

    private void addGeofence(JSONObject geometry, int floorIndex, String id, String info) {
        GeofencingApi api = GeofencingApi.getApi(this);
        if (geometry != null) {
            try {
                api.addGeofence(new Geofence.Builder()
                        .addPolygon(geometry)

                        // optional but helpful
                        .setInfo(info)

                        // be sure to add these
                        .setLevelIndex(floorIndex)
                        .setRequestId(id)

                        // transition types as bitmask
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setLoiteringDelay(5000)

                        .build());

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private static @Nullable
    JSONObject getFeatureJson(MetaFeature feature, JSONObject rootJson) {
        if (rootJson.has(K.features)) {
            try {
                JSONArray features = rootJson.getJSONArray(K.features);
                for (int i=0; i<features.length(); i++) {
                    JSONObject tmp = features.getJSONObject(i);
                    if (tmp.has(K.id) && tmp.get(K.id).equals(feature.getId())) {
                        return tmp;
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Read geofence data from R.raw.geofence_template, add Geofences and move camera to first Geofence.
     * Be sure to modify R.raw.geofence_template before use. By default it has invalid id and building ref.
     * Example geofence area is in Helsinki Railway Station.
     */
    private void setLocalGeofences() {
        // NOTE: requires local json file describing geofence areas!
        JSONObject featureCollection = Utils.toJSONObject(this, R.raw.geofence_template);
        addGeofences(getGeofencesFromAssets(featureCollection), featureCollection);

        // move camera to first Geofence
        try {
            JSONArray features = featureCollection.getJSONArray(K.features);
            JSONObject firstFeature = features.getJSONObject(0);
            JSONObject geometry = firstFeature.getJSONObject(K.geometry);
            JSONArray coordinates = geometry.getJSONArray(K.coordinates);
            JSONArray extraLayer = coordinates.getJSONArray(0); // ?
            JSONArray firstLatLng = extraLayer.getJSONArray(0);

            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(firstLatLng.getDouble(1), firstLatLng.getDouble(0))) // LatLng comes in reverse order
                    .zoom(18)
                    .build());

            JSONObject properties = firstFeature.getJSONObject(K.properties);
            map.setActiveLevel(properties.getString(K.buildingRef), properties.getInt(K.layerIndex));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private List<MetaFeature> getGeofencesFromAssets(JSONObject featureCollection) {
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
    public void onGeofencingEvent(final GeofencingEvent geofencingEvent) {
        runOnUiThread(new Runnable() { // result does not come in ui thread
            @Override
            public void run() {
                List<Geofence> geos = geofencingEvent.getTriggeringGeofences();
                if (!geofencingEvent.hasError() && geos != null && geos.size() > 0) {
                    switch (geofencingEvent.getGeofenceTransition()) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            Toast.makeText(mapView.getContext(), "ENTER " + geofencingEvent.getTriggeringGeofences().get(0).getInfo(), Toast.LENGTH_LONG).show();
                            break;
                        case Geofence.GEOFENCE_TRANSITION_DWELL:
                            Toast.makeText(mapView.getContext(), "DWELL " + geofencingEvent.getTriggeringGeofences().get(0).getInfo(), Toast.LENGTH_LONG).show();
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            Toast.makeText(mapView.getContext(), "EXIT " + geofencingEvent.getTriggeringGeofences().get(0).getInfo(), Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        GeofencingApi.getApi(this).addGeofenceListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // remove listener when done to avoid potential memory leak!
        GeofencingApi.getApi(this).removeGeofenceListener(this);
        MetaLoader.removeListener(this);
    }

    private void setMapClickListenerForAddingPoints() {
        map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                if (map.getFocusedBuilding() != null) {
                    SteerpathAnnotationOptions option = AnnotationOptionsFactory.createSimpleMarker(latLng, map.getFocusedBuilding().getActiveLevelIndex());
                    tmpMarkers.add(map.addAnnotation(option));
                    latLngList.add(latLng);
                }
            }
        });
    }

    private void createCustomGeofence() {
        try {
            JSONObject feature = generateFeatureJson(latLngList);
            JSONObject geometry = feature.getJSONObject(K.geometry);
            JSONObject properties = feature.getJSONObject(K.properties);
            addGeofence(geometry, properties.getInt(K.layerIndex), String.valueOf(feature.getInt(K.id)), "le zone");

            SteerpathAnnotationOptions opts = AnnotationOptionsFactory.createGeofenceOptions(this, feature);
            map.addAnnotation(opts);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject generateFeatureJson(List<LatLng> latLngList) throws JSONException {
        JSONObject feature = new JSONObject();
        feature.put(K.id, generateId());
        feature.put(K.type, K.featureType);

        JSONObject properties = new JSONObject();
        properties.put(K.buildingRef, map.getFocusedBuilding().getId());
        properties.put(K.layerIndex, map.getFocusedBuilding().getActiveLevelIndex());
        properties.put("drawnShape", "drawn-polygon");
        feature.put(K.properties, properties);

        JSONObject geometry = new JSONObject();
        JSONArray coordinates = new JSONArray();

        JSONArray extraArrayLayer = new JSONArray(); // ?

        for (LatLng latLng : latLngList) {
            JSONArray tmp = new JSONArray();
            tmp.put(latLng.getLongitude());
            tmp.put(latLng.getLatitude());
            extraArrayLayer.put(tmp);
        }

        coordinates.put(extraArrayLayer);

        geometry.put(K.coordinates, coordinates);
        geometry.put(K.type, K.polygonType);
        feature.put(K.geometry, geometry);

        return feature;
    }

    private static int ID = 0;
    private static int generateId() {
        ID+=1;
        return ID;
    }
}