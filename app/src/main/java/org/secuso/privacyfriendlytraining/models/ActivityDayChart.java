package org.secuso.privacyfriendlytraining.models;

import java.util.Map;

/**
 * Activity day chart model
 *
 * @author Tobias Neidig, Alexander Karakuz
 * @version 20170707
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
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
        TIME, DISTANCE, CALORIES
    }
}
