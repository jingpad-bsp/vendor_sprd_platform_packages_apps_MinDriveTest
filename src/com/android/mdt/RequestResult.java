package com.android.mdt;

import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;


public class RequestResult {
    static AtomicInteger sNextSerial = new AtomicInteger(0);
    private static final String TAG = "DataHandler";
    int mSerial;
    String cmd;
    String response;

    public static RequestResult obtain(String cmd) {
        if (cmd == null) {
            return null;
        }
        RequestResult rr = new RequestResult();
        rr.mSerial = sNextSerial.getAndIncrement();
        rr.cmd = cmd;

        Log.d(TAG, "RequestResult, obtain: serial: " + rr.mSerial);
        return rr;
    }

}