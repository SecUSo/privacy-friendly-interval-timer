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

package org.secuso.privacyfriendlyintervaltimer.activities;


import android.os.Bundle;

import org.secuso.privacyfriendlyintervaltimer.R;
import org.secuso.privacyfriendlyintervaltimer.fragments.DailyReportFragment;
import org.secuso.privacyfriendlyintervaltimer.fragments.MonthlyReportFragment;
import org.secuso.privacyfriendlyintervaltimer.fragments.StatisticsFragment;
import org.secuso.privacyfriendlyintervaltimer.fragments.WeeklyReportFragment;

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
        final android.support.v4.app.FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, new StatisticsFragment(), "StatisticsFragment");
        fragmentTransaction.commit();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_statistics;
    }

}


