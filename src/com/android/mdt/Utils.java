package com.android.mdt;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Utils {
    private static final String TAG = "Utils";

    private static HashMap<Context, DataServiceConnection> sConnectionMap = new HashMap<Context, DataServiceConnection>();
    public static DataHandlerService mDataHandlerService;

    public static class ServiceToken {
        ContextWrapper mWrappedContext;

        ServiceToken(ContextWrapper context) {
            mWrappedContext = context;
        }
    }

    public static ServiceToken bindToService(Context context) {
        return bindToService(context, null);
    }

    public static ServiceToken bindToService(Context context, ServiceConnection callback) {
        Log.d(TAG, "bindToService: ");
        ContextWrapper cw = new ContextWrapper(context);

        Intent intent = new Intent(cw, DataHandlerService.class);

        try {
            cw.startService(intent);
        } catch (RuntimeException e) {
            Log.e(TAG, "bindToService: failed to start service: ", e);
            return null;
        }

        DataServiceConnection dataServiceConnection = new DataServiceConnection(callback);
        try {
            if (cw.bindService(intent, dataServiceConnection, 0)) {
                sConnectionMap.put(cw, dataServiceConnection);
                return new ServiceToken(cw);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "bindToService: failed to bind service: ", e);
        }
        
        Log.d(TAG, "bindToService ok!");
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            Log.e(TAG, "unbindFromService: try to unbind with null token");
            return;
        }
        
        ContextWrapper cw = token.mWrappedContext;
        DataServiceConnection dataServiceConnection = sConnectionMap.remove(cw);
        if (dataServiceConnection == null) {
            Log.e(TAG, "unbindFromService: try to unbind for unknown Context");
            return;
        }
        
        cw.unbindService(dataServiceConnection);
        Log.d(TAG, "unbindFromService: OK");
    }

    private static class DataServiceConnection implements ServiceConnection {
        ServiceConnection mCallback;

        DataServiceConnection(ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDataHandlerService = ((DataHandlerService.ServiceBinder)iBinder).getService();

            if (mCallback != null) {
                mCallback.onServiceConnected(componentName, iBinder);
            }
            Log.d(TAG, "onServiceConnected: ok");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(componentName);
            }

            mDataHandlerService = null;
            Log.d(TAG, "onServiceDisconnected: service disconnect");
        }
    }

    static int stringToInt(String s) {
        int i = -1;

        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return i;
    }

    static String byte2HexStr(byte[] b) {
        String tmp = "";
        StringBuilder s = new StringBuilder();

        for (int n = 0; n < b.length; n++) {
            tmp = Integer.toHexString(b[n] & 0xFF);
            s.append((tmp.length() == 1) ? ("0" + tmp) : tmp);
        }
        return s.toString().toUpperCase().trim();
    }

    static String intToHexString(int i) {
        String tmp = "";
        StringBuilder s = new StringBuilder();

        tmp = Integer.toHexString(i & 0xFF);
        s.append((tmp.length() == 1) ? ("0" + tmp) : tmp);

        return s.toString().toUpperCase().trim();
    }

    static List<String> parseNameList(String s) {
        List<String> nameList = new ArrayList<String>();
        Log.d(TAG, "^^parseNameList: s: " + s);

        String str = s.substring(1, s.length() - 1);
        int nameNum = Utils.stringToInt(str.substring(0, 2));

        parse(str.substring(2), nameList, nameNum);

        return nameList;
    }

    static List<String> parse(String str, List<String> nameList, int nameNum) {
        if ((nameList != null && nameList.size() == nameNum) ||
                (str == null) || (str.length() == 0)) {
            return nameList;
        }
        /*Bug 1199497:Utils.java may have NullPointException*/
        try {
            int len = Utils.stringToInt(str.substring(0, 2));
            String name = str.substring(2, 2 + len);
            nameList.add(name);
            String s = str.substring(2 + len);
            return parse(s, nameList, nameNum);
        }catch (Exception e){
            return null;
        }
    }

    static String removeAllQuotes(String str) {
        if(str.indexOf("\"") == 0) {
            str = str.substring(1,str.length());
        }

        if(str.lastIndexOf("\"") == (str.length()-1)) {
            str = str.substring(0,str.length()-1);
        }

        return str;
    }
}
