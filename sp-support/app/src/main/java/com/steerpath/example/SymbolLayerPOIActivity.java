package com.steerpath.example;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.Filter;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.Source;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.steerpath.example.utils.AnnotationOptionsFactory;
import com.steerpath.example.utils.MapHelper;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.SteerpathLayerOptions;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.steerpath.sdk.meta.MetaQuery.DataType.POINTS_OF_INTEREST;

/**
 *
 */

public class SymbolLayerPOIActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener, MapboxMap.OnMapClickListener, MetaLoader.LoadListener {

    private SteerpathMap map = null;
    private SteerpathMapView mapView = null;
    private View progressBar = null;
    private boolean markerSelected = false;

    // If you want to alter the appearance of markers, you need to create own layer for it.
    // SymbolLayer can have only one iconImage() assigned to it. See SteerpathAnnotationOptions createSymbolLayerOption()
    private String selectedMarkerSourceId;
    private String selectedMarkerLayerId;

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

        MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);
        getSupportActionBar().setSubtitle(building.getTitle() + " (id=" + building.getId() + ")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.symbol_layer_poi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_soures_and_layers:
                showSourcesAndLayers();
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

                MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);
                if (building != null) {
                    MapHelper.moveCameraTo(map, building);
                }

                map.getMapboxMap().setOnMapClickListener(SymbolLayerPOIActivity.this);

                // Add a image for the makers
                map.getMapboxMap().addImage(
                        "my-marker-image",
                        BitmapFactory.decodeResource(getResources(), R.drawable.mapbox_marker_icon_default)
                );

                // SVG works too
