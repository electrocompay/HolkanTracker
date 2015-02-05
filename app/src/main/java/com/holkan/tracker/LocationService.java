package com.holkan.tracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
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

import java.io.IOException;
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

    private GoogleCloudMessaging gcm;
    private String regid;
    private final String SENDER_ID = "820192468922";
    private Boolean forcedLocationEvent3 = false;


    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    static final String TAG = "HolkanTracker";
    private GcmBroadcastReceiver receiver;
    private boolean forcedLocationEvent4;


    private class GcmBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            forcedLocationEvent3 = true;
            forceCheckLocationRequest();
        }
    }

    @Override
    public void didResult(Connection connection, Response response) {

        if (response instanceof GetDeviceResponse) {

            GetDeviceResponse getDeviceResponse = (GetDeviceResponse) response;
            long imei = Long.valueOf(getDeviceResponse.getDevice().getImei());


            if (imei == Long.valueOf(Utils.getImei(getApplicationContext()))) {
                device = getDeviceResponse.getDevice();

                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Conexión con servidor exitosa", Toast.LENGTH_LONG).show();
                        checkLocationRequest();
                    }
                });

                prepareForReceiveCommands();
            }

        } else if (response instanceof PostTrackingResponse) {
            PostTrackingResponse postTrackingResponse = (PostTrackingResponse) response;
            PostTrackingRequest postTrackingRequest = (PostTrackingRequest) postTrackingResponse.getRequest();
            Tracking tracking = new Tracking(postTrackingRequest.getTrackingId());
            DataSession.getSession(getApplicationContext()).delete(tracking);
            Log.d("e", "Delete");
        }
    }

    private void prepareForReceiveCommands() {
        if (TextUtils.isEmpty(device.getNotification_id())){
            getGCMPreferences(getApplicationContext()).edit().putString(PROPERTY_REG_ID, null).commit();
        }
        regid = getRegistrationId(getApplicationContext());

        if (regid.isEmpty()) {
            registerInBackground();
        }
        registerLocationReceiver();
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
            if (location == null) {
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
            tracking.setActive_gps(Utils.locationServicesAvailable(getApplicationContext()));
            tracking.setBattery(Utils.getBatteryLevel(getApplicationContext()));
            if (firstTracking) {
                tracking.setEvent((byte) 6);
                firstTracking = false;
            } else if (forcedLocationEvent3) {
                tracking.setEvent((byte) 3);
                forcedLocationEvent3 = false;
            } else if (forcedLocationEvent4) {
                tracking.setEvent((byte) 4);
                forcedLocationEvent4 = false;
            } else
                tracking.setEvent((byte) 1);
            tracking.setAccuracy(location.getAccuracy());
            tracking.setProvider(location.getProvider());
            tracking.setSatellites(Utils.getSatellites(getApplicationContext()));

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
                .setContentText("Servicio en ejecución")
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
                    forcedLocationEvent4 = true;
                    forceCheckLocationRequest();
                }
            }
        });

    }

    private void registerLocationReceiver() {
        receiver = new GcmBroadcastReceiver();
        IntentFilter filter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
        filter.addCategory(getPackageName());
        getApplicationContext().registerReceiver(receiver, filter);
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
                if (Utils.locationServicesAvailable(getApplicationContext())) {
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setFastestInterval(fInterval * 1000);
                    locationRequest.setInterval(fInterval * 1000);
                    LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, LocationService.this);
                    scheduleLocationTimeOut(fInterval);
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

    private void scheduleLocationTimeOut(long interval) {
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                connection.requestPostTracking(new Location(null), );
//            }
//        }, interval + 2000);
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
        if (Utils.locationServicesAvailable(getApplicationContext())) {
            checkLocationRequest();
        } else {
            scheduledCheckGPSNetwork();
        }
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
        if (location != null) {
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


    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    sendRegistrationIdToBackend();

                    storeRegistrationId(regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String s) {
                Log.d(getPackageName(), s);
            }

        }.execute(null, null, null);
    }

    private void storeRegistrationId(String regId) {
        final SharedPreferences prefs = getGCMPreferences(getApplicationContext());
        int appVersion = getAppVersion(getApplicationContext());
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void sendRegistrationIdToBackend() {
        connection.requestSetNotificationId(regid);
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(LocationService.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }


    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
}
