package org.secuso.privacyfriendlytraining.activities;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.services.TimerService;

public class WorkoutActivity extends AppCompatActivity {

    //General
    private SharedPreferences settings;

    // Text
    private TextView workoutTimer = null;
    private TextView workoutTitle = null;
    private TextView currentSetsInfo = null;
    private ImageView prevTimer = null;
    private ImageView nextTimer = null;

    //GUI
    private FloatingActionButton fab = null;
    private ProgressBar progressBar = null;
    private ObjectAnimator animator = null;

    //Sound
    MediaPlayer mediaPlayer = null;

    // Service variables
    private TimerService timerService = null;
    private boolean serviceBound = false;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver();

    //Flag for prorgressbar checks if the progressbar should resume or start anew
    private boolean jumpedOverTimer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_workout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        this.progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        this.animator = ObjectAnimator.ofInt (progressBar, "progress", 0, 1000);

        // Bind to LocalService
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        this.currentSetsInfo = (TextView) this.findViewById(R.id.current_sets_info);
        this.prevTimer = (ImageView) this.findViewById(R.id.workout_previous);
        this.workoutTimer = (TextView) this.findViewById(R.id.workout_timer);
        this.workoutTitle = (TextView) this.findViewById(R.id.workout_title);
        this.nextTimer = (ImageView) this.findViewById(R.id.workout_next);

