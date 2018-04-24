package com.steerpath.example.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.models.Position;
import com.steerpath.example.R;
import com.steerpath.sdk.maps.MapUtils;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
import com.steerpath.sdk.maps.SteerpathLayerOptions;
import com.steerpath.sdk.maps.internal.SteerpathIcon;
import com.steerpath.sdk.maps.internal.SteerpathMarkerViewOptions;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.internal.JSONContract;
import com.steerpath.sdk.meta.internal.K;
import com.steerpath.sdk.utils.internal.DrawableUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textFont;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textHaloBlur;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textHaloColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textHaloWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

/**
 *
 */

public class AnnotationOptionsFactory {

    public static List<SteerpathAnnotationOptions> createAnnotationOptions(Context context, ArrayList<MetaFeature> features) {
        ArrayList<SteerpathAnnotationOptions> options = new ArrayList<>();
        for (MetaFeature feature : features) {
            options.add(createAnnotationOptions(context, feature));
        }

        return options;
    }

    public static SteerpathAnnotationOptions createAnnotationOptions(Context context, MetaFeature feature) {
        // choose & test, few alternatives here:
        //Object opts = createMyMarkerViewOptions(feature);
        //Object opts = createSteerpathMarkerViewOptions(feature);
        Object opts = createMarkerViewOptions(context, feature);
        //Object opts = createMarkerOptions(feature);
        //Object opts = createPolylineOptions(feature);
        //Object opts = createPolygonOptions(feature);
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withOptions(opts);
        builder.userId(feature.getId());
        builder.floor(feature.getFloor());
        return builder.build();
    }

    public static List<SteerpathAnnotationOptions> createMixedOptions(Context context, ArrayList<MetaFeature> features) {
        ArrayList<SteerpathAnnotationOptions> options = new ArrayList<>();
        int i = 0;
        for (MetaFeature feature : features) {
            switch (i) {
                case 0:
                    options.add(new SteerpathAnnotationOptions.Builder()
                            .withBaseMarkerViewOptions(createMarkerViewOptions(context, feature))
                            .floor(feature.getFloor())
                            .build());
                    break;

                case 1:
                    options.add(new SteerpathAnnotationOptions.Builder()
                            .withBaseMarkerOptions(createMarkerOptions(feature))
                            .floor(feature.getFloor())
                            .build());
                    break;

                case 2:
                    options.add(new SteerpathAnnotationOptions.Builder()
                            .withPolylineOptions(createPolylineOptions(feature))
                            .floor(feature.getFloor())
                            .build());
                    break;

                case 3:
                    options.add(new SteerpathAnnotationOptions.Builder()
                            .withPolygonOptions(createPolygonOptions(feature))
                            .floor(feature.getFloor())
                            .build());
                    break;
            }

            // some funny logic to get set of different AnnotationOptions
            i++;
            if (i > 3) {
                i = 0;
            }
        }

        return options;
    }

    /**
     * Custom Options and Views. Requires usage of MyMarkerAdapter.
     * @param feature
     * @return
     */
    /*private static MyMarkerViewOptions createMyMarkerViewOptions(Feature feature) {
        return new MyMarkerViewOptions()
                .position(new LatLng(feature.getLatitude(), feature.getLongitude()))
                //.anchor(0.5f, 0.5f)
                .title(feature.getTitle());
    }*/

    /**
     * SteerpathMarkerViewOptions build SteerpathMarkerView
     * @param feature
     * @return
     */
    private static SteerpathMarkerViewOptions createSteerpathMarkerViewOptions(MetaFeature feature) {
        return new SteerpathMarkerViewOptions()
                .position(new LatLng(feature.getLatitude(), feature.getLongitude()))
                .steerpathIcon(SteerpathIcon.FOOD_AND_DRINK)
                .title(feature.getTitle());
    }

