package com.android.mdt;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.android.mdt.WlanMeasResult.WifiScanResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import vendor.sprd.hardware.radio.lite.V1_0.ILiteRadio;

public class DataHandlerService extends Service {
    private static final String TAG = "DataHandlerService";

    HandlerThread mSenderThread;
    SendHandler mSendHandler;

    byte[] Buffer;
    long locTime;

    static final int EVENT_PERIOD_START = 1;
    static final int EVENT_PERIOD_STOP = 2;
    static final int EVENT_SEND_ONCE = 3;
    static final int EVENT_SEND = 4;
    static final int EVENT_AT_REQUEST = 5;

    static final int ELLIPSOID_POINT = 0;
    static final int ELLIPSOID_POINT_ALTITUDE = 1;

    static final String PERIOD_MDT_START = "1";
    static final String PERIOD_MDT_STOP = "2";
    static final String ONCE_MDT_INFO = "3";

    private static final String CONFIG_MDT_AT = "AT+SPMDTCONFIG=";
    private static final String AGPS_MDT_AT = "AT+SPAGPSMDTR=";
    private static final String UNSOL_AGPS_AT = "SPAGPSMDT";

    /* defined in divril.cpp */
    private static final String HIDL_SERVICE_NAME = "divservice1";

    static final int NORTH_LATITUDE = 0;
    static final int SOUTH_LATITUDE = 1;

    private boolean periodRequestFlag = false;
    private static final int LOCATION_MAX_DIFF_VALUE = (10 * 1000);

    ILiteRadio mILiteRadioProxy;
    LiteRadioResponse mLiteRadioResponse;
    LiteRadioIndication mLiteRadioIndication;
    SparseArray<RequestResult> mResultList = new SparseArray<RequestResult>();

    static final int REQUEST_GPS = 0x01;
    static final int REQUEST_WLAN = 0x02;
    static final int REQUEST_BT = 0x04;

    List<WifiScanResult> wifiScanResults;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mSenderThread = new HandlerThread("MDTSenderHandlerThread");
        mSenderThread.start();
        Looper looper = mSenderThread.getLooper();
        mSendHandler = new SendHandler(looper);

        Buffer = new byte[15];
        locTime = 0;

        mLiteRadioIndication = new LiteRadioIndication(this);
        mLiteRadioResponse = new LiteRadioResponse(this);
        mILiteRadioProxy = getILiteRadioProxy(HIDL_SERVICE_NAME, mLiteRadioResponse, mLiteRadioIndication);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public class ServiceBinder extends Binder {
        public DataHandlerService getService() {
            return DataHandlerService.this;
        }
    }

    /** Returns a {@link ILiteRadio} instance or null if the service is not available. */
    public ILiteRadio getILiteRadioProxy(String name, LiteRadioResponse mResponse, LiteRadioIndication mIndication) {
        ILiteRadio mILiteRadio = null;
        try {
            mILiteRadio = ILiteRadio.getService(name, true);

            if (mILiteRadio != null) {
                mILiteRadio.setResponseFunctions(mResponse, mIndication);
            } else {
                Log.e(TAG, "getILiteRadioProxy error: mILiteRadio is null");
            }
        } catch (RemoteException | RuntimeException e) {
            mILiteRadio = null;
            Log.e(TAG, "getILiteRadioProxy exception:", e);
        }

        return mILiteRadio;
    }

    class SendHandler extends Handler {
        public SendHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case EVENT_SEND:
                    String strcmd = (String) (msg.obj);

                    /* test */
                    strcmd = "AT+CMUT=0;";
                    //strcmd = null;
                    /* test */
                    if (strcmd != null) {
                        RequestResult rr = obtainRequestResult(strcmd);

                        Log.d(TAG, "EVENT_SEND, sendLiteCmd: " + strcmd);
                        sendLiteCmd(mILiteRadioProxy, rr);
                    } else {
                        Log.e(TAG, "handleMessage: cmd is null, so cant not send");
                    }
                    break;

