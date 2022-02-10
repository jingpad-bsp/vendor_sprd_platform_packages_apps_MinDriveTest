/*
 * Collection of Location Information and Privacy
 *
 * 1.Minimal Drive Test (hereafter "MDT Enable")
 * When the MDT Enable is enabled, either of Report Idle Logged Measurement Info or Report GNSS Location Info
 * is enabled, or both of Report Idle Logged Measurement Info and Report GNSS Location Info are enabled.
 * When the MDT Enable is off, both the Report Idle Logged Measurement Info and GNSS are off.
 * The MDT Enable cannot control the specific log content according to the network
 * configuration on your device and can only determine whether there is a log.
 * Disable Method: You may choose to disable the collection of Location Information at
 * any time with a method of: opening Settings on your device, tap MDT, and then turn off the MDT Enable.
 * Explanations: (1) when 4G data is enabled, the MDT function is enabled;
 *               (2) when your device is in flight mode, MDT is off (paused).
 *
 * 2.Report Idle Logged Measurement Info
 * "Location Information" means the location information with timestamps, including but
 * not limited to related information generated from real-time measurements on your device (such as phone).
 * Conditions for collecting Location Information: The MDT Enable and Report Idle Logged Measurement Info are both enabled.
 * Effects and/or Risks Prompts: Enabling the Report Idle Logged Measurement Info means that:
 * (1) Location Information collected on your device within 48 hours of being collected through a wireless air interface,
 *     the operator's data collection device, and the operator's data transmission network,
 *     will be reported to the data storage server of the operator who configures for the collection;
 * (2) there will be a definite increase in power consumption on your device and decrease in battery endurance time;
 * (3) turning off the Report Idle Logged Measurement Info will not affect the location service on your device;
 * (4) enabling the Report Idle Logged Measurement Info will not affect the data usage of your device.
 * Disable Method(s): You may choose to disable the collection of Location Information at any time with methods of:
 * (1) opening Settings on your device, tap MDT, and then turn off the Report Idle Logged Measurement Info;
 * or (2) opening Settings on your device, tap MDT, and then turn off the MDT Enable.
 * By enabling the Report Idle Logged Measurement Info for your device, you agree and consent to the above Effects
 * and Risks and the operator (including but not limited to Chinese Mobile and Orange) may collect, process,
 * and use the Location Information collected on your device in order to optimize and improve their network coverage service.
 *
 * 3.Report Global Navigation Satellite System Location Information (hereafter "Report GNSS Location Info")
 * "Accurate Location Information" means the location information with timestamps, including but not limited to latitude
 * and longitude, the direction and speed your device is moving in, and the accuracy of
 * the foresaid information generated from real-time measurements on your device (such as phone).
 * Conditions for collecting Accurate Location Information: The MDT Enable and Report GNSS Location Info are both enabled.
 * Effects and/or Risks Prompts: Enabling the Report GNSS Location Info means that:
 * (1) Accurate Location Information collected on your device through an air interface, the operator's data collection device,
 *     and the operator's data transmission network, will be reported to the operator's data storage server configured for
 ×     the collection;
 * (2) there will be a definite increase in power consumption on your device and decrease in battery endurance time;
 * (3) enabling the Report GNSS Location Info will not affect the data usage of your device.
 * Disable Method(s): You may choose to disable the collection of Accurate Location Information at any time with methods of:
 * (1) opening Settings on your device, tap MDT, and then turn off the Report GNSS Location Info;
 * or (2) opening Settings on your device, tap MDT, and then turn off the MDT Enable.
 * By enabling the Report GNSS Location Info for your device, you agree and consent to the above Effects and Risks and
 * the operator (including but not limited to Chinese Mobile and Orange) may collect, process, and use the Accurate Location
 * Information collected on your device in order to optimize and improve their signal tower (base station) products and services.
 */

package com.android.mdt;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.preference.CheckBoxPreference;
import android.util.Log;

public class MDTSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        ServiceConnection {
    private static final String TAG = "MDTSettings";

    private static final String KETY_MDT_ENABLE = "mdt_enable";
    private static final String KETY_MDT_LOGGED = "mdt_logged";
    private static final String KETY_MDT_LOCATION = "mdt_location";

    private SwitchPreference mMdtEnablePreference;
    private CheckBoxPreference mLoggedPreference;
    private CheckBoxPreference mLocationPreference;

    DataHandlerService mDataHandlerService = null;
    private Utils.ServiceToken mToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.mdt_settings);

        mMdtEnablePreference = (SwitchPreference)findPreference(KETY_MDT_ENABLE);
        mLoggedPreference = (CheckBoxPreference)findPreference(KETY_MDT_LOGGED);
        mLocationPreference = (CheckBoxPreference)findPreference(KETY_MDT_LOCATION);

        mMdtEnablePreference.setOnPreferenceChangeListener(this);
        mLoggedPreference.setOnPreferenceChangeListener(this);
        mLocationPreference.setOnPreferenceChangeListener(this);

        Log.d(TAG, "onCreate: bind to service");
        mToken = Utils.bindToService(this, this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        boolean enable = (Boolean)objValue;
        Log.d(TAG, "click: " + preference.getKey());

        if (preference == mMdtEnablePreference) {
            if (mDataHandlerService != null) {
                mDataHandlerService.reportSwitchChanged(enable, mLoggedPreference.isChecked(),
                        mLocationPreference.isChecked());
            }
        } else if (preference == mLoggedPreference) {
            if ((!enable) && (!mLocationPreference.isChecked())) {
                mMdtEnablePreference.setChecked(false);
            }
            if (mDataHandlerService != null) {
                mDataHandlerService.reportSwitchChanged(mMdtEnablePreference.isChecked(),
                        enable, mLocationPreference.isChecked());
            }
        } else if (preference == mLocationPreference) {
            if ((!enable) && (!mLoggedPreference.isChecked())) {
                mMdtEnablePreference.setChecked(false);
            }
            if (mDataHandlerService!= null) {
                mDataHandlerService.reportSwitchChanged(mMdtEnablePreference.isChecked(),
                        mLoggedPreference.isChecked(), enable);
            }
        }

        return true;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        mDataHandlerService = ((DataHandlerService.ServiceBinder)iBinder).getService();
        if (mDataHandlerService == null) {
            Log.e(TAG, "onServiceConnected: error: mDataHandlerService is null");
        }
        Log.d(TAG, "onServiceConnected: ok");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mDataHandlerService = null;
        Log.d(TAG, "onServiceDisconnected!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Utils.unbindFromService(mToken);
        Log.d(TAG, "onDestroy: OK");
    }
}

