package com.example.anamika.devicetracking;

public class MLocation {

    private int imei;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private int id;
    private String lat;
    private String lon;
    private String accuracy;
    private String dir;
    private String timestamp;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public MLocation(int imei, String lat, String lon, String accuracy, String dir , int id) {
        this.imei = imei;
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
        this.dir = dir;
        this.timestamp = timestamp;
    }

    public MLocation() {
    }

    public int getImei() {
        return imei;
    }

    public void setImei(int imei) {
        this.imei = imei;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}