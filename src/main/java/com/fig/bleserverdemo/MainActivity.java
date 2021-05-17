package com.fig.bleserverdemo;

import android.Manifest;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 低功耗蓝牙作为服务端的demo
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int CODE_REQUEST_BT = 123;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    /**
     * Android 6.0以上需要动态申请ACCESS_FINE_LOCATION权限
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 111) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限申请成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 检查蓝牙是否开启
     */
    private void checkBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "此设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                //弹窗请求开启蓝牙
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, CODE_REQUEST_BT);
            } else {
                startAdvertising("FFL");
            }
        }
    }

    /**
     * 创建Ble服务端，接收连接
     * 启动蓝牙广播，启动成功BLE客户端才能搜到你
     */
    public void startAdvertising(String name) {
        //广播设置(必须)
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setTimeout(0)
                .setConnectable(true)           //能否连接,广播分为可连接广播和不可连接广播
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)     //发射功率级别: 极低,低,中,高
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
                .build();
        //广播数据(必须，广播启动就会发送)
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)     //包含蓝牙名称，如果蓝牙名称过长，会导致startAdvertising失败返回errorCode 1错误；可设置为false不包含名称，或将系统蓝牙名称改短
                .setIncludeTxPowerLevel(true)   //包含发射功率级别
                //.addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
                .build();
        //设置蓝牙名称，长度不要太长，否则开启广播会失败（AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE）
        mBluetoothAdapter.setName(name);
        //开启服务
        BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, advertiseCallback);
    }

    /**
     * Ble服务监听
     */
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, "服务开启成功 " + settingsInEffect.toString());
            addService();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "服务开启失败: " + errorCode);
        }
    };

    //服务UUID
    public static UUID UUID_SERVER = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    //读的特征值¸
    public static UUID UUID_CHAR_READ = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb");
    //写的特征值
    public static UUID UUID_CHAR_WRITE = UUID.fromString("0000ffe4-0000-1000-8000-00805f9b34fb");

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
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ
                        | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
        //将特征值添加至服务里
        gattService.addCharacteristic(characteristicRead);
        gattService.addCharacteristic(characteristicWrite);
        //监听客户端的连接
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, gattServerCallback);
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
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                state = "连接断开";
            }
            Log.e(TAG, "onConnectionStateChange device=" + device.toString() + " status=" + status + " newState=" + state);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            String data = new String(value);
            Log.e(TAG, "收到了客户端发过来的数据=" + data);
            //告诉客户端发送成功
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };

    public void startBleServer(View view) {
        checkBluetooth();
    }


//    public static final UUID UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000"); //自定义UUID
//    public static final UUID UUID_CHAR_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000");
//    public static final UUID UUID_DESC_NOTITY = UUID.fromString("11100000-0000-0000-0000-000000000000");
//    public static final UUID UUID_CHAR_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000");
//    private BluetoothLeAdvertiser mBluetoothLeAdvertiser; // BLE广播
//    private BluetoothGattServer mBluetoothGattServer; // BLE服务端

