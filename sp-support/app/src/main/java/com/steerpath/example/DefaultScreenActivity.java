package com.steerpath.example;

import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;
import java.text.DateFormat;

public class DefaultScreenActivity extends AppCompatActivity {

    private String gate = "G14";
    private String boarding = "11:10";
    private int timeToGate = 10;
    private int freetime = 25;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.default_screen);
        setFlightInfo();
        switchToNavMode();
    }

    /**
     * Make device vibrate for 1,5s
     * @param v
     */
    public void vibrate(View v) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        for(int i = 0; i<5; i++) {
            vibrator.vibrate(1500);  //vibrate for 1,5s
            //TODO: pause for a second
        }
    }

    public void setFlightInfo() {
        TextView gate = (TextView) findViewById(R.id.gate);
        TextView boarding = (TextView) findViewById(R.id.boarding);
        TextView timeToSec = (TextView) findViewById(R.id.timeToGate);
        TextView freetime = (TextView) findViewById(R.id.freetime);

        gate.setText(this.gate);
        boarding.setText(this.boarding);
        timeToSec.setText(this.timeToGate+"min");
        freetime.setText(this.freetime+"min");
    }

    /*public void switchToNavMode(View v) {
        setContentView(R.layout.navigation);
    }

    public void switchToFlightInfoMode(View v) {
        setContentView(R.layout.default_screen);
        setFlightInfo();
    }*/

    /**
     * Switch to navigation mode by pressing the navigation button
     */
    public void switchToNavMode() {
        Button navModeButton = (Button) findViewById(R.id.navButton);
        navModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DefaultScreenActivity.this, NavigationActivity.class));
            }
        });
    }
}
