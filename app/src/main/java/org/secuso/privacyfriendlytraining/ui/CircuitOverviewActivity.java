package org.secuso.privacyfriendlytraining.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.secuso.privacyfriendlytraining.R;

/**
 * After choosing a circuit the user ist provided with an overview over the exercises in it.
 * This activity provides this overview and allows for the workout to begin by pressing the
 * "circuit_start_button".
 */
public class CircuitOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circuit_overview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onClick(View view) {

        Intent intent = null;

        switch(view.getId()) {
            case R.id.circuit_start_button:
                intent = new Intent(this, WorkoutActivity.class);
                break;
            default:
                break;
        }

        if (intent != null){
            startActivity(intent);
        }
    }

}
