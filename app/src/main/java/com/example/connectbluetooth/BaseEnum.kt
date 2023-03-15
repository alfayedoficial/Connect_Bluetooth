package com.example.connectbluetooth

import androidx.annotation.IntDef



object BaseEnum {

    const val NONE = -1
    const val CMD_ESC = 1
    const val CMD_TSC = 2
    const val CMD_CPCL = 3
    const val CMD_ZPL = 4
    const val CMD_PIN = 5
    const val CON_BLUETOOTH = 1
    const val CON_BLUETOOTH_BLE = 2
    const val CON_WIFI = 3
    const val CON_USB = 4
    const val CON_COM = 5
    const val NO_DEVICE = -1
    const val HAS_DEVICE = 1

    @IntDef(CMD_ESC, CMD_TSC, CMD_CPCL, CMD_ZPL, CMD_PIN)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class CmdType

    @IntDef(CON_BLUETOOTH, CON_WIFI, CON_USB, CON_COM, NONE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class ConnectType


    @IntDef(NO_DEVICE, HAS_DEVICE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class ChooseDevice
}