package com.holkan.tracker;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.holkan.holkantracker.R;
import com.holkan.tracker.Utils.Utils;
import com.holkan.tracker.service.Connection;
import com.holkan.tracker.service.Request;
import com.holkan.tracker.service.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {


    public static final String PREF_NAME = "NAME";
    private static final String PREF_PHONE1 = "PHONE1";
    private static final String PREF_PHONE2 = "PHONE2";
    private static final String PREF_PHONE3 = "PHONE3";
    public static final String PREF_AUTOSTART_SERVICE = "AUTOSTART_SERVICE";
    private Connection connection;
    private SwitchCompat switchCompat;

    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);


        EditText name = (EditText) view.findViewById(R.id.name);
        name.setText(loadFromPreference(PREF_NAME));
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveInPreference(PREF_NAME, s.toString());
            }
        });


        EditText phone1 = (EditText) view.findViewById(R.id.phone1);
        phone1.setText(loadFromPreference(PREF_PHONE1));
        phone1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveInPreference(PREF_PHONE1, s.toString());
            }
        });

        EditText phone2 = (EditText) view.findViewById(R.id.phone2);
        phone2.setText(loadFromPreference(PREF_PHONE2));
        phone2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveInPreference(PREF_PHONE2, s.toString());
            }
        });

        EditText phone3 = (EditText) view.findViewById(R.id.phone3);
        phone3.setText(loadFromPreference(PREF_PHONE3));
        phone3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                saveInPreference(PREF_PHONE3, s.toString());
            }
        });

        switchCompat = (SwitchCompat) view.findViewById(R.id.service_switch);
        switchCompat.setChecked(getServiceStatusStarted());
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkImeiRegistered();
                } else {
//                    Toast.makeText(getActivity(), "Deteniendo servicio de monitoreo", Toast.LENGTH_SHORT).show();
                    stopService();
                }
            }
        });

        return view;
    }

    private void checkImeiRegistered() {
        if (connection == null) {
            connection = new Connection(getActivity());
        }

        connection.setConnectionListener(new Connection.ConnectionListener() {
            @Override
            public void didResult(Connection connection, Response response) {
//                Toast.makeText(getActivity(), "Iniciando servicio de monitoreo", Toast.LENGTH_SHORT).show();
                if (getActivity() != null)
                    startService();
            }

            @Override
            public void didReceiveHttpError(Connection connection, Request request) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            stopService();
                            new AlertDialog.Builder(getActivity()).setTitle("Holkan Rastreador")
                                    .setMessage("IMEI\n" + Utils.getImei(getActivity()) +
                                            "\nHolkan Rastreo Satelital . Por favor contáctenos al 01 800 1614- 295 o envíe un correo a ventas@holkan.com.mx para activar su servicio.\nGracias.")
                                    .setCancelable(true)
                                    .setNegativeButton("Aceptar", null)
                                    .show();
                        }
                    }
                });
            }
        });

        connection.requestGetDevice(Utils.getImei(getActivity()));
    }

    private void startService() {
        saveInPreference(PREF_AUTOSTART_SERVICE, true);
        getActivity().startService(new Intent(getActivity(), LocationService.class));
    }

    private void stopService() {
        switchCompat.setChecked(false);
        saveInPreference(PREF_AUTOSTART_SERVICE, false);
        getActivity().stopService(new Intent(getActivity(), LocationService.class));
    }

    private boolean getServiceStatusStarted() {
        return getActivity().getSharedPreferences("status", Context.MODE_PRIVATE).getBoolean(LocationService.STAT_SERVICE_STARTED, false);
    }

    private void saveInPreference(String prefName, String s) {
        getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putString(prefName, s)
                .commit();
    }

    private void saveInPreference(String prefName, boolean b) {
        getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(prefName, b)
                .commit();
    }

    private String loadFromPreference(String prefName) {
        return getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE).getString(prefName, "");
    }

}
