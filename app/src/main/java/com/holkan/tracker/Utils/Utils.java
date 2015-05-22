package com.holkan.tracker.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.holkan.tracker.DataSession;
import com.holkan.tracker.LocationService;
import com.holkan.tracker.data.DaoSession;
import com.holkan.tracker.data.Tracking;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Date;

public class Utils {

    public static String readFully(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream);

        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }

    public static String getImei(Context context) {
//        return "357157050174843";
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return 50;
        }

        return Math.round(((float) level / (float) scale) * 100.0f);
    }

    public static boolean locationServicesAvailable(Context context) {
        LocationManager lm = null;
        boolean gps_enabled = false;
        if (lm == null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        return (gps_enabled);
    }


    public static void saveLocation(Context context, Location location, byte event) {
        internalSaveLocation(context, location, event, Utils.locationServicesAvailable(context), new Date());
    }

    private static void internalSaveLocation(Context context, Location location, byte event, boolean activeGps, Date requestDate){
        Tracking tracking = new Tracking();
        tracking.setDatetime(requestDate);
        tracking.setLat(location.getLatitude());
        tracking.setLng(location.getLongitude());
        tracking.setSpeed(Math.round(location.getSpeed()));
        tracking.setActive_gps(activeGps);
        tracking.setBattery(Utils.getBatteryLevel(context));
        tracking.setEvent(event);
        tracking.setAccuracy(location.getAccuracy());
        tracking.setProvider(location.getProvider());
        tracking.setSatellites(LocationService.satellitesCount);
        tracking.setActive_gprs(true);

        DaoSession dataSession = DataSession.getSession(context);
        dataSession.getTrackingDao().insertWithoutSettingPk(tracking);
        Log.d("e", "Insert: " + String.format("%f,%f acurracy: %f, time: %s", location.getLatitude(), location.getLongitude(), location.getAccuracy(), String.valueOf(new Date())));
    }

    public static void saveTimedOutLocation(Context context, Location location, Date requestDate) {
        internalSaveLocation(context, location, (byte) 1, false, requestDate);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            NetworkInfo info = manager.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public static void LogSentPacket(int size){

    }

    public static void logreceivedPacket(int size){

    }

}