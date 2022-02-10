package com.android.mdt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WlanMeasResult {
    private static final String TAG = "WlanMeasResult";
    DataHandlerService mDataHandlerService = null;
    WifiManager mWifiManager;
    Context context;
    WifiScanReceiver wifiScanReceiver;
    IntentFilter intentFilter;

    public WlanMeasResult (Context c) {
        context = c;
        mDataHandlerService = ((MDTApplication)context).mDataHandlerService;
    }

    public void wifiManagerInit() {
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            Log.d(TAG, "error: mWifiManager is null");
            return;
        }
        
        //getWifiScanResults();

        wifiScanReceiver = new WifiScanReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        context.registerReceiver(wifiScanReceiver, intentFilter);   //need to unregisterReceiver!!!!
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ssid;
            String bssid;
            long hessid;
            int rssi;

            if ((WifiManager.SCAN_RESULTS_AVAILABLE_ACTION).equals(intent.getAction())) {
                List<ScanResult> scanResults = mWifiManager.getScanResults();
                List<WifiScanResult> wifiScanResults = new ArrayList<WifiScanResult>();

                Log.d(TAG, "");
                Log.d(TAG, "");
                Log.d(TAG, "onReceive: wifi scan results: ----------------------------");

                if (scanResults == null) {
                    Log.d(TAG, "getWifiScanResults: scan result is null");
                    return;
                }

                for (ScanResult scanResult : scanResults) {
                    WifiScanResult result = new WifiScanResult();
                    final long sysTime = System.currentTimeMillis();

                    SimpleDateFormat sdf0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG, "systime:" + sdf0.format(new Date(sysTime)));

                    result.setSsid(scanResult.SSID);
                    result.setBssid(scanResult.BSSID);
                    result.setHessid(scanResult.hessid);
                    result.setRssi(scanResult.level);
                    result.setTime(sysTime);

                    wifiScanResults.add(result);

                    /* */
                    ssid = scanResult.SSID;
                    bssid = scanResult.BSSID;
                    hessid = scanResult.hessid;
                    rssi = scanResult.level;

                    /*Log.d(TAG, "#$$getWifiScanResults: ssid: " + ssid);
                    Log.d(TAG, "#$$getWifiScanResults: bssid: " + bssid);
                    Log.d(TAG, "#$$getWifiScanResults: hessid: " + hessid);
                    Log.d(TAG, "#$$getWifiScanResults: rssi: " + rssi);*/
                    /**/
                }
                /**/
                WifiScanResult rr = new WifiScanResult();
                rr.setSsid("CMCC1");
                rr.setBssid("11:1a:fa:66:84:00");
                rr.setHessid(0);
                rr.setRssi(-91);
                rr.setTime(System.currentTimeMillis());
                wifiScanResults.add(rr);

                WifiScanResult rr2 = new WifiScanResult();
                rr2.setSsid("CMCC2");
                rr2.setBssid("22:1a:fa:66:84:00");
                rr2.setHessid(0);
                rr2.setRssi(-92);
                rr2.setTime(System.currentTimeMillis());
                wifiScanResults.add(rr2);

                WifiScanResult rr3 = new WifiScanResult();
                rr3.setSsid("CMCC1");
                rr3.setBssid("33:1a:fa:66:84:00");
                rr3.setHessid(0);
                rr3.setRssi(-93);
                rr3.setTime(System.currentTimeMillis());

                wifiScanResults.add(rr3);
                /**/

                if (mDataHandlerService != null) {
                    mDataHandlerService.handleScanWlanInfo(wifiScanResults);
                } else {
                    Log.e(TAG, "error, mDataHandlerService is null");
                }
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                switch (state) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.d(TAG, "onReceive: wifi switch disable");
                        ((MDTApplication)context).mWlanEnabled = false;
                        break;

                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d(TAG, "onReceive: wifi switch enable");
                        ((MDTApplication)context).mWlanEnabled = true;
                        break;
                }

            }
        }
    }

    public class WifiScanResult {
        String ssid;
        String bssid;
        long hessid;
        int rssi;
        long time;

        String getSsid() {
            return ssid;
        }

        String getBssid() {
            return bssid;
        }

        long getHessid() {
            return hessid;
        }

        int getRssi() {
            return rssi;
        }

        long getTime() {
            return time;
        }

        void setSsid(String ssid) {
            this.ssid = ssid;
        }

        void setBssid(String bssid) {
            this.bssid = bssid;
        }

        void setHessid(long hessid) {
            this.hessid = hessid;
        }

        void setRssi(int rssi) {
            this.rssi = rssi;
        }

        void setTime(long time) {
            this.time = time;
        }
    }

    public void getWifiScanResults() {
        String ssid;
        String bssid;
        long hessid;
        int rssi;
        final List<ScanResult> scanResults = mWifiManager.getScanResults();

        if (scanResults == null) {
            Log.d(TAG, "getWifiScanResults: scan result is null############");
            return;
        }

        Log.d(TAG, "getWifiScanResults, size: " + scanResults.size());
        for (ScanResult result : scanResults) {
            ssid = result.SSID;
            bssid = result.BSSID;
            //hessid = result.hessid;
            rssi = result.level;

            Log.d(TAG, "getWifiScanResults: ssid: " + ssid);
            Log.d(TAG, "getWifiScanResults: bssid: " + bssid);
            Log.d(TAG, "getWifiScanResults: rssi: " + rssi);
        }
    }
}
