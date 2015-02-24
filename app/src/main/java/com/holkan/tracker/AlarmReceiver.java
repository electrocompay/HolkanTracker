package com.holkan.tracker;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.Date;

/**
 * Created by abel.miranda on 2/12/15.
 */
public class AlarmReceiver extends BroadcastReceiver {


    public static final int REQUEST_CODE_SEND_SMS = 1;
    public static final String ACTION_ALARM_SMS = "ALARM_SMS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(context.getPackageName(), String.format("Alarma recibida: %s", new Date().toString()));
        String action = intent.getAction();

        if (action != null) {
            switch (action) {

                case ACTION_ALARM_SMS: sendNextSMS(context);
                                       break;

                default:
                    break;

            }
        }

        setResultCode(Activity.RESULT_OK);
    }

    private void sendNextSMS(Context context) {
        Intent serviceIntent = new Intent(context, SMSSenderService.class);
        serviceIntent.setAction(SMSSenderService.ACTION_NEXT_SMS);
        context.startService(serviceIntent);
    }
}
