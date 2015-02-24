package com.holkan.tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.holkan.tracker.Utils.Utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by abel.miranda on 2/4/15.
 */
public class SMSSenderService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ALERT_MESSAGES_BASIC_COUNT = 3;
    private static final int ALERT_MESSAGES_ADVANCED_COUNT = 1;
    private static final int ALERT_MESSAGES_INTERVAL_MINUTES = 5;
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_NEXT_SMS = "NEXT_SMS";
    public static final String BROADCAST_SENDING_SMS_STOPPED = "com.holkan.tracker.SENDING_SMS_STOPPED";
    private Handler mainLooperHandler;
    private String action;

    private class SMSSender {


        private synchronized void sendAlertSMSs(Location location) {


            mainLooperHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.alert_sms_has_been_sent,
                            Toast.LENGTH_LONG).show();
                }
            });
            SharedPreferences preferences = getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
            Log.d(getApplicationContext().getPackageName(), "Sending SMS");

            for (int i = 1; i < 4; i++) {

                String phoneNumber = preferences.getString("PHONE" + String.valueOf(i), null);
                String name = preferences.getString(SettingsFragment.PREF_NAME, null);

                if (!TextUtils.isEmpty(phoneNumber)) {
                    String strLat = String.format("%.06f", location.getLatitude()).replace(",", ".");
                    String strLng = String.format("%.06f", location.getLongitude()).replace(",", ".");
                    String smsBody = getString(R.string.alert_sms_body, name, strLat, strLng, new Date().toString());
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNumber,
                                null,
                                smsBody,
                                null,
                                null);

                        Log.d(getClass().getSimpleName(), String.format("Alerta %d: %s", getCurrentMessageCount(), smsBody));

                    } catch (Exception ex) {
                        mainLooperHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.sms_has_not_been_send,
                                        Toast.LENGTH_LONG).show();

                            }
                        });
                        ex.printStackTrace();
                    }
                }

            }

            updateSendingState();

        }

        private void updateSendingState() {
            setCurrentMessagecount(getCurrentMessageCount() - 1);
            if (getCurrentMessageCount() == 0) {
                stopSendingSMS();
                Intent intent = new Intent(BROADCAST_SENDING_SMS_STOPPED);
                getApplicationContext().sendBroadcast(intent);
            }
        }

    }

    private int getCurrentMessageCount() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int currentMessageCount = sharedPreferences.getInt("currentSmsCount", 0);
        return currentMessageCount;
    }

    private GoogleApiClient googleApiClient;
    private SMSSender smsSender;

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        final int refreshInterval = 1;
        locationRequest.setFastestInterval(refreshInterval);
        locationRequest.setInterval(refreshInterval);
        locationRequest.setNumUpdates(1);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        smsSender = new SMSSender();
        smsSender.sendAlertSMSs(location);
        if (monitoringServiceActive() &&  getCurrentMessageCount() == 0){
            Utils.saveLocation(getApplicationContext(), location, (byte) 2);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), R.string.location_service_connection_failed + connectionResult.toString(), Toast.LENGTH_LONG).show();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        action = intent.getAction();

        if (action != null) {
            switch (action) {

                case ACTION_START:
                    startSendingSMS();
                    break;

                case ACTION_NEXT_SMS:
                    nextSMS();
                    break;

                case ACTION_STOP:
                    stopSendingSMS();
                    break;

                default:
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private void nextSMS() {
        if (googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void stopSendingSMS() {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Service.ALARM_SERVICE);
        PendingIntent pendingIntent = getPendingIntent();
        alarmManager.cancel(pendingIntent);
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM_SMS);
        return PendingIntent.getBroadcast(getApplicationContext(), AlarmReceiver.REQUEST_CODE_SEND_SMS, intent, 0);
    }

    private void startSendingSMS() {
        stopSendingSMS();
        if (monitoringServiceActive()) {
            setCurrentMessagecount(ALERT_MESSAGES_ADVANCED_COUNT);
        } else
            setCurrentMessagecount(ALERT_MESSAGES_BASIC_COUNT);

        PendingIntent pendingIntent = getPendingIntent();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, TimeUnit.MINUTES.toMillis(ALERT_MESSAGES_INTERVAL_MINUTES), pendingIntent);
    }

    private void setCurrentMessagecount(int currentMessageCount) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getPackageName(), MODE_PRIVATE);
        sharedPreferences.edit().putInt("currentSmsCount", currentMessageCount).commit();
    }

    private boolean monitoringServiceActive() {
        return getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean(SettingsFragment.PREF_AUTOSTART_SERVICE, false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainLooperHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
