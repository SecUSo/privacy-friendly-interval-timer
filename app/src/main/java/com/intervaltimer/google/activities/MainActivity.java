/**
 * This file is part of Privacy Friendly Interval Timer.
 * Privacy Friendly Interval Timer is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or any later version.
 * Privacy Friendly Interval Timer is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Privacy Friendly Interval Timer. If not, see <http://www.gnu.org/licenses/>.
 */

package com.intervaltimer.google.activities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.intervaltimer.google.R;
import com.intervaltimer.google.helpers.NotificationHelper;
import com.intervaltimer.google.services.TimerService;
import com.intervaltimer.google.tutorial.PrefManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Main view that lets the user choose the timer intervals and has a button to start the workout
 *
 * @author Alexander Karakuz
 * @version 20170809
 */
public class MainActivity extends BaseActivity {

    // Constants for input
    private final String timeVerificationPattern = "[0-9]{1,2} ?: ?[0-9]{1,2}";
    private final String setsVerificationPattern = "[0-9]*";


    // Default values for the timers
    private final int workoutTimeDefault = 60;
    private final int restTimeDefault = 30;
    private final int setsDefault = 6;
    private final int blockPeriodizationTimeDefault = 90;
    private final int blockPeriodizationSetsDefault = 1;

    // General
    private SharedPreferences settings = null;
    private PrefManager prefManager = null;
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
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_personalization, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_statistics, true);
        PreferenceManager.setDefaultValues(this, R.xml.pref_workout, true);

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

        //Suggest the user to enter his body data
        prefManager = new PrefManager(this);
        if(prefManager.isFirstTimeLaunch()){
            prefManager.setFirstTimeLaunch(false);
            showPersonalizationAlert();
        }
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
        SharedPreferences.Editor editor = this.settings.edit();


        switch(view.getId()) {
            case R.id.main_block_periodization:
                AlertDialog blockAlert = buildBlockAlert();
                blockAlert.show();
                break;
            case R.id.main_block_periodization_text:
                this.blockPeriodizationSwitchButton.setChecked(!this.blockPeriodizationSwitchButton.isChecked());
                break;
            case R.id.start_workout:
                intent = new Intent(this, WorkoutActivity.class);

                boolean parseFail = false;

                // Get and parse the workout interval
                EditText workoutText = (EditText)findViewById(R.id.main_workout_interval_time);
                String workoutValue = workoutText.getText().toString();
                if (!(workoutValue.matches(timeVerificationPattern))) {
                    workoutText.setError("Enter a time in the format mm:ss");
                    parseFail = true;
                }

                try {
                    this.workoutTime = parseTime(workoutValue);
                } catch (Exception e) {
                    e.printStackTrace();
                    workoutText.setError(e.getMessage());
                    parseFail = true;
                }

                // Get and parse the rest interval
                EditText restText = (EditText)findViewById(R.id.main_rest_interval_time);
                String restValue = restText.getText().toString();
                if (!(restValue.matches(timeVerificationPattern))) {
                    restText.setError("Enter a time in the format mm:ss");
                    parseFail = true;
                }

                try {
                    this.restTime = parseTime(restValue);
                } catch (Exception e) {
                    e.printStackTrace();
                    restText.setError(e.getMessage());
                    parseFail = true;
                }

                // Get and parse the number of sets
                EditText setsText = (EditText)findViewById(R.id.main_sets_amount);
                String setsValue = setsText.getText().toString();
                if (!(setsValue.matches(setsVerificationPattern))) {
                    setsText.setError("Enter an integer");
                    parseFail = true;
                }

                try {
                    this.sets = Integer.parseInt(setsValue);
                } catch (Exception e){
                    e.printStackTrace();
                }

                // Start the workout if there were no problems
                if (!parseFail) {
                    if (isStartTimerEnabled(this)) {
                        timerService.startWorkout(workoutTime, restTime, startTime, sets,
                                isBlockPeriodization, blockPeriodizationTime, blockPeriodizationSets);
                    } else {
                        timerService.startWorkout(workoutTime, restTime, 0, sets,
                                isBlockPeriodization, blockPeriodizationTime, blockPeriodizationSets);
                    }

                    this.startActivity(intent);
                }

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
        blockPeriodizationSets = (blockPeriodizationSets >= sets) ? sets-1 : blockPeriodizationSets;
        blockPeriodizationSets = (blockPeriodizationSets <= 0) ? 1 : blockPeriodizationSets;

        setsText.setText(Integer.toString(blockPeriodizationSets));
        timeText.setText(formatTime(blockPeriodizationTime));

        setButtonPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();

                if(blockPeriodizationSets < sets-1){ blockPeriodizationSets += 1; }
                setsText.setText(Integer.toString(blockPeriodizationSets));
                editor.putInt(getString(R.string.pref_timer_periodization_set), blockPeriodizationSets);
                editor.commit();
            }
        });

        setButtonMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();

                if(blockPeriodizationSets > 1){ blockPeriodizationSets -= 1; }
                setsText.setText(Integer.toString(blockPeriodizationSets));
                editor.putInt(getString(R.string.pref_timer_periodization_set), blockPeriodizationSets);
                editor.commit();
            }
        });


        timeButtonPlus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();

                if(blockPeriodizationTime < blockPeriodizationTimeMax){ blockPeriodizationTime += 10; }
                timeText.setText(formatTime(blockPeriodizationTime));
                editor.putInt(getString(R.string.pref_timer_periodization_time), (int) blockPeriodizationTime);
                editor.commit();

            }
        });

        timeButtonMinus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();

                if(blockPeriodizationTime > 10){ blockPeriodizationTime -= 10; }
                timeText.setText(formatTime(blockPeriodizationTime));
                editor.putInt(getString(R.string.pref_timer_periodization_time), (int) blockPeriodizationTime);
                editor.commit();
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

    private void showPersonalizationAlert(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

        alertBuilder.setTitle(R.string.alert_personalization_title);
        alertBuilder.setMessage(R.string.alert_personalization_message);

        alertBuilder.setNegativeButton(getString(R.string.alert_confirm_dialog_negative), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });


        alertBuilder.setPositiveButton(getString(R.string.alert_confirm_dialog_positive), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent i = new Intent( MainActivity.this, SettingsActivity.class );
                i.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.PersonalizationPreferenceFragment.class.getName() );
                i.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                startActivity(i);
            }
        });

        alertBuilder.create().show();
    }


    /**
     * Initializes the timer values for the GUI. Previously chosen setup is retrieved if one exists.
     */
    private void setDefaultTimerValues(){
        if(settings != null ) {
            this.workoutTime = settings.getInt(this.getString(R.string.pref_timer_workout), workoutTimeDefault);
            this.restTime = settings.getInt(this.getString(R.string.pref_timer_rest), restTimeDefault);
            this.sets = settings.getInt(this.getString(R.string.pref_timer_set), setsDefault);
            this.blockPeriodizationTime = settings.getInt(this.getString(R.string.pref_timer_periodization_time), blockPeriodizationTimeDefault);
            this.blockPeriodizationSets = settings.getInt(this.getString(R.string.pref_timer_periodization_set), blockPeriodizationSetsDefault);
        }
        else {
            this.workoutTime = workoutTimeDefault;
            this.restTime = restTimeDefault;
            this.sets = setsDefault;
            this.blockPeriodizationTime = blockPeriodizationTimeDefault;
            this.blockPeriodizationSets = blockPeriodizationSetsDefault;
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
        timerService.setIsAppInBackground(false);
        stopService(new Intent(this, TimerService.class));
        super.onDestroy();
    }


    /**
     * Helper methods and preference checks
     */
    public boolean isStartTimerEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_start_timer_switch_enabled), true);
        }
        return false;
    }

    private String formatTime(long seconds){
        long min = seconds/60;
        long sec = seconds%60;

        String time = String.format("%02d : %02d", min,sec);

        return time;
    }

    /**
     * A method to parse time entered by the user
     *
     * @param stringTime the time string entered by the user
     * @return an int representing the time in seconds
     * @throws NumberFormatException for: null string, empty string, too many sections, minute or
     * second ranges out of bounds
     */
    private int parseTime(String stringTime) throws NumberFormatException {
        if (stringTime == null)
            throw new NumberFormatException("parseTimeString null str");
        if (stringTime.isEmpty())
            throw new NumberFormatException("parseTimeString empty str");

        int minutes, seconds;

        // Split the string
        String units[] = stringTime.split(":");

        // Throw an exception if the string the wrong length
        if (units.length != 2)
            throw new NumberFormatException("parseTimeString too many sections");

        // Get the sections from the string
        minutes = Integer.parseInt(units[0].trim());
        seconds = Integer.parseInt(units[1].trim());

        // Check that the sections are within the correct range
        if ((minutes < 0) || (minutes > 60) || (seconds < 0) || (seconds > 60))
            throw new NumberFormatException("parseTimeString range error. The values for minutes and seconds must be within [0,60]");

        // Return the result
        return (minutes * 60) + seconds;
    }
}
