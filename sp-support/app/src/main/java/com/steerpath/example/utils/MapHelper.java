package com.steerpath.example.utils;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.steerpath.sdk.maps.SteerpathMap;
import com.steerpath.sdk.meta.MetaFeature;

/**
 *
 */

public class MapHelper {

    public static void moveCameraTo(SteerpathMap map, MetaFeature feature) {
        // Calling setActiveLevel() here is bad idea because user may not actually be in this floor.
        // Causes confusion when testing on site.
        //map.setActiveLevel(feature.getId(), feature.getFloor());
        map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(feature.getLatitude(), feature.getLongitude()))
                .zoom(18)
                //.tilt(40)
                .build());
    }

    /**
     * Limit camera panning outside of defined region. This example here does not allow camera to move
     * outside of Helsinki region in Finland.
     * @param map
     */
    public static void setCameraBounds(SteerpathMap map) {
        map.getMapboxMap().setMinZoomPreference(14);
        map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(60.171878, 24.941432)) // Helsinki Railway Station
                .zoom(16)
                .build());
        map.getMapboxMap().setLatLngBoundsForCameraTarget(new LatLngBounds.Builder()
                .include(new LatLng(60.176933, 24.897113)) // North-West
                .include(new LatLng(60.146451, 24.970666)) // South-East
                .build());
    }

    private MapHelper() {

    }
}
