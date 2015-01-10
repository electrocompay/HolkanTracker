package com.holkan.tracker.service;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.util.concurrent.ExecutorService;

/**
 * Created by abel.miranda on 1/7/15.
 */
public class PostTrackingRequest extends JsonRequest {


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

    @Override
    public Class<?> getResponseClass() {
        return PostTrackingResponse.class;
    }
}

