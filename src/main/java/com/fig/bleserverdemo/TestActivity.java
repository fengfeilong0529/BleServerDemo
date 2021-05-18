package com.fig.bleserverdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.AdvertiseSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TestActivity extends AppCompatActivity implements View.OnClickListener, BleServerManager.BleServerCallback {

    private static final int REQUEST_LOCATION_PERMISSION_CODE = 111;
    private TextView mTvLog;
    private ScrollView mScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        initView();
        checkPermissions();
    }

    private void initView() {
        findViewById(R.id.openBleServer).setOnClickListener(this);
        findViewById(R.id.closeBleServer).setOnClickListener(this);
        findViewById(R.id.btnNotify).setOnClickListener(this);
        mTvLog = findViewById(R.id.tvLog);
        mScrollView = findViewById(R.id.scrollView);
    }

    /**
     * Android 6.0以上需要动态申请ACCESS_FINE_LOCATION权限
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    printLog("定位权限申请成功");
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openBleServer:
                BleServerManager.getInstance().startAdvertising("FLX", this);
                break;
            case R.id.closeBleServer:
                BleServerManager.getInstance().stopAdvertising();
                break;
            case R.id.btnNotify:
                BleServerManager.getInstance().notifyData2();
                break;
            default:
                break;
        }
    }

    private void printLog(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvLog.append(txt + "\n");
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onServerStartSucceed(AdvertiseSettings advertiseSettings) {
        printLog("服务开启成功：" + advertiseSettings.toString());
    }

    @Override
    public void onServerStartFailed(int errorCode) {
        printLog("服务开启失败，错误码：" + errorCode);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onClientConnected(BluetoothDevice device, int status) {
        printLog(String.format("客户端连接成功（名称：%s | MAC：%s | status：%d）", device.getName(), device.getAddress(), status));
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onClientDisConnected(BluetoothDevice device, int status) {
        printLog(String.format("客户端连接断开（名称：%s | MAC：%s | status：%d）", device.getName(), device.getAddress(), status));
    }

    @Override
    public void onRecvClientMsg(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
        String data = new String(value);
        printLog("收到客户端发送的数据：" + data);
    }
}
