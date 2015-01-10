package com.holkan.tracker.service;

import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

public class JsonRequest extends Request
{

    private JsonParameters map;
    private JSONObject jsonObject;

    public JsonRequest(ExecutorService executorService)
    {
        super(executorService);
    }

    @Override
    public Class<?> getResponseClass()
    {
        return null;
    }

    @Override
    public String asHttpString()
    {
        return null;
    }

    public void setJsonParameters(JsonParameters jsonParameters)
    {
        this.map = jsonParameters;
    }

    public void setJsonParameters(JSONObject jsonObject)
    {
        this.jsonObject = jsonObject;
    }

    @Override
    public String getPayLoad()
    {
        if (map != null && !map.isEmpty())
        {
            JSONObject jsonObject = new JSONObject(map);
            String jsonString = jsonObject.toString();
            return jsonString;
        } else if (jsonObject != null)
        {
            return jsonObject.toString();
        } else
            return null;
    }

    public JsonParameters getMap()
    {
        return map;
    }

    public JSONObject getJsonObject()
    {
        return jsonObject;
    }
}
