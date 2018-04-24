package com.steerpath.example;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.steerpath.example.widgets.FeatureAdapter;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;

import java.util.ArrayList;

import static android.view.View.GONE;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_BUILDING;
import static com.steerpath.example.ExampleApplication.EXTRAS_HELPER_NEXT_ACTIVITY;
import static com.steerpath.sdk.meta.MetaQuery.DataType.BUILDINGS;

/**
 *
 */

public class BuildingChooserActivity extends AppCompatActivity implements MetaLoader.LoadListener {

    private FeatureAdapter adapter;
    private View loadingView;
    private View emptyView;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_building_chooser);

        loadingView = findViewById(R.id.feature_chooser_loading_view);
        emptyView = findViewById(R.id.feature_chooser_empty_view);

        boolean showCheckBoxes = false;
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.feature_chooser_recycler_view);
        adapter = new FeatureAdapter(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object tag = view.getTag();
                if (tag instanceof MetaFeature) {
                    launchMapActivity((MetaFeature)tag);
                }
            }
        }, showCheckBoxes);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating...");
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setMax(100);

        loadBuildings();
    }

    private void loadBuildings() {
        loadingView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        MetaQuery.Builder query = new MetaQuery.Builder(this, BUILDINGS);
        // if no params are added, fetch all buildings
        // OR:
        // choose meaningful coordinates here
        //query.coordinates(61.448159, 23.863781)
        //query.radius(1000)
        MetaLoader.load(query.build(), this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        if (!result.hasError()) {
            onBuildingsLoaded(result.getMetaFeatures());
        } else {
            loadingView.setVisibility(GONE);
            ((TextView)emptyView).setText(result.getError().toString());
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private void onBuildingsLoaded(final ArrayList<MetaFeature> buildings) {
        loadingView.setVisibility(GONE);
        adapter.setData(buildings);
        if (buildings.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private void launchMapActivity(MetaFeature building) {
        Class<?> cls = (Class)getIntent().getExtras().get(EXTRAS_HELPER_NEXT_ACTIVITY);
        Intent intent = new Intent(this, cls);
        intent.putExtra(EXTRAS_HELPER_BUILDING, building);
        startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        MetaLoader.removeListener(this);
    }
}
