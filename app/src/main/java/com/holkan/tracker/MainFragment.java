package com.holkan.tracker;


import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telephony.SmsManager;
import android.text.TextUtils;
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

import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {


    private static final int ALERT_MESSAGES_COUNT = 3;
    private GoogleApiClient googleApiClient;
    private int currentMessageCount;
    private ToggleButton alertButton;
    private TextView textAlert;

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
                if (isChecked){
                    getLocationAndSendSMS();
                    updateButtonStateAlert(false);
                } else {
                    updateButtonStateAlert(true);
                }
            }
        });
        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return view;
    }

    private void getLocationAndSendSMS() {
        createLocationClient();
    }

    private synchronized void createLocationClient() {
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void sendAlertSMSs(Location location) {

        Toast.makeText(getActivity(), R.string.alert_sms_has_been_sent,
                Toast.LENGTH_LONG).show();
        SharedPreferences preferences = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

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
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), R.string.sms_has_not_been_send,
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }
            }

        }


    }

    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        final int refreshInterval = 5 * 60 * 1000;
        locationRequest.setFastestInterval(refreshInterval);
        locationRequest.setInterval(refreshInterval);
        locationRequest.setNumUpdates(ALERT_MESSAGES_COUNT);
        currentMessageCount = ALERT_MESSAGES_COUNT;
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
        if (alertButton.isChecked()){
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            return;
        }
        currentMessageCount--;
        sendAlertSMSs(location);
        if (currentMessageCount == 0){
            updateButtonStateAlert(true);
        }
    }

    private void updateButtonStateAlert(boolean b) {
        if (b){
            alertButton.setChecked(false);
            textAlert.setText("Alerta");
        } else {
            alertButton.setChecked(true);
            textAlert.setText("Cancelar");
        }
    }
}
