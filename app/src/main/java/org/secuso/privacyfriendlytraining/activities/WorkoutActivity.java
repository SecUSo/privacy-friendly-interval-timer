package org.secuso.privacyfriendlytraining.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.services.TimerService;

public class WorkoutActivity extends AppCompatActivity {

    private TextView workoutTimer = null;
    private TextView workoutTitle = null;
    private TextView currentSetsInfo = null;

    private long workoutTime = 0;
    private long restTime = 0;
    private int sets = 0;

    private TimerService timerService = null;
    private boolean serviceBound = false;
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.workoutTime =  getIntent().getExtras().getLong("workoutTime");
        this.restTime =  getIntent().getExtras().getLong("restTime");
        this.sets =  getIntent().getExtras().getInt("sets");

        this.workoutTimer = (TextView) this.findViewById(R.id.workout_timer);
        this.workoutTitle = (TextView) this.findViewById(R.id.workout_title);
        this.currentSetsInfo = (TextView) this.findViewById(R.id.current_sets_info);
        this.currentSetsInfo.setText(getResources().getString(R.string.workout_info) + ": 1/"+sets);


        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_pause_resume);
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

        startService(new Intent(this, TimerService.class));
    }



    /** Defines callbacks for service binding, passed to bindService()*/
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            serviceBound = true;
            timerService.startWorkout(workoutTime*1000, restTime*1000, sets);
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
            if (intent.getStringExtra("timer_finished") != null) {
                String message = intent.getStringExtra("timer_finished");
                workoutTitle.setText(message);
            }
            if (intent.getIntExtra("current_set", 0) != 0) {
                int currentSet = intent.getIntExtra("current_set", 0);
                currentSetsInfo.setText(getResources().getString(R.string.workout_info) +": "+Integer.toString(currentSet)+"/"+Integer.toString(sets));
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

    //TO DO: WHICH ARE NECESSARY
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, TimerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        registerReceiver(timeReceiver, new IntentFilter(TimerService.COUNTDOWN_BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(timeReceiver);
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, TimerService.class));
        super.onDestroy();
    }
}
