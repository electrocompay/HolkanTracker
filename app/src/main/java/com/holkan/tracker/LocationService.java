package com.holkan.tracker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.holkan.tracker.Utils.Utils;
import com.holkan.tracker.data.DaoSession;
import com.holkan.tracker.data.Tracking;
import com.holkan.tracker.model.Device;
import com.holkan.tracker.model.PlanInterval;
import com.holkan.tracker.service.Connection;
import com.holkan.tracker.service.GetDeviceResponse;
import com.holkan.tracker.service.PostTrackingResponse;
import com.holkan.tracker.service.Request;
import com.holkan.tracker.service.Response;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Created by abel.miranda on 12/23/14.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, Connection.ConnectionListener {

    private static final int NOTIFICATION_ID = 1;
    private GoogleApiClient googleApiClient;
    private TrackingDAOQueue trackingQueue;
    private Connection connection;
    private Device device;
    private PlanInterval currentPlan;

    @Override
    public void didResult(Connection connection, Response response) {

        if (response instanceof GetDeviceResponse) {

            GetDeviceResponse getDeviceResponse = (GetDeviceResponse) response;
            String imei = getDeviceResponse.getDevice().getImei();

            if (imei.equals(Utils.getImei(getApplicationContext()))) {

                device = getDeviceResponse.getDevice();

                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Server Connection Succeed", Toast.LENGTH_LONG).show();
                        createLocationClient();

                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                new Intent(getApplicationContext(), MainActivity.class),
                                0);

                        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                                .setContentTitle("Holkan Tracker")
                                .setContentText("Rastreador Holkan detector de posicion")
                                .setSmallIcon(android.R.drawable.stat_notify_sync)
                                .setContentIntent(pendingIntent)
                                .build();
                        startForeground(NOTIFICATION_ID, notification);

                        trackingQueue = new TrackingDAOQueue();
                    }
                });

            }

        } else if (response instanceof PostTrackingResponse){
            PostTrackingResponse postTrackingResponse = (PostTrackingResponse) response;

            DataSession.getSession(getApplicationContext()).delete(postTrackingResponse.getTracking());
            Log.d("e", "Delete");
        }
    }

    @Override
    public void didReceiveHttpError(Connection connection, Request request) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Server Connection Failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private class TrackingDAOQueue {

        public TrackingDAOQueue() {
            ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
            Runnable r = new Runnable() {
                @Override
                public void run() {

                    synchronized (this) {

                        DaoSession dataSession = DataSession.getSession(getApplicationContext());

                        List<Tracking> trackingList = dataSession.getTrackingDao().loadAll();


                        for (Tracking tracking : trackingList) {
                            connection.requestPostTracking(tracking.getLat(), tracking.getLng(), tracking.getSpeed(), tracking.getDatetime(), tracking.getEvent(), tracking.getAccuracy());
                        }

                    }

                }
            };
            timer.scheduleAtFixedRate(r, 0, 1, TimeUnit.SECONDS);
        }

        public void saveLocation(Location location) {
            Tracking tracking = new Tracking();
            tracking.setDatetime(new Date());
            tracking.setLat(location.getLatitude());
            tracking.setLng(location.getLongitude());
            tracking.setSpeed(location.getSpeed());
            tracking.setEvent(1);
            tracking.setAccuracy(location.getAccuracy());

            DaoSession dataSession = DataSession.getSession(getApplicationContext());
            synchronized (this) {
                dataSession.getTrackingDao().insertWithoutSettingPk(tracking);
            }
            Log.d("e", "Insert: " + String.format("%f,%f acurracy: %f", location.getLatitude(), location.getLongitude(), location.getAccuracy()));
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        connection = new Connection(getApplicationContext());
        connection.setConnectionListener(this);
        String imei = Utils.getImei(getApplicationContext());
        connection.requestGetDevice(imei);

        Toast.makeText(getApplicationContext(), "Starting Service", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private synchronized void createLocationClient() {
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }


    @Override
    public void onConnected(Bundle bundle) {
        checkLocationRequest();
    }

    private void checkLocationRequest(){

        for (PlanInterval planInterval : device.getMonitor_plan()){
            int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (planInterval.getFrom() <= hourOfDay  && hourOfDay <  planInterval.getUntil()){
                if (planInterval != currentPlan) {
                    currentPlan = planInterval;
                    locationRequest(planInterval.getInterval());
                }
            }
        }


    }

    private void locationRequest(long interval) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setFastestInterval(interval * 1000);
        locationRequest.setInterval(interval * 1000);
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Connection Suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        checkLocationRequest();
        trackingQueue.saveLocation(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
    }
}
