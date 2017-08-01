package org.secuso.privacyfriendlytraining.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import org.secuso.privacyfriendlytraining.R;
import org.secuso.privacyfriendlytraining.activities.WorkoutActivity;
import org.secuso.privacyfriendlytraining.database.PFASQLiteHelper;
import org.secuso.privacyfriendlytraining.models.WorkoutSessionData;

import java.text.SimpleDateFormat;
import java.util.Date;

//TO DO: Button on Notification
public class TimerService extends Service {

    //General
    private SharedPreferences settings;

    //Broadcast action identifier for the broadcasted service messages
    public static final String COUNTDOWN_BROADCAST = "org.secuso.privacyfriendlytraining.COUNTDOWN";
    public static final String NOTIFICATION_BROADCAST = "org.secuso.privacyfriendlytraining.NOTIFICATION";

    //Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    //Timer for the workout countdown
    private CountDownTimer workoutTimer = null;
    private CountDownTimer restTimer = null;

    //Sound
    MediaPlayer mediaPlayer = null;

    //Values for workout and rest time and sets to perform
    private String currentTitle = "";
    private boolean isBlockPeriodization = false;
    private long blockPeriodizationTime = 0;
    private int blockPeriodizationSets = 0;
    private long startTime = 0;
    private long workoutTime = 0;
    private long restTime = 0;
    private int sets = 0;

    //Values during the workout
    private long savedTime = 0;
    private int currentSet = 1;

    //Timer Flags
    private boolean isStarttimer = false;
    private boolean isWorkout = false;
    private boolean isPaused = false;
    private boolean isRunning = false;


    //Notification variables
    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notiBuilder = null;
    private NotificationManager notiManager = null;
    private boolean appInBackground = false;