    /**
     * MarkerViewOptions builds MarkerView.
     * @param feature
     * @return
     */
    private static MarkerViewOptions createMarkerViewOptions(Context context, MetaFeature feature) {

        String title = feature.getTitle();
        if (title.isEmpty()) {
            title = feature.getTags().toString();
        }

        return new MarkerViewOptions()
                .position(new LatLng(feature.getLatitude(), feature.getLongitude()))
                //.icon(IconFactory.getInstance(context).fromResource(R.drawable.ic_marker_filled))
                //.anchor(0.5f, 1f)
                .title(title);
    }

    /**
     * MarkerOptions builds Marker. Aka "low level" marker that trades customizability for performance.
     * @param feature
     * @return
     */
    private static MarkerOptions createMarkerOptions(MetaFeature feature) {
        return new MarkerOptions()
                .position(new LatLng(feature.getLatitude(), feature.getLongitude()))
                .title(feature.getTitle());
    }

    /**
     * https://www.mapbox.com/android-sdk/examples/polyline-simplification/
     * @param feature
     * @return
     */
    private static PolylineOptions createPolylineOptions(MetaFeature feature) {
        // some magical line that does not make sense
        List<Position> points = new ArrayList<>();
        points.add(Position.fromCoordinates(feature.getLongitude(), feature.getLatitude()));
        points.add(Position.fromCoordinates(feature.getLongitude() +0.00005, feature.getLatitude() +0.00005));
        points.add(Position.fromCoordinates(feature.getLongitude() +0.00010, feature.getLatitude() +0.00010));

        LatLng[] pointsArray = new LatLng[points.size()];
        for (int i = 0; i < points.size(); i++) {
            pointsArray[i] = new LatLng(points.get(i).getLatitude(), points.get(i).getLongitude());
        }

        return new PolylineOptions()
                .add(pointsArray)
                .color(R.color.route)
                .width(8);
    }

    /**
     * https://www.mapbox.com/android-sdk/examples/polygon/
     * @param feature
     * @return
     */
    private static PolygonOptions createPolygonOptions(MetaFeature feature) {
        // some magical hardcoded shape that does not make sense
        List<LatLng> polygon = new ArrayList<>();

        // move shape
        double x = feature.getLatitude() / 45.522585;
        double y = feature.getLongitude() / -122.685699;

        polygon.add(new LatLng(feature.getLatitude(), feature.getLongitude()));
        polygon.add(new LatLng(x * 45.534611, y * -122.708873));
        polygon.add(new LatLng(x * 45.530883, y * -122.678833));
        polygon.add(new LatLng(x * 45.547115, y * -122.667503));
        polygon.add(new LatLng(x * 45.530643, y * -122.660121));
        polygon.add(new LatLng(x * 45.533529, y * -122.636260));
        polygon.add(new LatLng(x * 45.521743, y * -122.659091));
        polygon.add(new LatLng(x * 45.510677, y * -122.648792));
        polygon.add(new LatLng(x * 45.515008, y * -122.664070));
        polygon.add(new LatLng(x * 45.502496, y * -122.669048));
        polygon.add(new LatLng(x * 45.515369, y * -122.678489));
        polygon.add(new LatLng(x * 45.506346, y * -122.702007));
        polygon.add(new LatLng(x * 45.522585, y * -122.685699));

        return new PolygonOptions()
                .addAll(polygon)
                .fillColor(R.color.route);
    }

    /**
     * This creates a custom Marker which has floating text element above location drawable.
     * @param feature
     */

    private static SteerpathMarkerViewOptions createWithFloatingText(Context context, MetaFeature feature) {
        View view = createMarkerWithFloatingTextView(context, feature);
        Drawable d = new BitmapDrawable(context.getResources(), toBitmap(view));

        // you can tint the Drawable
        d = DrawableUtils
                .withContext(context)
                .withColor(R.color.room)
                .withDrawable(d)
                .tint()
                .get();

        return new SteerpathMarkerViewOptions()
                        .position(new LatLng(feature.getLatitude(), feature.getLongitude()))
                        .title(feature.getTitle())
                        .icon(IconFactory.getInstance(context).fromDrawable(d))

                        // for more information about anchors, visit: https://developers.google.com/android/reference/com/google/android/gms/maps/model/MarkerOptions
                        //.anchor(0.5f, 1.0f)

                        .flat(true);
    }

