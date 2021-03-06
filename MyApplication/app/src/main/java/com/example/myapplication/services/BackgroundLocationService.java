package com.example.myapplication.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.example.myapplication.gridUtil;

public class BackgroundLocationService extends Service {

    private static final double R = 6371000;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final String TAG = "BACKGROUND SERVICE";
    String lat, lon;

    //for Retrofit
      //   APIInterface apiInterface ;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //registering receiver to tackle doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onReceive(Context context, Intent intent) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

                    if (pm.isDeviceIdleMode()) {
                        // the device is now in doze mode
                        Log.d(TAG, "Doze mode ACTIVE!!!");
                    } else {
                        // the device just woke up from doze mode
                        Intent serviceIntent = new Intent(context, ForegroundNotificationService.class);
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent);
                            return;
                        }
                        context.startService(serviceIntent);
                        Log.d(TAG, "Foreground service started");
                    }
                }
            };
            BackgroundLocationService.this.registerReceiver(receiver, new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED));
        }

        //getting location using fused location listener
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        Log.d(TAG, "Getting location");
        gridUtil.count++;

        if(gridUtil.count == 5) {
            Log.d(TAG, "Send alert to server");
            gridUtil.count = 0;
        }

        return START_STICKY;
    }

    private void getLastLocation(){
        mFusedLocationClient.getLastLocation().addOnCompleteListener(
                new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            
                            double horizontalDist, verticalDist;
                            double a1 = Math.pow(Math.cos(location.getLatitude()), 2) * Math.pow(Math.sin(location.getLongitude()/2), 2);
                            double a2 = Math.pow(Math.sin(location.getLatitude()/2), 2);
                            horizontalDist = R * 2 * Math.atan2(Math.sqrt(a1), Math.sqrt(1-a1));
                            verticalDist = R * 2 * Math.atan2(Math.sqrt(a2), Math.sqrt(1-a2));

                            horizontalDist /= 5;
                            horizontalDist += 2.5;
                            verticalDist /= 5;
                            verticalDist += 2.5;

                            gridUtil.grid[0] = horizontalDist;
                            gridUtil.grid[1] = verticalDist;
                            
                            Toast.makeText(getApplicationContext(), "Got latitude and longitude", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "SERVICE IS RUNNING!!!");
                        }
                    }
                });
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            lat = mLastLocation.getLatitude()+"";
            lon = mLastLocation.getLongitude()+"";
        }
    };

    private void requestNewLocationData(){

        //first time the service is run
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }


    //For removing and sending to the server
   /* void arrCopy() {
        ArrayList<String> arrClone = (ArrayList<String>) arr.clone();
        Collections.shuffle(arrClone);
        String str = arrClone.get(arrClone.size() - 1); //retrivieng the last value for sending to the server
        arrClone.remove(arrClone.size() - 1);

        initRetrofit();

    }
    void initRetrofit()
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("www.abcd.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(APIInterface.class);

        createPosts();
    }

    void createPosts()
    {
        LatLongModel latLongModel = new LatLongModel("ddsd"); //Pass here str from void arrCopy();
        Call<LatLongModel> call = apiInterface.createPost(latLongModel);

        call.enqueue(new Callback<LatLongModel>() {
            @Override
            public void onResponse(Call<LatLongModel> call, Response<LatLongModel> response) {
                if(!response.isSuccessful())
                {
                    Log.d("callEnqueue : ",response.code()+"");
                    Toast.makeText(MainActivity.this,"Code : "+response.code(),Toast.LENGTH_SHORT).show();
                }

                LatLongModel sendResponse = response.body();
            }

            @Override
            public void onFailure(Call<LatLongModel> call, Throwable t) {
                Log.d("call onFailure ","error" , t);
                Toast.makeText(MainActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }*/
}
