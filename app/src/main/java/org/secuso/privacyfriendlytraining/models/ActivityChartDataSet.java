package org.secuso.privacyfriendlytraining.models;

/**
 * Created by tobias on 26.07.16.
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
public class ActivityChartDataSet {
    public double value;

    public ActivityChartDataSet(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
