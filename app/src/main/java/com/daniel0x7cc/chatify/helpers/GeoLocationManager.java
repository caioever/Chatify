package com.daniel0x7cc.chatify.helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.utils.LogUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class GeoLocationManager implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static GeoLocationManager instance;
    private Location lastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private static final List<LocationListener> locListeners = new ArrayList<>();

    private GeoLocationManager(){
        mGoogleApiClient = new GoogleApiClient.Builder(App.getInstance())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3 * 60 * 1000); // 3 min
        mLocationRequest.setSmallestDisplacement(20); // 20 meters
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationManager = (LocationManager) App.getInstance().getSystemService(Context.LOCATION_SERVICE);
    }

    public static synchronized GeoLocationManager getInstance(){
        if(instance == null){
            instance = new GeoLocationManager();
        }
        return instance;
    }

    public void connect(){
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

        if (lastLocation == null) {
            try {

                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation == null) {
                    lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

            } catch (SecurityException e) {
                LogUtils.e("Erro ao acessar geolocalização.", e);
            }
        }
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            setLocation(location);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            LogUtils.e("Erro ao acessar geolocalização.", e);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void setLocation(Location location) {
        if (location != null && (lastLocation == null || lastLocation.distanceTo(location) != 0)) {
            this.lastLocation = location;
            notifyOnLocationChanged(lastLocation);
        }
    }

    private static void notifyOnLocationChanged(final Location location) {
        if (!locListeners.isEmpty()) {
            for (LocationListener listener : locListeners) {
                listener.onLocationChanged(location);
            }
        }
    }

    public double getLongitude(){
        if(lastLocation == null) {
            return 0;
        }
        return lastLocation.getLongitude();
    }

    public double getLatitude(){
        if (lastLocation == null) {
            return 0;
        }
        return lastLocation.getLatitude();
    }

    @Override
    public void onLocationChanged(Location location) {
        setLocation(location);
    }

    private boolean isNetworkEnabled() {
        boolean networkEnabled;
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            networkEnabled = false;
        }
        return networkEnabled;
    }

    private boolean isGpsEnabled() {
        boolean gpsEnabled;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            gpsEnabled = false;
        }
        return gpsEnabled;
    }

    public boolean isGeoLocationEnabled() {
        return (isGpsEnabled() || isNetworkEnabled());
    }

    public boolean hasGeoLocationPermissions() {
        return (ActivityCompat.checkSelfPermission(App.getInstance(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(App.getInstance(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }
}

