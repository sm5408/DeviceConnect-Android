/*
 DevicePlugin.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.manager;

import android.content.ComponentName;

import org.deviceconnect.android.manager.util.VersionName;

import java.util.ArrayList;
import java.util.List;

/**
 * デバイスプラグイン.
 * @author NTT DOCOMO, INC.
 */
public class DevicePlugin {
    /** デバイスプラグインのパッケージ名. */
    private String mPackageName;
    /** デバイスプラグインのクラス名. */
    private String mClassName;
    /** デバイスプラグインのバージョン名. */
    private String mVersionName;
    /** プラグインD. */
    private String mPluginId;
    /** デバイスプラグイン名. */
    private String mDeviceName;
    /** Class name of service for restart. */
    private String mStartServiceClassName;
    /* プラグインSDKバージョン名. */
    private VersionName mPluginSdkVersionName;
    /**
     * サポートしているプロファイルを格納する.
     */
    private List<String> mSupports = new ArrayList<String>();

    /**
     * デバイスプラグインのパッケージ名を取得する.
     * @return パッケージ名
     */
    public String getPackageName() {
        return mPackageName;
    }
    /**
     * デバイスプラグインのパッケージ名を設定する.
     * @param packageName パッケージ名
     */
    public void setPackageName(final String packageName) {
        this.mPackageName = packageName;
    }

    /**
     * デバイスプラグインのバージョン名を取得する.
     * @return バージョン名
     */
    public String getVersionName() {
        return mVersionName;
    }

    /**
     * デバイスプラグインのバージョン名を設定する.
     * @param versionName バージョン名
     */
    public void setVersionName(final String versionName) {
        mVersionName = versionName;
    }

    /**
     * デバイスプラグインのクラス名を取得する.
     * @return クラス名
     */
    public String getClassName() {
        return mClassName;
    }
    /**
     * デバイスプラグインのクラス名を設定する.
     * @param className クラス名
     */
    public void setClassName(final String className) {
        this.mClassName = className;
    }
    /**
     * デバイスプラグインIDを取得する.
     * @return デバイスプラグインID
     */
    public String getPluginId() {
        return mPluginId;
    }
    /**
     * デバイスプラグインIDを設定する.
     * @param pluginId デバイスプラグインID
     */
    public void setPluginId(final String pluginId) {
        this.mPluginId = pluginId;
    }
    /**
     * デバイスプラグイン名を取得する.
     * @return デバイスプラグイン名
     */
    public String getDeviceName() {
        return mDeviceName;
    }
    /**
     * デバイスプラグイン名を設定する.
     * @param deviceName デバイスプラグイン名
     */
    public void setDeviceName(final String deviceName) {
        mDeviceName = deviceName;
    }
    /**
     * ComponentNameを取得する.
     * @return ComponentNameのインスタンス
     */
    public ComponentName getComponentName() {
        return new ComponentName(mPackageName, mClassName);
    }
    
    /**
     * Get a class name of service for restart.
     * @return class name or null if there are no service for restart
     */
    public String getStartServiceClassName() {
        return mStartServiceClassName;
    }
    /**
     * Set a class name of service for restart.
     * @param className class name
     */
    public void setStartServiceClassName(final String className) {
        this.mStartServiceClassName = className;
    }
    /**
     * サポートするプロファイルを追加する.
     * @param profileName プロファイル名
     */
    public void addProfile(final String profileName) {
        mSupports.add(profileName);
    }
    /**
     * サポートするプロファイルを設定する.
     * @param profiles プロファイル名一覧
     */
    public void setSupportProfiles(final List<String> profiles) {
        mSupports = profiles;
    }
    /**
     * デバイスプラグインがサポートするプロファイルの一覧を取得する.
     * @return サポートするプロファイルの一覧
     */
    public List<String> getSupportProfiles() {
        return mSupports;
    }

    public boolean supportsProfile(final String profileName) {
        if (mSupports == null) {
            return false;
        }
        for (String support : mSupports) {
            if (support.equalsIgnoreCase(profileName)) { // MEMO パスの大文字小文字無視
                return true;
            }
        }
        return false;
    }

    public void setPluginSdkVersionName(final VersionName pluginSdkVersionName) {
        mPluginSdkVersionName = pluginSdkVersionName;
    }

    public VersionName getPluginSdkVersionName() {
        return mPluginSdkVersionName;
    }
    
    @Override
    public String toString() {
        return "ServiceId: " + mPluginId + "DeviceName: " + mDeviceName
                + " package: " + mPackageName + " class: " + mClassName;
    }
}
