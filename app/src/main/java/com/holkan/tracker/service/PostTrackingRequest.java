package com.holkan.tracker.service;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.ExecutorService;

/**
 * Created by abel.miranda on 1/7/15.
 */
public class PostTrackingRequest extends JsonRequest {


    private long trackingId;

    public PostTrackingRequest(ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public String getMethod() {
        return HttpPost.METHOD_NAME;
    }

    @Override
    public String asHttpString() {
        return "tracking";
    }

    public void setTrackingId(long trackingId) {
        this.trackingId = trackingId;
    }

    public long getTrackingId() {
        return trackingId;
    }

    @Override
    public Class<?> getResponseClass() {
        return PostTrackingResponse.class;
    }
}

