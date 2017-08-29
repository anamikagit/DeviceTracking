package com.example.anamika.devicetracking.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Util{

    private static String IMEI ="";

    public static String getIMEI() {
        return IMEI;
    }

    public static void setIMEI(String IMEI) {
        Util.IMEI = IMEI;
    }

    public static String  getDateTime(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(new Date());
    }
    public static String getCurrentDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateTime = sdf.format(new Date());
        return currentDateTime;
    }
}