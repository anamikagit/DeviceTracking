package com.example.anamika.devicetracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fused extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DRIVER";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 30000;
    private static final float LOCATION_DISTANCE = 0;
    private double currentLat, currentLng,currentSpeed;
    public String currentDir = "se", deviceNum ;
    private int currentAcc;
    private SharedPreferences pref;
    private String driverId;
    private String currentDateTime;
    private GoogleApiClient mGoogleApiClient;
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    private LocationListener locationListener;


    Timer timer;
    LocationManager lm;
    boolean gps_enabled = false;
    boolean network_enabled = false;

    RestInterface apiService = RestClient.getClient().create(RestInterface.class);
    private class LocationListener implements
            com.google.android.gms.location.LocationListener {
        public LocationListener() {
        }

        private Context mContext;
        private int mProgressStatus = 0;
        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                float percentage = level / (float) scale;
                mProgressStatus = (int) ((percentage) * 100);
                Toast.makeText(Fused.this, "batt :" + mProgressStatus + "%", Toast.LENGTH_LONG).show();
            }
        };

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            currentLat = location.getLatitude();
            currentLng = location.getLongitude();
            currentAcc = (int) location.getAccuracy();
            //currentDir = location.getBearingAccuracyDegrees();
            //currentSpeed = location.getSpeed();
            // currentDateTime = com.example.aarya.fieldofficersurveilance.model.Util.getDateTime();
            mContext = getApplicationContext();
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, iFilter);
            // getCompleteAddressString(double LATITUDE, double LONGITUDE);
            String deviceNum;
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(Fused.this, Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;

            }
            deviceNum = telephonyManager.getDeviceId();
            Toast.makeText(Fused.this,"loc:" +currentLat + "/ " + currentLng +
                    " /" + currentAcc + "/ " +deviceNum,Toast.LENGTH_LONG).show();

           // putInfoToDb(currentDir, currentLat, currentLng, currentAcc , deviceNum);

        }
    }

    private void putInfoToDb(String currentDir, double currentLat, double currentLng, float currentAcc, String deviceNum) {
        SQLiteDatabase db = LocationDBHelper.getInstance(Fused.this).getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(LocationDBHelper.LocationEntry.COLUMN_NAME_LATITUDE, currentLat);
        values.put(LocationDBHelper.LocationEntry.COLUMN_NAME_LOGITUDE, currentLng);
        values.put(LocationDBHelper.LocationEntry.COLUMN_NAME_ACCURACY, currentAcc);
        values.put(LocationDBHelper.LocationEntry.COLUMN_NAME_DIRECTION, currentDir);
        values.put(LocationDBHelper.LocationEntry.COLUMN_NAME_IMEI, deviceNum);

        long newRowId = db.insert(LocationDBHelper.LocationEntry.TABLE_NAME, null, values);
        db.close();
    }

    public List<MLocation> getAllLocation() {

        SQLiteDatabase db = LocationDBHelper.getInstance(Fused.this).getWritableDatabase();
        List<MLocation> mLocationsList = new ArrayList<MLocation>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + LocationDBHelper.LocationEntry.TABLE_NAME;
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                MLocation mLocation = new MLocation();
                mLocation.setId(Integer.parseInt(cursor.getString(0)));
                mLocation.setLat(cursor.getString(1));
                mLocation.setLon(cursor.getString(2));
                mLocation.setAccuracy(cursor.getString(3));
                mLocation.setDir(cursor.getString(4));
                mLocation.setImei(cursor.getInt(5));
                // Adding contact to list
                mLocationsList.add(mLocation);
            } while (cursor.moveToNext());
        }

        db.close();
        // return contact list
        return mLocationsList;
    }
    public void deleteLocation(MLocation location) {

        SQLiteDatabase db = LocationDBHelper.getInstance(Fused.this).getWritableDatabase();
        int isDeleted = db.delete(LocationDBHelper.LocationEntry.TABLE_NAME, LocationDBHelper.LocationEntry._ID + " = ?",
                new String[] { String.valueOf(location.getId()) });
        db.close();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        boolean stopService = false;
        if (intent != null)
            stopService = intent.getBooleanExtra("stopservice", false);

        System.out.println("stopservice " + stopService);

        locationListener = new LocationListener();
        if (stopService)
            stopLocationUpdates();
        else {
            if (!mGoogleApiClient.isConnected())
                mGoogleApiClient.connect();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        pref = getSharedPreferences("driver_app", MODE_PRIVATE);
        driverId = pref.getString("driver_id", "");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gps_enabled || network_enabled) {
            Context context = getApplicationContext();

            Observable.interval(20, 20, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Long>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@NonNull Long aLong) {
                            Toast.makeText(Fused.this, "This happnes every mint :)", Toast.LENGTH_SHORT).show();
                            Log.e("anu", "This happnes every mint :)");
                            putInfoToDb(currentDir, currentLat, currentLng, currentAcc , deviceNum);
                            sendAllLocationToServer();
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });


        }
        else {
            Toast.makeText(Fused.this,"check your gps and internet",Toast.LENGTH_LONG).show();
            Log.e("anu", "check your gps and internet");
        }
    }

    private void sendAllLocationToServer() {
//		http://111.118.178.163/amrs_igl_api/webservice.asmx/tracking?imei=32432423&lat=23.2343196868896&lon=76.2342300415039&accuracy=98.34&dir=we

        List<MLocation> locations = getAllLocation();
        if (locations != null && locations.size() >= 1) {
            for (int i = 0; i < locations.size(); i++) {

                final MLocation mLocation = locations.get(i);
                Call<List<MLocation>> call = apiService.sendLocation("488787875456", currentLng+"",
                        currentLat+"",currentAcc+"","se");
                call.enqueue(new Callback<List<MLocation>>() {
                    @Override
                    public void onResponse(Call<List<MLocation>> call, Response<List<MLocation>> response) {
//                        if(response != null && response.body().size()>0){
//							if(response.body().get(0).response.equals("success"))
//							{
                            deleteLocation(mLocation);
//							}
//						}
                    }

                    @Override
                    public void onFailure(Call<List<MLocation>> call, Throwable t) {

                    }
                });

            }
        }
    }



    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, locationListener);

        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }
    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        // TODO Auto-generated method stub
    }
    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(35000);
        mLocationRequest.setFastestInterval(30000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startLocationUpdates();
    }
    private void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, locationListener);
    }
    @Override
    public void onConnectionSuspended(int arg0) {
        // TODO Auto-generated method stub
    }
}