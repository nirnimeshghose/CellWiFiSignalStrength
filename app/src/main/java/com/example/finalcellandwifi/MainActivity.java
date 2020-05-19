package com.example.finalcellandwifi;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    //find location integrated together
    EditText mEdit;
    Button mButton;

    boolean isWifi;
    int userFreq;
    private Handler mHandler = new Handler();
    private TelephonyManager telephonyManager;
    private final static String LTE_TAG = "LTE_Tag";
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private boolean isContinue = true;
    private boolean isGPS = true;
    private FusedLocationProviderClient mFusedLocationClient;

    WifiManager wifiManager;
    WifiInfo connection;

    FileOutputStream outputStream = null;

    String display;
    String FILE_NAME_WIFI = "mywifi";
    String FILE_NAME_CELL = "mycell";
    String cellId;
    String signalText;
    String strCurrentSpeed;

    double locationLatitude;
    double locationLongitude;
    double speed = 0;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // Listener for the signal strength.
        final PhoneStateListener mListener = new PhoneStateListener() {

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onSignalStrengthsChanged(SignalStrength sStrength) {
                getLTEsignalStrength();
            }


        };

        // Register the listener for the telephony manager
        telephonyManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0); // 10 milliseconds
        locationRequest.setFastestInterval(0); // 10 milliseconds
        locationRequest.setSmallestDisplacement(0); // 1 meter

        new GpsUtils(this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = true;
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        locationLatitude = location.getLatitude();
                        locationLongitude = location.getLongitude();

                        if (!isContinue && mFusedLocationClient != null) {
                            mFusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };
        mFusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper());
    }

    public void startWifi(View v) {
        isWifi = true;
        setContentView(R.layout.activity_second);
        mButton = findViewById(R.id.button1);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mEdit = findViewById(R.id.editText1);
                String value = mEdit.getText().toString();
                userFreq = Integer.parseInt(value);
                FILE_NAME_WIFI = FILE_NAME_WIFI + Double.toString(Math.random()) + ".txt";
                File file = new File(FILE_NAME_WIFI);
                setContentView(R.layout.activity_third);
                try {
                    outputStream = openFileOutput(FILE_NAME_WIFI, MODE_APPEND);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void startCell(View v) {
        isWifi = false;
        setContentView(R.layout.activity_second);
        mButton = findViewById(R.id.button1);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mEdit = findViewById(R.id.editText1);
                String value = mEdit.getText().toString();
                userFreq = Integer.parseInt(value);
                FILE_NAME_CELL = FILE_NAME_CELL + Double.toString(Math.random()) + ".txt";
                File file = new File(FILE_NAME_CELL);
                setContentView(R.layout.activity_third);

                try {
                    outputStream = openFileOutput(FILE_NAME_CELL, MODE_APPEND);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void startRepeating(View v) {
        if (isWifi) {
            mToastRunnableWifi.run();
        } else {
            mToastRunnableCell.run();
        }
    }

    public void stopRepeating(View v) {
        if (isWifi) {
            mHandler.removeCallbacks(mToastRunnableWifi);
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mHandler.removeCallbacks(mToastRunnableCell);
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Runnable mToastRunnableWifi = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void run() {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            connection = wifiManager.getConnectionInfo();

            Calendar calendar = Calendar.getInstance();
            long timeNow = calendar.getTimeInMillis();
            String ts = Long.toString(timeNow);

            display = ts + "  " + connection.getRssi() + " " + connection.getFrequency() + "\n";

            try {
                 outputStream.write(display.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(mToastRunnableWifi, userFreq);

        }
    };

    private Runnable mToastRunnableCell = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void run() {
            getLTEsignalStrength();
            getLocation();

            Calendar calendar = Calendar.getInstance();
            long timeNow = calendar.getTimeInMillis();
            String ts = Long.toString(timeNow);

            display = "" + ts + " " + signalText + " " + cellId + " " + strCurrentSpeed + " " + locationLatitude + " " + locationLongitude + "\n";

            try {
                outputStream.write(display.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(mToastRunnableCell, userFreq);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getLTEsignalStrength() {

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                // Permission is not granted
                // Should we show an explanation?
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},AppConstants.LOCATION_REQUEST);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else
                {

                }
                return;
            }
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
            if (cellInfos != null) {
                for (int i = 0; i < cellInfos.size(); i++) {
                    if (cellInfos.get(i).isRegistered()) {
                        if (cellInfos.get(i) instanceof CellInfoWcdma) {
                            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                            cellId = String.valueOf(cellInfoWcdma.getCellIdentity().getCid());
                            CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                            signalText = String.valueOf(cellSignalStrengthWcdma.getDbm());
                        } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                            CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                            cellId = String.valueOf(cellInfogsm.getCellIdentity().getCid());
                            CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                            signalText = String.valueOf(cellSignalStrengthGsm.getDbm());
                        } else if (cellInfos.get(i) instanceof CellInfoLte) {
                            CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                            cellId = String.valueOf(cellInfoLte.getCellIdentity().getCi());
                            CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                            signalText = String.valueOf(cellSignalStrengthLte.getDbm());
                        } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                            CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                            cellId = String.valueOf(cellInfoCdma.getCellIdentity());
                            CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                            signalText = String.valueOf(cellSignalStrengthCdma.getDbm());
                        }

                    }
                }
            }
        } catch (Exception e) {
            Log.e(LTE_TAG, "Exception: " + e.toString());
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Permission is not granted
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},AppConstants.GPS_REQUEST);

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else
            {

            }
            return;
        }
            mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                if (location != null) {
                    locationLatitude = location.getLatitude();
                    locationLongitude = location.getLongitude();
                    //Log.e(LTE_TAG,locationLatitude+" "+locationLongitude);
                    if (this.lastLocation != null&&location.distanceTo(this.lastLocation)!=0)
                        speed = (location.distanceTo(this.lastLocation)*1000) / (location.getTime() - this.lastLocation.getTime());
                    //Log.e(LTE_TAG,Double.toString(speed));
                    //if there is speed from location
                    if (location.hasSpeed()){
                        //get location speed
                        speed = location.getSpeed();
                        //Log.e(LTE_TAG,Double.toString(speed)+" from Location");
                    }
                    this.lastLocation = location;

                    strCurrentSpeed = Double.toString(speed);
                    strCurrentSpeed = strCurrentSpeed.replace(" ", "0");
                } else {
                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isContinue) {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    } else {
                        mFusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
                            if (location != null) {
                                locationLatitude = location.getLatitude();
                                locationLongitude = location.getLongitude();
                                if (this.lastLocation != null&&location.distanceTo(this.lastLocation)!=0)
                                    speed = (location.distanceTo(this.lastLocation)*1000) / (location.getTime() - this.lastLocation.getTime());
                                //if there is speed from location
                                if (location.hasSpeed()){
                                    //get location speed
                                    speed = location.getSpeed();
                                }

                                this.lastLocation = location;

                                strCurrentSpeed = Double.toString(speed);
                                strCurrentSpeed = strCurrentSpeed.replace(" ", "0");

                            } else {
                                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }
}
