package org.secuso.privacyfriendlytraining.models;

/**
 * Activity summary model
 *
 * @author Tobias Neidig, Alexander Karakuz
 * @version 20170707
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class ActivitySummary {
    private int time;
    private int calories;
    private String title;

    public ActivitySummary(int time, int calories) {
        this(time, calories, "");
    }

    public ActivitySummary(int time, int calories, String title) {
        this.time = time;
        this.calories = calories;
        this.title = title;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
