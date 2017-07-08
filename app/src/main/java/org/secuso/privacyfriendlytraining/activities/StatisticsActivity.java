package org.secuso.privacyfriendlytraining.activities;


import android.os.Bundle;
import android.preference.PreferenceManager;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.fragments.DailyReportFragment;
import org.secuso.privacyfriendlytraining.fragments.MonthlyReportFragment;
import org.secuso.privacyfriendlytraining.fragments.StatisticsFragment;
import org.secuso.privacyfriendlytraining.fragments.WeeklyReportFragment;

/**
 * Statistics view incl. navigation drawer and fragments
 *
 * @author Tobias Neidig, Karola Marky, Alexander Karakuz
 * @version 20170612
 */
public class StatisticsActivity extends BaseActivity implements DailyReportFragment.OnFragmentInteractionListener, WeeklyReportFragment.OnFragmentInteractionListener, MonthlyReportFragment.OnFragmentInteractionListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // init preferences
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        // Load first view
        final android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new StatisticsFragment(), "StatisticsFragment");
        fragmentTransaction.commit();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_main;
    }

}




