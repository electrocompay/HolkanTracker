package com.holkan.tracker.model;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by abel.miranda on 1/8/15.
 */
public class Device {

    private String imei;
    private Context context;
    private ArrayList<PlanInterval> monitor_plan;
    private String notification_id;

    public String getImei() {
        return imei;
    }

    public ArrayList<PlanInterval> getMonitor_plan() {
        return monitor_plan;
    }
}
