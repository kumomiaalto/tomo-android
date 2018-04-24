package com.example.helena.tomo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class NavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation);
        switchToDefaultScreenMode();
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
}

