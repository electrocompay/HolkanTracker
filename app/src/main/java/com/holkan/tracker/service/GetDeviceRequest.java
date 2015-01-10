package com.holkan.tracker.service;

import org.apache.http.client.methods.HttpGet;

import java.util.concurrent.ExecutorService;

/**
 * Created by abel.miranda on 1/7/15.
 */
public class GetDeviceRequest extends JsonRequest {

    private String imei;

    public GetDeviceRequest(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public String getMethod() {
        return HttpGet.METHOD_NAME;
    }

    @Override
    public String asHttpString() {
        return String.format("devices/%s", imei);
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getImei() {
        return imei;
    }

    @Override
    public Class<?> getResponseClass() {
        return GetDeviceResponse.class;
    }
}

