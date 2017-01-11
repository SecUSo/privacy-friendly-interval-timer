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
 *
 * TO DO: Remaining logic will be implemented once the exercises are finished
 */
public class ExerciseActivity extends AppCompatActivity {

    CircuitTimer timer = null;
    ToggleButton toggle = null;

    //Fixed times for now, later times adjust to exercises
    long preExerciseTime = 10000;
    long exerciseTime = 30000;
    long restTime = 15000;
    long savedTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CountDownTimer preTimer = new CircuitTimer(preExerciseTime, 1000, new PreExerciseFinish());
        preTimer.start();

        toggle = (ToggleButton) findViewById(R.id.pauseButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    timer.cancel();
                } else {
                    timer = new CircuitTimer(savedTime, 1000, timer.getFinishMethod());
                    timer.start();
                }
            }
        });
        toggle.setEnabled(false);
        toggle.setBackgroundResource(R.drawable.button_disabled);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * This class represents the countdown timer, showing the user how much time
     * is left for the exercise or his pause in between exercises.
     * The timer requires an OnTimerFinish object, which provides the method to be performed,
     * once it hits 00:00 (see onFinish()).
     */
    public class CircuitTimer extends CountDownTimer
    {
        TextView textTimer = (TextView) findViewById(R.id.timer);
        OnTimerFinish finishMethod;

        /**
         * Creates a new timer for circuit exercises
         *
         * @param millisInFuture Time for the timer to run in milliseconds
         * @param countDownInterval The countdown intervals
         * @param finishMethod Object which defines what the timer does once he runs out
         */
        public CircuitTimer(long millisInFuture, long countDownInterval,
                              OnTimerFinish finishMethod)
        {
            super(millisInFuture, countDownInterval);
            this.finishMethod = finishMethod;
        }

        @Override
        public void onFinish()
        {
            finishMethod.performOnFinish();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long minutes = millisUntilFinished/60000;
            long seconds = millisUntilFinished/1000 % 60;

            String string = String.format("%02d:%02d", minutes, seconds);

            textTimer.setText(string);
            savedTime = millisUntilFinished;
        }

        public OnTimerFinish getFinishMethod(){
            return this.finishMethod;
        }
    }



    /* Strategy pattern for the different finish behaviours of the timer
    *
    *  The following classes implement different finish method variations to be executed once the
    *  timer finishes.
    *
    *  The PreExerciseFinish variant, enables the pause button, updates the gui with the first
    *  exercise description and starts the exercise timer.
    *
    *  The ExerciseFinish variant, checks if exercises are left. If so, it updates the description
    *  to show the upcoming exercise and starts the timer for the rest period. Otherwise the
    *  workout ends.
    *
    *  The RestFinish variant, updates the description (from upcoming to current exercise) and
    *  starts the exercise timer.
    * */
    public interface OnTimerFinish {
        void performOnFinish();
    }

    public class PreExerciseFinish implements OnTimerFinish {
        TextView textMessage = (TextView) findViewById(R.id.timerMessage);

        @Override
        public void performOnFinish() {
            //1. Take first exercise from list
            //2. Update description
            //3. Start first exercise timer
            textMessage.setText(R.string.exercise_timer_text_during);
            toggle.setEnabled(true);
            toggle.setBackgroundResource(R.drawable.button_fullwidth);
            timer = new CircuitTimer(exerciseTime, 1000, new ExerciseFinish());
            timer.start();
        }
    }

    public class ExerciseFinish implements OnTimerFinish {
        TextView textMessage = (TextView) findViewById(R.id.timerMessage);

        @Override
        public void performOnFinish() {
            //if(exercises left)
                //Take next exercise
                //Update description to show upcoming exercise after the break
                textMessage.setText(R.string.exercise_timer_text_break);
                timer = new CircuitTimer(restTime, 1000, new RestFinish());
                timer.start();
            //else
                //finish output
        }
    }

    public class RestFinish implements OnTimerFinish {
        TextView textMessage = (TextView) findViewById(R.id.timerMessage);

        @Override
        public void performOnFinish() {
            //Update description
            textMessage.setText(R.string.exercise_timer_text_during);
            timer = new CircuitTimer(exerciseTime, 1000, new ExerciseFinish());
            timer.start();
        }
    }
}