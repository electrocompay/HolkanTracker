package com.holkan.tracker;


import android.content.Context;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.Toast;

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
public class MainFragment extends Fragment {


    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, null);

        Button alertButton = (Button) view.findViewById(R.id.alertButton);

        alertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAlertSMSs();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = new Intent(getActivity(), LocationService.class);
        getActivity().startService(intent);
    }

    private void sendAlertSMSs() {

        SharedPreferences preferences = getActivity().getSharedPreferences(getActivity().getPackageName(), Context.MODE_PRIVATE);

        for (int i = 1; i < 4; i++) {

            String phoneNumber = preferences.getString("phone" + String.valueOf(i), null);
            String name = preferences.getString("name" + String.valueOf(i), null);

            if (!TextUtils.isEmpty(phoneNumber)) {
                String lat = String.format("%.06f", 1.0).replace(",", ".");
                String lng = String.format("%.06f", 1.0).replace(",", ".");
                String smsBody = getString(R.string.alert_sms_body, name, lat, lng, new Date().toString());
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber,
                            null,
                            smsBody,
                            null,
                            null);
                    Toast.makeText(getActivity(), R.string.alert_sms_has_been_sent,
                            Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    Toast.makeText(getActivity(), R.string.sms_has_not_been_send,
                            Toast.LENGTH_LONG).show();
                    ex.printStackTrace();
                }
            }

        }


    }


}
