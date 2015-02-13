package com.holkan.tracker;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.holkan.tracker.Utils.Utils;
import com.holkan.tracker.data.DaoSession;
import com.holkan.tracker.data.Tracking;
import com.holkan.tracker.data.TrackingDao;
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
public class LocationService extends Service implements Connection.ConnectionListener {

    private static final int NOTIFICATION_ID = 1;
    public static final String STAT_SERVICE_STARTED = "SERVICE_STARTED";
    private TrackingDAOQueue trackingQueue;
    private Connection connection;
    private Device device;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private GoogleCloudMessaging gcm;
    private String regid;
    private final String SENDER_ID = "820192468922";


    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    static final String TAG = "HolkanTracker";
    private GcmBroadcastReceiver receiver;
    public static int satellitesCount;
    private ScheduledThreadPoolExecutor scheduledLocationTimeout;
    private long lastRequestTime;
    private InstantLocationClient instantLocationClient;
    private ScheduledThreadPoolExecutor poolLocatorExecutor;
    private long currentInterval;
    private PowerManager.WakeLock wl;


    private class GcmBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            instantLocationClient.requestLocation(3);
            setResultCode(Activity.RESULT_OK);
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
                        createLocationClient();
                        prepareForReceiveCommands();
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

    private void prepareForReceiveCommands() {
        if (TextUtils.isEmpty(device.getNotification_id())) {
            getGCMPreferences(getApplicationContext()).edit().putString(PROPERTY_REG_ID, null).commit();
        }
        regid = getRegistrationId(getApplicationContext());

//        if (regid.isEmpty()) {
        registerInBackground();
//        }
        registerLocationReceiver();
    }

    @Override
    public void didReceiveHttpError(Connection connection, Request request) {
        if (request instanceof GetDeviceRequest) {
            tryConnectLater();
        } else if (request instanceof PostTrackingRequest) {
            PostTrackingRequest postTrackingRequest = (PostTrackingRequest) request;

            TrackingDao trackingDao = DataSession.getSession(getApplicationContext()).getTrackingDao();

            long trackingId = postTrackingRequest.getTrackingId();
            Tracking tracking = trackingDao.load(trackingId);
            if (tracking != null) {
                tracking.setActive_gprs(false);
            }
            trackingDao.insertOrReplace(tracking);
        }
    }

    private class TrackingDAOQueue {

        private Boolean sendingTracking = false;

        public TrackingDAOQueue() {
            ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
            timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
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

    }

    private class InstantLocationClient implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private Location cachedLocation;
        private GoogleApiClient instantGooleApiClient;
        private int event;
        private long lastLocationTime;

        public InstantLocationClient() {
            super();
            instantGooleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            instantGooleApiClient.connect();
        }

        @Override
        public void onConnected(Bundle bundle) {
            requestLocation(6);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onLocationChanged(Location location) {
            if (location == null) {
                location = cachedLocation;
            } else {
                cachedLocation = location;
            }

            if (location == null) return;

            lastLocationTime = location.getTime();

            synchronized (trackingQueue.sendingTracking) {
                Utils.saveLocation(getApplicationContext(), location, (byte) event);
            }

        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }

        public void requestLocation(int event) {
            Log.d(getPackageName(), String.format("Request Location: %d %s", event, new Date().toString()));
            this.event = event;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setFastestInterval(0);
                    locationRequest.setInterval(0);
                    locationRequest.setNumUpdates(1);
                    LocationServices.FusedLocationApi.requestLocationUpdates(instantGooleApiClient, locationRequest, InstantLocationClient.this);
                }
            });
        }


        public Location getCachedLocation() {
            if (cachedLocation == null)
                return new Location("");
            return cachedLocation;
        }

        public long getLastLocationTime() {
            return lastLocationTime;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        wl.acquire();

        if (!autoStart()) {
            stopSelf();
            return;
        }


        setStatusCreated(true);
        connection = new Connection(getApplicationContext());
        connection.setConnectionListener(this);
        callGetDevice();

        createInstantLocationClient();

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
                } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

                    satellitesCount = 0;
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    for (GpsSatellite gpsSatellite : gpsStatus.getSatellites()) {
                        if (gpsSatellite.usedInFix()) {
                            satellitesCount++;
                        }
                    }
                }
            }
        });

    }

    private void createInstantLocationClient() {
        instantLocationClient = new InstantLocationClient();
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

        if (poolLocatorExecutor != null)
            poolLocatorExecutor.shutdown();

        wl.release();
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
        return Service.START_STICKY;
    }

    private synchronized void createLocationClient() {

        PlanInterval currentPlanInterval = getCurrentPlanInterval();
        long interval = 300;
        if (currentPlanInterval != null) {
            interval = currentPlanInterval.getInterval();
        }

        poolLocatorExecutor = new ScheduledThreadPoolExecutor(1);
        poolLocatorExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        scheduleLocatorInterval(interval);


    }

    private void scheduleLocatorInterval(long interval) {

        currentInterval = interval;

        poolLocatorExecutor.scheduleAtFixedRate(new Runnable() {

            public static final long TIMEOUT_DELAY = 30;

            @Override
            public void run() {
                instantLocationClient.requestLocation(1);

                final Date requestTime = new Date();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        long timeElapsed = new Date().getTime() - instantLocationClient.getLastLocationTime();
                        if (TimeUnit.MILLISECONDS.toSeconds(timeElapsed) >= 30) {
                            synchronized (trackingQueue.sendingTracking) {
                                Utils.saveTimedOutLocation(getApplicationContext(), instantLocationClient.getCachedLocation(), requestTime);
                            }
                            Log.d(getPackageName(), "Request Location timeout");
                        }

                    }
                }, TimeUnit.SECONDS.toMillis(TIMEOUT_DELAY));


                PlanInterval configuredIntervalPlan = getCurrentPlanInterval();

                if (currentInterval != configuredIntervalPlan.getInterval()) {
                    scheduleLocatorInterval(configuredIntervalPlan.getInterval());
                }
            }
        }, interval, interval, TimeUnit.SECONDS);
    }


    private PlanInterval getCurrentPlanInterval() {
        for (PlanInterval planInterval : device.getMonitor_plan()) {
            int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (planInterval.getFrom() <= hourOfDay && hourOfDay < planInterval.getUntil()) {
                return planInterval;
            }
        }
        return null;
    }

    private void tryConnectLater() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                callGetDevice();
            }
        }, 10000);
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
