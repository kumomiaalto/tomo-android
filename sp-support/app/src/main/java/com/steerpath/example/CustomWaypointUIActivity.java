package com.steerpath.example;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.steerpath.sdk.directions.DirectionsResponse;
import com.steerpath.sdk.directions.RoutePlan;
import com.steerpath.sdk.directions.RouteStep;
import com.steerpath.sdk.directions.RouteTrackerProgress;
import com.steerpath.sdk.directions.RouteUtils;
import com.steerpath.sdk.directions.Waypoint;
import com.steerpath.sdk.maps.RoutePreviewHolder;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.maps.WaypointViewHolder;
import com.steerpath.sdk.maps.defaults.DirectionsAssetHelper;

import java.util.List;

/**
 *
 */

public class CustomWaypointUIActivity extends WaypointsActivity {

    @Override
    public void onMapViewReady(SteerpathMapView mapView) {
        super.onMapViewReady(mapView);
        mapView.setRoutePreviewHolder(new MyRoutePreviewHolder(mapView));
        mapView.setWaypointViewHolder(new MyWaypointViewHolder(mapView));
    }

    private static class MyRoutePreviewHolder implements RoutePreviewHolder {

        private final SteerpathMapView mapView;

        private MyRoutePreviewHolder(SteerpathMapView mapView) {
            this.mapView = mapView;
        }

        @Override
        public View create(Context context, RoutePlan plan, DirectionsResponse directions) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup root = (ViewGroup)inflater.inflate(R.layout.custom_route_preview_badge, null, false);

            Button startButton = (Button) root.findViewById(R.id.route_preview_start_button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.acceptRoute();
                }
            });

            ImageButton cancelButton = (ImageButton) root.findViewById(R.id.route_preview_cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.stopNavigation();
                }
            });

            // generate RouteTrackerProgress object and calculate total distance
            List<RouteStep> steps = directions.getRoutes().get(0).getSteps();
            RouteStep first = steps.get(0);
            RouteTrackerProgress progress = RouteUtils.createRouteTrackerProgress(steps, first);
            TextView distanceInfo = (TextView)root.findViewById(R.id.route_preview_distance);

            // If you are using Waypoints, you may show different info on the badge
            if (plan.waypoints.isEmpty() || plan.waypoints.size() == 1) {
                distanceInfo.setText(DirectionsAssetHelper.getComposer().compose(context, progress));

            } else {
                String newLine = "\n";
                StringBuffer buffer = new StringBuffer();
                buffer.append(DirectionsAssetHelper.getComposer().compose(context, progress));
                buffer.append(newLine);
                buffer.append("via");
                buffer.append(newLine);
                for (Waypoint waypoint : plan.waypoints) {
                    // can't identify it by name. You may also use Waypoint.getId().
                    if (waypoint.getLocation() != plan.destination) {
                        buffer.append(waypoint.getName());
                        buffer.append(newLine);
                    }
                }

                // remove last newLine
                buffer.delete(buffer.lastIndexOf(newLine), buffer.length());
                distanceInfo.setText(buffer.toString());
            }

            return root;
        }

        @Override
        public boolean useInBuiltActionButton() {
            return false;
        }
    }

    private static class MyWaypointViewHolder implements WaypointViewHolder {

        private final SteerpathMapView mapView;

        private MyWaypointViewHolder(SteerpathMapView mapView) {
            this.mapView = mapView;
        }

        @Override
        public View create(Context context, RoutePlan plan, Waypoint waypoint) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup root = (ViewGroup)inflater.inflate(R.layout.custom_waypoint_badge, null, false);

            TextView description = (TextView) root.findViewById(R.id.custom_waypoint_badge_title);
            int index = plan.waypoints.indexOf(waypoint);
            if (index < plan.waypoints.size()-1) {
                index++;
                final Waypoint next = plan.waypoints.get(index);
                String format = context.getString(R.string.sp_next_waypoint);
                description.setText(String.format(format, next.getName()));

                Button startButton = (Button) root.findViewById(R.id.custom_waypoint_start_button);
                startButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mapView.navigateTo(next);
                    }
                });
            }

            ImageButton cancelButton = (ImageButton) root.findViewById(R.id.custom_waypoint_cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.stopNavigation();
                }
            });

            return root;
        }

        @Override
        public boolean useInBuiltActionButton() {
            return false;
        }
    }
}
