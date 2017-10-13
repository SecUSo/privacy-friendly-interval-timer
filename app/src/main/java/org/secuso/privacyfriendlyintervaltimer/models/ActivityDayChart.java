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

package org.secuso.privacyfriendlyintervaltimer.models;

import java.util.Map;

/**
 * Activity day chart model
 *
 * @author Tobias Neidig, Alexander Karakuz
 * @version 20170707
 */

public class ActivityDayChart {
    private String title;
    private Map<String, ActivityChartDataSet> time;
    private Map<String, ActivityChartDataSet> calories;
    private DataType displayedDataType;

    public ActivityDayChart(Map<String, ActivityChartDataSet> time, Map<String, ActivityChartDataSet> calories, String title) {
        this.time = time;
        this.title = title;
        this.calories = calories;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, ActivityChartDataSet> getTime() {
        return time;
    }

    public void setTime(Map<String, ActivityChartDataSet> time) {
        this.time = time;
    }

    public Map<String, ActivityChartDataSet> getCalories() {
        return calories;
    }

    public void setCalories(Map<String, ActivityChartDataSet> calories) {
        this.calories = calories;
    }

    public DataType getDisplayedDataType() {
        return displayedDataType;
    }

    public void setDisplayedDataType(DataType displayedDataType) {
        this.displayedDataType = displayedDataType;
    }

    public enum DataType {
        TIME, CALORIES
    }
}
