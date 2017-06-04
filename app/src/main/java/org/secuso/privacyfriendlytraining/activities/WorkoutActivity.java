package org.secuso.privacyfriendlytraining.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

    //Buttons
    private FloatingActionButton fab = null;

    // Service variables
    private TimerService timerService = null;
    private boolean serviceBound = false;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // Bind to LocalService
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        this.workoutTimer = (TextView) this.findViewById(R.id.workout_timer);
        this.workoutTitle = (TextView) this.findViewById(R.id.workout_title);
        this.currentSetsInfo = (TextView) this.findViewById(R.id.current_sets_info);

        if(isKeepScreenOnEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        fab = (FloatingActionButton) findViewById(R.id.fab_pause_resume);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setSelected(!fab.isSelected());
                if (fab.isSelected()){
                    fab.setImageResource(R.drawable.ic_media_embed_play);
                    timerService.pauseTimer();
                } else {
                    fab.setImageResource(R.drawable.ic_media_pause);
                    timerService.resumeTimer();
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

            updateGUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    public class BroadcastReceiver extends android.content.BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                int seconds = intent.getIntExtra("countdown_seconds", 0);
                workoutTimer.setText(Integer.toString(seconds));
            }
            if (intent.getStringExtra("timer_title") != null) {
                String message = intent.getStringExtra("timer_title");
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
        }
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.workout_previous:
                timerService.prevTimer();
                break;
            case R.id.workout_next:
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
            String title = timerService.getCurrentTitle();
            String time = Integer.toString(timerService.getSavedTime());
            boolean isPaused = timerService.getIsPaused();

            currentSetsInfo.setText(getResources().getString(R.string.workout_info) +": "+Integer.toString(currentSet)+"/"+Integer.toString(sets));
            workoutTitle.setText(title);
            workoutTimer.setText(time);

            if (isPaused){
                fab.setSelected(!fab.isSelected());
                fab.setImageResource(R.drawable.ic_media_embed_play);
            } else {
                fab.setImageResource(R.drawable.ic_media_pause);
            }
        }
    }

    /*Build an AlertDialog for when the workout is finished*/
    private AlertDialog buildAlert(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(getResources().getString(R.string.workout_finished_info));
        alertBuilder.setCancelable(true);

        alertBuilder.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alertBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
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
        timerService.startNotifying(true);
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

        registerReceiver(timeReceiver, new IntentFilter(TimerService.COUNTDOWN_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(timeReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /*Stop all timers and remove notification when navigating back to the main activity*/
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

    public boolean isKeepScreenOnEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_keep_screen_on_switch_enabled), true);
        }
        return false;
    }
}
