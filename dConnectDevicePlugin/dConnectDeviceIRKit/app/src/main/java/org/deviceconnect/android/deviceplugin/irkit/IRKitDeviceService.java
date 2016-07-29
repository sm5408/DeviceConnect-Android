/*
 IRKitDeviceService.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.irkit;

import android.content.Intent;
import android.net.wifi.WifiManager;

import org.deviceconnect.android.deviceplugin.irkit.IRKitManager.DetectionListener;
import org.deviceconnect.android.deviceplugin.irkit.data.IRKitDBHelper;
import org.deviceconnect.android.deviceplugin.irkit.data.VirtualDeviceData;
import org.deviceconnect.android.deviceplugin.irkit.network.WiFiUtil;
import org.deviceconnect.android.deviceplugin.irkit.profile.IRKitSystemProfile;
import org.deviceconnect.android.deviceplugin.irkit.service.IRKitService;
import org.deviceconnect.android.deviceplugin.irkit.service.VirtualService;
import org.deviceconnect.android.event.EventManager;
import org.deviceconnect.android.event.cache.MemoryCacheController;
import org.deviceconnect.android.localoauth.LocalOAuth2Main;
import org.deviceconnect.android.message.DConnectMessageService;
import org.deviceconnect.android.profile.SystemProfile;
import org.deviceconnect.android.service.DConnectService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IRKitデバイスプラグインサービス.
 * @author NTT DOCOMO, INC.
 */
public class IRKitDeviceService extends DConnectMessageService implements DetectionListener {

    /**
     * IRKitの検知を再スタートさせるためのアクションを定義.
     */
    public static final String ACTION_RESTART_DETECTION_IRKIT = "action.ACTION_RESTART_DETECTION_IRKIT";

    /**
     * 仮想デバイスの追加を通知するアクションを定義.
     */
    public static final String ACTION_VIRTUAL_DEVICE_ADDED = "action.ACTION_VIRTUAL_DEVICE_ADDED";

    /**
     * 仮想デバイスの削除を通知するアクションを定義.
     */
    public static final String ACTION_VIRTUAL_DEVICE_REMOVED = "action.ACTION_VIRTUAL_DEVICE_REMOVED";

    /**
     * 仮想デバイスのIDを取得するためのキー.
     */
    public static final String EXTRA_VIRTUAL_DEVICE_ID = "extra.EXTRA_VIRTUAL_DEVICE_ID";

    /**
     * 検知したデバイス群.
     */
    private final ConcurrentHashMap<String, IRKitDevice> mDevices
        = new ConcurrentHashMap<String, IRKitDevice>();

    /**
     * 現在のSSID.
     */
    private String mCurrentSSID;

    /** DB Helper. */
    private IRKitDBHelper mDBHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        mDBHelper = new IRKitDBHelper(getContext());
        for (VirtualDeviceData device : mDBHelper.getVirtualDevices(null)) {
            getServiceProvider().addService(new VirtualService(device, mDBHelper,
                getServiceProvider()));
        }

        EventManager.INSTANCE.setController(new MemoryCacheController());
        IRKitApplication app = (IRKitApplication) getApplication();
        app.setIRKitDevices(mDevices);
        IRKitManager.INSTANCE.init(this);
        IRKitManager.INSTANCE.setDetectionListener(this);
        if (WiFiUtil.isOnWiFi(this)) {
            startDetection();
        }

        mCurrentSSID = WiFiUtil.getCurrentSSID(this);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                if (!WiFiUtil.isOnWiFi(this) && IRKitManager.INSTANCE.isDetecting()) {
                    stopDetection();
                } else if (WiFiUtil.isOnWiFi(this) && WiFiUtil.isChangedSSID(this, mCurrentSSID)) {
                    restartDetection();
                }
                return START_STICKY;
            } else if (ACTION_RESTART_DETECTION_IRKIT.equals(action)) {
                if (WiFiUtil.isOnWiFi(this)) {
                    restartDetection();
                } else {
                    stopDetection();
                }
                return START_STICKY;
            } else if (ACTION_VIRTUAL_DEVICE_ADDED.equals(action)) {
                String id = intent.getStringExtra(EXTRA_VIRTUAL_DEVICE_ID);
                if (id != null) {
                    DConnectService service = getServiceProvider().getService(id);
                    if (service == null) {
                        List<VirtualDeviceData> devices = mDBHelper.getVirtualDevices(id);
                        if (devices.size() > 0) {
                            VirtualDeviceData device = devices.get(0);
                            service = new VirtualService(device, mDBHelper,
                                getServiceProvider());
                            getServiceProvider().addService(service);
                        }
                    }
                }
                return START_STICKY;
            } else if (ACTION_VIRTUAL_DEVICE_REMOVED.equals(action)) {
                String id = intent.getStringExtra(EXTRA_VIRTUAL_DEVICE_ID);
                if (id != null) {
                    DConnectService service = getServiceProvider().getService(id);
                    if (service != null) {
                        getServiceProvider().removeService(service);
                    }
                }
                return START_STICKY;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDetection();
        // 参照をきっておく
        IRKitManager.INSTANCE.setDetectionListener(null);
        LocalOAuth2Main.destroy();
    }

    /**
     * サービスIDからIRKitのデバイスを取得する.
     * 
     * @param serviceId サービスID
     * @return デバイス
     */
    public IRKitDevice getDevice(final String serviceId) {
        return mDevices.get(serviceId);
    }

    @Override
    protected SystemProfile getSystemProfile() {
        return new IRKitSystemProfile();
    }

    @Override
    public void onFoundDevice(final IRKitDevice device) {
        updateDeviceList(device, true);

        DConnectService service = getServiceProvider().getService(device.getName());
        if (service == null) {
            service = new IRKitService(device);
            getServiceProvider().addService(service);
        }
        service.setOnline(true);
    }

    @Override
    public void onLostDevice(final IRKitDevice device) {
        updateDeviceList(device, false);

        DConnectService service = getServiceProvider().getService(device.getName());
        if (service != null) {
            service.setOnline(false);
        }
    }


    /**
     * デバイスリストを更新する.
     * 
     * @param device デバイス
     * @param isOnline trueなら発見、falseなら消失を意味する
     */
    private void updateDeviceList(final IRKitDevice device, final boolean isOnline) {
        synchronized (mDevices) {
            IRKitDevice d = mDevices.get(device.getName());
            if (d != null) {
                if (!isOnline) {
                    mDevices.remove(device.getName());
                }
            } else if (isOnline) {
                mDevices.put(device.getName(), device);
            }
        }

        IRKitApplication app = (IRKitApplication) getApplication();
        app.setIRKitDevices(mDevices);
    }

    /**
     * 検知を開始する.
     */
    private void startDetection() {
        mCurrentSSID = WiFiUtil.getCurrentSSID(this);
        IRKitManager.INSTANCE.startDetection(this);
    }

    /**
     * 検知を終了する.
     */
    private void stopDetection() {
        mCurrentSSID = null;
        mDevices.clear();
        IRKitManager.INSTANCE.stopDetection();
    }

    private void restartDetection() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                stopDetection();
                startDetection();
            }
        }).start();
    }
}
