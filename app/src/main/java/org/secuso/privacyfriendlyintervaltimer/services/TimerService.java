package org.secuso.privacyfriendlyintervaltimer.services;

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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import org.secuso.privacyfriendlyintervaltimer.R;
import org.secuso.privacyfriendlyintervaltimer.activities.WorkoutActivity;
import org.secuso.privacyfriendlyintervaltimer.database.PFASQLiteHelper;
import org.secuso.privacyfriendlyintervaltimer.models.WorkoutSessionData;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Workout timer as a service.
 * Has two different timers for workout and rest functionality.
 * Can play sounds depending ot the current settings.
 * Can pause, resume, skip and go back to the pervious timer.
 *
 * @author Alexander Karakuz
 * @version 20170809
 * @license GNU/GPLv3 http://www.gnu.org/licenses/gpl-3.0.html
 */
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
    private boolean isBlockPeriodization = false;
    private boolean isStarttimer = false;
    private boolean isWorkout = false;
    private boolean isPaused = false;
    private boolean isCancelAlert = false;

    //Broadcast string messages
    private String currentTitle = "";

    //Notification variables
    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notiBuilder = null;
    private NotificationManager notiManager = null;
    private boolean isAppInBackground = false;

    //Database for the statistics
    private PFASQLiteHelper database = null;
    private WorkoutSessionData statistics = null;
    private int timeSpentWorkingOut = 0;
    private int caloriesBurned = 0;
    private int caloriesPerExercise = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        this.restTimer = createRestTimer(this.startTime);
        this.workoutTimer = createWorkoutTimer(this.workoutTime);
        this.settings = PreferenceManager.getDefaultSharedPreferences(this);

        registerReceiver(notificationReceiver, new IntentFilter(NOTIFICATION_BROADCAST));

        notiBuilder = new NotificationCompat.Builder(this);
        notiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!currentTitle.equals(getString(R.string.workout_headline_done)) && !isCancelAlert){
                if(isPaused){
                    resumeTimer();
                    int secondsUntilFinished = (int) Math.ceil(savedTime / 1000.0);
                    updateNotification(secondsUntilFinished);
                }
                else {
                    pauseTimer();
                }
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
        unregisterReceiver(notificationReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Creates the workout timer.
     * Broadcasts the current millis on every tick.
     * Broadcasts the seconds left on every second.
     * Starts the rest timer if there are sets left to perform.
     *
     * Maybe Strategy Pattern for the onFinish() if another Timer would be introduce.
     * Tick has to be 10ms for the progress bar to animate fluently.
     *
     * @param duration Duration of the workout timer
     * @return CountDownTimer
     */
    private CountDownTimer createWorkoutTimer(final long duration) {

        return new CountDownTimer(duration, 10) {
            int lastBroadcastedSecond = (int) Math.ceil(duration / 1000.0);


            /**
             * Broadcasts the current milis on every tick and the current seconds every second
             *
             * @param millisUntilFinished
             */
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
                    timeSpentWorkingOut += 1;
                }
                sendBroadcast(broadcast);
            }


            /**
             * Calculates the calories burned during the workout and adds them to global variable.
             * Starts the rest timer if there are sets left to perform. Othterwise boradcasts
             * that the workout tis over and how much calories were burned overall.
             */
            @Override
            public void onFinish() {
                Intent broadcast = new Intent(COUNTDOWN_BROADCAST);

                caloriesBurned += caloriesPerExercise;

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
                    isWorkout = false;
                    timeSpentWorkingOut += 1;
                    restTimer.start();
                }
                else {
                    currentTitle = getResources().getString(R.string.workout_headline_done);
                    updateNotification(0);
                    broadcast = new Intent(COUNTDOWN_BROADCAST)
                        .putExtra("timer_title", currentTitle)
                        .putExtra("workout_finished", true);
                    sendBroadcast(broadcast);
                    timeSpentWorkingOut += 1;
                }
            }
        };
    }


    /**
     * Creates the rest timer.
     * Broadcasts the current millis on every tick.
     * Broadcasts the seconds left on every second.
     * Starts the workout timer when finished.
     *
     * @param duration Duration of the rest timer.
     * @return CountDown Timer
     */
    private CountDownTimer createRestTimer(final long duration) {

        return new CountDownTimer(duration, 10) {
            int lastBroadcastedSecond = (int) Math.ceil(duration / 1000.0);

            /**
             * Broadcasts the current milis on every tick and the current seconds every second
             *
             * @param millisUntilFinished
             */
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

                    playSound(secondsUntilFinished, false);
                    updateNotification(secondsUntilFinished);
                    timeSpentWorkingOut += 1;
                }
                sendBroadcast(broadcast);
            }

            /**
             * Starts the next workout timer and broadcasts it.
             */
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
                timeSpentWorkingOut += 1;
            }
        };
    }


    /**
     * Initialize all timer and set values and start the workout routine.
     *
     * @param workoutTime Duration of each workout timer
     * @param restTime Duration of each rest timer
     * @param startTime Duration of the start timer
     * @param sets Amount of sets to be performed
     * @param isBlockPeriodization Flag if block periodization feature was enabled
     * @param blockPeriodizationTime Duration of the block periodization rest phase
     * @param blockPeriodizationSets Interval determining after how many sets a block rest occurs
     */
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
        this.timeSpentWorkingOut = 0;
        this.caloriesBurned = 0;
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


    /**
     * Pause the currently working timer
     */
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


    /**
     * Resume the currently working timer
     */
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


    /**
     * Switch to the next timer
     */
    public void nextTimer() {
        //If user is not in the final workout switch to rest phase
        if(isWorkout && currentSet < sets && restTime != 0) {
            this.workoutTimer.cancel();
            isWorkout = false;

            //Check if the next rest phase is normal or a block rest
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

        //If user is in the rest phase or the rest phase is 0 switch to the workout phase
        else if (currentSet < sets){
            this.restTimer.cancel();
            this.workoutTimer.cancel();
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


    /**
     * Switch to the previous timer
     */
    public void prevTimer() {

        //If user is not in the first workout phase go back to the rest phase
        if (isWorkout && currentSet >= 2 && restTime != 0) {
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
        else if(isWorkout && currentSet == 1) {
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

        //If user is in the rest phase or rest phase is 0 go back to the previous workout phase
        else if (!isStarttimer) {
            this.restTimer.cancel();
            this.workoutTimer.cancel();
            isWorkout = true;
            this.currentTitle = getResources().getString(R.string.workout_headline_workout);
            this.currentSet = (restTime == 0) ? currentSet - 1 : currentSet;

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


    /**
     * Plays a sound for the countdown timer.
     * MediaPlayer is checked for a necessary release beforehand.
     *
     * @param seconds Current seconds to check which sound should be played.
     * @param isWorkout Flag determining if current phase is workout or rest
     */
    private void playSound(int seconds, boolean isWorkout){

        int soundId = 0;
        boolean isHalfTime = seconds == (int)workoutTime/2000;

        //Determine which sound should be played
        if(!isSoundsMuted(this)){
            if(seconds <= 10 && isWorkout && isVoiceCountdownWorkoutEnabled(this)){
                soundId = getResources().getIdentifier("num_"+seconds, "raw", getPackageName());
            }
            else if(seconds <= 5 && !isWorkout && isVoiceCountdownRestEnabled(this)){
                soundId = getResources().getIdentifier("num_"+seconds, "raw", getPackageName());
            }
            else if(isVoiceHalfTimeEnabled(this) && isWorkout && isHalfTime){
                soundId = getResources().getIdentifier("half_time", "raw", getPackageName());
            }
            else if(isWorkoutRythmEnabled(this) && isWorkout && seconds != 0){
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


    /**
     * Calculates the calories burned based on the settings and the duration provided.
     * Calculation is based on MET
     * https://www.fitness-gesundheit.uni-wuppertal.de/fileadmin/fitness-gesundheit/pdf-Dokumente/Publikationen/2015/Prof.Stemper_F_G_4-15.pdf
     *
     * @param workoutDurationSeconds Duration of workout to calculate calories burned.
     * @return Amount of calories burned.
     */
    private int calculateUserCalories(float workoutDurationSeconds){
        int age = 0;
        int height = 0;
        int weight = 0;
        int circleTrainingMET = 8;

        if(this.settings != null) {
            age = Integer.parseInt(settings.getString(this.getString(R.string.pref_age), "25"));
            height = Integer.parseInt(settings.getString(this.getString(R.string.pref_height), "170"));
            weight = (int)Double.parseDouble(settings.getString(this.getString(R.string.pref_weight), "70"));
        }

        float caloriesPerExercise = circleTrainingMET * (weight * workoutDurationSeconds/3600);

        return (int) caloriesPerExercise;
    }


    /**
     * Build a notification showing the current progress of the workout.
     * This notification is shown whenever the app goes into the background.
     *
     * @param time The current timer value
     * @return Notification
     */
    public Notification buildNotification(int time) {
        Intent intent = new Intent(this, WorkoutActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Intent buttonIntent = new Intent(NOTIFICATION_BROADCAST);
        PendingIntent buttonPendingIntent = PendingIntent.getBroadcast(this, 4, buttonIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notiBuilder.setContentIntent(pendingIntent);

        String message = "";
        message = currentTitle;
        message += " | "+ this.getResources().getString(R.string.workout_notification_time)+ ": " + time;
        message += " | "+ this.getResources().getString(R.string.workout_info)+ ": " + currentSet + "/" + sets;

        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.workout_notification);


        int buttonID = (isPaused && !currentTitle.equals(getResources().getString(R.string.workout_headline_done)))
                ? R.drawable.ic_notification_play_24dp : R.drawable.ic_notification_pause_24dp;


        notificationView.setImageViewResource(R.id.notification_button, buttonID);
        notificationView.setImageViewResource(R.id.notification_icon,R.drawable.ic_circletraining_logo_24dp);

        notificationView.setTextViewText(R.id.notification_title,this.getResources().getString(R.string.app_name));
        notificationView.setTextViewText(R.id.notification_icon_title,this.getResources().getString(R.string.app_name));
        notificationView.setTextViewText(R.id.notification_info, message);

        notificationView.setOnClickPendingIntent(R.id.notification_button, buttonPendingIntent);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_circletraining_logo_white_24dp)
                .setAutoCancel(true)
                .setCustomContentView(notificationView)
                .setCustomBigContentView(notificationView)
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    /**
     * Update the notification with current title and timer values.
     *
     * @param time The current timer value
     */
    private void updateNotification(int time) {
        if(isAppInBackground) {
            Notification notification = buildNotification(time);
            notiManager.notify(NOTIFICATION_ID, notification);
        }
        else if(notiManager != null) {
            notiManager.cancel(NOTIFICATION_ID);
        }
    }

    /**
     * Check if the app is in the background.
     * If so, start a notification showing the current timer.
     *
     * @param isInBackground Sets global flag to determine whether the app is in the background
     */
    public void setIsAppInBackground(boolean isInBackground){
        this.isAppInBackground = isInBackground;

        //Execute after short delay to prevent short notification popup if workoutActivity is closed
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int time = currentTitle.equals(getString(R.string.workout_headline_done)) ? 0 : (int) Math.ceil(savedTime / 1000.0);
                updateNotification(time);
            }
        }, 700);
    }

    /**
     * Cancel the notification when workout activity is destroyed
     */
    public void workoutClosed(){
        this.isAppInBackground = false;
        notiManager.cancel(NOTIFICATION_ID);
    }

    /**
     * Clean timer stop.
     * Stops all timers, cancels the notification, resets the variables and saves
     * statistics
     */
    public void cleanTimerFinish() {
        this.isAppInBackground = false;
        if (workoutTimer != null) {
            this.workoutTimer.cancel();
        }
        if (restTimer != null) {
            this.restTimer.cancel();
        }

        saveStatistics();
        savedTime = 0;
        isPaused = false;
        isCancelAlert = false;
        isBlockPeriodization = false;
        isStarttimer = false;
        isWorkout = false;
        isPaused = false;
        isCancelAlert = false;
        currentTitle = getString(R.string.workout_headline_done);
    }

    /**
     * Returns todays date as int in the form of yyyyMMdd
     * @return Today as in id
     */
    private int getTodayAsID() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String concatDate = dateFormat.format(new Date());

        return Integer.parseInt(concatDate);
    }

    /**
     * Updates the database with calculated global values.
     * Saved values are the workout duration and calories burned.
     */
    private void saveStatistics(){
        database = new PFASQLiteHelper(getBaseContext());
        int id = getTodayAsID();

        statistics = database.getWorkoutData(id);

        if(statistics.getID() == 0){
            database.addWorkoutDataWithID(new WorkoutSessionData(id, 0, 0));
            statistics = database.getWorkoutData(id);
        }

        int totalTimeSpentTraining = statistics.getWORKOUTTIME() + this.timeSpentWorkingOut;
        int totalCaloriesBurnt = isCaloriesEnabled(this) ? statistics.getCALORIES() + this.caloriesBurned : statistics.getCALORIES();

        database.updateWorkoutData(new WorkoutSessionData(id, totalTimeSpentTraining, totalCaloriesBurnt));
        this.timeSpentWorkingOut = 0;
        this.caloriesBurned = 0;
    }


    /**
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


    /**
    * Getter and Setter
    */
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

    public long getBlockRestTime(){
        return  this.blockPeriodizationTime;
    }

    public int getSets(){
        return this.sets;
    }

    public int getCaloriesBurned(){
        return this.caloriesBurned;
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

    public void setCancelAlert(boolean isCancelAlert){
        this.isCancelAlert = isCancelAlert;
    }

    public void setCurrentTitle(String title){
        this.currentTitle = title;
    }
}