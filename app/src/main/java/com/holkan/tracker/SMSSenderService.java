package com.holkan.tracker;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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

/**
 * Created by abel.miranda on 2/4/15.
 */
public class SMSSenderService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ALERT_MESSAGES_BASIC_COUNT = 3;
    private static final int ALERT_MESSAGES_ADVANCED_COUNT = 1;
    private static final int ALERT_MESSAGES_INTERVAL_MINUTES = 5;
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String BROADCAST_SENDING_SMS_STOPPED = "com.holkan.holkantracker.SENDING_SMS_STOPPED";
    private Runnable runnable;
    private static Handler mainLooperHandler = new Handler(Looper.getMainLooper());
    private static boolean alreadeyOneInstance;

    private class SMSSender {


        private int currentMessageCount;

        public void startSendingSMSs(final Location location) {
            if (monitoringServiceActive())
                currentMessageCount = ALERT_MESSAGES_ADVANCED_COUNT;
            else
                currentMessageCount = ALERT_MESSAGES_BASIC_COUNT;
            final Location fLocation = location;

            runnable = new Runnable() {

                @Override
                public void run() {
                    if (serviceCanceled) {
                        smsSender.cancelSendingSMSs();
                        return;
                    }
                    sendAlertSMSs(fLocation);

                    if (currentMessageCount > 0) {
                        mainLooperHandler.postDelayed(runnable, ALERT_MESSAGES_INTERVAL_MINUTES * 100 * 60);
                    }

                }

            };

            mainLooperHandler.post(runnable);
        }

        public synchronized void cancelSendingSMSs() {
            Log.d(getApplicationContext().getPackageName(), "Cancel Sending SMS");
            mainLooperHandler.removeCallbacksAndMessages(null);
            stopSelf();
        }

        private synchronized void sendAlertSMSs(Location location) {

            if ((currentMessageCount == 0))
                return;

            Utils.saveLocation(getApplicationContext(), location, (byte) 2);

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

                        Log.d(getClass().getSimpleName(), String.format("Alerta %d: %s", currentMessageCount, smsBody));

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
            currentMessageCount--;
            if (currentMessageCount == 0) {
                Intent intent = new Intent(BROADCAST_SENDING_SMS_STOPPED);
                getApplicationContext().sendBroadcast(intent);

            }
        }

    }

    private static boolean serviceCanceled;
    private GoogleApiClient googleApiClient;
    private SMSSender smsSender;


    public SMSSenderService() {
        super("SMSSenderService");
    }

    private synchronized void createLocationClient() {
        if (googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

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
        if (!serviceCanceled) {
            if (smsSender != null)
                smsSender.cancelSendingSMSs();
            smsSender = new SMSSender();
            smsSender.startSendingSMSs(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), R.string.location_service_connection_failed + connectionResult.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (alreadeyOneInstance) {
            return;
        } else {
            alreadeyOneInstance = true;
        }
        if (intent.getAction().equalsIgnoreCase(ACTION_START)) {
            startSendingSMS();
        } else if (intent.getAction().equalsIgnoreCase(ACTION_STOP)) {
            stopSendingSMS();
        }
    }

    private void stopSendingSMS() {
        mainLooperHandler.removeCallbacksAndMessages(null);
        serviceCanceled = true;
        if (smsSender != null) {
            smsSender.cancelSendingSMSs();
            smsSender = null;
        }
    }

    private void startSendingSMS() {
        stopSendingSMS();
        serviceCanceled = false;
        createLocationClient();
    }

    private boolean monitoringServiceActive() {
        return getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean(SettingsFragment.PREF_AUTOSTART_SERVICE, false);
    }

    @Override
    public void onDestroy() {
        alreadeyOneInstance = false;
        super.onDestroy();
    }

}
