package com.holkan.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;


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

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);

    }


}
