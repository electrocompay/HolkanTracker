package com.holkan.tracker;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.holkan.holkantracker.R;


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
