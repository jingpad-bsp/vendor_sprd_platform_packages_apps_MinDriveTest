package com.android.mdt;

import android.content.Context;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

public class LocationTracking {
    private static final String TAG = "LocationTracking";

    Context mContext;
    DataHandlerService mDataHandlerService = null;

    private LocationManager mLocatonManager;
    private LocationTrackingListener mListener;

    private static final int LOCATION_PASSIVE_UPDATE_MINTIME = (10 * 1000);

    public LocationTracking(Context mCon) {
        mContext = mCon;

        mDataHandlerService = ((MDTApplication)mContext).mDataHandlerService;

        mLocatonManager = (LocationManager)mContext.getSystemService(mContext.LOCATION_SERVICE);
        if (mLocatonManager == null) {
            Log.e(TAG, "mLocatonManager is null");

        }

        mListener = new LocationTrackingListener();
        if (mListener == null) {
            Log.e(TAG, "mListener is null");
        }

        ((MDTApplication)mContext).mGpsEnabled = mLocatonManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.d(TAG, "the init value, mGpsEnabled = " + ((MDTApplication)mContext).mGpsEnabled);

        mContext.getContentResolver().registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.LOCATION_PROVIDERS_ALLOWED), false,
            mGpsObserver);
    }

    public void initLocationListener() {
        try {
            if ((mLocatonManager != null) && (mListener != null)) {
                mLocatonManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, LOCATION_PASSIVE_UPDATE_MINTIME, 0, mListener);
            } else {
                Log.e(TAG, "initLocationListener failed");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "initLocationListener Exception: " + e.getMessage());
        }
    }

    public void stopListeners() {
        try {
            if ((mLocatonManager != null) && (mListener != null)) {
                mLocatonManager.removeUpdates(mListener);
            } else {
                Log.e(TAG, "removeUpdates error!");
            }
        } catch (Exception ex) {
            Log.e(TAG, "fail to remove location listener,ex: ", ex);
        }
    }

    private class LocationTrackingListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged ...");

            if (location == null) {
                Log.d(TAG, "location is null");
                return;
            }

            if (mDataHandlerService == null) {
                Log.d(TAG, "onLocationChanged: mDataHandlerService is null");
                return;
            }

            mDataHandlerService.handleScanLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle bundle) {
            Log.d(TAG, "onStatusChanged, provider:" + provider + ", status:" + status);
            if (mDataHandlerService == null) {
                Log.e(TAG, "onStatusChanged: mDataHandlerService is null");
                return;
            }

            if ((status == LocationProvider.OUT_OF_SERVICE) || (status == LocationProvider.TEMPORARILY_UNAVAILABLE)) {
                mDataHandlerService.reportProviderUnavailable();
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled, provider:" + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled, provider:" + provider);
        }
    }

    private final ContentObserver mGpsObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            boolean enabled = mLocatonManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.d(TAG, "gps enabled = " + enabled);

            ((MDTApplication)mContext).mGpsEnabled = enabled;

            if (mDataHandlerService == null) {
                Log.d(TAG, "onChange: mDataHandlerService is null");
                return;
            }

            if (!enabled) {
                mDataHandlerService.reportProviderUnavailable();
            }
        }
    };
}
