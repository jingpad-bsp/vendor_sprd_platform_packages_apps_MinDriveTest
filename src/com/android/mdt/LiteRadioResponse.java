package com.android.mdt;

import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioResponseType;
import android.util.Log;
import vendor.sprd.hardware.radio.lite.V1_0.ILiteRadioResponse;
import vendor.sprd.hardware.radio.V1_0.ExtRadioResponseInfo;

public class LiteRadioResponse extends ILiteRadioResponse.Stub {
    private static final String TAG = "DataHandlerService";
    DataHandlerService dataHandlerService;

    public LiteRadioResponse(DataHandlerService s) {
        dataHandlerService = s;
    }

    /* when send a at, DivRil will call this interface to return a response(default is "OK") */
    @Override
    public void sendCmdResponse(ExtRadioResponseInfo info, String response) {
        Log.d(TAG, "sendCmdResponse: type:" + info.type +  ", serial:" + info.serial +
                ", response:" + response);

        if (info.type == RadioResponseType.SOLICITED) {
            dataHandlerService.findAndRemoveResultFromList(info.serial, response);
        }
    }
}

