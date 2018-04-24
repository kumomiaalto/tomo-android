package com.steerpath.example.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.steerpath.example.R;
import com.steerpath.sdk.directions.Waypoint;
import com.steerpath.sdk.maps.defaults.DefaultRouteOptionsFactory;
import com.steerpath.sdk.utils.internal.Utils;

/**
 *
 */

public class PreviewRouteOptionsFactory extends DefaultRouteOptionsFactory {

    @Override
    public int getRouteLineMode() {
        // show route segments from each floor
        return ROUTELINE_MODE_FULL;
    }

    @Override
    public PolylineOptions createRouteLineOptions(Context context, int routeLineIndex, int floorSegmentIndex) {

        // Assume route takes place in three floors. Three RouteLines are created, one per each floor.
        // When floor is selected, only corresponding RouteLine is rendered.
        //
        // ======================
        // RouteLine index 2        ----> this is same than "selected floor index"
        // ======================
        // RouteLine index 1
        // ======================
        // RouteLine index 0
        // ======================

        // RouteLine can contain multiple FloorSegments:
        //
        // ======================
        // RouteLine index 2
        // - FloorSegment index 2   ---> this is currently rendered route line
        // - FloorSegment index 1
        // - FloorSegment index 0
        // ======================
        // RouteLine index 1
        // - FloorSegment index 2
        // - FloorSegment index 1
        // - FloorSegment index 0
        // ======================
        // RouteLine index 0
        // - FloorSegment index 2
        // - FloorSegment index 1
        // - FloorSegment index 0
        // ======================

        // If RouteLine and FloorSegment indices matches, it means we are rendering route line for currently selected floor.
        //
        // Examples:
        // * routeLineIndex == 0 && floorSegmentIndex == 0
        // ---> we are at bottom floor and we are rendering route line for the bottom floor
        //
        // * routeLineIndex == 0 && floorSegmentIndex == 1
        // ---> we are at the bottom floor and rendering route line for the first floor

        float alpha;
        int color;

        if (routeLineIndex == floorSegmentIndex) {
            alpha = 1.0f;
            color = R.color.route_2;

        } else {
            int delta = Math.abs(routeLineIndex - floorSegmentIndex);
            if (delta == 1) {
                alpha = 0.30f;
            } else if (delta == 2) {
                alpha = 0.20f;
            } else {
                alpha = 0.10f;
            }

            color = R.color.route_2_other_floor;
        }

        return new PolylineOptions()
                .color(ContextCompat.getColor(context, color))
                .alpha(alpha)
                .width(6);
    }

    // change the color of other elements as well:

    @Override
    public MarkerViewOptions createOriginOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        setDrawable(context, opts, iconFactory, R.drawable.ic_route_segment_start_point);
        return opts;
    }

    @Override
    public MarkerViewOptions createDestinationOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        setDrawable(context, opts, iconFactory, R.drawable.ic_route_end_point);
        return opts;
    }

    @Override
    public MarkerViewOptions createFloorUpOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        setDrawable(context, opts, iconFactory, R.drawable.ic_sp_travel_upward);
        return opts;
    }

    @Override
    public MarkerViewOptions createFloorDownOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        setDrawable(context, opts, iconFactory, R.drawable.ic_sp_travel_downward);
        return opts;
    }

    @Override
    public MarkerViewOptions createWaypointOptions(Context context, Waypoint waypoint, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        setDrawable(context, opts, iconFactory, com.steerpath.sdk.R.drawable.ic_route_segment_start_point);
        return opts;
    }

    private static void setDrawable(Context context, MarkerViewOptions mvo, IconFactory iconFactory, @DrawableRes int icon) {
        Bitmap bitmap = Utils.vectorToBitmap(context, icon);
        int from = ContextCompat.getColor(context, R.color.route);//Color.parseColor("#59C9EE"); // color hardcoded into xml
        int to = ContextCompat.getColor(context, R.color.route_2);
        bitmap = Utils.replacePixels(bitmap, from, to);
        mvo.icon(iconFactory.fromBitmap(bitmap));
    }
}
