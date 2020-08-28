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


import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import com.intervaltimer.google.R;
import com.intervaltimer.google.fragments.DailyReportFragment;
import com.intervaltimer.google.fragments.MonthlyReportFragment;
import com.intervaltimer.google.fragments.StatisticsFragment;
import com.intervaltimer.google.fragments.WeeklyReportFragment;

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


        // Load first view
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new StatisticsFragment(), "StatisticsFragment");
        fragmentTransaction.commit();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_statistics;
    }

}


