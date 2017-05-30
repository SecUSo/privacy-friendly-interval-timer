package org.secuso.privacyfriendlytraining.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;

import org.secuso.privacyfriendlytraining.R;

//TO DO: CANCEL TIMER ON BACK
public class TimerService extends Service {

    /**
     * Broadcast action identifier for the broadcasted timerService messages
     */
    public static final String COUNTDOWN_BROADCAST = "org.secuso.privacyfriendlytraining.COUNTDOWN";

    /**
     * Binder given to clients
     */
    private final IBinder mBinder = new LocalBinder();

    /**
     * Timer for the workout countdown
     */
    CountDownTimer workoutTimer = null;
    CountDownTimer restTimer = null;


    /**
     * Values for workout and rest time and sets to perform
     */
    private long startTime = 0;
    private long workoutTime = 0;
    private long restTime = 0;
    private int sets = 0;

    private long savedTime = 0;
    private int currentSet = 1;
    private boolean isWorkout = false;


    @Override
    public void onCreate() {
        super.onCreate();
    }


    public class LocalBinder extends Binder {
        public TimerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        workoutTimer.cancel();
        restTimer.cancel();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    private CountDownTimer createWorkoutTimer(final long duration) {

        //The interval is half a second, because otherwise the last tick from 1 to 0 does not occur
        return new CountDownTimer(duration, 500) {

            @Override
            public void onTick(long millisUntilFinished) {
                savedTime = millisUntilFinished;
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("countdown_seconds", (int) millisUntilFinished / 1000);
                sendBroadcast(broadcast);
            }

            @Override
            public void onFinish() {
                currentSet += 1;
                if(currentSet <= sets){
                    Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                            .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_rest));
                    sendBroadcast(broadcast);
                    isWorkout = false;
                    restTimer = createRestTimer(restTime);
                    restTimer.start();
                }
                else {
                    Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                            .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_done));
                    sendBroadcast(broadcast);
                }

            }
        };
    }

    private CountDownTimer createRestTimer(final long duration/*, final CountDownTimer workoutTimer*/) {

        //The interval is half a second, because otherwise the last tick from 1 to 0 does not occur
        return new CountDownTimer(duration, 500) {

            @Override
            public void onTick(long millisUntilFinished) {
                savedTime = millisUntilFinished;
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("countdown_seconds", (int) millisUntilFinished / 1000);
                sendBroadcast(broadcast);
            }

            @Override
            public void onFinish() {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_workout))
                        .putExtra("current_set", currentSet);
                sendBroadcast(broadcast);
                isWorkout = true;
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
        };
    }

    /** method for clients */
    public void startWorkout(long workoutTime, long restTime, long startTime, int sets) {
        this.startTime = startTime*1000;
        this.workoutTime = workoutTime * 1000;
        this.restTime = restTime * 1000;
        this.sets = sets;

        this.workoutTimer = createWorkoutTimer(this.workoutTime);
        this.restTimer = createRestTimer(this.startTime);

        //Use rest timer as a start timer before the workout begins
        if(startTime != 0){
            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_start_timer));
            sendBroadcast(broadcast);

            isWorkout = false;
            restTimer.start();
        } else {
            isWorkout = true;
            this.workoutTimer.start();
        }
    }

    public void pauseTimer() {
        if(isWorkout && workoutTimer != null) {
            this.workoutTimer.cancel();
        }
        else if (restTimer !=null) {
            this.restTimer.cancel();
        }
    }

    public void resumeTimer() {
        if(isWorkout){
            this.workoutTimer = createWorkoutTimer(savedTime);
            this.workoutTimer.start();
        }
        else {
            this.restTimer = createRestTimer(savedTime);
            this.restTimer.start();
        }
    }

    //TO DO: Same as of finish maybe make a default method
    public void nextTimer() {
        if(isWorkout && currentSet <= sets) {
            this.workoutTimer.cancel();
            this.currentSet += 1;

            if(currentSet <= sets){
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_rest));
                sendBroadcast(broadcast);
                isWorkout = false;
                restTimer = createRestTimer(restTime);
                restTimer.start();
            }
            else {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_done));
                sendBroadcast(broadcast);
            }
        }
        else if (currentSet <= sets){
            this.restTimer.cancel();

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_workout))
                    .putExtra("current_set", currentSet);
            sendBroadcast(broadcast);
            isWorkout = true;
            workoutTimer = createWorkoutTimer(workoutTime);
            workoutTimer.start();
        }
    }

    //TO DO: Fix prevTimer from final state "DONE"
    public void prevTimer() {
        if (isWorkout && currentSet >= 2) {
            this.workoutTimer.cancel();

            if (currentSet <= sets) {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_rest));
                sendBroadcast(broadcast);
                isWorkout = false;
                restTimer = createRestTimer(restTime);
                restTimer.start();
            } else {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_done));
                sendBroadcast(broadcast);
            }
        } else if (currentSet >= 2) {
            this.restTimer.cancel();
            this.currentSet -= 1;

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("new_timer_starts", getResources().getString(R.string.workout_headline_workout))
                    .putExtra("current_set", currentSet);
            sendBroadcast(broadcast);
            isWorkout = true;
            workoutTimer = createWorkoutTimer(workoutTime);
            workoutTimer.start();
        }
        else if(isWorkout) {
            this.workoutTimer.cancel();
            workoutTimer = createWorkoutTimer(workoutTime);
            workoutTimer.start();
        }
    }
}