    //Database for the statistics
    private PFASQLiteHelper database = null;
    private WorkoutSessionData statistics = null;
    private int workoutDuration = 0;
    private int caloriesBurnt = 0;
    private int caloriesPerExercise = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        this.restTimer = createRestTimer(this.startTime);
        this.workoutTimer = createWorkoutTimer(this.workoutTime);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(isPaused){
                resumeTimer();
                int secondsUntilFinished = (int) Math.ceil(savedTime / 1000.0);
                updateNotification(secondsUntilFinished);
            }
            else {
                pauseTimer();
            }
        }
    };

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
        saveStatistics();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(myReceiver, new IntentFilter(NOTIFICATION_BROADCAST));
        return super.onStartCommand(intent, flags, startId);
    }

    //Maybe Strategy Pattern for the onFinish() if another Timer would be introduced
    //Tick has to be 10ms for the progress bar to animate fluently
    private CountDownTimer createWorkoutTimer(final long duration) {

        return new CountDownTimer(duration, 10) {
            int lastBroadcastedSecond = (int) Math.ceil(duration / 1000.0);

            @Override
            public void onTick(long millisUntilFinished) {

                int secondsUntilFinished = (int) Math.ceil(millisUntilFinished / 1000.0);

                savedTime = millisUntilFinished;

                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("onTickMillis", millisUntilFinished);

                if(lastBroadcastedSecond > secondsUntilFinished) // send and play sound only every minute
                {
                    broadcast.putExtra("timer_title", currentTitle)
                             .putExtra("countdown_seconds", secondsUntilFinished);

                    lastBroadcastedSecond = secondsUntilFinished;

                    playSound(secondsUntilFinished, true);
                    updateNotification(secondsUntilFinished);
                    workoutDuration += 1;
                }
                sendBroadcast(broadcast);
            }

            @Override
            public void onFinish() {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST);

                caloriesBurnt += caloriesPerExercise;

                if(currentSet < sets) {
                    if (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) {
                        currentTitle = getResources().getString(R.string.workout_block_periodization_headline);
                        broadcast.putExtra("timer_title", currentTitle)
                                 .putExtra("countdown_seconds", (int) blockPeriodizationTime/1000)
                                 .putExtra("new_timer", blockPeriodizationTime);

                        restTimer = createRestTimer(blockPeriodizationTime);
                    } else {
                        currentTitle = getResources().getString(R.string.workout_headline_rest);
                        broadcast.putExtra("timer_title", currentTitle)
                                  .putExtra("countdown_seconds", (int) restTime/1000)
                                  .putExtra("new_timer", restTime);


                        restTimer = createRestTimer(restTime);
                    }
                    sendBroadcast(broadcast);
                    restTimer.start();
                }
                else {
                    currentTitle = getResources().getString(R.string.workout_headline_done);
                    broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("timer_title", currentTitle)
                        .putExtra("workout_finished", true)
                        .putExtra("calories_burned", caloriesBurnt);
                    sendBroadcast(broadcast);
                }
                isWorkout = false;
                workoutDuration += 1;
            }
        };
    }

    private CountDownTimer createRestTimer(final long duration) {

        return new CountDownTimer(duration, 10) {
            int lastBroadcastedSecond = (int) Math.ceil(duration / 1000.0);

            @Override
            public void onTick(long millisUntilFinished) {
                int secondsUntilFinished = (int) Math.ceil(millisUntilFinished / 1000.0);

                savedTime = millisUntilFinished;

                Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("onTickMillis", millisUntilFinished);

                if(lastBroadcastedSecond > secondsUntilFinished) // send and play sound only every minute
                {
                    broadcast.putExtra("timer_title", currentTitle)
                             .putExtra("countdown_seconds", secondsUntilFinished);

                    lastBroadcastedSecond = secondsUntilFinished;

                    playSound(secondsUntilFinished, true);
                    updateNotification(secondsUntilFinished);
                    workoutDuration += 1;
                }
                sendBroadcast(broadcast);
            }

            @Override
            public void onFinish() {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST);

                if(isStarttimer){
                    isStarttimer = false;
                }
                else {
                    currentSet += 1;
                }
                currentTitle = getResources().getString(R.string.workout_headline_workout);

                broadcast.putExtra("timer_title", currentTitle)
                        .putExtra("current_set", currentSet)
                        .putExtra("sets", sets)
                        .putExtra("countdown_seconds", (int) workoutTime/1000)
                        .putExtra("new_timer", workoutTime);

                sendBroadcast(broadcast);
                isWorkout = true;

                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
                workoutDuration += 1;
            }
        };
    }

    /** Initialize all timer and set values and start the workout routine */
    public void startWorkout(long workoutTime, long restTime, long startTime, int sets,
                             boolean isBlockPeriodization, long blockPeriodizationTime, int blockPeriodizationSets) {
        this.blockPeriodizationTime = blockPeriodizationTime*1000;
        this.blockPeriodizationSets = blockPeriodizationSets;
        this.isBlockPeriodization = isBlockPeriodization;
        this.isRunning = true;
        this.workoutTime = workoutTime * 1000;
        this.startTime = startTime * 1000;
        this.restTime = restTime * 1000;
        this.currentSet = 1;
        this.sets = sets;
        this.workoutDuration = 0;
        this.caloriesBurnt = 0;
        this.caloriesPerExercise = calculateUserCalories((float) workoutTime);

        this.workoutTimer = createWorkoutTimer(this.workoutTime);
        this.restTimer = createRestTimer(this.startTime);


        //Use rest timer as a start timer before the workout begins
        if(startTime != 0){
            this.savedTime = this.restTime;
            this.currentTitle = getResources().getString(R.string.workout_headline_start_timer);
            isWorkout = false;
            isStarttimer = true;

            restTimer.start();
        } else {
            this.savedTime = this.workoutTime;
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
        updateNotification((int) Math.ceil(savedTime / 1000.0));
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

        int secondsUntilFinished = (int) Math.ceil(savedTime / 1000.0);
        Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                .putExtra("countdown_seconds", secondsUntilFinished);
        sendBroadcast(broadcast);

        isPaused = false;
    }


    /*Switch to the next timer */
    public void nextTimer() {
        //If user is not in the final workout switch to rest phase
        if(isWorkout && currentSet < sets) {
            this.workoutTimer.cancel();
            isWorkout = false;

            long time = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ? this.blockPeriodizationTime : this.restTime;
            this.currentTitle = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ?
                    getResources().getString(R.string.workout_block_periodization_headline) : getResources().getString(R.string.workout_headline_rest);

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", currentTitle)
                    .putExtra("new_timer", time);

            if(isPaused){
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

            this.currentTitle = getResources().getString(R.string.workout_headline_workout);

            //If rest timer was used as a start timer, ignore the first set increase
            if(isStarttimer){ this.isStarttimer = false; }
            else { this.currentSet += 1; }

            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", currentTitle)
                    .putExtra("current_set", currentSet)
                    .putExtra("new_timer", workoutTime)
                    .putExtra("sets", sets);

            if(isPaused){
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
            this.currentTitle = (isBlockPeriodization && currentSet % blockPeriodizationSets == 0) ?
                    getResources().getString(R.string.workout_block_periodization_headline) : getResources().getString(R.string.workout_headline_rest);


            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", currentTitle)
                    .putExtra("sets", sets)
                    .putExtra("new_timer", time)
                    .putExtra("current_set", currentSet);

            if(isPaused){
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
            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                .putExtra("new_timer", workoutTime);

            if(isPaused){
                this.savedTime = workoutTime;
            }
            else {
                this.workoutTimer.cancel();
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
            sendBroadcast(broadcast);
        }

        //If user is in the rest phase go back to the workout phase
        else if (!isStarttimer) {
            this.restTimer.cancel();
            isWorkout = true;
            this.currentTitle = getResources().getString(R.string.workout_headline_workout);


            Intent broadcast = new Intent(COUNTDOWN_BROADCAST)
                    .putExtra("timer_title", currentTitle)
                    .putExtra("current_set", currentSet)
                    .putExtra("new_timer", workoutTime)
                    .putExtra("sets", sets);

            if(isPaused){
                this.savedTime = workoutTime;
            }
            else {
                workoutTimer = createWorkoutTimer(workoutTime);
                workoutTimer.start();
            }
            sendBroadcast(broadcast);
        }
    }

    /*
    * Plays a sound for the countdown timer. MediaPlayer is checked for a necessary release beforehand.
    */
    private void playSound(int seconds, boolean workout){

        int soundId = 0;
        boolean isHalfTime = seconds == (int)workoutTime/2000;

        if(!isSoundsMuted(this)){
            if(seconds <= 10 && workout && isVoiceCountdownWorkoutEnabled(this)){
                soundId = getResources().getIdentifier("num_"+seconds, "raw", getPackageName());
            }
            else if(seconds <= 5 && !workout && isVoiceCountdownRestEnabled(this)){
                soundId = getResources().getIdentifier("num_"+seconds, "raw", getPackageName());
            }
            else if(isVoiceHalfTimeEnabled(this) && workout && isHalfTime){
                soundId = getResources().getIdentifier("half_time", "raw", getPackageName());
            }
            else if(isWorkoutRythmEnabled(this) && workout && seconds != 0){
                soundId = seconds != 1 ? getResources().getIdentifier("beep", "raw", getPackageName()) : getResources().getIdentifier("beep_long", "raw", getPackageName());
            }
        }

        if(soundId != 0){

            if (mediaPlayer != null){
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            this.mediaPlayer = MediaPlayer.create(this, soundId);
            mediaPlayer.start();
        }
    }

    //https://www.fitness-gesundheit.uni-wuppertal.de/fileadmin/fitness-gesundheit/pdf-Dokumente/Publikationen/2015/Prof.Stemper_F_G_4-15.pdf
    private int calculateUserCalories(float workoutDurationSeconds){
        int age = 0;
        int height = 0;
        int weight = 0;
        int circleTrainingMET = 8;

        if(this.settings != null) {
            age = Integer.parseInt(settings.getString(this.getString(R.string.pref_age), "0"));
            height = Integer.parseInt(settings.getString(this.getString(R.string.pref_heigh), "0"));
            weight = (int)Double.parseDouble(settings.getString(this.getString(R.string.pref_weight), "0"));
        }

        float caloriesPerExercise = circleTrainingMET * (weight * workoutDurationSeconds/3600);

        return (int) caloriesPerExercise;
    }


    /*Build a notification showing the current progress of the workout*/
    public Notification buildNotification(int time) {
        notiBuilder = new NotificationCompat.Builder(this);
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        Intent intent = new Intent(this, WorkoutActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent buttonIntent = new Intent(NOTIFICATION_BROADCAST);
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 4, buttonIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        notiBuilder.setContentIntent(pendingIntent);


        int buttonID = isPaused ? R.drawable.ic_play_24dp : R.drawable.ic_pause_24dp;

        String message = "";

        if(isStarttimer) {message = "START IN";}
        else { message = isWorkout ? this.getResources().getString(R.string.workout_headline_workout) : this.getResources().getString(R.string.workout_headline_rest);}
        message += " | "+ this.getResources().getString(R.string.workout_notification_time)+ ": " + time;
        message += " | "+ this.getResources().getString(R.string.workout_info)+ ": " + currentSet + "/" + sets;

        String buttonText = isPaused ? this.getResources().getString(R.string.workout_notification_resume) : this.getResources().getString(R.string.workout_notification_pause);

        NotificationCompat.Action action = new NotificationCompat.Action.Builder(buttonID, buttonText, buttonPendingIntent).build();


        notiBuilder.setContentTitle(this.getResources().getString(R.string.app_name))
                .setContentText(message)
                .addAction(action)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_menu_info)
                .setLights(ContextCompat.getColor(this, R.color.colorPrimary), 1000, 1000)
                .setStyle(new NotificationCompat.BigTextStyle()
                );

        return notiBuilder.build();
    }


    /*Update the notification with current title and timer values*/
    private void updateNotification(int time) {
        if(appInBackground) {
            Notification notification = buildNotification(time);
            notiManager.notify(NOTIFICATION_ID, notification);
        }
        else if(notiManager != null) {
            notiManager.cancel(NOTIFICATION_ID);
        }
    }

    public void startNotifying(boolean isInBackground){
       this.appInBackground = isInBackground && isRunning;

       //Update just once in case of a pause
       if(isRunning){
           updateNotification((int) Math.ceil(savedTime / 1000.0));
       }
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

        saveStatistics();
        this.savedTime = 0;
        this.isPaused = false;
        this.isRunning = false;
    }

    private int getTodayAsID() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String concatDate = dateFormat.format(new Date());

        return Integer.parseInt(concatDate);
    }

    private void saveStatistics(){
        database = new PFASQLiteHelper(getBaseContext());
        int id = getTodayAsID();
        statistics = database.getWorkoutData(id);

        if(statistics.getID() == 0){
            database.addWorkoutDataWithID(new WorkoutSessionData(id, 0, 0));
            statistics = database.getWorkoutData(id);
        }

        int totalTimeSpentTraining = statistics.getWORKOUTTIME() + this.workoutDuration;
        int totalCaloriesBurnt = isCaloriesEnabled(this) ? statistics.getCALORIES() + this.caloriesBurnt : statistics.getCALORIES();

        database.updateWorkoutData(new WorkoutSessionData(id, totalTimeSpentTraining, totalCaloriesBurnt));
        this.workoutDuration = 0;
        this.caloriesBurnt = 0;
    }

    /*
    * Multiple checks for what was enabled inside the settings
    */
    public boolean isVoiceCountdownWorkoutEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_voice_countdown_workout), false);
        }
        return false;
    }

    public boolean isVoiceCountdownRestEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_voice_countdown_rest), false);
        }
        return false;
    }

    public boolean isWorkoutRythmEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_sound_rythm), false);
        }
        return false;
    }

    public boolean isVoiceHalfTimeEnabled(Context context){
        if(this.settings != null){
            return settings.getBoolean(context.getString(R.string.pref_voice_halftime), false);
        }
        return false;
    }

    public boolean isCaloriesEnabled(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_calories_counter), false);
        }
        return false;
    }

    public boolean isSoundsMuted(Context context) {
        if (this.settings != null) {
            return settings.getBoolean(context.getString(R.string.pref_sounds_muted), true);
        }
        return true;
    }




    /*Getter and Setter*/
    public long getWorkoutTime(){
        return  this.workoutTime;
    }

    public boolean getIsWorkout(){
        return  this.isWorkout;
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

    public int getCaloriesBurnt(){
        return this.caloriesBurnt;
    }

    public int getCurrentSet(){
        return  this.currentSet;
    }

    public String getCurrentTitle() {
        return this.currentTitle;
    }

    public long getSavedTime(){
        return this.savedTime;
    }

    public boolean getIsPaused(){
        return this.isPaused;
    }
}