    private static View createMarkerWithFloatingTextView(Context context, MetaFeature feature) {
        TextView v = (TextView) LayoutInflater.from(context).inflate(R.layout.marker_icon_and_text, null);
        v.setText(feature.getTitle());
        // remove compound drawable if you don't like it
        //v.setCompoundDrawables(null, null, null, null);
        return v;
    }

    private static Bitmap toBitmap(View view) {
        view.setDrawingCacheEnabled(true);

        // Without it the View will have a dimension of 0,0 and the bitmap will be null
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        view.buildDrawingCache(true);
        Bitmap b = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false); // clear drawing cache
        return b;
    }

    public static SteerpathAnnotationOptions createGeofenceOptions(Context context, JSONObject featureJson) {
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withOptions(createPolygonOptions(context, featureJson));
        builder.floor(JSONContract.getFloor(featureJson, 0));
        return builder.build();
    }

    private static PolygonOptions createPolygonOptions(Context context, JSONObject featureJson) {
        List<LatLng> polygons = new ArrayList<>();
        if (featureJson.has(K.geometry)) {
            try {
                JSONObject geometry = featureJson.getJSONObject(K.geometry);
                if (geometry.has(K.coordinates)) {
                    JSONArray coordinates = geometry.getJSONArray(K.coordinates);
                    for (int i=0; i<coordinates.length(); i++) {
                        JSONArray coordinate = coordinates.getJSONArray(i);
                        for (int j=0; j<coordinate.length(); j++) {
                            JSONArray c = coordinate.getJSONArray(j);
                            polygons.add(new LatLng(c.getDouble(1), c.getDouble(0))); // GeoJson brings LatLon in reverse order: LonLat
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return new PolygonOptions()
                .addAll(polygons)
                .alpha(0.5f)
                .strokeColor(ContextCompat.getColor(context, R.color.geofence_area))
                .fillColor(ContextCompat.getColor(context, R.color.geofence_area));
    }

    private static PolylineOptions createPolylineOptions(Context context, JSONObject featureJson) {
        List<Position> points = new ArrayList<>();
        if (featureJson.has(K.geometry)) {
            try {
                JSONObject geometry = featureJson.getJSONObject(K.geometry);
                if (geometry.has(K.coordinates)) {
                    JSONArray coordinates = geometry.getJSONArray(K.coordinates);
                    for (int i=0; i<coordinates.length(); i++) {
                        JSONArray coordinate = coordinates.getJSONArray(i);
                        for (int j=0; j<coordinate.length(); j++) {
                            JSONArray c = coordinate.getJSONArray(j);
                            points.add(Position.fromCoordinates(c.getDouble(0), c.getDouble(1)));
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        LatLng[] pointsArray = new LatLng[points.size()];
        for (int i = 0; i < points.size(); i++) {
            pointsArray[i] = new LatLng(points.get(i).getLatitude(), points.get(i).getLongitude());
        }

        return new PolylineOptions()
                .add(pointsArray)
                .color(ContextCompat.getColor(context, R.color.geofence_area))
                .width(8);
    }

    public static SteerpathAnnotationOptions createSimpleMarker(LatLng latLng, int floorIndex) {
        Object opts = new MarkerViewOptions().position(new LatLng(latLng.getLatitude(), latLng.getLongitude()));
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withOptions(opts);
        builder.floor(floorIndex);
        return builder.build();
    }

    public static SteerpathAnnotationOptions createSymbolLayerOption(String sourceId, String layerId) {
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withSteerpathLayerOptions(new SteerpathLayerOptions()
                .withSymbolLayer(new SymbolLayer(layerId, sourceId)
                                .withProperties(
                                        iconImage("my-marker-image"),
                                        iconAllowOverlap(true),

                                        textField("{title}"),
                                        textColor(Color.BLACK),
                                        textSize(20f),
                                        textAnchor(Property.TEXT_ANCHOR_TOP),
                                        textAllowOverlap(false),
                                        textHaloColor(Color.WHITE),
                                        textHaloWidth(0.5f),
                                        textHaloBlur(0.5f),
                                        // If you are not seeing text at all, check the font!
                                        textFont(new String[] {"arial"})

                                        // this is funny. You should try it.
//                                iconRotate(
//                                        zoom(
//                                                exponential(
//                                                        stop(17, iconRotate(0f)),
//                                                        stop(18, iconRotate(30f)),
//                                                        stop(19, iconRotate(60f)),
//                                                        stop(20, iconRotate(90f))
//                                                ).withBase(0.8f)
//                                        )
//                                )
                                )
                ));
        return builder.build();
    }

    /**
     * Create Mapbox FeatureCollection from Steerpath MetaFeatures
     * @param metaFeatures
     * @return
     */
    public static FeatureCollection toFeatureCollection(List<MetaFeature> metaFeatures) {
        Feature[] featureList = new Feature[metaFeatures.size()];
        for (int i=0; i<metaFeatures.size(); i++) {
            MetaFeature metaFeature = metaFeatures.get(i);
            featureList[i] = Feature.fromGeometry(Point.fromCoordinates(new double[] {metaFeature.getLongitude(), metaFeature.getLatitude()}), featureProperties(metaFeature));
        }
        return FeatureCollection.fromFeatures(featureList);
    }

    /**
     * Here you can add whatever properties to JsonObject. You can use Layer.setFilter(Filter.Statement) to
     * control what properties are actually rendered.
     *
     * Example json: {"title":"1234","type":"marker","localRef":"5678","layerIndex":4,"buildingRef":"abc","id":"59b822feb6gcba0b2b77ad99"}
     *
     * @param metaFeature
     * @return
     */
    private static JsonObject featureProperties(MetaFeature metaFeature) {
        JsonObject object = new JsonObject();
        object.add("title", new JsonPrimitive(metaFeature.getTitle()));
        object.add("type", new JsonPrimitive("marker"));
        object.add("localRef", new JsonPrimitive(metaFeature.getLocalRef()));
        object.add(SteerpathLayerOptions.LAYER_INDEX, new JsonPrimitive(metaFeature.getFloor())); // required for floor switching
        object.add(SteerpathLayerOptions.BUILDING_REF, new JsonPrimitive(metaFeature.getBuildingReference())); // required for floor switching
        object.add("id", new JsonPrimitive(metaFeature.getId()));
        return object;
    }


    /**
     * Create FillExtrusionLayer that shows only selected Feature.
     * @param feature
     * @return
     */
    public static SteerpathAnnotationOptions createFillExtrusionForLatLng(Feature feature) {
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withSteerpathLayerOptions(new SteerpathLayerOptions()
                .withFilterStatement(MapUtils.toFilterStatement(feature))
                .withFillExtrusionLayer(MapUtils.createFillExtrusionLayer(getRandomHeight(), getRandomColor(), feature)));
        return builder.build();
    }

    /**
     * Create FillExtrusionLayer that shows all Features with any tag.
     * @param tags
     * @return
     */
    public static SteerpathAnnotationOptions createFillExtrusionForTags(String... tags) {
        SteerpathAnnotationOptions.Builder builder = new SteerpathAnnotationOptions.Builder();
        builder.withSteerpathLayerOptions(new SteerpathLayerOptions()
                .withFilterStatement(MapUtils.toFilterStatement(tags))
                .withFillExtrusionLayer(MapUtils.createFillExtrusionLayer(getRandomHeight(), getRandomColor(), tags)));
        return builder.build();
    }

    private static float getRandomHeight() {
        int min = 5;
        int max = 40;
        Random r = new Random();
        return Float.valueOf(r.nextInt(max - min + 1) + min);
    }

    private static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
    }

    private AnnotationOptionsFactory() {

    }
}
