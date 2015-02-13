package com.holkan.tracker;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    public class SMSSenderReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateButtonStateAlert(true, false);
        }

    }

    private ToggleButton alertButton;
    private TextView textAlert;
    private SMSSenderReceiver smsSenderReceiver;


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
                        updateButtonStateAlert(false, true);
                    } else {
                        alertButton.setChecked(false);
                        textAlert.setText("Alerta");
                        Toast.makeText(getActivity(), R.string.should_configure_al_least_one_phone, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    updateButtonStateAlert(true, false);
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

    private void updateButtonStateAlert(boolean b, boolean fromButton) {
        Intent intent = new Intent(getActivity(), SMSSenderService.class);
        if (b) {
            alertButton.setChecked(false);
            textAlert.setText("Alerta");

            intent.setAction(SMSSenderService.ACTION_STOP);
            intent.putExtra("canceled", fromButton);
            getActivity().startService(intent);
        } else {
            alertButton.setChecked(true);
            textAlert.setText("Cancelar");
            intent.setAction(SMSSenderService.ACTION_START);
            getActivity().startService(intent);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        smsSenderReceiver = new SMSSenderReceiver();
        getActivity().registerReceiver(smsSenderReceiver, new IntentFilter(SMSSenderService.BROADCAST_SENDING_SMS_STOPPED));
    }

    @Override
    public void onDestroy() {
        if (smsSenderReceiver != null)
            getActivity().unregisterReceiver(smsSenderReceiver);
        super.onDestroy();
    }
}
