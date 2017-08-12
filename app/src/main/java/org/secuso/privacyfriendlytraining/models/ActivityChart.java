package org.secuso.privacyfriendlytraining.models;

import java.util.Map;

/**
 * Activity chart model
 *
 * @author Tobias Neidig, Alexander Karakuz
 * @version 20170707
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class ActivityChart {
    private String title;
    private Map<String, Double> time;
    private Map<String, Double> calories;
    private ActivityDayChart.DataType displayedDataType;

    public ActivityChart(Map<String, Double> time, Map<String, Double> calories, String title) {
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

    public Map<String, Double> getTime() {
        return time;
    }

    public void setTime(Map<String, Double> time) {
        this.time = time;
    }

    public Map<String, Double> getCalories() {
        return calories;
    }

    public void setCalories(Map<String, Double> calories) {
        this.calories = calories;
    }

    public ActivityDayChart.DataType getDisplayedDataType() {
        return displayedDataType;
    }

    public void setDisplayedDataType(ActivityDayChart.DataType displayedDataType) {
        this.displayedDataType = displayedDataType;
    }
}
