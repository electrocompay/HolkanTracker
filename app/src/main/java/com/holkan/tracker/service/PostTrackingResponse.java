package com.holkan.tracker.service;

import com.google.gson.Gson;
import com.holkan.tracker.data.Tracking;

import org.json.JSONObject;

/**
 * Created by abel.miranda on 1/9/15.
 */
public class PostTrackingResponse extends JsonResponse {

    Tracking tracking;

    @Override
    public void processResponse(JSONObject jsonObject, boolean isSucess) {
        tracking = new Gson().fromJson(jsonObject.toString(), Tracking.class);
    }

    public Tracking getTracking() {
        return tracking;
    }
}
