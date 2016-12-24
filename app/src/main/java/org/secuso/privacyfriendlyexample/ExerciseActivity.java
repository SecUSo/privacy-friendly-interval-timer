package org.secuso.privacyfriendlyexample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.os.CountDownTimer;
import android.widget.ToggleButton;
import android.widget.CompoundButton;

/**
 * This activity represents an exercise during the circuit workout. A timer counts down
 * the time remaining for this exercises and initialises the next exercise, once the timer
 * runs out.
 */
public class ExerciseActivity extends AppCompatActivity {

    CountDownTimer timer = null;
    long counterMillis = 65000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        timer = new ExerciseCounter(counterMillis, 1000);
        timer.start();


        ToggleButton toggle = (ToggleButton) findViewById(R.id.pauseButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    timer.cancel();
                } else {
                    timer = new ExerciseCounter(counterMillis, 1000);
                    timer.start();
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public class ExerciseCounter extends CountDownTimer
    {
        TextView textMessage = (TextView) findViewById(R.id.timerMessage);
        TextView textCounter = (TextView) findViewById(R.id.timer);

        public ExerciseCounter(long millisInFuture, long countDownInterval)
        {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish()
        {
            textMessage.setText(R.string.exercise_timer_text_after);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long minutes = millisUntilFinished/60000;
            long seconds = millisUntilFinished/1000 % 60;

            String string = String.format("%02d:%02d", minutes, seconds);

            textCounter.setText(string);
            counterMillis = millisUntilFinished;
        }
    }
}