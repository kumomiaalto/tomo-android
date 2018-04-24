package com.steerpath.example;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.camera.CameraPosition;
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
import com.steerpath.sdk.directions.RouteUtils;
import com.steerpath.sdk.directions.Waypoint;
import com.steerpath.sdk.location.LocationTimeoutListener;
import com.steerpath.sdk.maps.DirectionSheet;
import com.steerpath.sdk.maps.OnMapReadyCallback;
import com.steerpath.sdk.maps.RoutePreviewHolder;
import com.steerpath.sdk.maps.RouteStepViewHolder;
import com.steerpath.sdk.maps.SteerpathAnnotation;
import com.steerpath.sdk.maps.SteerpathAnnotationOptions;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.maps.SteerpathMapFragment;
import com.steerpath.sdk.maps.SteerpathMapView;
import com.steerpath.sdk.maps.defaults.DirectionsAdapter;
import com.steerpath.sdk.maps.defaults.DirectionsAssetHelper;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.utils.LocalizationHelper;
import com.steerpath.sdk.utils.internal.Utils;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;

/**
 *
 */

public class CustomDirectionsUIActivity extends AppCompatActivity implements SteerpathMapFragment.MapViewListener {

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

        // Optional helpers to customize user visible texts and icons
        LocalizationHelper.setTranslator(new MyTranslator());
        DirectionsAssetHelper.setIconChooser(new MyIconChooser());
        DirectionsAssetHelper.setComposer(new MyComposer());

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
                    showRoutePreview(selectedPOI);
                } else {
                    Toast.makeText(this, "Select destination first", Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapViewReady(final SteerpathMapView mapView) {
        this.mapView = mapView;

        setupCustomUI();

        // add some random button for no reason
        addCustomButton();

        // intercept FollowMe-button clicks. See also EidUpdaterActivity.
        mapView.setLocateMeButtonListener(new SteerpathMapView.LocateMeButtonListener() {
            @Override
            public boolean onClick(SteerpathMap.MapMode mapMode) {
                if (map != null && map.getUserLocation() == null) {
                    // o-no, no location available! Can't go to "Follow Me" mode and can't start navigation!
                    Toast.makeText(mapView.getContext(), "No location!", Toast.LENGTH_LONG).show();

                    // NOTE: if map is initially in MapMode.NORMAL mode, this prevents map mode to be changed.
                    // You can use map.setMapMode(); to change initial MapMode.
                    return true; // consume event
                }

                // false: calls internally map.toggleMapMode()
                return false;
            }
        });

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final SteerpathMap steerpathMap) {
                map = steerpathMap;
                // enable positioning
                map.setMyLocationEnabled(true);

                MetaFeature building = getIntent().getParcelableExtra(EXTRAS_HELPER_BUILDING);
                MapHelper.moveCameraTo(map, building);

                listenLocationTimeouts();
                setupOnMapClickListener();
            }
        });
    }

    /**
     * Change appearance of Route Preview Bagde, Step-by-Step Badge and Directions List
     */
    private void setupCustomUI() {
        // route preview badge
        mapView.setRoutePreviewHolder(new MyRoutePreviewHolder(mapView));

        // step-by-step badge
        RouteStepViewHolder peekBadge = new MyRouteStepViewHolder(mapView);
        mapView.setRouteStepViewHolder(peekBadge);

        // directions list
        mapView.setDirectionSheet(new MyDirectionSheet(peekBadge));
    }

    private void listenLocationTimeouts() {
        // Use FusedLocationProviderApi.Api.get().setLocationTimeout(); to change location timeout window
        map.setLocationTimeoutListener(new LocationTimeoutListener() {
            @Override
            public void onLocationTimeout() {
                // Now map.getUserLocation() returns null
                Toast.makeText(CustomDirectionsUIActivity.this, "Location timeout!", Toast.LENGTH_SHORT).show();
            }
        });
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

    private void showRoutePreview(MetaFeature destination) {
        RoutePlan plan = new RoutePlan.Builder()
                .destination(destination)

                // change initial preview mode zoom level
                .previewCameraPadding(300)

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
            distanceInfo.setText(DirectionsAssetHelper.getComposer().compose(context, progress));

            return root;
        }

        @Override
        public boolean useInBuiltActionButton() {
            return false;
        }
    }

    // example how to make custom route step badge
    private static class MyRouteStepViewHolder implements RouteStepViewHolder {
        private final SteerpathMapView mapView;
        private ImageView image;
        private TextView stepInfo;
        private TextView distanceInfo;
        private DirectionsResponse directions;
        // RouteTrackerProgress does not carry "action" information but RouteStep does. Keep it for later updates.
        private RouteStep latestRouteStep;

        private MyRouteStepViewHolder(SteerpathMapView mapView) {
            this.mapView = mapView;
        }

        @Override
        public View create(Context context, DirectionsResponse directions, RouteStep routeStep) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ViewGroup stepBadge = (ViewGroup)inflater.inflate(R.layout.custom_route_step_badge, null, false);
            image = (ImageView)stepBadge.findViewById(R.id.my_step_badge_image);
            stepInfo = (TextView)stepBadge.findViewById(R.id.my_step_badge_directions);
            distanceInfo = (TextView)stepBadge.findViewById(R.id.my_step_badge_distance);
            Button stopButton = (Button) stepBadge.findViewById(R.id.my_step_badge_stop_button);
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mapView.stopNavigation();
                }
            });
            this.directions = directions;
            latestRouteStep = routeStep;
            return stepBadge;
        }

        @Override
        public void onReRoute(Context context, DirectionsResponse directions) {
            this.directions = directions;
        }

        @Override
        public void update(Context context, RouteStep step) {
            // NOTE: you may use in-built helpers to obtain proper icon and text for this RouteStep.
            latestRouteStep = step;
            image.setImageResource(DirectionsAssetHelper.getIconChooser().getDrawable(context, step));
            stepInfo.setText(DirectionsAssetHelper.getComposer().compose(context, step));
        }

        @Override
        public void update(Context context, RouteTrackerProgress progress) {
            // NOTE: you may use in-built helpers to obtain proper icon and text for this RouteTrackerProgress.
            stepInfo.setText(DirectionsAssetHelper.getComposer().compose(context, latestRouteStep, progress));
            distanceInfo.setText(DirectionsAssetHelper.getComposer().compose(context, directions, latestRouteStep, progress));
        }

        @Override
        public void onDestinationReached(Context context) {
            // NOTE: you may use in-built helpers to obtain proper icon and text when user has reached destination
            // yay were are here!
            stepInfo.setText(DirectionsAssetHelper.getComposer().getDestinationReachedMessage(context));
            image.setImageResource(DirectionsAssetHelper.getIconChooser().getDestinationReachedDrawableRes());
            distanceInfo.setText("");
        }

        @Override
        public boolean useInBuiltActionButton() {
            return false;
        }
    }

    // example how to make custom DirectionSheet
    private class MyDirectionSheet implements DirectionSheet {

        private final RouteStepViewHolder peekBadge;
        private DirectionsAdapter adapter;

        private MyDirectionSheet(RouteStepViewHolder peekBadge) {
            this.peekBadge = peekBadge;
        }

        @Override
        public void show(final Context context, final Data data) {
            // DirectionSheet does not necessarily have to be BottomSheetDialog, but it is pretty convenient!
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);

            LayoutInflater li = LayoutInflater.from(context);
            View view = li.inflate(R.layout.custom_direction_sheet, null);
            final RecyclerView recyclerView = (RecyclerView)view.findViewById(R.id.directions_recycler_view);

            // you can use in-built Adapter or create one of your own
            adapter = new DirectionsAdapter(view.getContext());
            adapter.setOnRouteStepSelectedListener(new DirectionsAdapter.OnRouteStepClickListener() {
                @Override
                public void onClick(RouteStep step) {
                    bottomSheetDialog.dismiss();
                    // camera keeps moving to the user location if map and bluedot modes are not changed
                    map.setMapMode(SteerpathMap.MapMode.NORMAL);
                    map.setBlueDotMode(SteerpathMap.BlueDotMode.NORMAL);

                    zoomToRouteStep(step);
                    updatePeekBadge(recyclerView.getContext(), step);

                    // If we have fixed origin, update UI.
                    // User Location based updates won't be called. Instead, update UI with selected
                    // RouteStep
                    if (data.plan.origin != null) {
                        peekBadge.update(context, step);
                        peekBadge.update(context, RouteUtils.createRouteTrackerProgress(data.steps, step));
                    }
                }
            });

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

            update(context, data);

            bottomSheetDialog.setContentView(view);
            // you can manipulate BottomSheet Behaviour from here:
            //populateFromTheTop(context, view, recyclerView);

            bottomSheetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    mapView.notifyDirectionSheetCanceled();
                }
            });

            bottomSheetDialog.show();
        }

        @Override
        public void update(Context context, Data data) {

            // remove past steps
            int index = data.steps.indexOf(data.currentStep);
            adapter.setSteps(data.steps.subList(index, data.steps.size()));

            adapter.setCurrentStep(data.currentStep);
            adapter.setProgress(data.progress);
            adapter.notifyDataSetChanged();
        }

        private void populateFromTheTop(Context context, View view, RecyclerView recyclerView) {
            // With little tricks BottomSheetDialog opens and populates RouteStep starting from the top
            // Trick #1: get magically suitable height for RecyclerView: screen height - status bar height
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            int magicHeight = metrics.heightPixels - Utils.statusBarHeight(context.getResources());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, magicHeight);
            recyclerView.setLayoutParams(params);

            // Trick #2: change initial state
            BottomSheetBehavior behavior = BottomSheetBehavior.from((View)view.getParent());
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true); // forget "collapsed" state
        }

        private void zoomToRouteStep(RouteStep step) {
            if (!step.getCoordinates().isEmpty()) {
                LatLng first = step.getCoordinates().get(0);
                map.setCameraPosition(new CameraPosition.Builder()
                        .target(first)
                        .zoom(20)
                        .build());
                map.setActiveLevel(step.getBuildingRef(), step.getFloor());
            }
        }

        private void updatePeekBadge(Context context, RouteStep step) {
            peekBadge.update(context, step);
        }
    }

    private void addCustomButton() {
        FloatingActionButton button = (FloatingActionButton) getLayoutInflater().inflate(R.layout.external_fab, null);

        // LayoutParams needs to be set programmatically. If declared in xml, they seems to be ignored for some reason.
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = getResources().getDimensionPixelSize(R.dimen.fab_margin_between_buttons);
        int marginRight = getResources().getDimensionPixelSize(R.dimen.fab_margin_right);
        params.setMargins(0, margin, marginRight, margin);
        params.gravity = Gravity.RIGHT;
        button.setLayoutParams(params);
        mapView.addCustomView(button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "Hello", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * LocalizationHelper.Translator allows you to localize user visible strigns that are coming from the backend.
     * Subclass or implement your own.
     */
    private static class MyTranslator extends LocalizationHelper.DefaultTranslator {
        @Nullable
        @Override
        public String translate(Context context, String string) {
            if (string != null) {
                if (string.equals("skytrain")) { // <--- this may come from the server
                    return context.getString(R.string.example_translation_skytrain); // "skytrain" to "Skytrain"
                }
            }

            return super.translate(context, string);
        }
    }

    /**
     * Subclass or implement your own.
     */
    private static class MyIconChooser extends DirectionsAssetHelper.DefaultChooser {
        @Override
        public int getDrawable(Context context, RouteStep step) {
            if (step.getAction().equals(RouteStep.ACTION_GO)) {
                String travelType = step.getTravelType();
                if (travelType != null) {
                    if (travelType.equals("skytrain")) {
                        return R.drawable.ic_directions_railway;
                    }
                }
            }

            return super.getDrawable(context, step);
        }
    }

    /**
     * Subclass or implement your own.
     */
    private static class MyComposer extends DirectionsAssetHelper.DefaultComposer {

        @Override
        public String compose(Context context, RouteStep step) {
            if (step.getAction().equals(RouteStep.ACTION_GO)) {
                String travelType = LocalizationHelper.getTranslator().translate(context, step.getTravelType());
                String destination = LocalizationHelper.getTranslator().translate(context, step.getDestinationName());
                return "Hop in " + travelType + " and go to " + destination + " !";
            }

            return super.compose(context, step);
        }

        @Override
        public String compose(Context context, RouteStep step, RouteTrackerProgress progress) {
            if (step.getAction().equals(RouteStep.ACTION_GO)) {
                String travelType = LocalizationHelper.getTranslator().translate(context, step.getTravelType());
                String destination = LocalizationHelper.getTranslator().translate(context, step.getDestinationName());
                return "Hop in " + travelType + " and go to " + destination + " !";
            }

            return super.compose(context, step, progress);
        }

        @Override
        public String getDestinationReachedMessage(Context context) {
            return "You are here!";
        }
    }
}
