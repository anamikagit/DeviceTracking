package com.example.anamika.devicetracking;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RestInterface {

    @GET("tracking")
    Call<List<MLocation>> sendLocation(
            @Query("imei") String imei,
            @Query("lat") String lat,
            @Query("lon") String lon,
            @Query("accuracy") String accuracy,
            @Query("dir") String dir
    );
}
