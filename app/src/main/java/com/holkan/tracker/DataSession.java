package com.holkan.tracker;

import android.content.Context;

import com.holkan.tracker.data.DaoMaster;
import com.holkan.tracker.data.DaoSession;

/**
 * Created by abel.miranda on 12/7/14.
 */
public class DataSession {

    private static DaoSession session;

    public static DaoSession getSession(Context context) {

        if (session == null) {
            DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(context, "HolkanDB", null);
            DaoMaster daoMaster = new DaoMaster(openHelper.getWritableDatabase());
            session = daoMaster.newSession();
        }

        return session;
    }
}
