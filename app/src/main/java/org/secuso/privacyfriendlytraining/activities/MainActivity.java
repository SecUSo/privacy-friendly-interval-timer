package org.secuso.privacyfriendlytraining.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.helpers.NotificationHelper;
import org.secuso.privacyfriendlytraining.services.TimerService;

/**
 * Main view that lets the user choose the timer intervals and has a button to start the workout
 *
 * @author Alexander Karakuz
 * @version 20170809
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class MainActivity extends BaseActivity {

    // CONFIGURE TIMER VARIABLES HERE
    // Max and min values for the workout and rest timer as well as the sets
    private int workoutMaxTime = 300; // 5 min
    private int workoutMinTime = 10; // 10 sec
    private int restMaxTime = 300; // 5 min
    private int restMinTime = 10; // 10 sec
    private int maxSets = 16;
    private int minSets = 1;

    // General
    private SharedPreferences settings = null;
    private Intent intent = null;

    // Block periodization values and Button
    private final long blockPeriodizationTimeMax = 300; // 5:00 min
    private boolean isBlockPeriodization = false;
    private long blockPeriodizationTime = 0;
    private int blockPeriodizationSets = 0;
    private Switch blockPeriodizationSwitchButton;

    // Timer values
    private final long startTime = 10; // Starttimer 10 sec
    private long workoutTime = 0;
    private long restTime = 0;
    private int sets = 0;

    // GUI text
    private TextView workoutIntervalText = null;
    private TextView restIntervalText = null;
    private TextView setsText = null;

    //Timer service variables
    private TimerService timerService = null;
    private boolean serviceBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

        //Set default values for the timer configurations
        setDefaultTimerValues();

        //Set the GUI text
        this.workoutIntervalText = (TextView) this.findViewById(R.id.main_workout_interval_time);
        this.restIntervalText = (TextView) this.findViewById(R.id.main_rest_interval_time);
        this.setsText = (TextView) this.findViewById(R.id.main_sets_amount);
        this.workoutIntervalText.setText(formatTime(workoutTime));
        this.restIntervalText.setText(formatTime(restTime));
        this.setsText.setText(Integer.toString(sets));

        //Start timer service
        overridePendingTransition(0, 0);
        startService(new Intent(this, TimerService.class));

        //Schedule the next motivation notification (necessary if permission was not granted)
        if(NotificationHelper.isMotivationAlertEnabled(this)){
            NotificationHelper.setMotivationAlert(this);
        }

        //Set the change listener for the switch button to turn block periodization on and off
        blockPeriodizationSwitchButton = (Switch) findViewById(R.id.main_block_periodization_switch);
        blockPeriodizationSwitchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isBlockPeriodization = isChecked;
            }
        });
    }


    /**
     * This method connects the Activity to the menu item
     * @return ID of the menu item it belongs to
     */
    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_main;
    }


    /**
     * Click functions for timer values, block periodization AlertDialog and workout start button
     */
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.main_workout_interval_minus:
                workoutTime = (workoutTime <= workoutMinTime) ? workoutMaxTime : workoutTime - 10;
                workoutIntervalText.setText(formatTime(workoutTime));
                break;
            case R.id.main_workout_interval_plus:
                this.workoutTime = (workoutTime >= workoutMaxTime) ? workoutMinTime : this.workoutTime + 10;
                this.workoutIntervalText.setText(formatTime(workoutTime));
                break;
            case R.id.main_rest_interval_minus:
                this.restTime = (restTime <= restMinTime) ? restMaxTime : this.restTime - 10;
                this.restIntervalText.setText(formatTime(restTime));
                break;
            case R.id.main_rest_interval_plus:
                this.restTime = (restTime >= restMaxTime) ? restMinTime : this.restTime + 10;
                this.restIntervalText.setText(formatTime(restTime));
                break;
            case R.id.main_sets_minus:
                this.sets = (sets <= minSets) ? maxSets : this.sets - 1;
                this.setsText.setText(Integer.toString(sets));
                break;
            case R.id.main_sets_plus:
                this.sets = (sets >= maxSets) ? minSets : this.sets + 1;
                this.setsText.setText(Integer.toString(sets));
                break;
            case R.id.main_block_periodization:
                AlertDialog finishedAlert = buildBlockAlert();
                finishedAlert.show();
                break;
            case R.id.start_workout:
                intent = new Intent(this, WorkoutActivity.class);
                storeDefaultTimerValues();

                if (isStartTimerEnabled(this)) {
                    timerService.startWorkout(workoutTime, restTime, startTime, sets,
                            isBlockPeriodization, blockPeriodizationTime, blockPeriodizationSets);
                }
                else {
                    timerService.startWorkout(workoutTime, restTime, 0, sets,
                            isBlockPeriodization, blockPeriodizationTime, blockPeriodizationSets);
                }

                this.startActivity(intent);
                break;
            default:
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     **/
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };



   /**
    * Build an AlertDialog for the block periodization configurations
    */
    private AlertDialog buildBlockAlert(){
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.block_periodization, null);

        Button timeButtonPlus = (Button)dialogLayout.findViewById(R.id.main_block_periodization_time_plus);
        Button timeButtonMinus = (Button)dialogLayout.findViewById(R.id.main_block_periodization_time_minus);
        Button setButtonPlus = (Button)dialogLayout.findViewById(R.id.main_block_periodization_sets_plus);
        Button setButtonMinus = (Button)dialogLayout.findViewById(R.id.main_block_periodization_sets_minus);

        final TextView setsText = (TextView)dialogLayout.findViewById(R.id.main_block_periodization_sets_amount);
        final TextView timeText = (TextView)dialogLayout.findViewById(R.id.main_block_periodization_time);

        setsText.setText(Integer.toString(blockPeriodizationSets));
        timeText.setText(formatTime(blockPeriodizationTime));

        setButtonPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(blockPeriodizationSets <= sets){ blockPeriodizationSets += 1; }
                setsText.setText(Integer.toString(blockPeriodizationSets));
            }
        });

        setButtonMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(blockPeriodizationSets > 1){ blockPeriodizationSets -= 1; }
                setsText.setText(Integer.toString(blockPeriodizationSets));
            }
        });


        timeButtonPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(blockPeriodizationTime < blockPeriodizationTimeMax){ blockPeriodizationTime += 10; }
                timeText.setText(formatTime(blockPeriodizationTime));
            }
        });

        timeButtonMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(blockPeriodizationTime > 10){ blockPeriodizationTime -= 10; }
                timeText.setText(formatTime(blockPeriodizationTime));
            }
        });


        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setView(dialogLayout);

        alertBuilder.setTitle(getResources().getString(R.string.main_block_periodization_headline)).setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                });

        return alertBuilder.create();
    }


    /**
     * Initializes the timer values for the GUI. Previously chosen setup is retrieved if one exists.
     */
    private void setDefaultTimerValues(){
        if(settings != null ) {
            this.workoutTime = settings.getInt(this.getString(R.string.pref_timer_workout), 10);
            this.restTime = settings.getInt(this.getString(R.string.pref_timer_rest), 20);
            this.sets = settings.getInt(this.getString(R.string.pref_timer_set), 5);
            this.blockPeriodizationTime = settings.getInt(this.getString(R.string.pref_timer_periodization_time), 150);
            this.blockPeriodizationSets = settings.getInt(this.getString(R.string.pref_timer_periodization_set), 1);
        }
        else {
            this.workoutTime = 10;
            this.restTime = 20;
            this.sets = 5;
            this.blockPeriodizationTime = 150;
            this.blockPeriodizationSets = 1;
        }
    }

    /**
     * Stores the chosen timer values for next GUI initialization
     */
    private void storeDefaultTimerValues(){
        if(settings != null ) {
            SharedPreferences.Editor editor = this.settings.edit();
            editor.putInt(this.getString(R.string.pref_timer_workout),(int) this.workoutTime);
            editor.putInt(this.getString(R.string.pref_timer_rest),(int) this.restTime);
            editor.putInt(this.getString(R.string.pref_timer_set), this.sets);
            editor.putInt(this.getString(R.string.pref_timer_periodization_time), (int) this.blockPeriodizationTime);
            editor.putInt(this.getString(R.string.pref_timer_periodization_set), this.blockPeriodizationSets);
            editor.commit();
        }
    }


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
    public void onDestroy() {
        timerService.startNotifying(false);
        stopService(new Intent(this, TimerService.class));
        super.onDestroy();
    }


    /**
     * Helper methods and preference checks
     */
    public boolean isStartTimerEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_start_timer_switch_enabled), false);
        }
        return false;
    }

    private String formatTime(long seconds){
        long min = seconds/60;
        long sec = seconds%60;

        String time = String.format("%02d : %02d", min,sec);

        return time;
    }
}
