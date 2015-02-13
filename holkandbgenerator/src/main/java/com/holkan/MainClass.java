package com.holkan;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class MainClass {

    public static void main(String[] args) {

        Schema schema = new Schema(3, "com.holkan.tracker.data");

        Entity client = schema.addEntity("Tracking");
        client.addIdProperty();
        client.addDoubleProperty("lat");
        client.addDoubleProperty("lng");
        client.addIntProperty("speed");
        client.addByteProperty("event");
        client.addDateProperty("datetime");
        client.addFloatProperty("accuracy");
        client.addStringProperty("provider");
        client.addBooleanProperty("active_gps");
        client.addIntProperty("battery");
        client.addIntProperty("satellites");
        client.addBooleanProperty("active_gprs");

        try {
            DaoGenerator daoGenerator = new DaoGenerator();
            daoGenerator.generateAll(schema, "app/src/main/java/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
