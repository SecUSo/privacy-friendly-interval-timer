package com.intervaltimer.google.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.intervaltimer.google.helpers.NotificationHelper.isMotivationAlertEnabled;
import static com.intervaltimer.google.helpers.NotificationHelper.setMotivationAlert;

/**
 * Receives a broadcast when boot is completed and restarts the motivation notification if
 * it is enabled.
 *
 * @author Alexander Karakuz
 * @version 20170812
 */
public class OnBootCompletedBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(isMotivationAlertEnabled(context)){
            setMotivationAlert(context);
        }
    }
}
