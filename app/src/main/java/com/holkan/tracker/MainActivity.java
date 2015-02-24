package com.holkan.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TabWidget;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private FragmentTabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTabHost = new FragmentTabHost(this);
        LayoutInflater.from(this).inflate(R.layout.activity_main, mTabHost);

        setContentView(mTabHost);

        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        mTabHost.addTab(mTabHost.newTabSpec("alert").setIndicator(getString(R.string.alert)),
                MainFragment.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("tools").setIndicator(getString(R.string.tools)),
                SettingsFragment.class, null);

        customizeTabHost();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);

    }

    private void customizeTabHost() {
        TabWidget widget = mTabHost.getTabWidget();
        for(int i = 0; i < widget.getChildCount(); i++) {
            View v = widget.getChildAt(i);

            // Look for the title view to ensure this is an indicator and not a divider.
            TextView tv = (TextView)v.findViewById(android.R.id.title);
            if(tv == null) {
                continue;
            }
            v.setBackgroundResource(R.drawable.tab_indicator);
        }

    }


}
