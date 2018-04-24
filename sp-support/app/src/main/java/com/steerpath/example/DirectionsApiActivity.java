package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.steerpath.sdk.directions.DirectionsApi;
import com.steerpath.sdk.directions.DirectionsException;
import com.steerpath.sdk.directions.DirectionsListener;
import com.steerpath.sdk.directions.DirectionsRequest;
import com.steerpath.sdk.directions.DirectionsResponse;
import com.steerpath.sdk.directions.Route;
import com.steerpath.sdk.directions.RouteStep;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;

import static com.steerpath.sdk.meta.MetaQuery.DataType.POINTS_OF_INTEREST;

/**
 *
 */

public class DirectionsApiActivity extends AppCompatActivity implements MetaLoader.LoadListener {

    private TextView info;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions_api);
        info = (TextView) findViewById(R.id.directions_info);
        loadPOIsAndGetDirections();
    }

    private void loadPOIsAndGetDirections() {
        info.setText("Loading...");
        // if no query params are set, search for everything
        MetaQuery query = new MetaQuery.Builder(getApplicationContext(), POINTS_OF_INTEREST).build();
        MetaLoader.load(query, this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        if (!result.hasError()) {
            if (result.getMetaFeatures().size() > 1) {
                MetaFeature origin = result.getMetaFeatures().get(0);
                MetaFeature destination = result.getMetaFeatures().get(1);
                getDirections(origin, destination);
            } else {
                info.setText("Not enough POIs");
            }
        } else {
            info.setText(result.getErrorMessage());
        }
    }

    private void getDirections(final MetaFeature origin, final MetaFeature destination) {
        final DirectionsRequest req = new DirectionsRequest.Builder()
                .setSource(origin.getLatitude(), origin.getLongitude(), origin.getFloor(), origin.getTags().toString())
                .addDestination(destination.getLatitude(), destination.getLongitude(), destination.getFloor(), destination.getTags().toString())
                .build();

        DirectionsApi.getInstance().startCalculatingDirections(this, req, new DirectionsListener() {
            @Override
            public void onDirections(DirectionsResponse directions) {
                Route route = directions.getRoutes().get(0);
                StringBuffer buffer = new StringBuffer();
                String newLine = "\n";
                for (RouteStep step : route.getSteps()) {
                    buffer.append(step.toString());
                    buffer.append(newLine);
                    buffer.append(newLine);
                }

                // NOTE: if Activity dies before directions request finishes, this will crash:
                info.setText(buffer.toString());
            }

            @Override
            public void onDirectionsError(DirectionsException error) {
                info.setText(error.getLocalizedMessage());
            }

            @Override
            public void onCancelled(DirectionsRequest request) {
                info.setText("Canceled");
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        MetaLoader.removeListener(this);
    }
}
