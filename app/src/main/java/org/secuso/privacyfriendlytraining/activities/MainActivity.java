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
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.services.TimerService;

import java.util.ArrayList;


public class MainActivity extends BaseActivity {

    // General
    private SharedPreferences settings = null;
    private Intent intent = null;

    // Timer values
    private final long blockPeriodizationTimeMax = 300; // 5:00 min
    private final long startTime = 10;
    private long blockPeriodizationTime = 0;
    private long workoutTime = 0;
    private long restTime = 0;
    private int blockPeriodizationSets = 0;
    private int sets = 0;
    private boolean isBlockPeriodization = false;

    // GUI text
    private TextView workoutIntervalText = null;
    private TextView restIntervalText = null;
    private TextView setsText = null;

    //Service variables
    private TimerService timerService = null;
    private boolean serviceBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

        //Set default values for  the workout configuration
        getDefaultTimerValues();

        this.workoutIntervalText = (TextView) this.findViewById(R.id.main_workout_interval_time);
        this.restIntervalText = (TextView) this.findViewById(R.id.main_rest_interval_time);
        this.setsText = (TextView) this.findViewById(R.id.main_sets_amount);

        this.workoutIntervalText.setText(formatTime(workoutTime));
        this.restIntervalText.setText(formatTime(restTime));
        this.setsText.setText(Integer.toString(sets));


       overridePendingTransition(0, 0);
       startService(new Intent(this, TimerService.class));
    }



    /**
     * This method connects the Activity to the menu item
     * @return ID of the menu item it belongs to
     */
    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_main;
    }


    //Intervals
    //http://www.dtb-online.de/portal/verband/service-fuer-mitglieder/ratgeber-gesundheit/funktionelles-zirkeltraining.html
    //http://www.sportunterricht.de/lksport/circuitkraft.html
    //Added additional 15 seconds to rest and 30 seconds to exercise for user convenience
    //Usual maximum of sets is 12 but I added additional 4 just in case
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.main_workout_interval_minus:
                this.workoutTime = (workoutTime <= 10) ? 120 : this.workoutTime - 10;
                this.workoutIntervalText.setText(formatTime(workoutTime));
                break;
            case R.id.main_workout_interval_plus:
                this.workoutTime = (workoutTime >= 120) ? 10 : this.workoutTime + 10;
                this.workoutIntervalText.setText(formatTime(workoutTime));
                break;
            case R.id.main_rest_interval_minus:
                this.restTime = (restTime <= 0) ? 60 : this.restTime - 10;
                this.restIntervalText.setText(formatTime(restTime));
                break;
            case R.id.main_rest_interval_plus:
                this.restTime = (restTime >= 60) ? 0 : this.restTime + 10;
                this.restIntervalText.setText(formatTime(restTime));
                break;
            case R.id.main_sets_minus:
                this.sets = (sets <= 1) ? 15 : this.sets - 1;
                this.setsText.setText(Integer.toString(sets));
                break;
            case R.id.main_sets_plus:
                this.sets = (sets >= 15) ? 1 : this.sets + 1;
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

    /** Defines callbacks for service binding, passed to bindService()*/
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



    /*Build an AlertDialog for the block periodization*/
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

        final CharSequence[] item = { getResources().getString(R.string.main_block_periodization) };
        final boolean[] selection = { isBlockPeriodization };
        final ArrayList selectedItem = new ArrayList();

        alertBuilder.setTitle(getResources().getString(R.string.main_block_periodization_headline))
                .setMultiChoiceItems(item, selection, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                        isBlockPeriodization = isChecked;
                        if (isChecked) {
                            selectedItem.add(indexSelected);
                        } else if (selectedItem.contains(indexSelected)) {
                            selectedItem.remove(Integer.valueOf(indexSelected));
                        }
                    }
                }).setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                });

        return alertBuilder.create();
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

    //Helper methods
    private String formatTime(long seconds){
        long min = seconds/60;
        long sec = seconds%60;

        String time = String.format("%02d : %02d", min,sec);

        return time;
    }

    /*
     * Initializes the timer values for the GUI. Previously chosen setup is retrieved if one exists.
     */
    private void getDefaultTimerValues(){
        if(settings != null ) {
            this.workoutTime = settings.getInt("WORKOUT_TIME", 10);
            this.restTime = settings.getInt("REST_TIME", 20);
            this.sets = settings.getInt("SETS", 5);
            this.blockPeriodizationTime = settings.getInt("PERIODIZATION_TIME", 150);
            this.blockPeriodizationSets = settings.getInt("PERIODIZATION_SETS", 1);
        }
        else {
            this.workoutTime = 10;
            this.restTime = 20;
            this.sets = 5;
            this.blockPeriodizationTime = 150;
            this.blockPeriodizationSets = 1;
        }
    }

    /*
     * Stores the chosen timer values for next GUI initialization
     */
    private void storeDefaultTimerValues(){
        if(settings != null ) {
            SharedPreferences.Editor editor = this.settings.edit();
            editor.putInt("WORKOUT_TIME",(int) this.workoutTime);
            editor.putInt("REST_TIME",(int) this.restTime);
            editor.putInt("SETS", (int) this.sets);
            editor.putInt("PERIODIZATION_TIME", (int) this.blockPeriodizationTime);
            editor.putInt("PERIODIZATION_SETS", this.blockPeriodizationSets);
            editor.commit();
        }
    }


    /*
     * Check if the setting for a start timer (before the workout beginns) is enabled
     */
    public boolean isStartTimerEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_start_timer_switch_enabled), false);
        }
        return false;
    }
}
