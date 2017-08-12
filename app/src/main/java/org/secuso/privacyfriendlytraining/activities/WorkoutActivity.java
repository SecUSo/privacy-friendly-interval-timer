package org.secuso.privacyfriendlytraining.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.services.TimerService;

/**
 * Workout view with a workout and rest timer.
 * Timers can be paused and skipped. Once the workout is finished a message is shown and
 * the view navigates back to the main view.
 *
 * @author Alexander Karakuz
 * @version 20170809
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class WorkoutActivity extends AppCompatActivity {

    //General
    private SharedPreferences settings;

    // GUI Text
    private TextView currentSetsInfo = null;
    private TextView workoutTimer = null;
    private TextView workoutTitle = null;

    // GUI Buttons
    private FloatingActionButton fab = null;
    private ImageButton volumeButton = null;
    private ImageView prevTimer = null;
    private ImageView nextTimer = null;

    //GUI Elements
    private ProgressBar progressBar = null;

    // Service variables
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver();
    private TimerService timerService = null;
    private boolean serviceBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_workout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bind to LocalService
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Initialize all variables
        this.currentSetsInfo = (TextView) this.findViewById(R.id.current_sets_info);
        this.prevTimer = (ImageView) this.findViewById(R.id.workout_previous);
        this.progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        this.workoutTimer = (TextView) this.findViewById(R.id.workout_timer);
        this.workoutTitle = (TextView) this.findViewById(R.id.workout_title);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);
        this.nextTimer = (ImageView) this.findViewById(R.id.workout_next);
        this.volumeButton = (ImageButton) findViewById(R.id.volume_button);

        // Set the workout screen to remain on if so enabled in the settings
        if(isKeepScreenOnEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Set the gui to workout gui colors if start timer wasn't enabled
        if(!isStartTimerEnabled(this)) {
            setWorkoutGuiColors(true);
        }

        // Register the function of the pause button
        fab = (FloatingActionButton) findViewById(R.id.fab_pause_resume);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setSelected(!fab.isSelected());
                if (fab.isSelected() && timerService != null){
                    fab.setImageResource(R.drawable.ic_play_24dp);
                    timerService.pauseTimer();
                } else if (timerService != null) {
                    fab.setImageResource(R.drawable.ic_pause_24dp);
                    timerService.resumeTimer();
                }
            }
        });

        // Set image and flag of the volume button according to the current sound settings
        int volumeImageId = isSoundsMuted(this) ? R.drawable.ic_volume_mute_24dp : R.drawable.ic_volume_loud_24dp;
        volumeButton.setImageResource(volumeImageId);
        volumeButton.setSelected(isSoundsMuted(this));

        // Register the function of the volume button
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                volumeButton.setSelected(!volumeButton.isSelected());
                if (volumeButton.isSelected()){
                    volumeButton.setImageResource(R.drawable.ic_volume_mute_24dp);
                    muteAllSounds(true);
                } else {
                    volumeButton.setImageResource(R.drawable.ic_volume_loud_24dp);
                    muteAllSounds(false);
                }
            }
        });

        // Secure against Screenshot
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     * Performs an initial GUI update when connection is established.
     **/
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            serviceBound = true;

            timerService.startNotifying(false);
            updateGUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };


    /**
     * Receives the timer ticks from the service and updates the GUI accordingly
     **/
    public class BroadcastReceiver extends android.content.BroadcastReceiver {

        // Threshold when progressbar starts blinking in milliseconds and the blinking speed
        final int workoutBlinkingTime = 10000; //10 sec
        final int restBlinkingTime = 5000; // 5 sec
        int pBarBlinkInterval = 500; // every 0.5 seconds

        // Timestamp to calculate when next progressBar color change should occur
        long oldTimeStamp = workoutBlinkingTime + pBarBlinkInterval;

        // Flags for the color switches of the GUI
        boolean progressBarFlip = false;
        boolean workoutColors = false;



        /**
         * Updates the GUI depending on the message recived
         *
         * onTickMillis - Updates the progressBar progress according to current timer millis and makes it blink
         * timer_title - Updates the GUI title and switches the GUI colors accordingly
         * countdown_seconds - Updates the current seconds in the GUI
         * current_set - Updates the current sets in the GUI
         * workout_finished - Shows the final message and navigates back to the main view
         * new_timer - Resets the progressBar
         **/
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                if (intent.getLongExtra("onTickMillis", 0) != 0) {

                    long millis = intent.getLongExtra("onTickMillis", 0);
                    progressBar.setProgress((int)millis);

                    if (isProgressBarBlinking(millis, context)) {
                        progressBarFlip = progressBarColorFlip(workoutColors, progressBarFlip);
                    } else if (isProgressBarBlinking(millis, context)) {
                        progressBarFlip = progressBarColorFlip(workoutColors, progressBarFlip);
                    }
                }
                if (intent.getStringExtra("timer_title") != null) {
                    String message = intent.getStringExtra("timer_title");

                    workoutColors = message.equals(getResources().getString(R.string.workout_headline_workout));
                    setWorkoutGuiColors(workoutColors);
                    workoutTitle.setText(message);
                }
                if (intent.getIntExtra("countdown_seconds", -1) != -1) {
                    int seconds = intent.getIntExtra("countdown_seconds", 0);
                    workoutTimer.setText(Integer.toString(seconds));
                }
                if (intent.getIntExtra("current_set", 0) != 0 && intent.getIntExtra("sets", 0) != 0) {
                    int currentSet = intent.getIntExtra("current_set", 0);
                    int sets = intent.getIntExtra("sets", 0);

                    currentSetsInfo.setText(getResources().getString(R.string.workout_info) + ": " + Integer.toString(currentSet) + "/" + Integer.toString(sets));
                }
                if (intent.getBooleanExtra("workout_finished", false) != false) {
                    int caloriesBurned = intent.getIntExtra("calories_burned", 0);
                    AlertDialog finishedAlert = buildAlert(caloriesBurned);
                    finishedAlert.show();
                }
                if (intent.getLongExtra("new_timer", 0) != 0) {
                    long timerDuration = intent.getLongExtra("new_timer", 0);
                    int seconds = (int) Math.ceil(timerDuration / 1000.0);
                    workoutTimer.setText(Integer.toString(seconds));
                    progressBar.setMax((int) timerDuration);
                    progressBar.setProgress((int) timerDuration);
                    this.oldTimeStamp = workoutBlinkingTime + workoutBlinkingTime;
                }
            }
        }

        /**
         * Check if the progressbar should be blinking
         *
         * @param millis The current milliseconds
         * @param context Context
         * @return Boolean if the progressbar should be blinking
         */
        private boolean isProgressBarBlinking(long millis, Context context){
            if (millis <= workoutBlinkingTime && workoutColors && isBlinkingProgressBarEnabled(context) && oldTimeStamp - pBarBlinkInterval >= millis) {
                oldTimeStamp = millis;
                return true;
            } else if (millis <= restBlinkingTime && isBlinkingProgressBarEnabled(context) && oldTimeStamp - pBarBlinkInterval >= millis) {
                oldTimeStamp = millis;
                return true;
            }
            return  false;
        }
    }

    /**
     * Switches between colors for rest timer and workout timer
     * according to the boolean flag provided/
     *
     * @param guiFlip True for workokut phase colors, false for rest phase colors
     */
    private void setWorkoutGuiColors(boolean guiFlip) {
        int textColor = guiFlip ? R.color.white : R.color.black;
        int backgroundColor = guiFlip ? R.color.lightblue : R.color.white;
        int progressBackgroundColor = guiFlip ? R.color.white : R.color.lightblue;
        int buttonColor = guiFlip ? R.color.white : R.color.darkblue;


        currentSetsInfo.setTextColor(getResources().getColor(textColor));
        workoutTitle.setTextColor(getResources().getColor(textColor));
        workoutTimer.setTextColor(getResources().getColor(textColor));
        prevTimer.setColorFilter(getResources().getColor(buttonColor));
        nextTimer.setColorFilter(getResources().getColor(buttonColor));

        View view = findViewById(R.id.workout_content);
        view.setBackgroundColor(getResources().getColor(backgroundColor));

        LayerDrawable progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();
        Drawable backgroundDrawable = progressBarDrawable.getDrawable(0);
        Drawable progressDrawable = progressBarDrawable.getDrawable(1);
        progressDrawable.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        backgroundDrawable.setColorFilter(ContextCompat.getColor(this, progressBackgroundColor), PorterDuff.Mode.SRC_IN);

        //progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getResources().getColor(progressBackgroundColor)));
    }

    /**
     * Changes the color of the progress bar, so that it blinks during the final seconds
     *
     * @param workoutGUI boolean flag to check if the color palate is from workout or rest phase
     * @param colorFlip boolean flag used to flip between colors so the progressBar blinks
     * @return boolean flag of last color
     */
    private boolean progressBarColorFlip(boolean workoutGUI, boolean colorFlip) {
        if(isBlinkingProgressBarEnabled(this)){
            LayerDrawable progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();
            Drawable progressDrawable = progressBarDrawable.getDrawable(1);

            if(this.progressBar != null && workoutGUI){
                int barColor = colorFlip ? R.color.white : R.color.colorPrimary;
                progressDrawable.setColorFilter(ContextCompat.getColor(this, barColor), PorterDuff.Mode.SRC_IN);

                //progressBar.setProgressTintList(ColorStateList.valueOf(barColor));
            }
            else if(this.progressBar != null){
                int barColor = colorFlip ? R.color.lightblue : R.color.colorPrimary;
                progressDrawable.setColorFilter(ContextCompat.getColor(this, barColor), PorterDuff.Mode.SRC_IN);

                //progressBar.setProgressTintList(ColorStateList.valueOf(barColor));
            }
        }
        return !colorFlip;
    }

    /**
     * Click functions to go to previous or next timer and to finish the current workout.
     * On workout finish an alert is build followed by a navigation to the main view.
     *
     * @param view View
     */
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.workout_previous:
                timerService.prevTimer();
                break;
            case R.id.workout_next:
                timerService.nextTimer();
                break;
            case R.id.finish_workout:
                AlertDialog finishedAlert;
                if(timerService != null) {
                    int caloriesBurned = timerService.getCaloriesBurnt();
                    finishedAlert = buildAlert(caloriesBurned);
                    finishedAlert.show();
                }
                else {
                    finishedAlert = buildAlert(0);
                    finishedAlert.show();
                }
                stopTimerInService();
                break;
            default:
        }
    }


    /**
     * Update all GUI elements by getting the current timer values from the TimerService
     */
    private void updateGUI(){
        if(timerService != null){
            int caloriesBurned = timerService.getCaloriesBurnt();
            boolean isPaused = timerService.getIsPaused();
            int currentSet = timerService.getCurrentSet();
            String title = timerService.getCurrentTitle();
            long savedTime = timerService.getSavedTime();
            int sets = timerService.getSets();
            long timerDuration = 0;

            if(title.equals(getResources().getString(R.string.workout_headline_workout))){
                timerDuration = timerService.getWorkoutTime();
                setWorkoutGuiColors(true);
            }
            else if(title.equals(getResources().getString(R.string.workout_headline_start_timer))){
                timerDuration = timerService.getStartTime();
                setWorkoutGuiColors(false);
            }
            else if(title.equals(getResources().getString(R.string.workout_headline_rest))){
                timerDuration = timerService.getRestTime();
                setWorkoutGuiColors(false);
            }

            String time = Long.toString((int) Math.ceil(savedTime / 1000.0));

            currentSetsInfo.setText(getResources().getString(R.string.workout_info) +": "+Integer.toString(currentSet)+"/"+Integer.toString(sets));
            workoutTitle.setText(title);
            workoutTimer.setText(time);
            progressBar.setMax((int) timerDuration);
            progressBar.setProgress((int) savedTime);


            if (isPaused){
                fab.setImageResource(R.drawable.ic_play_24dp);
                fab.setSelected(true);
            }
            else {
                fab.setImageResource(R.drawable.ic_pause_24dp);
                fab.setSelected(false);
            }

            if(title.equals(getResources().getString(R.string.workout_headline_done))){
                AlertDialog finishedAlert = buildAlert(caloriesBurned);
                finishedAlert.show();
            }
        }
    }


    /**
     * Build an AlertDialog for when the workout is finished
     *
     * @param caloriesBurned Amount of calories burned during the workout
     * @return The AlertDialog
     */
    private AlertDialog buildAlert(int caloriesBurned){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        String caloriesMessage = isCaloriesEnabled(this) ? "\n" + getResources().getString(R.string.workout_finished_calories_info)+ " " +
                String.valueOf(caloriesBurned) + " kcal." : "";

        alertBuilder.setMessage(getResources().getString(R.string.workout_finished_info)+ caloriesMessage);
        alertBuilder.setCancelable(true);

        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alertBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopTimerInService();
                finish();
            }
        });

        return alertBuilder.create();
    }


    @Override
    protected void onStop() {
        super.onStop();

        // Unbind from the service
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    /**
     * Stop the notification and update the GUI with current values
     */
    @Override
    public void onResume() {
        super.onResume();

        if(timerService != null){
            timerService.startNotifying(false);
        }
        updateGUI();
        registerReceiver(timeReceiver, new IntentFilter(TimerService.COUNTDOWN_BROADCAST));
    }

    /**
     * Start the notification when activity goes into the background
     */
    @Override
    public void onPause() {
        super.onPause();

        if(timerService != null){
            timerService.startNotifying(true);
        }
        unregisterReceiver(timeReceiver);
    }


    /*
     * Stop all timers and remove notification when navigating back to the main activity
     */
    @Override
    public void onBackPressed() {
        stopTimerInService();
        super.onBackPressed();
    }


    /**
     * Calls the service to stop an clear all timer
     */
    private void stopTimerInService(){
        if(timerService != null) {
            timerService.cleanTimerStop();
        }
    }

    /**
     * Mutes or unmutes all sound output
     *
     * @param mute Flag to mute or unmute all sounds
     */
    private void muteAllSounds(boolean mute){
        if(this.settings != null) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(getResources().getString(R.string.pref_sounds_muted), mute);
            editor.apply();
        }
    }


    /*
    * Multiple checks for what was enabled inside the settings
    */
    public boolean isKeepScreenOnEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_keep_screen_on_switch_enabled), false);
        }
        return false;
    }

    public boolean isStartTimerEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_start_timer_switch_enabled), true);
        }
        return false;
    }

    public boolean isBlinkingProgressBarEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_blinking_progress_bar), false);
        }
        return false;
    }

    public boolean isCaloriesEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_calories_counter), false);
        }
        return false;
    }

    public boolean isSoundsMuted(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_sounds_muted), true);
        }
        return true;
    }
}
