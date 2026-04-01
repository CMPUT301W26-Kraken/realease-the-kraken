package com.example.releasethekraken.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

/**
 * Utility class for retrieving the device's last known location.
 * Used to capture where an entrant joined a waiting list from.
 *
 * NOTE: This file was done with the assistance of Claude (Anthropic) AI.
 */
public class LocationHelper {

    /**
     * Callback interface for location retrieval results.
     */
    public interface LocationCallback {

        /**
         * Called when location is successfully retrieved.
         *
         * @param latitude  the latitude coordinate of the device
         * @param longitude the longitude coordinate of the device
         */
        void onLocation(double latitude, double longitude);

        /**
         * Called when location could not be retrieved.
         *
         * @param reason a message describing why location was unavailable
         */
        void onError(String reason);
    }

    /**
     * Retrieves the device's last known location.
     * Requires ACCESS_FINE_LOCATION permission to be granted before calling.
     *
     * @param context  the application context
     * @param callback callback returning coordinates or an error reason
     */
    public static void getLastLocation(Context context, LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Location permission not granted");
            return;
        }

        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(context);

        client.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        callback.onError("Location unavailable");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}