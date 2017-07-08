package org.secuso.privacyfriendlytraining.models;

/**
 *
 * @author Karola Marky, Alexander Karakuz
 * @version 20170701
 *
 * This class holds the "data type" of a single workout session.
 * A workout session consists of the time it to took to work out
 * and the calories burnt. The ID of each workout is the data eg. 20170708
 *
 */
public class WorkoutSessionData {

    private int ID;
    private int WORKOUT_TIME;
    private int CALORIES;

    public WorkoutSessionData() {    }


    /**
     * Always use this constructor to generate data with values.
     * @param ID The primary key for the database
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
