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

package com.intervaltimer.google.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import com.intervaltimer.google.R;
import com.intervaltimer.google.adapters.ReportAdapter;
import com.intervaltimer.google.database.PFASQLiteHelper;
import com.intervaltimer.google.models.ActivityChart;
import com.intervaltimer.google.models.ActivityDayChart;
import com.intervaltimer.google.models.ActivitySummary;
import com.intervaltimer.google.models.WorkoutSessionData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activities that contain this fragment must implement the
 * {@link WeeklyReportFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WeeklyReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * @author Tobias Neidig, Alexander Karakuz
 * @version 20170708
 */
public class WeeklyReportFragment extends Fragment implements ReportAdapter.OnItemClickListener {
    public static String LOG_TAG = WeeklyReportFragment.class.getName();

    private ReportAdapter mAdapter;
    private RecyclerView mRecyclerView;

    private OnFragmentInteractionListener mListener;

    private Calendar day;
    private ActivitySummary activitySummary;
    private ActivityChart activityChart;
    private List<Object> reports = new ArrayList<>();
    private boolean generatingReports;


    public WeeklyReportFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of WeeklyReportFragment.
     */
    public static WeeklyReportFragment newInstance() {
        WeeklyReportFragment fragment = new WeeklyReportFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_daily_report, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view);

        // specify an adapter
        day = Calendar.getInstance();
        generateReports();
        mAdapter = new ReportAdapter(reports);
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        if(day == null){
            day = Calendar.getInstance();
        }
    }


    /**
     * @return is the day which is currently shown today?
     */
    private boolean isTodayShown() {
        if(day == null){
            return false;
        }
        Calendar start = (Calendar) day.clone();
        start.set(Calendar.DAY_OF_WEEK, day.getFirstDayOfWeek());
        start.set(Calendar.MILLISECOND, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.HOUR_OF_DAY, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.WEEK_OF_YEAR, 1);
        return (start.before(day) || start.equals(day)) && end.after(day);
    }

    /**
     * Generates the report objects and adds them to the recycler view adapter.
     * The following reports will be generated:
     * * ActivitySummary
     * * ActivityChart
     * If one of these reports does not exist it will be created and added at the end of view.
     */
    private void generateReports() {
        if (!this.isTodayShown() || isDetached() || getContext() == null || generatingReports) {
            Log.i(LOG_TAG, "Skipping generating reports");
            // the day shown is not today or is detached
            return;
        }
        Log.i(LOG_TAG, "Generating reports");
        generatingReports = true;
        final Context context = getActivity().getApplicationContext();
        final Locale locale = context.getResources().getConfiguration().locale;
        final PFASQLiteHelper database = new PFASQLiteHelper(context);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Get all workout data for this day.
                day.set(Calendar.DAY_OF_WEEK, day.getFirstDayOfWeek());
                Calendar start = (Calendar) day.clone();

                SimpleDateFormat formatDate = new SimpleDateFormat("dd.MM", locale);
                SimpleDateFormat idDateFormat = new SimpleDateFormat("yyyyMMdd");

                Map<String, Double> timeData = new LinkedHashMap<>();
                Map<String, Double> caloriesData = new LinkedHashMap<>();
                timeData.put("", null);
                caloriesData.put("", null);

                int totalTime = 0;
                int totalCalories = 0;
                for (int i = 0; i < 7; i++) {

                    int id = Integer.parseInt(idDateFormat.format(start.getTime()));
                    WorkoutSessionData statistics = database.getWorkoutData(id);

                    int time = statistics.getWORKOUTTIME();
                    int calories = statistics.getCALORIES();

                    timeData.put(formatDate.format(start.getTime()), (double) time/60);
                    caloriesData.put(formatDate.format(start.getTime()), (double) calories);
                    totalTime += time;
                    totalCalories += calories;
                    if (i != 6) {
                        start.add(Calendar.DAY_OF_MONTH, 1);
                    }
                }

                timeData.put(" ", null);
                caloriesData.put(" ", null);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.", locale);
                SimpleDateFormat simpleDateMonthFormat = new SimpleDateFormat("dd. MMMM", locale);

                String title = simpleDateFormat.format(day.getTime()) + " - " + simpleDateMonthFormat.format(start.getTime());

                // create view models
                if (activitySummary == null) {
                    activitySummary = new ActivitySummary(totalTime, totalCalories, title);
                    reports.add(activitySummary);
                } else {
                    activitySummary.setTime(totalTime);
                    activitySummary.setCalories(totalCalories);
                    activitySummary.setTitle(title);
                }
                if (activityChart == null) {
                    activityChart = new ActivityChart(timeData, caloriesData, title);
                    activityChart.setDisplayedDataType(ActivityDayChart.DataType.TIME);
                    reports.add(activityChart);
                } else {
                    activityChart.setTime(timeData);
                    activityChart.setCalories(caloriesData);
                    activityChart.setTitle(title);
                }
                // notify ui
                if (mAdapter != null && mRecyclerView != null && !mRecyclerView.isComputingLayout()) {
                    mAdapter.notifyItemChanged(reports.indexOf(activitySummary));
                    mAdapter.notifyItemChanged(reports.indexOf(activityChart));
                    mAdapter.notifyDataSetChanged();
                } else {
                    Log.w(LOG_TAG, "Cannot inform adapter for changes.");
                }
                generatingReports = false;
            }
        });
    }

    @Override
    public void onActivityChartDataTypeClicked(ActivityDayChart.DataType newDataType) {
        Log.i(LOG_TAG, "Changing  displayed data type to " + newDataType.toString());
        if (this.activityChart == null) {
            return;
        }
        if (this.activityChart.getDisplayedDataType() == newDataType) {
            return;
        }
        this.activityChart.setDisplayedDataType(newDataType);
        if (this.mAdapter != null) {
            this.mAdapter.notifyItemChanged(this.reports.indexOf(this.activityChart));
        }
    }

    @Override
    public void setActivityChartDataTypeChecked(Menu menu) {
        if (this.activityChart == null) {
            return;
        }
        if (this.activityChart.getDisplayedDataType() == null) {
            menu.findItem(R.id.menu_time).setChecked(true);
        }
        switch (this.activityChart.getDisplayedDataType()) {
            case CALORIES:
                menu.findItem(R.id.menu_calories).setChecked(true);
                break;
            case TIME:
            default:
                menu.findItem(R.id.menu_time).setChecked(true);
        }
    }

    @Override
    public void onPrevClicked() {
        this.day.add(Calendar.WEEK_OF_YEAR, -1);
        this.generateReports();
    }

    @Override
    public void onNextClicked() {
        this.day.add(Calendar.WEEK_OF_YEAR, 1);
        this.generateReports();
    }

    @Override
    public void onTitleClicked() {
        int year = this.day.get(Calendar.YEAR);
        int month = this.day.get(Calendar.MONTH);
        int day = this.day.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        DatePickerDialog dialog = new DatePickerDialog(getContext(), R.style.AppTheme_DatePickerDialog, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                WeeklyReportFragment.this.day.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                WeeklyReportFragment.this.day.set(Calendar.MONTH, monthOfYear);
                WeeklyReportFragment.this.day.set(Calendar.YEAR, year);
                WeeklyReportFragment.this.generateReports();
            }
        }, year, month, day);
        dialog.show();
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments cotained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        // Currently doing nothing here.
    }
}
