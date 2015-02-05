package com.holkan.tracker.service;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by abel.miranda on 2/4/15.
 */
public class SMSSenderService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SMSSenderService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
