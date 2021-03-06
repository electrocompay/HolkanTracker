package com.holkan.tracker.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.holkan.tracker.data.Tracking;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table TRACKING.
*/
public class TrackingDao extends AbstractDao<Tracking, Long> {

    public static final String TABLENAME = "TRACKING";

    /**
     * Properties of entity Tracking.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Lat = new Property(1, Double.class, "lat", false, "LAT");
        public final static Property Lng = new Property(2, Double.class, "lng", false, "LNG");
        public final static Property Speed = new Property(3, Integer.class, "speed", false, "SPEED");
        public final static Property Event = new Property(4, Byte.class, "event", false, "EVENT");
        public final static Property Datetime = new Property(5, java.util.Date.class, "datetime", false, "DATETIME");
        public final static Property Accuracy = new Property(6, Float.class, "accuracy", false, "ACCURACY");
        public final static Property Provider = new Property(7, String.class, "provider", false, "PROVIDER");
        public final static Property Active_gps = new Property(8, Boolean.class, "active_gps", false, "ACTIVE_GPS");
        public final static Property Battery = new Property(9, Integer.class, "battery", false, "BATTERY");
        public final static Property Satellites = new Property(10, Integer.class, "satellites", false, "SATELLITES");
        public final static Property Active_gprs = new Property(11, Boolean.class, "active_gprs", false, "ACTIVE_GPRS");
    };


    public TrackingDao(DaoConfig config) {
        super(config);
    }
    
    public TrackingDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'TRACKING' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'LAT' REAL," + // 1: lat
                "'LNG' REAL," + // 2: lng
                "'SPEED' INTEGER," + // 3: speed
                "'EVENT' INTEGER," + // 4: event
                "'DATETIME' INTEGER," + // 5: datetime
                "'ACCURACY' REAL," + // 6: accuracy
                "'PROVIDER' TEXT," + // 7: provider
                "'ACTIVE_GPS' INTEGER," + // 8: active_gps
                "'BATTERY' INTEGER," + // 9: battery
                "'SATELLITES' INTEGER," + // 10: satellites
                "'ACTIVE_GPRS' INTEGER);"); // 11: active_gprs
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'TRACKING'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Tracking entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Double lat = entity.getLat();
        if (lat != null) {
            stmt.bindDouble(2, lat);
        }
 
        Double lng = entity.getLng();
        if (lng != null) {
            stmt.bindDouble(3, lng);
        }
 
        Integer speed = entity.getSpeed();
        if (speed != null) {
            stmt.bindLong(4, speed);
        }
 
        Byte event = entity.getEvent();
        if (event != null) {
            stmt.bindLong(5, event);
        }
 
        java.util.Date datetime = entity.getDatetime();
        if (datetime != null) {
            stmt.bindLong(6, datetime.getTime());
        }
 
        Float accuracy = entity.getAccuracy();
        if (accuracy != null) {
            stmt.bindDouble(7, accuracy);
        }
 
        String provider = entity.getProvider();
        if (provider != null) {
            stmt.bindString(8, provider);
        }
 
        Boolean active_gps = entity.getActive_gps();
        if (active_gps != null) {
            stmt.bindLong(9, active_gps ? 1l: 0l);
        }
 
        Integer battery = entity.getBattery();
        if (battery != null) {
            stmt.bindLong(10, battery);
        }
 
        Integer satellites = entity.getSatellites();
        if (satellites != null) {
            stmt.bindLong(11, satellites);
        }
 
        Boolean active_gprs = entity.getActive_gprs();
        if (active_gprs != null) {
            stmt.bindLong(12, active_gprs ? 1l: 0l);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Tracking readEntity(Cursor cursor, int offset) {
        Tracking entity = new Tracking( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getDouble(offset + 1), // lat
            cursor.isNull(offset + 2) ? null : cursor.getDouble(offset + 2), // lng
            cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3), // speed
            cursor.isNull(offset + 4) ? null : (byte) cursor.getShort(offset + 4), // event
            cursor.isNull(offset + 5) ? null : new java.util.Date(cursor.getLong(offset + 5)), // datetime
            cursor.isNull(offset + 6) ? null : cursor.getFloat(offset + 6), // accuracy
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7), // provider
            cursor.isNull(offset + 8) ? null : cursor.getShort(offset + 8) != 0, // active_gps
            cursor.isNull(offset + 9) ? null : cursor.getInt(offset + 9), // battery
            cursor.isNull(offset + 10) ? null : cursor.getInt(offset + 10), // satellites
            cursor.isNull(offset + 11) ? null : cursor.getShort(offset + 11) != 0 // active_gprs
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Tracking entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setLat(cursor.isNull(offset + 1) ? null : cursor.getDouble(offset + 1));
        entity.setLng(cursor.isNull(offset + 2) ? null : cursor.getDouble(offset + 2));
        entity.setSpeed(cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3));
        entity.setEvent(cursor.isNull(offset + 4) ? null : (byte) cursor.getShort(offset + 4));
        entity.setDatetime(cursor.isNull(offset + 5) ? null : new java.util.Date(cursor.getLong(offset + 5)));
        entity.setAccuracy(cursor.isNull(offset + 6) ? null : cursor.getFloat(offset + 6));
        entity.setProvider(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
        entity.setActive_gps(cursor.isNull(offset + 8) ? null : cursor.getShort(offset + 8) != 0);
        entity.setBattery(cursor.isNull(offset + 9) ? null : cursor.getInt(offset + 9));
        entity.setSatellites(cursor.isNull(offset + 10) ? null : cursor.getInt(offset + 10));
        entity.setActive_gprs(cursor.isNull(offset + 11) ? null : cursor.getShort(offset + 11) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Tracking entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(Tracking entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
