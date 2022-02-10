/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mdt;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MDTApplication extends Application implements ServiceConnection {
    private static final String TAG = "MDTApplication";
    private Context mContext;
    private Utils.ServiceToken mToken = null;
    DataHandlerService mDataHandlerService;

    LocationTracking locationTrack;
    public boolean mGpsEnabled = false;
    public boolean mWlanEnabled = false;
    public boolean mBtEnabled = false;

    BtMeasResult btMeasResult;
    WlanMeasResult wlanMeasResult;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        Log.d(TAG, "onCreate");

        mToken = Utils.bindToService(mContext, this);
        if (mToken == null) {
            Log.e(TAG, "onCreate: mToken is null");
        }

        /*locationTrack = new LocationTracking(mContext);
        locationTrack.initLocationListener();

        btMeasResult = new BtMeasResult(mContext);
        wlanMeasResult = new WlanMeasResult(mContext);
        wlanMeasResult.wifiManagerInit();*/
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mDataHandlerService = ((DataHandlerService.ServiceBinder)iBinder).getService();
        if (mDataHandlerService == null) {
            Log.e(TAG, "onServiceConnected: error: mDataHandlerService is null");
        }
        Log.d(TAG, "onServiceConnected: ok");

        locationTrack = new LocationTracking(mContext);
        locationTrack.initLocationListener();

        btMeasResult = new BtMeasResult(mContext);
        wlanMeasResult = new WlanMeasResult(mContext);
        wlanMeasResult.wifiManagerInit();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mDataHandlerService = null;
        Log.d(TAG, "onServiceDisconnected!");
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "onTerminate");
        super.onTerminate();
    }
}
