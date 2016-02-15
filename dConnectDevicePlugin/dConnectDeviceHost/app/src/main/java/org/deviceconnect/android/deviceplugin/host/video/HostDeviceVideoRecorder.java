/*
 HostDeviceVideoRecorder.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.host.video;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import org.deviceconnect.android.deviceplugin.host.HostDeviceStreamRecorder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Host Device Video Recorder.
 *
 * @author NTT DOCOMO, INC.
 */
public class HostDeviceVideoRecorder implements HostDeviceStreamRecorder {

    private static final String ID = "video";

    private static final String NAME = "AndroidHost Video Recorder";

    private static final String MIME_TYPE = "video/3gp";

    private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd_kkmmss", Locale.JAPAN);

    private final Context mContext;

    public HostDeviceVideoRecorder(final Context context) {
        mContext = context;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public RecorderState getState() {
        String className = getClassnameOfTopActivity();
        if (VideoRecorderActivity.class.getName().equals(className)) {
            return RecorderState.RECORDING;
        } else {
            return RecorderState.INACTTIVE;
        }
    }

    private String getClassnameOfTopActivity() {
        ActivityManager activityMgr = (ActivityManager) mContext.getSystemService(Service.ACTIVITY_SERVICE);
        return activityMgr.getRunningTasks(1).get(0).topActivity.getClassName();
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public synchronized void start(final RecordingListener listener) {
        if (getState() == RecorderState.RECORDING) {
            throw new IllegalStateException();
        }

        final String filename = generateVideoFileName();
        Intent intent = new Intent();
        intent.setClass(mContext, VideoRecorderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(VideoConst.EXTRA_FILE_NAME, filename);
        intent.putExtra(VideoConst.EXTRA_CALLBACK, new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(final int resultCode, final Bundle resultData) {
                if (resultCode == Activity.RESULT_OK) {
                    listener.onRecorded(HostDeviceVideoRecorder.this, filename);
                } else {
                    String msg =
                        resultData.getString(VideoConst.EXTRA_CALLBACK_ERROR_MESSAGE, "Unknown error.");
                    listener.onFailed(HostDeviceVideoRecorder.this, msg);
                }
            }
        });
        mContext.startActivity(intent);
    }

    private String generateVideoFileName() {
        return "video" + mSimpleDateFormat.format(new Date()) + VideoConst.FORMAT_TYPE;
    }

    @Override
    public synchronized void stop() {
        Intent intent = new Intent(VideoConst.SEND_HOSTDP_TO_VIDEO);
        intent.putExtra(VideoConst.EXTRA_NAME, VideoConst.EXTRA_VALUE_VIDEO_RECORD_STOP);
        mContext.sendBroadcast(intent);
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException();
    }

}
