package com.steerpath.example.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.steerpath.example.R;
import com.steerpath.sdk.maps.defaults.DefaultRouteOptionsFactory;

/**
 * Extend DefaultRouteOptionsFactory or implement RouteOptionsFactory
 */

public class RouteOptionsFactory extends DefaultRouteOptionsFactory {

    @Override
    public PolylineOptions createRouteLineOptions(Context context, int routeLineIndex, int floorSegmentIndex) {
        return new PolylineOptions()
                .color(ContextCompat.getColor(context, R.color.route))
                .width(6);
    }

    @Override
    public MarkerViewOptions createFloorUpOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f);
        View view = createIconWithTextView(context, "Go UP", R.layout.custom_floor_up_icon);
        Drawable d = new BitmapDrawable(context.getResources(), toBitmap(view));
        setDrawable(context, opts, iconFactory, d);
        return opts;
    }

    @Override
    public MarkerViewOptions createFloorDownOptions(Context context, int floor) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        MarkerViewOptions opts = new MarkerViewOptions()
                .flat(true)
                .anchor(0.5f, 0.5f); // mind the anchor value!
        View view = createIconWithTextView(context, "Go DOWN", R.layout.custom_floor_down_icon);
        Drawable d = new BitmapDrawable(context.getResources(), toBitmap(view));
        setDrawable(context, opts, iconFactory, d);
        return opts;
    }

    private static View createIconWithTextView(Context context, String text, @LayoutRes int layoutResId) {
        TextView v = (TextView) LayoutInflater.from(context).inflate(R.layout.custom_floor_down_icon, null);
        v.setText(text);
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

    private static void setDrawable(Context context, MarkerViewOptions mvo, IconFactory iconFactory, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= 21) {
            // In Lollipop++, color references works in svg
            mvo.icon(iconFactory.fromDrawable(drawable));
        } else {
            // pre-lollipop has some difficulties with tinting the svg.
            // Tint replaces all pixels, but we need to replace only one color:
            // This hack here changes the target color (#59C9EE) manually. It needs to be in given R.drawable.my_svg.xml file.
            /*Bitmap bitmap = toBitmap(context, drawable);
            int to = Color.parseColor("#59C9EE"); // color hardcoded into xml
            int from = ContextCompat.getColor(context, com.steerpath.sdk.R.color.route);
            bitmap = Utils.replacePixels(bitmap, to, from);
            mvo.icon(iconFactory.fromBitmap(bitmap));*/
        }
    }
}
