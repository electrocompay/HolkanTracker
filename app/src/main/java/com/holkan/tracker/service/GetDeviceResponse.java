package com.holkan.tracker.service;

import com.google.gson.Gson;
import com.holkan.tracker.model.Device;

import org.json.JSONObject;

/**
 * Created by abel.miranda on 1/7/15.
 */
public class GetDeviceResponse extends JsonResponse {

    private Device device;

    @Override
    public void processResponse(JSONObject jsonObject, boolean isSucess) {

        if (isSucess){
            Gson gson = new Gson();
            device = gson.fromJson(jsonObject.toString(), Device.class);
        }

    }

    public Device getDevice() {
        return device;
    }

}
