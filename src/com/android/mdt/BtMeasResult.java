package com.android.mdt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BtMeasResult {
    private static final String TAG = "BtMeasResult";
    Context context;
    BtReceiver btReceiver;
    IntentFilter intentFilter;
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;
    boolean btSwitchState = false;
    
    public BtMeasResult(Context c) {
        context = c;
        btReceiver = new BtReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        context.registerReceiver(btReceiver, intentFilter);          //need to unregisterReceiver!!!!

        mBluetoothAdapter = ((BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        Log.d(TAG, "@@BtMeasResult: registerReceiver ok!!");

        //startScanning();
        Log.d(TAG, "!!BtMeasResult: startscanning ...");
    }

    private class BtReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String rssi = intent.getStringExtra(BluetoothDevice.EXTRA_RSSI);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = device.getAddress();

                Log.d(TAG, "@@@@ onReceive: rssi:" + rssi);
                Log.d(TAG, "@@@@ onReceive: name: " + name);
                Log.d(TAG, "@@@@ onReceive: address: " + address);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: bt switch off");
                        ((MDTApplication)context).mBtEnabled = false;
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: bt switch on");
                        ((MDTApplication)context).mBtEnabled = true;
                        break;
                }
            }
        }
    }

    public void startScanning() {
        mScanCallback = new BtScanCallback();
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
        } else {
            Log.e(TAG, "error: startScanning: mBluetoothLeScanner is null");
        }
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();

        //builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

        return builder.build();
    }

    private class BtScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult r : results) {
                Log.d(TAG, "###onScanResult: rssi:" + r.getRssi());
                Log.d(TAG, "###onScanResult: address:" + r.getDevice().getAddress());
                Log.d(TAG, "###onScanResult: name:" + r.getDevice().getName());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult: rssi:" + result.getRssi());
            Log.d(TAG, "onScanResult: address:" + result.getDevice().getAddress());
            Log.d(TAG, "onScanResult: name:" + result.getDevice().getName());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: errorCode: " + errorCode);
        }
    }
}