//                map.getMapboxMap().addImage(
//                        "my-marker-image",
//                        DrawableUtils.toBitmap(getResources(), R.drawable.ic_bug, R.color.building)
//                );

                selectedMarkerSourceId = "selected-marker-source-" + building.getId();
                selectedMarkerLayerId = "selected-marker-layer-" + building.getId();

                // Add the selected marker source and layer
                FeatureCollection emptySource = FeatureCollection.fromFeatures(new Feature[]{});
                Source selectedMarkerSource = new GeoJsonSource(selectedMarkerSourceId, emptySource);
                map.getMapboxMap().addSource(selectedMarkerSource);

                SymbolLayer selectedMarker = new SymbolLayer(selectedMarkerLayerId, selectedMarkerSourceId)
                        .withProperties(
                                iconImage("my-marker-image"));
                map.getMapboxMap().addLayer(selectedMarker);

                loadPOIs();
            }
        });
    }

    private void loadPOIs() {
        progressBar.setVisibility(VISIBLE);
        MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);
        MetaLoader.load(new MetaQuery.Builder(this, POINTS_OF_INTEREST)
                .building(building.getId())
                .build(), this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        if (!result.hasError()) {
            MetaFeature building = getIntent().getParcelableExtra(ExampleApplication.EXTRAS_HELPER_BUILDING);

            String sourceId = "marker-source-" + building.getId();
            String layerId = "marker-layer-" + building.getId();

            // Using own Source because we want to add custom properties that are not available in default Source ("blueprint").
            // For example, if you need to show only "green" items, you can do following:
            // - add "isGreen" property to Source data
            // - use Filter.has("isGreen") to show only green items.
            Source source = new GeoJsonSource(sourceId, AnnotationOptionsFactory.toFeatureCollection(result.getMetaFeatures()));
            map.getMapboxMap().addSource(source);

            map.addAnnotation(AnnotationOptionsFactory.createSymbolLayerOption(sourceId, layerId));

            getSupportActionBar().setSubtitle(building.getTitle() + " (id=" + building.getId() + ") has " + result.getMetaFeatures().size() + " POIs");
        }
        progressBar.setVisibility(GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        MetaLoader.removeListener(this);
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

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (map != null && map.getFocusedBuilding() != null) { // against abuse
            final SymbolLayer selectedMarkerLayer = (SymbolLayer) map.getMapboxMap().getLayer(selectedMarkerLayerId);
            final PointF pixel = map.getMapboxMap().getProjection().toScreenLocation(point);

            String type = "type";
            String marker = "marker";
            String localRef = "localRef";
            String currentMarkerLayer = "marker-layer-" + map.getFocusedBuilding().getId();

            // move this layer to top
            map.getMapboxMap().removeLayer(selectedMarkerLayer);
            map.getMapboxMap().addLayer(selectedMarkerLayer);

            for (Feature feature : map.getMapboxMap().queryRenderedFeatures(pixel, currentMarkerLayer)) {
                if (feature.hasProperty(type) && feature.getStringProperty(type).equals(marker)) {
                    List<Feature> selectedFeature = map.getMapboxMap().queryRenderedFeatures(pixel, selectedMarkerLayerId);
                    if (selectedFeature.size() > 0 && markerSelected) {
                        return;
                    }

                    if (markerSelected) {
                        deselectMarker(selectedMarkerLayer, currentMarkerLayer);
                        return;
                    }

                    String id = feature.getStringProperty("id");
                    FeatureCollection featureCollection = FeatureCollection.fromFeatures(
                            new Feature[]{Feature.fromGeometry(feature.getGeometry(), featurePropertiesId(id))});

                    GeoJsonSource source = map.getMapboxMap().getSourceAs(selectedMarkerSourceId);
                    if (source != null) {
                        source.setGeoJson(featureCollection);
                    }

                    if (markerSelected) {
                        deselectMarker(selectedMarkerLayer, currentMarkerLayer);
                    }

                    selectMarker(selectedMarkerLayer, id);

                    String ref = feature.getStringProperty(localRef);
                    showPopupWindow("Local ref:\n" + ref, selectedMarkerLayer, currentMarkerLayer);

                    // hiding marker here is not necessary as selectedMarkerLayer is rendered on top of it
                    // It flickers little too.
                    //hideMarker(id, currentMarkerLayer);

                    break;
                }
            }
        }
    }

    private void hideMarker(String id, String markerLayer) {
        SymbolLayer layer = (SymbolLayer) map.getMapboxMap().getLayer(markerLayer);
        layer.setFilter(Filter.all(
                Filter.neq("id", id),
                Filter.eq(SteerpathLayerOptions.LAYER_INDEX, map.getFocusedBuilding().getActiveLevel().getNumber())
        ));
    }

    private JsonObject featurePropertiesId(String id) {
        JsonObject object = new JsonObject();
        object.add("id", new JsonPrimitive(id));
        return object;
    }

    private void selectMarker(final SymbolLayer marker, String id) {
        marker.setFilter(Filter.eq("id", id));
        ValueAnimator markerAnimator = new ValueAnimator();
        markerAnimator.setObjectValues(1f, 2f);
        markerAnimator.setDuration(300);
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                marker.setProperties(
                        iconSize((float) animator.getAnimatedValue())
                );
            }
        });
        markerAnimator.start();
        markerSelected = true;
    }

    private void deselectMarker(final SymbolLayer marker, final String markerLayer) {
        ValueAnimator markerAnimator = new ValueAnimator();
        markerAnimator.setObjectValues(2f, 1f);
        markerAnimator.setDuration(300);
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                marker.setProperties(
                        iconSize((float) animator.getAnimatedValue())
                );
            }
        });

        markerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                // hiding marker here is not necessary as selectedMarkerLayer is rendered on top of it
                // It flickers little too.
                // showMarker(markerLayer);

                marker.setFilter(Filter.eq("id", Integer.MAX_VALUE)); // nothing matches with MAX_VALUE
            }
        });

        markerAnimator.start();
        markerSelected = false;
    }

    private void showMarker(String markerLayer) {
        SymbolLayer layer = (SymbolLayer) map.getMapboxMap().getLayer(markerLayer);
        layer.setFilter(Filter.all(
                Filter.eq(SteerpathLayerOptions.LAYER_INDEX, map.getFocusedBuilding().getActiveLevel().getNumber()),
                Filter.eq(SteerpathLayerOptions.BUILDING_REF, map.getFocusedBuilding().getId())
        ));
    }

    /**
     * InfoWindow works with Markers and MarkerViews. Now that SymbolLayer replaces deprecated MarkerView, we need
     * to implement own PopupWindow too.
     *
     * TODO: this popup appears annoyingly in the center of the screen
     *
     * @param text
     * @param marker
     */
    private void showPopupWindow(String text, final SymbolLayer marker, final String currentMarkerLayer) {

        // get a reference to the already created main layout
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.activity_main_map_layout);

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        TextView textView = (TextView) popupView.findViewById(R.id.popup_window_text);
        textView.setText(text);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                deselectMarker(marker, currentMarkerLayer);
            }
        });
    }
}