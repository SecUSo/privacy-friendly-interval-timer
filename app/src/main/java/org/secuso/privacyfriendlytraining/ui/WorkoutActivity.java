package org.secuso.privacyfriendlytraining.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.os.CountDownTimer;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import android.view.WindowManager;
import org.secuso.privacyfriendlytraining.R;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.media.AudioManager;
import android.app.Notification;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.NotificationManager;

/**
 * This activity represents an exercise during the circuit workout. A timer counts down
 * the time remaining for this exercises and initialises the next exercise, once the timer
 * runs out.
 *
 * TO DO: Remaining logic will be implemented once the exercises are finished
 */
public class WorkoutActivity extends AppCompatActivity {

    CircuitTimer timer = null;
    ToggleButton toggle = null;

    //Flag to make sure onResume() is not executed together with onCreate()
    private boolean resumeHasRun = false;

    //Check if activity is in focus to prevent next Exercise from starting
    private boolean activityIsPaused = false;

    //Workout notifications with sounds being short and long
    MediaPlayer mpShort = null;
    MediaPlayer mpLong = null;
    AudioManager audioMgr = null;
    Vibrator v = null;

    //Fixed times for now, later times adjust to exercises
    long preExerciseTime = 10000;
    long exerciseTime = 10000;
    long restTime = 15000;
    long savedTime = 0;

    //Notification to notify the user to continue his workout
    Notification.Builder notiBuilder = null;
    NotificationManager notiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Initialize workout notification sounds/vibrations
        mpShort = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        mpLong = MediaPlayer.create(getApplicationContext(), R.raw.beep_long);
        audioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //Update information about the first exercise here

        timer = new CircuitTimer(preExerciseTime, 1000, new PreExerciseFinish());
        timer.start();

        toggle = (ToggleButton) findViewById(R.id.pauseButton);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mpShort.pause();
                    mpLong.pause();
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
        buildNotification();
    }

    @Override
    public void onPause() {
        super.onPause();
        activityIsPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!resumeHasRun) {
            resumeHasRun = true;
            return;
        }
        else {
            activityIsPaused = false;
            //Continue to next exercise once user returns to the app
            if(timer.finishedDuringPause) {
                timer.getFinishMethod().performOnFinish();
            }
        }
    }

    public void buildNotification() {
        notiBuilder = new Notification.Builder(this);
        notiBuilder.setContentTitle(this.getResources().getString(R.string.app_name));
        notiBuilder.setContentText(this.getResources().getString(R.string.resume_your_workout));
        notiBuilder.setSmallIcon(R.drawable.ic_menu_info);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notiBuilder.setContentIntent(pendingIntent);
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * Plays either a sound or vibrates on every second of the last 5 seconds during an exercise
     * or before a new exercise starts. This feedback is adjusted to the users ringtone settings.
     * If the user e.g. has his mobile on vibrate, this feedback will vibrate as well.
     * @param seconds Amount of seconds left in the exercise
     */
    public void playFeedback(int seconds) {
        switch(audioMgr.getRingerMode()){
            case AudioManager.RINGER_MODE_NORMAL:
                if (seconds <= 5 && seconds > 1) { mpShort.start(); }
                else if(seconds == 1){ mpLong.start(); }
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                if (seconds <= 5 && seconds > 1) { v.vibrate(200); }
                else if(seconds == 1){ v.vibrate(400); }
                break;
        }
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
        private boolean finishedDuringPause = false;
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
            if(!activityIsPaused) {
                finishMethod.performOnFinish();
            }
            else {
                finishedDuringPause = true;
                notiManager.notify(1, notiBuilder.build());
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long minutes = millisUntilFinished/60000;
            long seconds = millisUntilFinished/1000%60;

            String string = String.format("%02d:%02d", minutes, seconds);

            textTimer.setText(string);
            savedTime = millisUntilFinished;


            if (seconds <= 5 && !activityIsPaused) {
                playFeedback((int)seconds);
            }
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
            //1. Update description
            //2. Start first exercise timer
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
