package com.fig.bleserverdemo;

import android.text.TextUtils;

public class BleParser {

    /**
     * 解析蓝牙开门指令
     *
     * @param value     蓝牙数据
     * @param devicePwd 设备密码
     * @return
     */
    public static boolean openDoor(byte[] value, String devicePwd) {
        if (value == null || value.length != 7 || value[0] != BleProtocol.CMD_OPEN_DOOR) return false;
        byte[] newBytes = new byte[6];
        System.arraycopy(value, 1, newBytes, 0, newBytes.length);
        String pwd = new String(newBytes);
        return TextUtils.equals(pwd, devicePwd);
    }
}
