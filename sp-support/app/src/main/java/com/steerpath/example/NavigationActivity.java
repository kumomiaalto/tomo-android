package com.steerpath.example;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.steerpath.sdk.location.FusedLocationProviderApi;
import com.steerpath.sdk.location.LocationRequest;
import com.steerpath.sdk.utils.internal.Utils;

public class NavigationActivity extends AppCompatActivity implements LocationListener {
  
    private String nextDestination = "";
    private String landmark = "";
    private String directionText = "";
    private String directionImage = "";
    private String arrivalTime = "";
    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private TextView info;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);
        switchToDefaultScreenMode();
        updateDirection();
    }



    private void startPositioning() {
        if (checkBluetooth()) {
            if (checkLocationService()) {
                FusedLocationProviderApi.Api.get().requestLocationUpdates(this);

                // ALTERNATIVE:
                //FusedLocationProviderApi.Api.get().requestLocationUpdates(createLocationRequestWithGpsEnabled(), this);

                info.setText("Waiting for location...");
            }
        }
    }

    /**
     * By default, SDK has disabled GPS (priority is PRIORITY_STEERPATH_ONLY).
     * With LocationRequest, you may enable GPS and also define paramaters such as how accurate or how frequently positioning is collected.
     * Usually Steerpath advices against of enabling GPS, but this is the way you can do it.
     *
     * @return
     */
    private static LocationRequest createLocationRequestWithGpsEnabled() {
        LocationRequest request = new LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // GPS threshold determines the minimum accuracy that GPS must have in order for automatic bluetooth to GPS switch to happen.
        request.setGpsThreshold(8);
        return request;
    }



    private boolean checkBluetooth() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }

        return true;
    }

    private boolean checkLocationService() {
        if (!Utils.isLocationOn(this)) {
            // you may want to show some kind of "Enable Location Services? Yes/No" - dialog before going to Settings
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return false;
        }

        return true;
    }



    /**
     * Switch to default screen mode by pressing the flight info button
     */
    public void switchToDefaultScreenMode() {
        Button navModeButton = (Button) findViewById(R.id.infoButton);
        navModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    public void updateDirection() {
        ImageView landmark = (ImageView) findViewById(R.id.landmark);
        ImageView directionImg = (ImageView) findViewById(R.id.direction_img);
        TextView directionText = (TextView) findViewById(R.id.direction_text);
        
        landmark.setImageResource(R.mipmap.escalator);
        directionImg.setImageResource(R.mipmap.right_arrow);
        directionText.setText("Turn right towards the escalator");
    }
    
    @Override
    public void onLocationChanged(Location location) {
        
    }
    
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        
    }
    
    @Override
    public void onProviderEnabled(String s) {
        
    }
    
    @Override
    public void onProviderDisabled(String s) {
        
    }

}
