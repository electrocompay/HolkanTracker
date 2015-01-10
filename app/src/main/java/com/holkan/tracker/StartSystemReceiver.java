package com.holkan.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class StartSystemReceiver extends BroadcastReceiver {

    public StartSystemReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, LocationService.class);
        context.startService(i);
    }
}
