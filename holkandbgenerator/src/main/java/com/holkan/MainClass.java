package com.holkan;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class MainClass {

    public static void main(String[] args) {

        Schema schema = new Schema(1, "com.holkan.tracker.data");

        Entity client = schema.addEntity("Tracking");
        client.addIdProperty();
        client.addDoubleProperty("lat");
        client.addDoubleProperty("lng");
        client.addFloatProperty("speed");
        client.addIntProperty("event");
        client.addDateProperty("datetime");
        client.addFloatProperty("accuracy");

        try {
            DaoGenerator daoGenerator = new DaoGenerator();
            daoGenerator.generateAll(schema, "app/src/main/java/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
