package com.example.anamika.devicetracking;

public class MLocation {

    private int imei;
    private String lat;
    private String lon;
    private String accuracy;
    private String dir;

    public MLocation(int imei, String lat, String lon, String accuracy, String dir) {
        this.imei = imei;
        this.lat = lat;
        this.lon = lon;
        this.accuracy = accuracy;
        this.dir = dir;
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