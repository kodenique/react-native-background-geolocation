package com.marianhello.bgloc.provider;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.marianhello.bgloc.Config;

/**
 * Raw location provider using FusedLocationProviderClient.
 * Replaces legacy LocationManager which is throttled in background on Android 8+.
 */
public class RawLocationProvider extends AbstractLocationProvider {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isStarted = false;

    public RawLocationProvider(Context context) {
        super(context);
        PROVIDER_ID = Config.RAW_PROVIDER;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    logger.debug("Location change: {}", location.toString());
                    showDebugToast("acy:" + location.getAccuracy() + ",v:" + location.getSpeed());
                    handleLocation(location);
                }
            }
        };
    }

    @Override
    public void onStart() {
        if (isStarted) {
            return;
        }

        int priority = translateDesiredAccuracy(mConfig.getDesiredAccuracy());

        LocationRequest locationRequest = new LocationRequest.Builder(priority, mConfig.getInterval())
                .setMinUpdateIntervalMillis(mConfig.getFastestInterval())
                .setMinUpdateDistanceMeters(mConfig.getDistanceFilter())
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isStarted = true;
        } catch (SecurityException e) {
            logger.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }
    }

    @Override
    public void onStop() {
        if (!isStarted) {
            return;
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isStarted = false;
    }

    @Override
    public void onConfigure(Config config) {
        super.onConfigure(config);
        if (isStarted) {
            onStop();
            onStart();
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * Translates desired accuracy to FusedLocationProvider priority.
     * 0:  most aggressive, most accurate, worst battery drain
     * 1000:  least aggressive, least accurate, best for battery.
     */
    private int translateDesiredAccuracy(Integer accuracy) {
        if (accuracy >= 10000) {
            return Priority.PRIORITY_PASSIVE;
        }
        if (accuracy >= 1000) {
            return Priority.PRIORITY_LOW_POWER;
        }
        if (accuracy >= 100) {
            return Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        }
        return Priority.PRIORITY_HIGH_ACCURACY;
    }

    @Override
    public void onDestroy() {
        logger.debug("Destroying RawLocationProvider");
        this.onStop();
        super.onDestroy();
    }
}
