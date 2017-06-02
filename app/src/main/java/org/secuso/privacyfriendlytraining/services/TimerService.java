package org.secuso.privacyfriendlytraining.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.activities.WorkoutActivity;

//TO DO: OnStop gives false positves for the notification
//TO DO: Button on Notification
public class TimerService extends Service {

    //Broadcast action identifier for the broadcasted timerService messages
    public static final String COUNTDOWN_BROADCAST = "org.secuso.privacyfriendlytraining.COUNTDOWN";

    //Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    //Timer for the workout countdown
    private CountDownTimer workoutTimer = null;
    private CountDownTimer restTimer = null;


    //Values for workout and rest time and sets to perform
    private String currentTitle = "";
    private boolean isBlockPeriodization = false;
    private long blockPeriodizationTime = 0;
    private int blockPeriodizationSets = 0;
    private long startTime = 0;
    private long workoutTime = 0;
    private long restTime = 0;
    private int sets = 0;

    private long savedTime = 0;
    private int currentSet = 1;
    private boolean isWorkout = false;
    private boolean isStarttimer = false;
    private boolean isPaused = false;


    //Notification variables
    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notiBuilder = null;
    private NotificationManager notiManager = null;
    private boolean appInBackground = false;


    @Override
    public void onCreate() {
        super.onCreate();
        this.restTimer = createRestTimer(this.startTime);
        this.workoutTimer = createWorkoutTimer(this.workoutTime);
    }


    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        if(workoutTimer != null){
            workoutTimer.cancel();
        }
        if(restTimer != null){
            restTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //Maybe Strategy Pattern for the onFinish() if another Timer would be introduced
    private CountDownTimer createWorkoutTimer(final long duration) {

        //The interval is half a second, because otherwise the last tick from 1 to 0 does not get send
        return new CountDownTimer(duration, 500) {

            @Override
            public void onTick(long millisUntilFinished) {
                savedTime = millisUntilFinished;
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("countdown_seconds", (int) millisUntilFinished / 1000);
                sendBroadcast(broadcast);
                updateNotification(millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST).putExtra("timer_title", currentTitle);
                if(currentSet < sets) {
                    if (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) {
                        currentTitle = getResources().getString(R.string.workout_block_periodization_headline);
                        broadcast = new Intent(COUNTDOWN_BROADCAST).putExtra("timer_title", currentTitle);

                        restTimer = createRestTimer(blockPeriodizationTime);
                    } else {
                        currentTitle = getResources().getString(R.string.workout_headline_rest);
                        broadcast = new Intent(COUNTDOWN_BROADCAST).putExtra("timer_title", currentTitle);

                        restTimer = createRestTimer(restTime);
                    }
                    restTimer.start();
                }
                else {
                    currentTitle = getResources().getString(R.string.workout_headline_done);
                    broadcast = new Intent(COUNTDOWN_BROADCAST)
                            .putExtra("timer_title", currentTitle)
                            .putExtra("workout_finished", true);
                }
                sendBroadcast(broadcast);
                isWorkout = false;
            }
        };
    }

    private CountDownTimer createRestTimer(final long duration) {

        //The interval is half a second, because otherwise the last tick from 1 to 0 does not get send
        return new CountDownTimer(duration, 500) {

            @Override
            public void onTick(long millisUntilFinished) {
                savedTime = millisUntilFinished;
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("countdown_seconds", (int) millisUntilFinished / 1000);
                sendBroadcast(broadcast);
                updateNotification(millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                if(isStarttimer){
                    isStarttimer = false;
                }
                else {
                    currentSet += 1;
                }
                currentTitle = getResources().getString(R.string.workout_headline_workout);

                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("timer_title", currentTitle)
                        .putExtra("current_set", currentSet)
                        .putExtra("sets", sets);
                sendBroadcast(broadcast);
                isWorkout = true;
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
        };
    }

    /** Initialize all timer and set values and start the workout routine */
    public void startWorkout(long workoutTime, long restTime, long startTime, int sets,
                             boolean isBlockPeriodization, long blockPeriodizationTime, int blockPeriodizationSets) {
        this.blockPeriodizationTime = blockPeriodizationTime*1000;
        this.blockPeriodizationSets = blockPeriodizationSets;
        this.isBlockPeriodization = isBlockPeriodization;
        this.workoutTime = workoutTime * 1000;
        this.startTime = startTime * 1000;
        this.restTime = restTime * 1000;
        this.currentSet = 1;
        this.sets = sets;

        this.workoutTimer = createWorkoutTimer(this.workoutTime);
        this.restTimer = createRestTimer(this.startTime);

        //Use rest timer as a start timer before the workout begins
        if(startTime != 0){
            this.currentTitle = getResources().getString(R.string.workout_headline_start_timer);
            isWorkout = false;
            isStarttimer = true;
            restTimer.start();
        } else {
            this.currentTitle = getResources().getString(R.string.workout_headline_workout);
            isWorkout = true;
            this.workoutTimer.start();
        }
    }

    /*Pause the currently working timer*/
    public void pauseTimer() {
        if(isWorkout && workoutTimer != null) {
            this.workoutTimer.cancel();
        }
        else if (restTimer !=null) {
            this.restTimer.cancel();
        }
        isPaused = true;
        updateNotification(savedTime/1000);
    }

    /*Resume the currently working timer*/
    public void resumeTimer() {
        if(isWorkout){
            this.workoutTimer = createWorkoutTimer(savedTime);
            this.workoutTimer.start();
        }
        else {
            this.restTimer = createRestTimer(savedTime);
            this.restTimer.start();
        }
        isPaused = false;
    }


    /*Switch to the next timer */
    public void nextTimer() {
        //If user is not in the final workout switch to rest phase
        if(isWorkout && currentSet < sets) {
            this.workoutTimer.cancel();
            isWorkout = false;

            long time = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ? this.blockPeriodizationTime : this.restTime;
            String title = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ?
                    getResources().getString(R.string.workout_block_periodization_headline) : getResources().getString(R.string.workout_headline_rest);

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST).putExtra("timer_title", title);

            if(isPaused){
                broadcast.putExtra("countdown_seconds", (int) time/1000);
                this.savedTime = time;
            }
            else {
                restTimer = createRestTimer(time);
                restTimer.start();
            }
            sendBroadcast(broadcast);
        }

        //If user is in the rest phase switch to the workout phase
        else if (currentSet < sets){
            this.restTimer.cancel();
            isWorkout = true;

            //If rest timer was used as a start timer, ignore the first set increase
            if(isStarttimer){ this.isStarttimer = false; }
            else { this.currentSet += 1; }

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", getResources().getString(R.string.workout_headline_workout))
                    .putExtra("current_set", currentSet)
                    .putExtra("sets", sets);

            if(isPaused){
                broadcast.putExtra("countdown_seconds", (int) workoutTime/1000);
                this.savedTime = workoutTime;
            }
            else {
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
            sendBroadcast(broadcast);
        }
    }


    /*Switch to the previous timer */
    public void prevTimer() {

        //If user is not in the first workout phase go back to the rest phase
        if (isWorkout && currentSet >= 2) {
            this.workoutTimer.cancel();
            isWorkout = false;
            this.currentSet -= 1;


            long time = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ? this.blockPeriodizationTime : this.restTime;
            String title = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ?
                    getResources().getString(R.string.workout_block_periodization_headline) : getResources().getString(R.string.workout_headline_rest);


            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", title)
                    .putExtra("sets", sets)
                    .putExtra("current_set", currentSet);

            if(isPaused){
                broadcast.putExtra("countdown_seconds", (int) time/1000);
                this.savedTime = time;
            }
            else {
                restTimer = createRestTimer(time);
                restTimer.start();
            }
            sendBroadcast(broadcast);
        }

        //If user is in the first workout phase, just reset the timer
        else if(isWorkout) {
            if(isPaused){
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST).putExtra("countdown_seconds", (int) workoutTime/1000);
                this.savedTime = workoutTime;
                sendBroadcast(broadcast);
            }
            else {
                this.workoutTimer.cancel();
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
        }

        //If user is in the rest phase go back to the workout phase
        else if (!isStarttimer) {
            this.restTimer.cancel();
            isWorkout = true;

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", getResources().getString(R.string.workout_headline_workout))
                    .putExtra("current_set", currentSet)
                    .putExtra("sets", sets);

            if(isPaused){
                broadcast.putExtra("countdown_seconds", (int) workoutTime/1000);
                this.savedTime = workoutTime;
            }
            else {
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
            sendBroadcast(broadcast);
        }
    }

    //TO DO: Button not working
    /*Build a notification showing the current progress of the workout*/
    public Notification buildNotification(long time) {
        notiBuilder = new NotificationCompat.Builder(this);
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        Intent intent = new Intent(this, WorkoutActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notiBuilder.setContentIntent(pendingIntent);


        int buttonID = isPaused ? R.drawable.ic_media_embed_play : R.drawable.ic_media_pause ;

        String message = "";

        if(isStarttimer) {message = "START IN";}
        else { message = isWorkout ? this.getResources().getString(R.string.workout_headline_workout) : this.getResources().getString(R.string.workout_headline_rest);}
        message += " | "+ this.getResources().getString(R.string.notification_time)+ ": " + time;
        message += " | "+ this.getResources().getString(R.string.workout_info)+ ": " + currentSet + "/" + sets;

        String buttonText = isPaused ? this.getResources().getString(R.string.notification_resume) : this.getResources().getString(R.string.notification_pause);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(buttonID, buttonText, pendingIntent).build();


        notiBuilder.setContentTitle(this.getResources().getString(R.string.app_name));
        notiBuilder.setContentText(message);
        notiBuilder.addAction(action);
        notiBuilder.setSmallIcon(R.drawable.ic_menu_info);


        return notiBuilder.build();
    }

    /*Update the notification with current title and timer values*/
    private void updateNotification(long time) {
        if(appInBackground) {
            Notification notification = buildNotification(time);
            notiManager.notify(NOTIFICATION_ID, notification);
        }
        else if(notiManager != null) {
            notiManager.cancel(NOTIFICATION_ID);
        }
    }

    public void startNotifying(boolean isInBackground){
       this.appInBackground = isInBackground;
    }

    public void cleanTimerStop() {
        if (workoutTimer != null) {
            this.workoutTimer.cancel();
        }
        if (restTimer != null) {
            this.restTimer.cancel();
        }
        if (notiManager != null){
            notiManager.cancel(NOTIFICATION_ID);
        }
    }

    /*Getter and Setter*/
    public long getWorkoutTime(){
        return  this.workoutTime;
    }

    public long getStartTime(){
        return  this.startTime;
    }

    public long getRestTime(){
        return  this.restTime;
    }

    public int getSets(){
        return this.sets;
    }

    public int getCurrentSet(){
        return  this.currentSet;
    }

    public String getCurrentTitle() {
        return this.currentTitle;
    }

    public int getSavedTime(){
        return (int)this.savedTime/1000;
    }

    public boolean getIsPaused(){
        return this.isPaused;
    }
}