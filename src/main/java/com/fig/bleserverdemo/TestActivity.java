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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class TestActivity extends AppCompatActivity implements View.OnClickListener, BleServerManager.BleServerCallback {
    private static final String TAG = "TestActivity";

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
                BleServerManager.getInstance().startAdvertising("GateMachine", this);
                break;
            case R.id.closeBleServer:
                BleServerManager.getInstance().stopAdvertising();
                break;
            case R.id.btnNotify:
//                BleServerManager.getInstance().notifyDataTest();
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
        //0x2a 383838383838
        if (value != null) {
            if (value[0] == BleProtocol.CMD_OPEN_DOOR) {
                parseOpenDoor(device, characteristic, value);
            }
        }
    }

    private void parseOpenDoor(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value) {
        boolean succeed = BleParser.openDoor(value, "888888");
        if (succeed) {
            showToast("已开门");
        } else {
            showToast("密码错误");
        }
        byte[] openDoorResp = BleProtocol.getOpenDoorResp(succeed);
        BleServerManager.getInstance().sendNotification(device, characteristic, openDoorResp);
    }

    public void showToast(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TestActivity.this, txt, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
