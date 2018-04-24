package com.steerpath.example;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.steerpath.example.widgets.FeatureAdapter;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaFeatureParser;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;

import org.json.JSONException;
import org.json.JSONObject;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.steerpath.sdk.meta.MetaQuery.DataType.BUILDINGS;
import static com.steerpath.sdk.meta.MetaQuery.DataType.POINTS_OF_INTEREST;

/**
 *
 */

public class MetaExampleActivity extends AppCompatActivity implements MetaLoader.LoadListener {

    private static final String TAG = MetaExampleActivity.class.getSimpleName();

    private FeatureAdapter adapter;
    private View loadingView;
    private View emptyView;
    private TextView buildingsInfo;
    private TextView poisInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meta);

        loadingView = findViewById(R.id.feature_chooser_loading_view);
        emptyView = findViewById(R.id.feature_chooser_empty_view);
        adapter = new FeatureAdapter(null, true);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.feature_chooser_recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        buildingsInfo = (TextView)findViewById(R.id.meta_info_buildings);
        buildingsInfo.setText("Loading buildings...");

        poisInfo = (TextView)findViewById(R.id.meta_info_pois);
        poisInfo.setText("Loading pois...");

        loadBuildings();
        loadAllPOIs();
        //searchPOIsByKeyword();
        //customJSONParserExample();
    }

    private void loadBuildings() {
        // if no query params are set, search for everything
        MetaLoader.load(new MetaQuery.Builder(getApplicationContext(), BUILDINGS)
                .name("buildings")
                .build(), this);
    }

    private void loadAllPOIs() {
        loadingView.setVisibility(VISIBLE);
        // if no query params are set, search for everything
        MetaLoader.load(new MetaQuery.Builder(getApplicationContext(), POINTS_OF_INTEREST)
                .name("pois")
                .build(), this);
    }

    /**
     * Query POI information from the Steerpath server.
     */
    private void searchPOIsByKeyword() {
        loadingView.setVisibility(VISIBLE);

        // Tags are case sensitive
        // Also, Steerpath Meta API does not support "keyword suggestions", i.e keywords such as
        // "ele" or "elevato" returns no results if backend knows only "Elevator".
        // However it is possible to implement keyword suggestions locally:
        // - fetch all POIs from the backend (MetaQuery with no params, see loadAllPOIs())
        // - now you can iterate through all search results and implement String comparison as you will. For example String.contains() etc.
        String[] anyTags = {"WC", "Elevator"};
        String[] allTags = {"Coffee"};
        MetaLoader.load(new MetaQuery.Builder(getApplicationContext(), POINTS_OF_INTEREST)
                //.building(building.getId()) // to search POIs from specific building
                .name("tags")
                .withAnyTags(anyTags)
                .floor(1)
                //.withAllTags(allTags)
                .build(), this);
    }

    /**
     * In case you are not happy with default JSON-to-Feature parser, you can define your own.
     */
    private void customJSONParserExample() {
        loadingView.setVisibility(VISIBLE);
        // if no query params are set, search for everything
        MetaLoader.load(new MetaQuery.Builder(getApplicationContext(), POINTS_OF_INTEREST)
                .name("customParser")
                .parser(new MyFeatureParser())
                .build(), this);
    }

    @Override
    public void onLoaded(MetaQueryResult result) {
        MetaQuery query = result.getMetaQuery();
        if (query.getName().equals("buildings")) {
            if (!result.hasError()) {
                String buildingsString = "";
                for (MetaFeature building : result.getMetaFeatures()) {
                    buildingsString += building.getTitle() + ", ";
                }
                buildingsString = buildingsString.substring(0, buildingsString.lastIndexOf(", "));
                buildingsInfo.setText("Loaded " + result.getMetaFeatures().size() + " buildings: " + buildingsString);
            } else {
                buildingsInfo.setText(result.getErrorMessage());
            }

        } else if (query.getName().equals("pois")) {
            if (!result.hasError()) {
                poisInfo.setText("Loaded " + result.getMetaFeatures().size() + " POIs:");
                loadingView.setVisibility(GONE);
                adapter.setData(result.getMetaFeatures());
            } else {
                poisInfo.setText(result.getErrorMessage());
                emptyView.setVisibility(VISIBLE);
            }

        } else if (query.getName().equals("tags")) {
            if (!result.hasError()) {
                poisInfo.setText("Loaded " + result.getMetaFeatures().size() + " POIs");
                loadingView.setVisibility(GONE);
                adapter.setData(result.getMetaFeatures());
            } else {
                poisInfo.setText(result.getErrorMessage());
                emptyView.setVisibility(VISIBLE);
            }

        } else if (query.getName().equals("customParser")) {
            if (!result.hasError()) {
                loadingView.setVisibility(GONE);
                adapter.setData(result.getMetaFeatures());
            } else {
                emptyView.setVisibility(VISIBLE);
            }
        }
    }

    private static class MyFeatureParser implements MetaFeatureParser {

        @Override
        public MetaFeature parseFeature(JSONObject featureJson) {
            return new MetaFeature.Builder()
                    .id(getText(featureJson))
                    .title("")
                    .building("")
                    .floor(0)
                    .latitude(0.0)
                    .longitude(0.0)
                    .icon("")
                    .subtype("")
                    .tags(null)
                    .hasArea(false)
                    .build();
        }
    }

    private static String getText(JSONObject json) {
        try {
            JSONObject properties = json.getJSONObject("imaginary_properties");
            return properties.getString("imaginary_cad_text");
        } catch (JSONException e) {
            Log.e(TAG, "getText: failed: " + e.getLocalizedMessage());
        }

        return "";
    }

    @Override
    public void onStop() {
        super.onStop();
        MetaLoader.removeListener(this);
    }
}

