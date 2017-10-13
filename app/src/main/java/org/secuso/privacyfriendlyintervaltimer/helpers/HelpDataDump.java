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

package org.secuso.privacyfriendlyintervaltimer.helpers;

import android.content.Context;

import org.secuso.privacyfriendlyintervaltimer.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Class structure taken from tutorial at http://www.journaldev.com/9942/android-expandablelistview-example-tutorial
 * last access 27th October 2016
 */

public class HelpDataDump {

    private Context context;

    public HelpDataDump(Context context) {
        this.context = context;
    }

    public LinkedHashMap<String, List<String>> getDataGeneral() {
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap<String, List<String>>();

        List<String> general = new ArrayList<String>();
        general.add(context.getResources().getString(R.string.help_whatis_answer));

        expandableListDetail.put(context.getResources().getString(R.string.help_whatis), general);

        List<String> feature1 = new ArrayList<String>();

        feature1.add(context.getResources().getString(R.string.help_feature_workout_timer_answer));
        expandableListDetail.put(context.getResources().getString(R.string.help_feature_workout_timer), feature1);

        List<String> feature2 = new ArrayList<String>();

        feature2.add(context.getResources().getString(R.string.help_feature_motivation_alert_answer));
        expandableListDetail.put(context.getResources().getString(R.string.help_feature_motivation_alert), feature2);

        List<String> feature3 = new ArrayList<String>();

        feature3.add(context.getResources().getString(R.string.help_feature_block_periodization_answer));
        expandableListDetail.put(context.getResources().getString(R.string.help_feature_block_periodization), feature3);

        List<String> feature4 = new ArrayList<String>();

        feature4.add(context.getResources().getString(R.string.help_feature_workout_history_answer));
        expandableListDetail.put(context.getResources().getString(R.string.help_feature_workout_history), feature4);


        List<String> privacy = new ArrayList<String>();
        privacy.add(context.getResources().getString(R.string.help_privacy_answer));

        expandableListDetail.put(context.getResources().getString(R.string.help_privacy), privacy);

        List<String> permissions = new ArrayList<String>();
        permissions.add(context.getResources().getString(R.string.help_permission_answer));

        expandableListDetail.put(context.getResources().getString(R.string.help_permission), permissions);

        return expandableListDetail;
    }
}