                /*case EVENT_PERIOD_START:
                case EVENT_SEND_ONCE:
                    int action_type = msg.what;
                    final long sysTime = System.currentTimeMillis();
                    SimpleDateFormat sdf0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG, "EVENT_SEND_START/ONCE, systime:" + sdf0.format(new Date(sysTime)));

                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Log.d(TAG, "gps time:" + sdf1.format(new Date(locTime)));
                    if (msg.what == EVENT_PERIOD_START) {
                        Log.d(TAG, "EVENT_PERIOD_START, set periodRequestFlag true");
                        periodRequestFlag = true;
                    }

                    // if the time of the received location data is 10s less than now,
                    // then send it, or send reportProviderUnavailable

                    if (sysTime - locTime < LOCATION_MAX_DIFF_VALUE) {
                        String buf = byte2HexStr(Buffer);
                        Log.d(TAG, "location is ok, send! buf.length:" + buf.length() + ", buf:" + buf);

                        StringBuilder cmdBuilder = new StringBuilder(AGPS_MDT_AT);
                        cmdBuilder.append(action_type);
                        cmdBuilder.append(',');
                        cmdBuilder.append(ELLIPSOID_POINT);
                        cmdBuilder.append(",");
                        cmdBuilder.append(buf.length());
                        cmdBuilder.append(",");
                        cmdBuilder.append('\"');
                        cmdBuilder.append(buf);
                        cmdBuilder.append('\"');

                        String cmd = cmdBuilder.toString();

                        Message cmdMsg = obtainMessage(EVENT_SEND, cmd);
                        sendMessage(cmdMsg);
                    } else {
                        Log.d(TAG, "location time is not ok, send reportProviderUnavailable");
                        reportProviderUnavailable();
                    }

                    if (msg.what == EVENT_PERIOD_START) {
                        Log.d(TAG, "EVENT_PERIOD_START, set periodRequest Flag true");
                        boolean enabled = ((MDTApplication)getApplicationContext()).mGpsEnabled;
                        Log.d(TAG, "EVENT_PERIOD_START, enabled = " + enabled);
                        if (!enabled) {
                            reportProviderUnavailable();
                        }
                    }
                    break;

                case EVENT_PERIOD_STOP:
                    Log.d(TAG, "EVENT_PERIOD_STOP, set periodRequest Flag false");
                    periodRequestFlag = false;
                    break;*/

                case EVENT_AT_REQUEST:
                    String cmd = (String)msg.obj;
                    Log.d(TAG, "handleMessage: EVENT_AT_REQUEST, cmd: " + cmd);

