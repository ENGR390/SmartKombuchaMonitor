package com.example.kombuchaapp.models;

public class SensorReadings {
    private String sensor_id;
    private float temperature_c;
    private float temperature_f;

    public SensorReadings() {}

    public SensorReadings(String sensor_id, float temperature_c, float temperature_f) {
        this.sensor_id = sensor_id;
        this.temperature_c = temperature_c;
        this.temperature_f = temperature_f;
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

    @Override
    public String toString() {
        return String.format("Sensor: %s\nTemp: %.1f°C / %.1f°F", sensor_id, temperature_c, temperature_f);
    }
}
