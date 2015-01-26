package com.holkan.tracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.holkan.holkantracker.R;
import com.holkan.tracker.Utils.Utils;
import com.holkan.tracker.data.DaoSession;
import com.holkan.tracker.data.Tracking;
import com.holkan.tracker.model.Device;
import com.holkan.tracker.model.PlanInterval;
import com.holkan.tracker.service.Connection;
import com.holkan.tracker.service.GetDeviceRequest;
import com.holkan.tracker.service.GetDeviceResponse;
import com.holkan.tracker.service.PostTrackingRequest;
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
    public static final String STAT_SERVICE_STARTED = "SERVICE_STARTED";
    private GoogleApiClient googleApiClient;
    private TrackingDAOQueue trackingQueue;
    private Connection connection;
    private Device device;
    private PlanInterval currentPlan;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Location cachedLocation;

    @Override
    public void didResult(Connection connection, Response response) {

        if (response instanceof GetDeviceResponse) {

            GetDeviceResponse getDeviceResponse = (GetDeviceResponse) response;
            int imei = Integer.valueOf(getDeviceResponse.getDevice().getImei());


            if (imei == Integer.valueOf(Utils.getImei(getApplicationContext()))) {
                device = getDeviceResponse.getDevice();
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Conexión con servidor exitosa", Toast.LENGTH_LONG).show();
                        checkLocationRequest();
                    }
                });
            }

        } else if (response instanceof PostTrackingResponse) {
            PostTrackingResponse postTrackingResponse = (PostTrackingResponse) response;
            PostTrackingRequest postTrackingRequest = (PostTrackingRequest) postTrackingResponse.getRequest();
            Tracking tracking = new Tracking(postTrackingRequest.getTrackingId());
            DataSession.getSession(getApplicationContext()).delete(tracking);
            Log.d("e", "Delete");
        }
    }

    @Override
    public void didReceiveHttpError(Connection connection, Request request) {
        if (request instanceof GetDeviceRequest) {
            tryConnectLater();
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Conexion con servidor fallida", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private class TrackingDAOQueue {

        private Boolean sendingTracking = false;
        private boolean firstTracking = true;

        public TrackingDAOQueue() {
            ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
            Runnable r = new Runnable() {

                @Override
                public void run() {

                    synchronized (sendingTracking) {

                        try {
                            if (sendingTracking == false) {
                                sendingTracking = true;
                            } else {
                                return;
                            }
                            DaoSession dataSession = DataSession.getSession(getApplicationContext());

                            List<Tracking> trackingList = dataSession.getTrackingDao().loadAll();


                            for (Tracking tracking : trackingList) {
                                try {

                                    connection.requestPostTracking(tracking);


                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            sendingTracking = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }
            };
            timer.scheduleAtFixedRate(r, 0, 1, TimeUnit.SECONDS);
        }

        public void saveLocation(Location location) {
            if (location == null){
                location = cachedLocation;
            } else {
                cachedLocation = location;
            }

            if (location == null) return;

            Tracking tracking = new Tracking();
            tracking.setDatetime(new Date());
            tracking.setLat(location.getLatitude());
            tracking.setLng(location.getLongitude());
            tracking.setSpeed(Math.round(location.getSpeed()));
            tracking.setActive_gps(locationServicesAvailable());
            if (firstTracking) {
                tracking.setEvent((byte) 6);
                firstTracking = false;
            } else
                tracking.setEvent((byte) 1);
            tracking.setAccuracy(location.getAccuracy());
            tracking.setProvider(location.getProvider());

            DaoSession dataSession = DataSession.getSession(getApplicationContext());
            dataSession.getTrackingDao().insertWithoutSettingPk(tracking);
            Log.d("e", "Insert: " + String.format("%f,%f acurracy: %f, time: %s", location.getLatitude(), location.getLongitude(), location.getAccuracy(), String.valueOf(new Date())));
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (!autoStart()) {
            stopSelf();
            return;
        }
        setStatusCreated(true);
        connection = new Connection(getApplicationContext());
        connection.setConnectionListener(this);
        callGetDevice();

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
        Toast.makeText(getApplicationContext(), "Iniciando servicio de monitoreo", Toast.LENGTH_LONG).show();

        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Service.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(new GpsStatus.Listener() {
            @Override
            public void onGpsStatusChanged(int event) {
                if (event == GpsStatus.GPS_EVENT_STOPPED) {
                    forceCheckLocationRequest();
                }
            }
        });

    }

    private boolean autoStart() {
        return getApplicationContext().getSharedPreferences("settings", MODE_PRIVATE).getBoolean(SettingsFragment.PREF_AUTOSTART_SERVICE, false);
    }

    @Override
    public void onDestroy() {
        if (getStatusCreated()) {
            setStatusCreated(false);
            Toast.makeText(getApplicationContext(), "Deteniendo servicio de monitoreo", Toast.LENGTH_SHORT).show();
        }
        if (googleApiClient != null && googleApiClient.isConnected())
            googleApiClient.disconnect();
        super.onDestroy();
    }

    private void setStatusCreated(boolean b) {
        getApplicationContext().getSharedPreferences("status", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(STAT_SERVICE_STARTED, b)
                .commit();
    }

    private boolean getStatusCreated() {
        return getApplicationContext().getSharedPreferences("status", Context.MODE_PRIVATE).getBoolean(STAT_SERVICE_STARTED, false);
    }

    private void callGetDevice() {
        new AsyncTask<Void, Void, Void>() {


            @Override
            protected Void doInBackground(Void... params) {
                String imei = Utils.getImei(getApplicationContext());
                connection.requestGetDevice(imei);
                return null;
            }

        }.execute();
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

    private synchronized void checkLocationRequest() {
        if (device == null || googleApiClient == null || !googleApiClient.isConnected()) {
            return;
        }

        for (PlanInterval planInterval : device.getMonitor_plan()) {
            int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (planInterval.getFrom() <= hourOfDay && hourOfDay < planInterval.getUntil()) {
                if (planInterval != currentPlan) {
                    currentPlan = planInterval;
                    locationRequest(planInterval.getInterval());
                }
            }
        }


    }

    private void forceCheckLocationRequest() {
        currentPlan = null;
        checkLocationRequest();
    }

    private void tryConnectLater() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                callGetDevice();
            }
        }, 10000);
    }

    private void locationRequest(long interval) {
        final long fInterval = interval;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (locationServicesAvailable()) {
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setFastestInterval(fInterval * 1000);
                    locationRequest.setInterval(fInterval * 1000);
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, LocationService.this);
                } else {
                    onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));
                    currentPlan = null;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyGPSNetworkNotAvailable();
                        }
                    });
                    scheduledCheckGPSNetwork();
                }
            }

        });

    }

    private void scheduledCheckGPSNetwork() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkLocationServicesAvailable();
            }
        }, 10000);
    }

    private void checkLocationServicesAvailable() {
        if (locationServicesAvailable()) {
            checkLocationRequest();
        } else {
            scheduledCheckGPSNetwork();
        }
    }

    private boolean locationServicesAvailable() {
        LocationManager lm = null;
        boolean gps_enabled = false;
        if (lm == null)
            lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        return (gps_enabled);
    }


    private void notifyGPSNetworkNotAvailable() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.servicios_localizacion_inactivo));
        Intent resultIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(0, notification);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Conexión con servicio de localización suspendida", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            cachedLocation = location;
        }
        Log.d(getApplication().getPackageName(), String.format("LocationChanged %s", String.valueOf(new Date())));
        if (device != null) {
            trackingQueue.saveLocation(location);
            checkLocationRequest();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getApplicationContext(), R.string.location_service_connection_failed + connectionResult.toString(), Toast.LENGTH_LONG).show();
    }
}
