package com.holkan.tracker.service;

import android.os.Bundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;

public abstract class JsonResponse extends Response {

    private static class BadDoubleDeserializer implements JsonDeserializer<Double> {

        @Override
        public Double deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
            try {
                return Double.parseDouble(element.getAsString().replace(",", ""));
            } catch (NumberFormatException e) {
                throw new JsonParseException(e);
            }
        }

    }

    JSONObject jsonObject;
    private Bundle bundle;
    public static String CUSTOM_ERROR_KEY = "customerror";

    @Override
    public void init(String content, HttpResponse httpResponse) {
        super.init(content, httpResponse);

        if (content != null) {
            try {
                jsonObject = new JSONObject(content);

                boolean isSucess = !jsonObject.has("ErrorCode");

                if (jsonObject.has("ErrorCode")) {
                    int errorCode = jsonObject.getInt("ErrorCode");
                    setResultCode(errorCode);
                    processError(errorCode, jsonObject);
                }
                processResponse(jsonObject, isSucess);

            } catch (JSONException e) {
                setResultCode(99);
                e.printStackTrace();
                processResponse(jsonObject, false);
            }
        }

    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public abstract void processResponse(JSONObject jsonObject, boolean isSucess);

    private void processError(int errorCode, JSONObject jsonObject) {
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Gson getGson() {
        return new GsonBuilder().registerTypeAdapter(Double.class, new BadDoubleDeserializer()).create();
    }

}
