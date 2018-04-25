package com.steerpath.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class NavigationActivity extends AppCompatActivity {

    private String nextDestination = "";
    private String landmark = "";
    private String directionText = "";
    private String directionImage = "";
    private String arrivalTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);
        switchToDefaultScreenMode();
        updateDirection();
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
}
