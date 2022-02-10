package com.android.mdt;

import android.hardware.radio.V1_0.RadioIndicationType;
import android.util.Log;
import vendor.sprd.hardware.radio.lite.V1_0.ILiteRadioIndication;

public class LiteRadioIndication extends ILiteRadioIndication.Stub {
    private static final String TAG = "DataHandlerService";
    DataHandlerService dataHandlerService;

    public LiteRadioIndication(DataHandlerService s) {
        dataHandlerService = s;
    }

    @Override
    public void sendCmdInd(int type, String data) {
        Log.d(TAG, "sendCmdInd: type:" + type + ",data:" + data);

        if (type == RadioIndicationType.UNSOLICITED) {
            dataHandlerService.parseRecvInfo(data);
        }
    }
}