//    /**
//     * 初始化蓝牙设置
//     */
//    private void initBleSettings() {
//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
////        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        // ============启动BLE蓝牙广播(广告) =================================================================================
//        //广播设置(必须)
//        AdvertiseSettings settings = new AdvertiseSettings.Builder()
//                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //广播模式: 低功耗,平衡,低延迟
//                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //发射功率级别: 极低,低,中,高
//                .setConnectable(true) //能否连接,广播分为可连接广播和不可连接广播
//                .build();
//        //广播数据(必须，广播启动就会发送)
//        AdvertiseData advertiseData = new AdvertiseData.Builder()
//                .setIncludeDeviceName(true) //包含蓝牙名称
//                .setIncludeTxPowerLevel(true) //包含发射功率级别
//                .addManufacturerData(1, new byte[]{23, 33}) //设备厂商数据，自定义
//                .build();
//        //扫描响应数据(可选，当客户端扫描时才发送)
//        AdvertiseData scanResponse = new AdvertiseData.Builder()
//                .addManufacturerData(2, new byte[]{66, 66}) //设备厂商数据，自定义
//                .addServiceUuid(new ParcelUuid(UUID_SERVICE)) //服务UUID
//                //      .addServiceData(new ParcelUuid(UUID_SERVICE), new byte[]{2}) //服务数据，自定义
//                .build();
//        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
//        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);
//
//        // 注意：必须要开启可连接的BLE广播，其它设备才能发现并连接BLE服务端!
//        // =============启动BLE蓝牙服务端=====================================================================================
//        BluetoothGattService service = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);
//        //添加可读+通知characteristic
//        BluetoothGattCharacteristic characteristicRead = new BluetoothGattCharacteristic(UUID_CHAR_READ_NOTIFY,
//                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
//        characteristicRead.addDescriptor(new BluetoothGattDescriptor(UUID_DESC_NOTITY, BluetoothGattCharacteristic.PERMISSION_WRITE));
//        service.addCharacteristic(characteristicRead);
//        //添加可写characteristic
//        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHAR_WRITE,
//                BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
//        service.addCharacteristic(characteristicWrite);
//        if (bluetoothManager != null)
//            mBluetoothGattServer = bluetoothManager.openGattServer(this, mBluetoothGattServerCallback);
//        mBluetoothGattServer.addService(service);
//    }
//
//    // BLE广播Callback
//    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
//        @Override
//        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
//            logTv("BLE广播开启成功");
//        }
//
//        @Override
//        public void onStartFailure(int errorCode) {
//            logTv("BLE广播开启失败,错误码:" + errorCode);
//        }
//    };
//
//    // BLE服务端Callback
//    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
//            Log.i(TAG, String.format("onConnectionStateChange:%s,%s,%s,%s", device.getName(), device.getAddress(), status, newState));
//            logTv(String.format(status == 0 ? (newState == 2 ? "与[%s]连接成功" : "与[%s]连接断开") : ("与[%s]连接出错,错误码:" + status), device));
//        }
//
//        @Override
//        public void onServiceAdded(int status, BluetoothGattService service) {
//            Log.i(TAG, String.format("onServiceAdded:%s,%s", status, service.getUuid()));
//            logTv(String.format(status == 0 ? "添加服务[%s]成功" : "添加服务[%s]失败,错误码:" + status, service.getUuid()));
//        }
//
//        @Override
//        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
//            Log.i(TAG, String.format("onCharacteristicReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, characteristic.getUuid()));
//            String response = "CHAR_" + (int) (Math.random() * 100); //模拟数据
//            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes());// 响应客户端
//            logTv("客户端读取Characteristic[" + characteristic.getUuid() + "]:\n" + response);
//        }
//
//        @Override
//        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
//            // 获取客户端发过来的数据
//            String requestStr = new String(requestBytes);
//            Log.i(TAG, String.format("onCharacteristicWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, characteristic.getUuid(),
//                    preparedWrite, responseNeeded, offset, requestStr));
//            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);// 响应客户端
//            logTv("客户端写入Characteristic[" + characteristic.getUuid() + "]:\n" + requestStr);
//        }
//
//        @Override
//        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
//            Log.i(TAG, String.format("onDescriptorReadRequest:%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, offset, descriptor.getUuid()));
//            String response = "DESC_" + (int) (Math.random() * 100); //模拟数据
//            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response.getBytes()); // 响应客户端
//            logTv("客户端读取Descriptor[" + descriptor.getUuid() + "]:\n" + response);
//        }
//
//        @Override
//        public void onDescriptorWriteRequest(final BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
//            // 获取客户端发过来的数据
//            String valueStr = Arrays.toString(value);
//            Log.i(TAG, String.format("onDescriptorWriteRequest:%s,%s,%s,%s,%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, descriptor.getUuid(),
//                    preparedWrite, responseNeeded, offset, valueStr));
//            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);// 响应客户端
//            logTv("客户端写入Descriptor[" + descriptor.getUuid() + "]:\n" + valueStr);
//
//            // 简单模拟通知客户端Characteristic变化
//            if (Arrays.toString(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE).equals(valueStr)) { //是否开启通知
//                final BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        for (int i = 0; i < 5; i++) {
//                            SystemClock.sleep(3000);
//                            String response = "CHAR_" + (int) (Math.random() * 100); //模拟数据
//                            characteristic.setValue(response);
//                            mBluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
//                            logTv("通知客户端改变Characteristic[" + characteristic.getUuid() + "]:\n" + response);
//                        }
//                    }
//                }).start();
//            }
//        }
//
//        @Override
//        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
//            Log.i(TAG, String.format("onExecuteWrite:%s,%s,%s,%s", device.getName(), device.getAddress(), requestId, execute));
//        }
//
//        @Override
//        public void onNotificationSent(BluetoothDevice device, int status) {
//            Log.i(TAG, String.format("onNotificationSent:%s,%s,%s", device.getName(), device.getAddress(), status));
//        }
//
//        @Override
//        public void onMtuChanged(BluetoothDevice device, int mtu) {
//            Log.i(TAG, String.format("onMtuChanged:%s,%s,%s", device.getName(), device.getAddress(), mtu));
//        }
//    };
//
//    private void logTv(String s) {
//        Log.i(TAG, "logTv: " + s);
//    }
}
