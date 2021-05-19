package com.fig.bleserverdemo;

public class BleProtocol {

    public static final byte CMD_OPEN_DOOR = 0x2a;

    /**
     * 开门回复
     */
    public static byte[] getOpenDoorResp(boolean isSuccessful) {
        byte[] bytes = new byte[2];
        bytes[0] = CMD_OPEN_DOOR;
        //0x01成功，0x02密码错误
        if (isSuccessful) {
            bytes[1] = 0x01;
        } else {
            bytes[1] = 0x02;
        }
        return bytes;
    }
}
