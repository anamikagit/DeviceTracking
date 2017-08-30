package com.example.anamika.devicetracking;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fused extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DRIVER";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 30000;
    private static final float LOCATION_DISTANCE = 0;
    private double currentLat, currentLng, currentSpeed;
    private float currentAcc;
    private SharedPreferences pref;
    private String driverId;
    private String currentDateTime;
    private GoogleApiClient mGoogleApiClient;
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    private LocationListener locationListener;

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
            currentAcc = location.getAccuracy();
            currentSpeed = location.getSpeed();
            // currentDateTime = com.example.aarya.fieldofficersurveilance.model.Util.getDateTime();
            mContext = getApplicationContext();
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver, iFilter);
            // getCompleteAddressString(double LATITUDE, double LONGITUDE);
            String deviceNum;
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(Fused.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
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
                    " /" + currentAcc + "/ " + currentSpeed +"/"+currentDateTime +"/"+deviceNum,Toast.LENGTH_LONG).show();


            Call<List<MLocation>> call = apiService.sendLocation(deviceNum, currentLng+"",currentLat+"",
                    currentAcc+"","");

            call.enqueue(new Callback<List<MLocation>>() {

                @Override
                public void onResponse(Call<List<MLocation>> call, Response<List<MLocation>> response) {

                }

                @Override
                public void onFailure(Call<List<MLocation>> call, Throwable t) {

                }
            });
        }
    }
   /* private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.e("My Current address", "" + strReturnedAddress.toString());
            } else {
                Log.e("My Current address", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("My Current address", "Can not get Address!");
        }
        return strAdd;
    }*/

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