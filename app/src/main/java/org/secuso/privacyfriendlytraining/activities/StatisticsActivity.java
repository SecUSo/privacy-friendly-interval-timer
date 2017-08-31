package org.secuso.privacyfriendlytraining.activities;


import android.os.Bundle;

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
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class StatisticsActivity extends BaseActivity implements DailyReportFragment.OnFragmentInteractionListener, WeeklyReportFragment.OnFragmentInteractionListener, MonthlyReportFragment.OnFragmentInteractionListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);


        // Load first view
        final android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new StatisticsFragment(), "StatisticsFragment");
        fragmentTransaction.commit();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_statistics;
    }

}


