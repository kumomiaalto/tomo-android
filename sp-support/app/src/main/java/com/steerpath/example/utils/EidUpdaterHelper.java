package com.steerpath.example.utils;

import android.content.Context;

import com.steerpath.sdk.common.SteerpathClient;
import com.steerpath.sdk.meta.MetaFeature;
import com.steerpath.sdk.meta.MetaLoader;
import com.steerpath.sdk.meta.MetaQuery;
import com.steerpath.sdk.meta.MetaQueryResult;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.steerpath.sdk.meta.MetaQuery.DataType.BUILDINGS;

/**
 * NOTE: in the future, EID updating may be moved to the SDK so app don't need to worry about it.
 * At this point app should manually trigger eid update.
 */

public class EidUpdaterHelper {

    private static List<String> BUILDING_IDS = new ArrayList<>();
    private static boolean BUILDINGS_LOADED = false;

    /**
     * Downloads EID packages for each building
     * @param context
     */
    public static void updateAllEids(final Context context) {
        BUILDINGS_LOADED = false;
        MetaQuery query = new MetaQuery.Builder(context, BUILDINGS).name(EidUpdaterHelper.class.getSimpleName()).build();
        MetaLoader.load(query, new MetaLoader.LoadListener() {
            @Override
            public void onLoaded(MetaQueryResult result) {
                BUILDINGS_LOADED = true;
                BUILDING_IDS.clear();
                for (MetaFeature building : result.getMetaFeatures()) {
                    BUILDING_IDS.add(building.getId());
                    updateEid(context, building.getId());
                }
            }
        });
    }

    private static void updateEid(Context context, String building) {
        Calendar c = Calendar.getInstance();
        // EID buffer is is here 30 days
        c.add(Calendar.DAY_OF_YEAR, 30);
        // last argument of update() is UpdateListener. You may monitor download progress but its completely optional.
        SteerpathClient.createEidUpdater(context).update(building, c.getTime(), null);
    }

    public static boolean hasValidEids() {
        if (!BUILDINGS_LOADED) {
            return false;
        }

        List<String> buildingsWithInvalidEid = SteerpathClient.getInstance().getBuildingsWithInvalidEid(BUILDING_IDS, new Date());
        // If we don't have EID for some building, here you can check if currently focused building has EID or not.
        return buildingsWithInvalidEid.isEmpty();
    }

    private EidUpdaterHelper() {

    }
}
