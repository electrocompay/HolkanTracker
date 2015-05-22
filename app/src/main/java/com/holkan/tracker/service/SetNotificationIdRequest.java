package com.holkan.tracker.service;

import org.apache.http.client.methods.HttpPost;

import android.content.Context;

import java.util.concurrent.ExecutorService;

/**
 * Created by abel.miranda on 2/3/15.
 */
public class SetNotificationIdRequest extends JsonRequest {

    public SetNotificationIdRequest(ExecutorService executorService, Context context) {
        super(executorService, context);
    }

    @Override
    public String getMethod() {
        return HttpPost.METHOD_NAME;
    }

    @Override
    public String asHttpString() {
        return "devices/setnotificationid";
    }

    @Override
    public Class<?> getResponseClass() {
        return SetNotificationResponse.class;
    }

}
