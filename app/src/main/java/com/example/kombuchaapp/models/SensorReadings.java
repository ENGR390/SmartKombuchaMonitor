package com.example.kombuchaapp.models;

public class SensorReadings {
    private String recipe_id;
    private String sensor_id;
    private float temperature_c;
    private float temperature_f;
    private String timestamp;
    private String user_id;

    public SensorReadings() {}

    public SensorReadings(String recipe_id, String sensor_id, float temperature_c, float temperature_f, String timestamp) {
        this.recipe_id = recipe_id;
        this.sensor_id = sensor_id;
        this.temperature_c = temperature_c;
        this.temperature_f = temperature_f;
        this.timestamp = timestamp;
    }

    public SensorReadings(String recipe_id, String sensor_id, float temperature_c, float temperature_f, String timestamp, String user_id) {
        this.recipe_id = recipe_id;
        this.sensor_id = sensor_id;
        this.temperature_c = temperature_c;
        this.temperature_f = temperature_f;
        this.timestamp = timestamp;
        this.user_id = user_id;
    }

    public String getRecipe_id() {
        return recipe_id;
    }

    public void setRecipe_id(String recipe_id) {
        this.recipe_id = recipe_id;
    }

    public String getSensor_id() {
        return sensor_id;
    }

    public void setSensor_id(String sensor_id) {
        this.sensor_id = sensor_id;
    }

    public float getTemperature_c() {
        return temperature_c;
    }

    public void setTemperature_c(float temperature_c) {
        this.temperature_c = temperature_c;
    }

    public float getTemperature_f() {
        return temperature_f;
    }

    public void setTemperature_f(float temperature_f) {
        this.temperature_f = temperature_f;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return String.format("Sensor: %s\nTemp: %.1f°C / %.1f°F\nRecipe: %s\nTimestamp: %s\nUser ID: %s", sensor_id, temperature_c, temperature_f, recipe_id, timestamp, user_id);
    }
}
