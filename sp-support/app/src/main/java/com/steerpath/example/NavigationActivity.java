package com.steerpath.example;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.steerpath.sdk.location.FusedLocationProviderApi;
import com.steerpath.sdk.location.LocationRequest;
import com.steerpath.sdk.utils.internal.Utils;
import java.util.HashMap;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private TextView info;

    private Map<String, Integer> directionVisualOutputMap = new HashMap();
    private Map<String, String> directionTextOutputMap = new HashMap();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);
        switchToDefaultScreenMode();
        createDirectionToInterfaceMapping();
        //updateDirection("straight", "elevator");
        demo();
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

    /**
     * Define the mapping of direction input to visual and textual direction output
     */
    public void createDirectionToInterfaceMapping() {
        // define direction mapping to visual mapping
        directionVisualOutputMap.put("right", R.mipmap.right_arrow);
        directionVisualOutputMap.put("left", R.mipmap.left_arrow);
        directionVisualOutputMap.put("straight", R.mipmap.straight_arrow);
        directionVisualOutputMap.put("back", R.mipmap.back_arrow);
        directionVisualOutputMap.put("slight right", R.mipmap.slight_right);
        directionVisualOutputMap.put("slight left", R.mipmap.slight_left);
        directionVisualOutputMap.put("right u-turn", R.mipmap.right_u_turn);
        directionVisualOutputMap.put("left u-turn", R.mipmap.left_u_turn);
        directionVisualOutputMap.put("right s-turn", R.mipmap.right_s_turn);
        directionVisualOutputMap.put("left s-turn", R.mipmap.left_s_turn);

        directionVisualOutputMap.put("elevator", R.mipmap.elevator);
        directionVisualOutputMap.put("escalator", R.mipmap.escalator);
        directionVisualOutputMap.put("seat", R.mipmap.seat);
        directionVisualOutputMap.put("wc", R.mipmap.wc);
        directionVisualOutputMap.put("ramp", R.mipmap.ramp);

        // define direction mapping to textual mapping
        directionTextOutputMap.put("right", "Turn right");
        directionTextOutputMap.put("left", "Turn left");
        directionTextOutputMap.put("straight", "Continue straight");
        directionTextOutputMap.put("back", "Turn backwards");
        directionTextOutputMap.put("slight right", "Turn slight right");
        directionTextOutputMap.put("right u-turn", "Make a right u-turn");
        directionTextOutputMap.put("left u-turn", "Make a left u-turn");
        directionTextOutputMap.put("right s-turn", "Make a right s-turn");
        directionTextOutputMap.put("left s-turn", "Make a left s-turn");

        directionTextOutputMap.put("elevator", "towards the elevator");
        directionTextOutputMap.put("escalator", "towards the escalator");
        directionTextOutputMap.put("seat", "towards the seats");
        directionTextOutputMap.put("wc", "towards the bathroom");
        directionTextOutputMap.put("ramp", "towards the ramp");
    }

    /**
     * Update the navigation direction interface according to the given direction and landmark
     * @param direction direction to navigate to
     * @param landmark landmark to navigate to, if the is no landmark, pass landmark=""
     */
    public void updateDirection(String direction, String landmark) {
        ImageView landm = (ImageView) findViewById(R.id.landmark);
        ImageView directionImg = (ImageView) findViewById(R.id.direction_img);
        TextView directionText = (TextView) findViewById(R.id.direction_text);

        if(landmark == "") {
            landm.setImageResource(0);
            directionText.setText(directionTextOutputMap.get(direction));
        } else {
            landm.setImageResource(directionVisualOutputMap.get(landmark));
            directionText.setText(directionTextOutputMap.get(direction)+" "+
                    directionTextOutputMap.get(landmark));
        }
        directionImg.setImageResource(directionVisualOutputMap.get(direction));
    }

    public void demo() {
        updateDirection("right", "");
        //try {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("straight", "");
            }
        }, 3000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("left", "");
            }
        }, 7000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("straight", "");
            }
        }, 10000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("left", "");
            }
        }, 22000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("right", "");
            }
        }, 28000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("straight", "");
            }
        }, 31000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("left u-turn", "escalator");
            }
        }, 68000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("right", "");
            }
        }, 86000);
        handler.postDelayed(new Runnable() {
            public void run() {
                updateDirection("straight", "");
            }
        }, 89000);
        handler.postDelayed(new Runnable() {
            public void run() {
                ImageView directionImg = (ImageView) findViewById(R.id.direction_img);
                TextView directionText = (TextView) findViewById(R.id.direction_text);
                directionImg.setImageResource(directionVisualOutputMap.get("right"));
                directionText.setText("Your gate is on your right");
            }
        }, 99000);
    }
    

}
