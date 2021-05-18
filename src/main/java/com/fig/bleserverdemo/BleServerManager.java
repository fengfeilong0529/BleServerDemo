package com.fig.bleserverdemo;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.UUID;

public class BleServerManager {
    private static final String TAG = "BleServerManager";

    //服务UUID，自定义
    public static UUID UUID_SERVER = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    //读的特征值，自定义
    public static UUID UUID_CHAR_READ = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb");
    //写的特征值，自定义
    public static UUID UUID_CHAR_WRITE = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");

    private Application context;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mBluetoothGattServer;
    private BleServerCallback mBleServerCallback;
    private BluetoothGattCharacteristic mCharacteristicWrite;
    private BluetoothDevice mTDevice;

    public static BleServerManager getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final BleServerManager INSTANCE = new BleServerManager();
    }

    /**
     * 初始化，app启动时调用
     */
    public void init(Application app) {
        if (context == null && app != null) {
            context = app;
            if (isSupportBleServer()) {
                enableBt();
                mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                if (mBluetoothManager != null) {
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                }
            }
        }
    }

    private BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        return mBluetoothAdapter.getBluetoothLeAdvertiser();
    }

    private BluetoothGattServer getBluetoothGattServer() {
        return mBluetoothGattServer;
    }

    private BluetoothAdapter BluetoothAdapter() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        return mBluetoothAdapter;
    }

    /**
     * 创建Ble服务端，接收连接
     * 开启广播，启动成功BLE客户端才能搜到你
     */
    public void startAdvertising(String name, BleServerCallback bleServerCallback) {
        mBleServerCallback = bleServerCallback;
        //广播设置(必须)
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setTimeout(0)
                .setConnectable(true)                                           //能否连接,广播分为可连接广播和不可连接广播
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)     //发射功率级别: 极低,低,中,高
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                .build();
        //广播数据(必须，广播启动就会发送)
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)     //包含蓝牙名称，如果蓝牙名称过长，会导致startAdvertising失败返回errorCode 1错误；可设置为false不包含名称，或将系统蓝牙名称改短
                .setIncludeTxPowerLevel(true)   //包含发射功率级别
                .addManufacturerData(1, new byte[]{73, 66, (byte) 6d}) //设备厂商数据(sfm)，自定义
                .build();
        //设置蓝牙名称，长度不要太长，否则开启广播会失败（AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE）
        mBluetoothAdapter.setName(name);
        //开启服务
        getBluetoothLeAdvertiser().startAdvertising(settings, advertiseData, mAdvertiseCallback);
    }

    /**
     * 停止广播
     */
    public void stopAdvertising() {
        getBluetoothLeAdvertiser().stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Ble服务监听
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.e(TAG, "服务开启成功 " + settingsInEffect.toString());
            addService();
            if (mBleServerCallback != null) {
                mBleServerCallback.onServerStartSucceed(settingsInEffect);
            }
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "服务开启失败: " + errorCode);
            if (mBleServerCallback != null) {
                mBleServerCallback.onServerStartFailed(errorCode);
            }
        }
    };

    /**
     * 添加读写服务UUID，特征值等
     */
    private void addService() {
        BluetoothGattService gattService = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //只读的特征值
        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(UUID_CHAR_READ,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        //只写的特征值
        mCharacteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ
                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        //将特征值添加至服务里
        gattService.addCharacteristic(characteristicRead);
        gattService.addCharacteristic(mCharacteristicWrite);
        //监听客户端的连接
        mBluetoothGattServer = mBluetoothManager.openGattServer(context, gattServerCallback);
        //添加服务
        mBluetoothGattServer.addService(gattService);
    }


    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            String state = "";
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                state = "连接成功";
                if (mBleServerCallback != null) {
                    mBleServerCallback.onClientConnected(device, status);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                state = "连接断开";
                if (mBleServerCallback != null) {
                    mBleServerCallback.onClientDisConnected(device, status);
                }
            }
            Log.e(TAG, "onConnectionStateChange device=" + device.toString() + " status=" + status + " newState=" + state);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            String data = new String(value);
            Log.e(TAG, "收到了客户端发过来的数据=" + data);
            if (mBleServerCallback != null) {
                mBleServerCallback.onRecvClientMsg(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }
            //告诉客户端发送成功
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            mCharacteristicWrite.setValue("haha");
            mTDevice = device;
//            notifyData(device,mCharacteristicWrite,false);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.e(TAG, "onCharacteristicReadRequest: " );
            mBluetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS, offset,"imresp".getBytes());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.e(TAG, "onNotificationSent, status: " +status);

        }
    };

    public void notifyData(BluetoothDevice device, BluetoothGattCharacteristic characteristic, boolean confirm) {
        mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, confirm);
    }

    public void notifyData2() {
        mBluetoothGattServer.notifyCharacteristicChanged(mTDevice, mCharacteristicWrite, false);
    }

    /**
     * BleServer需要API21以上才支持
     */
    private boolean isSupportBleServer() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 开启蓝牙
     */
    public void enableBt() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    public interface BleServerCallback {
        void onServerStartSucceed(AdvertiseSettings advertiseSettings);

        void onServerStartFailed(int errorCode);

        void onClientConnected(BluetoothDevice device, int status);

        void onClientDisConnected(BluetoothDevice device, int status);

        void onRecvClientMsg(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value);
    }
}
