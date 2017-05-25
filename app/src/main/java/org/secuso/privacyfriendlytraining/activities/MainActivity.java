package org.secuso.privacyfriendlytraining.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.tutorial.PrefManager;
import org.secuso.privacyfriendlytraining.tutorial.TutorialActivity;

public class MainActivity extends BaseActivity {

    private long workoutTime;
    private long restTime;
    private int sets;

    private TextView workoutIntervalText;
    private TextView restIntervalText;
    private TextView setsText;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Default values for  the workout configuration
        workoutTime = 10;
        restTime = 20;
        sets = 5;


        this.workoutIntervalText = (TextView) this.findViewById(R.id.main_workout_interval_time);
        this.restIntervalText = (TextView) this.findViewById(R.id.main_rest_interval_time);
        this.setsText = (TextView) this.findViewById(R.id.main_sets_amount);

        this.workoutIntervalText.setText(formatTime(workoutTime));
        this.restIntervalText.setText(formatTime(restTime));
        this.setsText.setText(Integer.toString(sets));


//        PFASQLiteHelper database = new PFASQLiteHelper(getBaseContext());
//        database.addSampleData(new PFASampleDataType(0, "eins.de", "hugo1", 11));
//        database.addSampleData(new PFASampleDataType(0, "zwei.de", "hugo2", 12));
//        database.addSampleData(new PFASampleDataType(0, "drei.de", "hugo3", 13));
//        database.addSampleData(new PFASampleDataType(0, "vier.de", "hugo4", 14));

//        DatabaseExporter porter = new DatabaseExporter(getBaseContext().getDatabasePath(PFASQLiteHelper.DATABASE_NAME).toString(), "PF_EXAMPLE_DB");
//
//        try {
//            porter.dbToJSON();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        // Use the a button to display the welcome screen
        Button b = (Button) findViewById(R.id.button_welcomedialog);
        if(b != null) {
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    WelcomeDialog welcomeDialog = new WelcomeDialog();
//                    welcomeDialog.show(getFragmentManager(), "WelcomeDialog");
                    PrefManager prefManager = new PrefManager(getBaseContext());
                    prefManager.setFirstTimeLaunch(true);
                    Intent intent = new Intent(MainActivity.this, TutorialActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });
        }

        overridePendingTransition(0, 0);
    }

    /**
     * This method connects the Activity to the menu item
     * @return ID of the menu item it belongs to
     */
    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_example;
    }


//    public static class WelcomeDialog extends DialogFragment {
//
//        @Override
//        public void onAttach(Activity activity) {
//            super.onAttach(activity);
//        }
//
//        @Override
//        public Dialog onCreateDialog(Bundle savedInstanceState) {
//
//            LayoutInflater i = getActivity().getLayoutInflater();
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            builder.setView(i.inflate(R.layout.welcome_dialog, null));
//            builder.setIcon(R.mipmap.icon);
//            builder.setTitle(getActivity().getString(R.string.welcome));
//            builder.setPositiveButton(getActivity().getString(R.string.okay), null);
//            builder.setNegativeButton(getActivity().getString(R.string.viewhelp), new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    ((MainActivity)getActivity()).goToNavigationItem(R.id.nav_help);
//                }
//            });
//
//            return builder.create();
//        }
//    }


    //Intervals
    //http://www.dtb-online.de/portal/verband/service-fuer-mitglieder/ratgeber-gesundheit/funktionelles-zirkeltraining.html
    //http://www.sportunterricht.de/lksport/circuitkraft.html
    //Added additional 15 seconds to rest and 30 seconds to exercise for user convenience
    //Usual maximum of sets is 12 but I added additional 4 just in case
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.main_workout_interval_minus:
                this.workoutTime = (workoutTime <= 10) ? 90 : this.workoutTime - 10;
                this.workoutIntervalText.setText(formatTime(workoutTime));
                break;
            case R.id.main_workout_interval_plus:
                this.workoutTime = (workoutTime >= 90) ? 10 : this.workoutTime + 10;
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
                this.sets = (sets <= 1) ? 16 : this.sets - 1;
                this.setsText.setText(Integer.toString(sets));
                break;
            case R.id.main_sets_plus:
                this.sets = (sets >= 16) ? 1 : this.sets + 1;
                this.setsText.setText(Integer.toString(sets));
                break;
            case R.id.start_workout:
                intent = new Intent(this, WorkoutActivity.class);
                intent.putExtra("workoutTime", this.workoutTime);
                intent.putExtra("restTime", this.restTime);
                intent.putExtra("sets", this.sets);
                this.startActivity(intent);
                break;
            default:
        }
    }

    //Helper methods
    private String formatTime(long seconds){
        long min = seconds/60;
        long sec = seconds%60;

        String time = String.format("%02d : %02d", min,sec);

        return time;
    }
}
