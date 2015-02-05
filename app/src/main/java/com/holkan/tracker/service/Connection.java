package com.holkan.tracker.service;

import android.content.Context;
import android.location.Location;
import android.text.format.DateUtils;

import com.holkan.tracker.Utils.Utils;
import com.holkan.tracker.data.Tracking;

import org.apache.http.HttpResponse;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Connection implements Request.RequestListener {

    private final Context context;

    public interface ConnectionListener {

        void didResult(Connection connection, Response response);

        void didReceiveHttpError(Connection connection, Request request);
    }

    private static final int MAX_SIM_THREADS = 5;

    private ConnectionListener connectionListener;
    @SuppressWarnings("unused")
    private final ExecutorService serialExecutor;
    private final ExecutorService poolExecutor;
    private final ArrayList<String> requestedImages;

    private boolean canceled = false;

    public Connection(Context context) {
        this.context = context;
        serialExecutor = Executors.newSingleThreadExecutor();
        poolExecutor = Executors.newFixedThreadPool(MAX_SIM_THREADS);
        requestedImages = new ArrayList<String>();
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    private Response processResponse(Request request, HttpResponse httpResponse, String content) {
        if (content != null) {
            Class<?> responseClass = request.getResponseClass();
            Response response;
            try {
                response = (Response) responseClass.newInstance();
                response.setReference(request.getReference());
                response.setRequest(request);
                response.init(content, httpResponse);
                return response;
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void didReceiveContent(Request request, HttpResponse httpResponse, String content) {
        if (connectionListener != null && !canceled)
        {
            Response processResponse = processResponse(request, httpResponse, content);

            if (processResponse.getResultCode() == -1 && httpResponse.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK)
                processResponse.setResultCode(0);
            connectionListener.didResult(this, processResponse);
        }
    }

    @Override
    public void didReceiveError(Request request, HttpResponse httpResponse, String content) {
        if (httpResponse != null) {
            Response response = processResponse(request, httpResponse, content);
            request.setResponse(response);
        }
        if (connectionListener != null && !canceled) {
            connectionListener.didReceiveHttpError(this, request);
        }
    }

    public void cancel() {
        canceled = true;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }


    public void requestGetDevice(String imei)
    {
        GetDeviceRequest request = new GetDeviceRequest(poolExecutor);
        request.setImei(imei);
        request.setRequestListener(this);
        request.run();
    }

    public void requestPostTracking(Tracking tracking)
    {
        String imei = Utils.getImei(context);
        PostTrackingRequest request = new PostTrackingRequest(null);
        JsonParameters parameters = new JsonParameters();
        parameters.put("imei", imei);
        parameters.put("lat", tracking.getLat());
        parameters.put("lng", tracking.getLng());
        parameters.put("speed", tracking.getSpeed());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        parameters.put("datetime", formatter.format(tracking.getDatetime()));
        parameters.put("event", tracking.getEvent());
        parameters.put("accuracy", tracking.getAccuracy());
        parameters.put("provider", tracking.getProvider());
        parameters.put("active_gps", tracking.getActive_gps());
        parameters.put("battery", tracking.getBattery());
        parameters.put("satellites", tracking.getSatellites());
        request.setJsonParameters(parameters);
        request.setRequestListener(this);
        request.setTrackingId(tracking.getId());
        request.run();
    }

    public void requestPostTracking(Location location, int event)
    {
        String imei = Utils.getImei(context);
        PostTrackingRequest request = new PostTrackingRequest(null);
        JsonParameters parameters = new JsonParameters();
        parameters.put("imei", imei);
        parameters.put("lat", location.getLatitude());
        parameters.put("lng", location.getLongitude());
        parameters.put("speed", Math.round(location.getSpeed()));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        parameters.put("datetime", formatter.format(location.getTime()));
        parameters.put("event", event);
        parameters.put("accuracy", location.getAccuracy());
        parameters.put("provider", location.getProvider());
        parameters.put("active_gps", Utils.locationServicesAvailable(context));
        parameters.put("battery", Utils.getBatteryLevel(context));
        parameters.put("satellites", Utils.getSatellites(context));
        request.setJsonParameters(parameters);
        request.setRequestListener(this);
        request.run();
    }

    public void requestSetNotificationId(String notificationId)
    {
        String imei = Utils.getImei(context);
        SetNotificationIdRequest request = new SetNotificationIdRequest(poolExecutor);
        JsonParameters parameters = new JsonParameters();
        parameters.put("imei", imei);
        parameters.put("notificationId", notificationId);
        request.setJsonParameters(parameters);
        request.setRequestListener(this);
        request.run();
    }


}
