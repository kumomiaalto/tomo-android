package com.steerpath.example;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.steerpath.example.utils.AnnotationOptionsFactory;
import com.steerpath.sdk.maps.MapUtils;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathAnnotation;
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

public class FillExtrusion3DActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

    private SteerpathMap map = null;
    private List<SteerpathAnnotation> anns = new ArrayList<>();

    private View mapRoot;
    private View listRoot;
    private ListView listView;
    private List<String> allTags = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extrusion);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mapRoot = findViewById(R.id.fragment_parent);
        listRoot = findViewById(R.id.list_parent);
        listView = (ListView) findViewById(R.id.list);

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
        inflater.inflate(R.menu.extrusion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_soures_and_layers:
                showSourcesAndLayers();
                return true;

            case R.id.tag_add_ext:
                showList();
                return true;

            case R.id.tag_remove_ext:
                removeTest();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapViewReady(final SteerpathMapView mapView) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap steerpathMap) {
                map = steerpathMap;
                map.setMyLocationEnabled(true);
                map.getMapboxMap().getUiSettings().setTiltGesturesEnabled(true);

                MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
                map.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(building.getLatitude(), building.getLongitude()))
                        .tilt(45)
                        .zoom(18)
                        .build());

                setMapClickListener();
            }
        });
    }

    private void setMapClickListener() {
        map.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                Feature feature = MapUtils.getFeature(map, latLng);
                if (feature != null) {

                    // If existing SteerpathAnnotation (layer id matches with Feature's localRef)
                    // is not removed, addAnnotation() will return same the SteerpathAnnotation -object.
                    // It's not dangerous, but you may have duplicate Objects in anns - list.
                    SteerpathAnnotation ann = MapUtils.getFillExtrusionAnnotation(map, feature);
                    if (ann != null) {
                        map.removeAnnotation(ann);
                    }
                    anns.add(map.addAnnotation(AnnotationOptionsFactory.createFillExtrusionForLatLng(feature)));
                    MapUtils.animateCameraWithIncreasingBearing(map, latLng);
                }
            }
        });
    }

    private void showSourcesAndLayers() {
        new AlertDialog.Builder(this)
                .setTitle("Sources and Layers Info")
                .setMessage(generateSourceAndLayersInfo())
                .setPositiveButton("Close", null).show();
    }

    private String generateSourceAndLayersInfo() {
        return listSources() + "\n\n" + listLayers();
    }

    private String listSources() {
        List<Source> sources = map.getMapboxMap().getSources();
        StringBuilder builder = new StringBuilder("SOURCES:");
        for (Source source : sources) {
            builder.append("\n");
            builder.append(source.getId());
        }
        return builder.toString();
    }

    private String listLayers() {
        List<Layer> layers = map.getMapboxMap().getLayers();
        StringBuilder builder = new StringBuilder("LAYERS:");
        for (Layer layer : layers) {
            builder.append("\n");
            builder.append(layer.getId());
        }
        return builder.toString();
    }

    private void removeTest() {
        map.removeAnnotations(anns);
        anns.clear();
    }

    private void showList() {
        mapRoot.setVisibility(GONE);
        listRoot.setVisibility(VISIBLE);
        listView.setChoiceMode(listView.CHOICE_MODE_MULTIPLE);
        listView.setTextFilterEnabled(true);
        if (allTags == null) {
            // NOTE: this gets all tags. It actually depends on GeoJson geometry type whether 3D effect can be rendered or not: it needs to be Polygon.
            // If it's Point, fill extrusion cannot be applied.
            // NOTE: getAllTags() is potentially slow!
            allTags = MapUtils.getAllTags(map);
        }

        getSupportActionBar().hide();
        listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, allTags));
    }

    public void onDoneBtnClick(View view) {
        int[] checkedPositions = getCheckedPositions(listView);
        String[] tags = new String[checkedPositions.length];
        for (int i=0; i<checkedPositions.length; i++) {
            tags[i] = allTags.get(checkedPositions[i]);
        }

        mapRoot.setVisibility(VISIBLE);
        listRoot.setVisibility(GONE);
        getSupportActionBar().show();

        // If existing SteerpathAnnotation (layer id matches with Feature's tags)
        // is not removed, addAnnotation() will return same the SteerpathAnnotation -object.
        SteerpathAnnotation ann = MapUtils.getFillExtrusionAnnotation(map, tags);
        if (ann != null) {
            map.removeAnnotation(ann);
        }
        anns.add(map.addAnnotation(AnnotationOptionsFactory.createFillExtrusionForTags(tags)));
    }

    // getting multiselection indices from ListView ain't fun
    private static int[] getCheckedPositions(ListView list) {
        SparseBooleanArray positions  = list.getCheckedItemPositions();
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            int key = positions.keyAt(i);
            if (positions.valueAt(i)) {
                arrayList.add(key);
            }
        }
        int[] result = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            result[i] = arrayList.get(i);
        }
        return result;
    }
}
