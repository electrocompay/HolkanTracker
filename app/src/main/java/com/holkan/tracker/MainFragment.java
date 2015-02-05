package com.holkan.tracker;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.holkan.holkantracker.R;
import com.holkan.tracker.service.Connection;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ALERT_MESSAGES_COUNT = 3;
    private static final int ALERT_MESSAGES_INTERVAL_MINUTES = 5;
    private GoogleApiClient googleApiClient;
    private ToggleButton alertButton;
    private TextView textAlert;
    private SMSSender smsSender;

    public MainFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, null);

        textAlert = (TextView) view.findViewById(R.id.textViewAlert);

        alertButton = (ToggleButton) view.findViewById(R.id.alertButton);
        alertButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (hasAtLeastOnePhone()) {
                        updateButtonStateAlert(false);
                    } else {
                        alertButton.setChecked(false);
                        textAlert.setText("Alerta");
                        Toast.makeText(getActivity(), R.string.should_configure_al_least_one_phone, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateButtonStateAlert(true);
                }
            }
        });

        return view;
    }

    private boolean hasAtLeastOnePhone() {
        SharedPreferences preferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        Log.d(getActivity().getPackageName(), "Sending SMS");

        for (int i = 1; i < 4; i++) {

            String phoneNumber = preferences.getString("PHONE" + String.valueOf(i), null);
            if (!TextUtils.isEmpty(phoneNumber)) {
                return true;
            }

        }
        return false;
    }

    private void getLocationAndSendSMS() {
        createLocationClient();
    }

    private synchronized void createLocationClient() {
        if (googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(MainFragment.this)
                .addOnConnectionFailedListener(MainFragment.this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), R.string.location_service_connection_failed + connectionResult.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (alertButton.isChecked()) {
            if (smsSender != null)
                smsSender.cancelSendingSMSs();
            smsSender = new SMSSender();
            smsSender.startSendingSMSs(location);
        }
    }

    private void updateButtonStateAlert(boolean b) {
        if (b) {
            alertButton.setChecked(false);
            textAlert.setText("Alerta");
            if (smsSender != null)
                smsSender.cancelSendingSMSs();
        } else {
            alertButton.setChecked(true);
            textAlert.setText("Cancelar");
            getLocationAndSendSMS();
        }
    }


    private class SMSSender {


        private ScheduledThreadPoolExecutor poolExecutor;
        private int currentMessageCount;
        private ScheduledFuture<?> scheduledFuture;

        public void startSendingSMSs(final Location location) {
            currentMessageCount = ALERT_MESSAGES_COUNT;
            poolExecutor = new ScheduledThreadPoolExecutor(1);
            final Location fLocation = location;

            scheduledFuture = poolExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {

                    sendAlertSMSs(fLocation);

                }


            }, 0, ALERT_MESSAGES_INTERVAL_MINUTES, TimeUnit.MINUTES);
        }

        public synchronized void cancelSendingSMSs() {
            Log.d(getActivity().getPackageName(), "Cancel Sending SMS");
            if (poolExecutor != null) {
                scheduledFuture.cancel(false);
                poolExecutor.shutdownNow();
            }
        }

        private synchronized void sendAlertSMSs(Location location) {

            if (currentMessageCount == 0) return;

            if (monitoringServiceActive()) {
                Connection connection = new Connection(getActivity());
                connection.requestPostTracking(location, 2);
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.alert_sms_has_been_sent,
                            Toast.LENGTH_LONG).show();
                }
            });
            SharedPreferences preferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
            Log.d(getActivity().getPackageName(), "Sending SMS");

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

                        Log.d(getClass().getSimpleName(), String.format("Alerta %d: %s", 4 - currentMessageCount, smsBody));

                    } catch (Exception ex) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), R.string.sms_has_not_been_send,
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateButtonStateAlert(true);

                    }
                });
            }
        }

    }

    private boolean monitoringServiceActive() {
        return getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean(SettingsFragment.PREF_AUTOSTART_SERVICE, false);
    }

}
