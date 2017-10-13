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

/**
 *
 * @author Karola Marky, Alexander Karakuz
 * @version 20170701
 *
 * This class holds the "data type" of a single workout session.
 * A workout session consists of the time it to took to work out
 * and the calories burnt. The ID of each workout is the data eg. 20170708
 */

public class WorkoutSessionData {

    private int ID;
    private int WORKOUT_TIME;
    private int CALORIES;

    public WorkoutSessionData() {    }

    /**
     * Always use this constructor to generate data with values.
     * @param ID The primary key for the database, its composed of the year, month and day as id in format yyyyMMdd
     * @param WORKOUT_TIME The length of the workout in seconds
     * @param CALORIES The calories burnt during the workout
     */
    public WorkoutSessionData(int ID, int WORKOUT_TIME, int CALORIES) {

        this.ID=ID;
        this.WORKOUT_TIME=WORKOUT_TIME;
        this.CALORIES=CALORIES;
    }

    /**
     * Getters and setters
     */
    public int getWORKOUTTIME() {
        return WORKOUT_TIME;
    }

    public void setWORKOUTTIME(int WORKOUT_TIME) {
        this.WORKOUT_TIME = WORKOUT_TIME;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public int getCALORIES() {
        return CALORIES;
    }

    public void setCALORIES(int CALORIES) {
        this.CALORIES = CALORIES;
    }

}