                    handleAtCmdRequest(cmd);
                    break;
            }/* switch */
        }
    }

    int actionType;
    int requestBitmap;
    int reportInterval;
    int wlanLength;
    String wlanNameList;
    int btLength;
    String btNameList;

    void handleAtCmdRequest(String cmd) {
        if (cmd == null) {
            Log.e(TAG, "handleAtCmdRequest: cmd is null");
        }

        String[] arg = cmd.split(":|,|;");
        Log.d(TAG, "handleInputCommand: arg length: " + arg.length);

        for (String s:arg) {
            Log.d(TAG, "split value = '" + s + "'");
        }

        if (arg.length == 1) {
            Log.d(TAG, "arg.length = 1, so not handle, str: " + arg[0]);
            return;
        }
        if (!UNSOL_AGPS_AT.equalsIgnoreCase(arg[0].trim())) {
            Log.d(TAG, "handleMessage: not support, arg[0]: " + arg[0]);
            return;
        }

        actionType = Utils.stringToInt(arg[1].trim());
        requestBitmap = Utils.stringToInt(arg[2].trim());
        reportInterval = Utils.stringToInt(arg[5].trim());

        wlanLength = Utils.stringToInt(arg[7].trim()) & 0xFF;
        wlanNameList = arg[8].trim();

        btLength = Utils.stringToInt(arg[9].trim()) & 0xFF;
        btNameList = arg[10].trim();

        Timer timer = new Timer();

        Log.d(TAG, "handleMessage: requestBitmap: " + requestBitmap);

        switch (actionType) {
            case EVENT_SEND_ONCE:
                getGpsWlanBtResult();
                //send cmd
                break;

            case EVENT_PERIOD_START:
                getGpsWlanBtResult();
                //send cmd

                if (timer != null) {
                    timer.schedule(new GetResponseTimerTask(), 0, 30000);
                }
                break;

            case EVENT_PERIOD_STOP:
                if (timer != null) {
                    timer.cancel();
                }
                break;
        }
    }

    public class GetResponseTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "run: 11");
        }
    }

    String getGpsWlanBtResult() {
        StringBuilder cmdBuilder = new StringBuilder();
        String s;
        int flag = 0x01;

        for (int i = 0; i < 3; i++) {
            Log.d(TAG, "handleMessage: flag: " + flag);
            switch (requestBitmap & flag) {
                case REQUEST_GPS:
                    s = handleGpsRequest(actionType);
                    Log.d(TAG, "getGpsWlanBtResult: s1: " + s);
                    cmdBuilder.append(s);
                    break;

                case REQUEST_WLAN:
                    s = handleWlanRequest(wlanNameList);
                    Log.d(TAG, "handleWlanRequest: s2: " + s);
                    cmdBuilder.append(s);
                    break;

                case REQUEST_BT:
                    s = handleBtRequest(actionType, btLength, btNameList);
                    Log.d(TAG, "handleBtRequest: s3: " + s);
                    cmdBuilder.append(s);
                    break;
            }

            flag = flag << 1;
        }

        return cmdBuilder.toString().trim();
    }

    String handleGpsRequest(int actionType) {
        final long sysTime = System.currentTimeMillis();

        SimpleDateFormat sdf0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(TAG, "EVENT_SEND_START/ONCE, systime:" + sdf0.format(new Date(sysTime)));

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(TAG, "gps time:" + sdf1.format(new Date(locTime)));
        if (actionType == EVENT_PERIOD_START) {
            Log.d(TAG, "EVENT_PERIOD_START, set periodRequestFlag true");
            periodRequestFlag = true;
        }

        /* if the time of the received location data is 10s less than now,
         * then send it, or send reportProviderUnavailable
        */
        if (sysTime - locTime < LOCATION_MAX_DIFF_VALUE) {
            String buf = Utils.byte2HexStr(Buffer);
            Log.d(TAG, "location is ok, send! buf.length:" + buf.length() + ", buf:" + buf);

            StringBuilder cmdBuilder = new StringBuilder(AGPS_MDT_AT);
            cmdBuilder.append(actionType);
            cmdBuilder.append(',');
            cmdBuilder.append(ELLIPSOID_POINT);
            cmdBuilder.append(",");
            cmdBuilder.append(buf.length());
            cmdBuilder.append(",");
            cmdBuilder.append('\"');
            cmdBuilder.append(buf);
            cmdBuilder.append('\"');

            String cmd = cmdBuilder.toString();

            return cmd;
            //Message cmdMsg = obtainMessage(EVENT_SEND, cmd);
            //sendMessage(cmdMsg);

            //sendCmd(cmd);
        } else {
            Log.d(TAG, "location time is not ok, send reportProviderUnavailable");
            //reportProviderUnavailable();

            return "";
        }

        /*if (actionType == EVENT_PERIOD_START) {
            Log.d(TAG, "EVENT_PERIOD_START, set periodRequest Flag true");
            boolean enabled = ((MDTApplication)getApplicationContext()).mGpsEnabled;
            Log.d(TAG, "EVENT_PERIOD_START, enabled = " + enabled);
            if (!enabled) {
                reportProviderUnavailable();
            }
        }


        if (actionType == EVENT_PERIOD_STOP) {
            Log.d(TAG, "EVENT_PERIOD_STOP, set periodRequest Flag false");
            periodRequestFlag = false;
        }*/
    }

    String handleWlanRequest(String list) {
        String wlanResponse = null;

        Log.d(TAG, "handleWlanRequest: list: " + list);

        List<String> nameList = Utils.parseNameList(list);

        for(String name : nameList) {
            Log.d(TAG, "##parseNameList: name$$: " + name);
        }

        if (!((MDTApplication) getApplicationContext()).mWlanEnabled) {
            //reportProviderUnavailable();

            return "255" + ", 01";
        }

        wlanResponse = getWlanResponse(nameList);

        return wlanResponse;
    }

    String getWlanResponse(List<String> nameList) {
        List<WifiScanResult> datas;
        String response;

        datas = getRequestMatchingData(wifiScanResults, nameList);
        for (WifiScanResult r : datas) {
            Log.d(TAG, "@@handleScanWlanInfo@@: SSID: " + r.getSsid() + ", BSSID: " + r.getBssid());
        }

        final long sysTime = System.currentTimeMillis();

        Log.d(TAG, "getWlanResponse: time: " + (sysTime - datas.get(0).getTime()));
        if (sysTime - datas.get(0).getTime() > 6 * 60 * 1000) {
            response = "00";
            Log.d(TAG, "getWlanResponse: no data");
            return response;
        }

        int wlanResultNumber = datas.size();
        StringBuilder cmdBuilder = new StringBuilder(Utils.intToHexString(wlanResultNumber));
        Log.d(TAG, "handleWlanRequest: cmdBuilder: " + cmdBuilder);

        for (int i = 0; i < wlanResultNumber; i++) {
            WifiScanResult data = datas.get(i);
            String ssid = data.getSsid();

            if (ssid != null) {
                String ssidExit = Utils.intToHexString(1);
                String len = Utils.intToHexString(ssid.length());
                cmdBuilder.append(ssidExit);
                cmdBuilder.append(len);
                cmdBuilder.append(ssid);
            } else {

            }

            String bssid = data.getBssid();
            if (bssid != null) {
                String bssidExit = Utils.intToHexString(1);
                String mac = bssid.replace(":", "");
                Log.d(TAG, "handleWlanRequest: mac: " + mac);
                cmdBuilder.append(bssidExit);
                cmdBuilder.append(mac);
            }

            long hessid = data.getHessid();
            if (hessid == 0) {
                String hessidExit = Utils.intToHexString(0);
                cmdBuilder.append(hessidExit);
            }

            int rssi = data.getRssi();
            cmdBuilder.append(Utils.intToHexString(rssi));
            Log.d(TAG, "handleWlanRequest: rssi: " + Utils.intToHexString(rssi));

            int rtt = 0;
            cmdBuilder.append(Utils.intToHexString(rtt));
        }
        response = cmdBuilder.toString();
        Log.d(TAG, "handleWlanRequest: wlan str: " + response);

        return response;
    }

    String handleBtRequest(int actionType, int length, String list) {
        /*Log.d(TAG, "handleBtRequest: actionType: " + actionType);
        Log.d(TAG, "handleBtRequest: reportInterval: " + reportInterval);
        Log.d(TAG, "handleBtRequest: length: " + length);
        Log.d(TAG, "handleBtRequest: list: " + list);

        List<String> nameList = Utils.parseNameList(list);
        for(String name : nameList) {
            Log.d(TAG, "##parseNameList: name##: " + name);
        }

        if (actionType == EVENT_PERIOD_START) {
            if (!((MDTApplication) getApplicationContext()).mWlanEnabled) {
                //reportProviderUnavailable();
            }
            Log.d(TAG, "handleBtRequest: time schedule ------------");

            //timer.schedule(new GetResponseTimerTask(), 0, 30000);
            Utils.parseNameList(list);
        }*/
        return "";
    }

    public List<WifiScanResult> getRequestMatchingData (List<WifiScanResult> results, List<String> nameList) {
        ArrayList<WifiScanResult> datas = new ArrayList<WifiScanResult>();

        for (String s : nameList) {
            for (WifiScanResult r : results) {
                if ((r.getSsid() != null) && (r.getSsid().equals(s))) {
                    datas.add(r);
                }
            }
        }

        return datas;
    }


    
    /**
    * parse received info sent from cp.
    * @param recvInfo is at request message from server
    */
    public void parseRecvInfo(String recvInfo) {
        String[] info = recvInfo.split("\\+");
        Log.d(TAG, "parseRecvInfo, info.length: " + info.length);

        for (String cmd : info) {
            Log.d(TAG, "parseRecvInfo: cmd: " + cmd);
            if (cmd.length() > 0) {
                handleInputCommand(cmd);
            }
        }
    }

    public void handleInputCommand(String cmd) {
        if (cmd == null) {
            return;
        }

        /*int requestBitmap = Utils.stringToInt(str[2]) & 0xFF;
        Log.d(TAG, "handleInputCommand: bitmap:" + requestBitmap);
        if ((requestBitmap & REQUEST_GPS) == REQUEST_GPS) {
            Log.d(TAG, "handleInputCommand: request GPS");

        }

        if ((requestBitmap & REQUEST_WLAN) == REQUEST_WLAN) {
            Log.d(TAG, "handleInputCommand: request WLAN");
        }

        if ((requestBitmap & REQUEST_BT) == REQUEST_BT) {
            Log.d(TAG, "handleInputCommand: request BT");
        }

        Log.d(TAG, "handleInputCommand: gps: " + ((MDTApplication)getApplicationContext()).mGpsEnabled);
        Log.d(TAG, "handleInputCommand: wlan: " + ((MDTApplication)getApplicationContext()).mWlanEnabled);
        Log.d(TAG, "handleInputCommand: bt: " + ((MDTApplication)getApplicationContext()).mBtEnabled);*/

        /*
        String str3 = str[3].trim();
        Log.d(TAG, "handleInputCommand: str3 = " + str3);


        if (UNSOL_AGPS_AT.equalsIgnoreCase(str[0].trim())) {
            String str1 = str[1].trim();

            Log.d(TAG, "str1 = '" + str1 + "'");

            if (PERIOD_MDT_START.equalsIgnoreCase(str1)) {
                msg.what = EVENT_PERIOD_START;
            } else if (PERIOD_MDT_STOP.equalsIgnoreCase(str1)) {
                msg.what = EVENT_PERIOD_STOP;
            } else if (ONCE_MDT_INFO.equalsIgnoreCase(str1)) {
                msg.what = EVENT_SEND_ONCE;
            } else {
                Log.d(TAG, "do not know, str[1] = '" + str1 + "'");
                return;
            }
        } else {
            Log.d(TAG, "not handle other AT command = '" + str[0] + "'");
            return;
        }*/

        Message msg = mSendHandler.obtainMessage(EVENT_AT_REQUEST, cmd);

        if (mSendHandler != null) {
            mSendHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "error, mSendHandler is null");
        }
    }

    public void handleScanLocation(Location location) {
        byte latSign;
        double degreesLatitude;
        double degreesLongitude;
        float bearing;
        float speed;

        if (location.getLatitude() < 0) {
            latSign = SOUTH_LATITUDE;
        } else {
            latSign = NORTH_LATITUDE;
        }

        /* transfer the location data according to the code rule */
        degreesLatitude = ((Math.abs(location.getLatitude()) / 90) * 0x800000);
        degreesLongitude = (location.getLongitude() / 360) * 0x1000000;
        bearing = location.getBearing();
        speed = location.getSpeed();
        locTime = location.getTime();

        if (true) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.d(TAG, "onLocationChanged gps time:" + sdf.format(new Date(locTime)));
        }

        Buffer[0] = latSign;
        Buffer[1] = (byte) (((int) degreesLatitude >> 16) & 0x7F);
        Buffer[2] = (byte) (((int) degreesLatitude >> 8) & 0xFF);
        Buffer[3] = (byte) ((int) degreesLatitude & 0xFF);

        Buffer[4] = (byte) (((int) degreesLongitude >> 16) & 0xFF);
        Buffer[5] = (byte) (((int) degreesLongitude >> 8) & 0xFF);
        Buffer[6] = (byte) ((int) degreesLongitude & 0xFF);

        Buffer[7] = (byte) (((int) bearing >> 8) & 0xFF);
        Buffer[8] = (byte) ((int) bearing & 0xFF);

        Buffer[9] = (byte) (((int) speed >> 8) & 0xFF);
        Buffer[10] = (byte) ((int) speed & 0xFF);

        Buffer[11] = (byte) 0xFF;
        Buffer[12] = (byte) 0xFF;
        Buffer[13] = (byte) 0xFF;
        Buffer[14] = (byte) 0xFF;

        if (true) {
            for (int m = 0; m < 15; m++) {
                Log.d(TAG, "Buffer[" + m + "] = 0x" + Integer.toHexString(Buffer[m] & 0xff));
            }
        }
        if (true) {
            if (false) Log.d(TAG, "getAccuracy: " + location.getAccuracy());
            if (false) Log.d(TAG, "getAltitude: " + location.getAltitude());
            if (false) Log.d(TAG, "getProvider: " + location.getProvider());
            Log.d(TAG, "getBearing: " + location.getBearing());
            Log.d(TAG, "getLatitude: " + location.getLatitude());
            Log.d(TAG, "getLongitude: " + location.getLongitude());
            Log.d(TAG, "getSpeed: " + location.getSpeed());
            Log.d(TAG, "getTime: " + location.getTime());
        }

        Log.d(TAG, "onLocationChanged, periodRequestFlag: " + periodRequestFlag);
        if (periodRequestFlag) {
            Log.d(TAG, "Period request true, so send locationi msg!");

            Message msg = mSendHandler.obtainMessage();
            msg.what = EVENT_PERIOD_START;
            mSendHandler.sendMessage(msg);
        }
    }

    public void handleScanWlanInfo(List<WifiScanResult> result) {
        wifiScanResults = new ArrayList<>(result);

        for (WifiScanResult r : result) {
            Log.d(TAG, "handleScanWlanInfo: SSID: " + r.getSsid() + ", BSSID: " + r.getBssid());
        }

    }

    /**
     * report the AT Command if the provider is unavailable
     */
    public void reportProviderUnavailable() {
        int locType = 255; //255 means location provider is unavailable
        byte length = 0;

        if (!periodRequestFlag) {
            Log.d(TAG, "return, because periodRequestFlag: " + periodRequestFlag);
            return;
        }

        StringBuilder cmdBuilder = new StringBuilder(AGPS_MDT_AT);
        cmdBuilder.append(PERIOD_MDT_START);
        cmdBuilder.append(',');
        cmdBuilder.append(locType);
        cmdBuilder.append(',');
        cmdBuilder.append(length);

        String configAt = cmdBuilder.toString();
        Log.d(TAG, "reportProviderUnavailable: configAt: " + configAt);

        sendCmd(configAt);
    }

    public void reportAllUnavailable(int actionType) {
        int requestBitmap = 7;
        int locType = 255; //255 means location provider is unavailable
        byte length = 0;

        StringBuilder cmdBuilder = new StringBuilder(AGPS_MDT_AT);
        cmdBuilder.append(Integer.toString(actionType));
        cmdBuilder.append(',');
        cmdBuilder.append(Integer.toString(requestBitmap));
        cmdBuilder.append(',');
        cmdBuilder.append(locType);
        cmdBuilder.append(',');
        cmdBuilder.append(length);
        cmdBuilder.append(',');
        cmdBuilder.append("");
        cmdBuilder.append(length);
        cmdBuilder.append(',');
        cmdBuilder.append("");
        cmdBuilder.append(length);
        cmdBuilder.append(',');
        cmdBuilder.append("");

        String configAt = cmdBuilder.toString();
        Log.d(TAG, "reportProviderUnavailable: configAt: " + configAt);

        sendCmd(configAt);
    }

    /**
    * report the UI switch status value to the CP
    * @mdtEnable is the value of mdt enable switch
    * @loggedInfo is the valude of the logged measurement info
    * @locationInfo is the value of the location info
    */
    public void reportSwitchChanged(Boolean mdtEnable, Boolean loggedInfo, Boolean locationInfo) {
        StringBuilder cmdBuilder = new StringBuilder(CONFIG_MDT_AT);
        cmdBuilder.append(mdtEnable?1:0);
        cmdBuilder.append(',');
        cmdBuilder.append(loggedInfo?1:0);
        cmdBuilder.append(",");
        cmdBuilder.append(locationInfo?1:0);

        String configAt = cmdBuilder.toString();
        Log.d(TAG, "reportSwitchChanged: mdtConfig: " + configAt);

        sendCmd(configAt);
    }

    private void sendCmd(String cmd) {
        if (cmd == null) {
            Log.d(TAG, "sendCmd, cmd is null!");
            return;
        }

        Log.d(TAG, "sendCmd: " + cmd);

        Message msg = mSendHandler.obtainMessage(EVENT_SEND, cmd);
        mSendHandler.sendMessage(msg);
    }

    public void sendLiteCmd(ILiteRadio mILiteRadio, RequestResult rr) {
        try {
            if ((mILiteRadio != null) && (rr != null)) {
                mILiteRadio.sendCmd(rr.mSerial, rr.cmd);
                Log.d(TAG, "sendCmd OK, cmd:" + rr.cmd);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendCmd error:", e);
        }
    }

    private RequestResult obtainRequestResult(String cmd) {
        RequestResult rr = RequestResult.obtain(cmd);
        if (rr != null) {
            addResultList(rr);
        }
        return rr;
    }

    private void addResultList(RequestResult rr) {
        synchronized (mResultList) {
            mResultList.append(rr.mSerial, rr);
            Log.d(TAG, "addResultList: mResultList.size:" + mResultList.size());
        }
    }

    public RequestResult findAndRemoveResultFromList(int serial, String response) {
        RequestResult rr = null;

        synchronized (mResultList) {
            rr = mResultList.get(serial);
            if (rr != null) {
                rr.response = response;
                mResultList.remove(serial);
                Log.d(TAG, "findAndRemoveResultFromList, remove serial: " + serial +
                        ", mResultList.size:" + mResultList.size());
            }
        }

        Log.d(TAG, "findAndRemoveResultFromList: cmd:" + rr.cmd + ", response:" + rr.response);
        return rr;
    }
}