        //Set the workout screen to remain on if so enabled in settings
        if(isKeepScreenOnEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        //Set the gui to workout gui colors if start timer wasn't enabled
        if(!isStartTimerEnabled(this)) {
            setGuiColors(true);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab_pause_resume);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setSelected(!fab.isSelected());
                if (fab.isSelected()){
                    fab.setImageResource(R.drawable.ic_media_embed_play);
                    timerService.pauseTimer();
                    pauseProgressbar();
                } else {
                    fab.setImageResource(R.drawable.ic_media_pause);
                    timerService.resumeTimer();
                    resumeProgressbar();
                }
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                 //       .setAction("Action", null).show();

            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    /** Defines callbacks for service binding, passed to bindService()*/
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            serviceBound = true;

            timerService.startNotifying(false);
            updateProgressbar(true, timerService.getSavedTime());
            updateGUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    public class BroadcastReceiver extends android.content.BroadcastReceiver {
        boolean workoutColors = false;
        boolean progressBarFlip = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                int seconds = intent.getIntExtra("countdown_seconds", 0);
                workoutTimer.setText(Integer.toString(seconds));

                if(seconds <= 10 && workoutColors && isBlinkingProgressBarEnabled(context)){
                    progressBarFlip = progressBarColorFlip(workoutColors, progressBarFlip);
                }
                else if(seconds <= 5 && isBlinkingProgressBarEnabled(context)){
                    progressBarFlip = progressBarColorFlip(workoutColors, progressBarFlip);
                }
            }
            if (intent.getStringExtra("timer_title") != null) {
                String message = intent.getStringExtra("timer_title");

                workoutColors = message.equals(getResources().getString(R.string.workout_headline_workout));
                setGuiColors(workoutColors);
                workoutTitle.setText(message);
            }
            if (intent.getIntExtra("current_set", 0) != 0 && intent.getIntExtra("sets", 0) != 0) {
                int currentSet = intent.getIntExtra("current_set", 0);
                int sets = intent.getIntExtra("sets", 0);

                currentSetsInfo.setText(getResources().getString(R.string.workout_info) +": "+Integer.toString(currentSet)+"/"+Integer.toString(sets));
            }
            if (intent.getBooleanExtra("workout_finished", false) != false) {
                AlertDialog finishedAlert = buildAlert();
                finishedAlert.show();
            }
            if (intent.getLongExtra("new_timer_started", 0) != 0) {
                long time = intent.getLongExtra("new_timer_started", 0);
                updateProgressbar(!timerService.getIsPaused(), time);
            }
        }
    }

    private void setGuiColors(boolean guiFlip) {
        int textColor = guiFlip ? R.color.white : R.color.black;
        int backgroundColor = guiFlip ? R.color.lightblue : R.color.white;
        int progressBackgroundColor = guiFlip ? R.color.white : R.color.lightblue;

        currentSetsInfo.setTextColor(getResources().getColor(textColor));
        workoutTitle.setTextColor(getResources().getColor(textColor));
        workoutTimer.setTextColor(getResources().getColor(textColor));
        prevTimer.setColorFilter(getResources().getColor(textColor));
        nextTimer.setColorFilter(getResources().getColor(textColor));

        View view = findViewById(R.id.workout_content);
        view.setBackgroundColor(getResources().getColor(backgroundColor));

        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(getResources().getColor(progressBackgroundColor)));
        progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
    }

    /*
     * Changes the color of the progress bar, so that it blinks during the final seconds
     */
    private boolean progressBarColorFlip(boolean currentGUI, boolean colorFlip) {
        if(isBlinkingProgressBarEnabled(this)){
            if(this.progressBar != null && currentGUI){
                int barColor = colorFlip ? getResources().getColor(R.color.white) : getResources().getColor(R.color.colorPrimary);
                progressBar.setProgressTintList(ColorStateList.valueOf(barColor));
            }
            else if(this.progressBar != null){
                int color = colorFlip ? getResources().getColor(R.color.lightblue) : getResources().getColor(R.color.colorPrimary);
                progressBar.setProgressTintList(ColorStateList.valueOf(color));
            }
        }
        return !colorFlip;
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.workout_previous:
                this.jumpedOverTimer = true;
                this.progressBar.setProgress(0);
                timerService.prevTimer();
                break;
            case R.id.workout_next:
                this.jumpedOverTimer = true;
                this.progressBar.setProgress(0);
                timerService.nextTimer();
                break;
            default:
        }
    }

    /* Update the GUI by getting the current timer values from the TimerService */
    private void updateGUI(){
        if(timerService != null){
            int sets = timerService.getSets();
            int currentSet = timerService.getCurrentSet();
            long savedTime = timerService.getSavedTime();
            String title = timerService.getCurrentTitle();
            String time = Long.toString(savedTime/1000);
            boolean isPaused = timerService.getIsPaused();

            currentSetsInfo.setText(getResources().getString(R.string.workout_info) +": "+Integer.toString(currentSet)+"/"+Integer.toString(sets));
            workoutTitle.setText(title);
            workoutTimer.setText(time);

            if(title.equals(getResources().getString(R.string.workout_headline_done))){
                AlertDialog finishedAlert = buildAlert();
                finishedAlert.show();
            }
        }
    }

    private void updateProgressbar(boolean start, long duration){
        animator.setDuration (duration);
        if(start){ animator.start();}
    }

    private void pauseProgressbar(){
        jumpedOverTimer = false;
        this.animator.pause();
    }

    private void resumeProgressbar(){
        if(jumpedOverTimer){
            animator.start();
            jumpedOverTimer = false;
        }
        else {
            animator.resume();
        }
    }

    /*Build an AlertDialog for when the workout is finished*/
    private AlertDialog buildAlert(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(getResources().getString(R.string.workout_finished_info));
        alertBuilder.setCancelable(true);

        alertBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alertBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                timerService.cleanTimerStop();
                finish();
            }
        });

        return alertBuilder.create();
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    @Override
    public void onResume() {
        super.onResume();

        if(timerService != null){
            timerService.startNotifying(false);
        }
        updateGUI();

        registerReceiver(timeReceiver, new IntentFilter(TimerService.COUNTDOWN_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();

        if(timerService != null){
            timerService.startNotifying(true);
        }

        unregisterReceiver(timeReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /*
     * Stop all timers and remove notification when navigating back to the main activity
     */
    @Override
    public void onBackPressed() {
        timerService.cleanTimerStop();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                timerService.cleanTimerStop();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
}
