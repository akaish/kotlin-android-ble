/*
 * ---
 *
 *  Copyright (c) 2021 iKey (ikey.ru)
 *  Author: Denis Bogomolov (akaish)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a part of Kotlin Android BLE library, more info at
 * https://ikey.ru
 *
 * ---
 */
package net.akaish.kab.utility

import android.bluetooth.BluetoothGatt

/**
 * Actually this is a bit confusing thing because there can be several codes with same id,
 * also, it is hard to find any information about possible gatt sates and errors as well as
 * in android we can receive both GATT status and HCI status in one callback, for example
 * GATT_INSUF_AUTHORIZATION has same value as GATT_CONN_TIMEOUT which is HCI_ERR_CONNECTION_TOUT
 * This class just provides few most possible states and errors.
 * **See Also:** [Google's GATT API implementation](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h)
 * **See Also:** [Google's HCI definition implementation](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h)
 */
@Suppress("Unused")
object GattCode {

    /**
     * All OK code
     * value 0x00 : 0
     */
    const val GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS

    const val GATT_INVALID_HANDLE = 0x01

    /**
     * GATT read operation is not permitted
     * value 0x02 : 2
     */
    const val GATT_READ_NOT_PERMIT = BluetoothGatt.GATT_READ_NOT_PERMITTED

    /**
     * GATT write operation is not permitted
     * value 0x03 : 3
     */
    const val GATT_WRITE_NOT_PERMIT = BluetoothGatt.GATT_WRITE_NOT_PERMITTED

    /**
     * Malformed packet PDU
     */
    const val GATT_INVALID_PDU = 0x04

    /**
     * Insufficient authentication for a given operation
     * value 0x05 : 5
     */
    const val GATT_INSUF_AUTHENTICATION = BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION

    /**
     * The given request is not supported
     * value 0x06 : 6
     */
    const val GATT_REQ_NOT_SUPPORTED = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED

    /**
     * A read or write operation was requested with an invalid offset
     * value 0x07 : 7
     */
    const val GATT_INVALID_OFFSET = BluetoothGatt.GATT_INVALID_OFFSET

    /**
     * Insufficient authorization
     * value 0x08 : 8
     * Clashes with [GATT_CONN_TIMEOUT], most likely when you receive this code that means that it is
     * timeout cause right now this library not design for pairing\bonding devices
     */
    const val GATT_INSUF_AUTHORIZATION = 0x08

    const val GATT_PREPARE_Q_FULL = 0x09
    const val GATT_NOT_FOUND = 0x0a
    const val GATT_NOT_LONG = 0x0b
    const val GATT_INSUF_KEY_SIZE = 0x0c

    /**
     * A write operation exceeds the maximum length of the attribute
     * value 0x0d : 13
     */
    const val GATT_INVALID_ATTR_LEN = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH

    const val GATT_ERR_UNLIKELY = 0x0e

    /**
     * Insufficient encryption for a given operation
     * value 0x0f : 15
     */
    const val GATT_INSUF_ENCRYPTION = BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION

    const val GATT_UNSUPPORT_GRP_TYPE = 0x10
    const val GATT_INSUF_RESOURCE = 0x11

    const val GATT_ILLEGAL_PARAMETER = 0x87
    const val GATT_NO_RESOURCES = 0x80

    /**
     * Internal error
     * value 0x81 : 129
     */
    const val GATT_INTERNAL_ERROR = 0x81
    const val GATT_WRONG_STATE = 0x82
    const val GATT_DB_FULL = 0x83
    const val GATT_BUSY = 0x84

    /**
     * Gatt error
     * value 0x85 : 133
     * Possible reason: device is not reachable (source: Nordic Android Ble)
     * **See Also:** [Nordic's ble lib source](https://github.com/NordicSemiconductor/Android-BLE-Library/blob/b1f3b1d0d65991e3b3aae15630528266f55ceed9/ble/src/main/java/no/nordicsemi/android/ble/error/GattError.java#L42)
     */
    const val GATT_ERROR = 0x85

    const val GATT_CMD_STARTED = 0x86
    const val GATT_PENDING = 0x88
    const val GATT_AUTH_FAIL = 0x89
    const val GATT_MORE = 0x8a
    const val GATT_INVALID_CFG = 0x8b
    const val GATT_SERVICE_STARTED = 0x8c
    const val GATT_ENCRYPED_MITM = GATT_SUCCESS
    const val GATT_ENCRYPED_NO_MITM = 0x8d
    const val GATT_NOT_ENCRYPTED = 0x8e
    /**
     *  A remote device connection is congested (used too intensely or what?).
     *  value 0x8f : 143
     *  @see [BluetoothGatt.GATT_CONNECTION_CONGESTED]
     */
    const val GATT_CONGESTED = 0x8f


    const val GATT_CONN_UNKNOWN = 0x00
    const val GATT_CONN_L2C_FAILURE = 0x01

    /**
     * Connection timeout
     * value 0x08 : 8
     * **See Also:** [HCI_ERR_CONNECTION_TOUT](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h#458)
     * Clashes with [GATT_INSUF_AUTHORIZATION]
     */
    const val GATT_CONN_TIMEOUT = 0x08

    /**
     * Connection terminated by peer
     * value 0x13 : 19
     * **See Also:** [HCI_ERR_PEER_USER](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h#469)
     */
    const val GATT_CONN_TERMINATE_PEER_USER = 0x13

    /**
     * Connection terminated by local host
     * value 0x16 : 22
     * **See Also:** [HCI_ERR_CONN_CAUSE_LOCAL_HOST](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h#472)
     */
    const val GATT_CONN_TERMINATE_LOCAL_HOST = 0x16

    /**
     * Failed to establish connection. Seems that you won't receive this code, cause [GATT_ERROR]
     * would be raised more likely
     * value 0x3E : 62
     * **See Also:** [HCI_ERR_CONN_FAILED_ESTABLISHMENT](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h#512)
     */
    const val GATT_CONN_FAIL_ESTABLISH = 0x3E

    /**
     * Connection fail for LMP response tout
     * value 0x22 : 34
     * **See Also:** [HCI_ERR_LMP_RESPONSE_TIMEOUT](https://android.googlesource.com/platform/external/libnfc-nci/+/master/src/include/hcidefs.h#484)
     */
    const val GATT_CONN_LMP_TIMEOUT = 0x22

    /**
     * L2CAP connection cancelled
     * value 0x100 : 256
     * **See Also:** [GATT_CONN_CANCEL](https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/master/stack/include/gatt_api.h#113)
     */
    const val GATT_CONN_CANCEL = 0x100

    /**
     * Mysterious error of all errors that may be any error. At Nordic Android Ble library somebody
     * mentioned that this error most likely means that there are too many connections held by device.
     * So, basically it can be raised when number of connections exceeds max ble connections set as magic number
     * in device settings (on all devices that I checked this value it is 5 by default) or max not closed
     * properly gat objects (AFAIK 32 max).
     * value 0x101 : 257
     * @see [BluetoothGatt.GATT_FAILURE]
     * **See Also:** [Nordic's Android Ble lib](https://github.com/NordicSemiconductor/Android-BLE-Library/blob/b1f3b1d0d65991e3b3aae15630528266f55ceed9/ble/src/main/java/no/nordicsemi/android/ble/error/GattError.java#L81)
     */
    const val GATT_FAILURE = BluetoothGatt.GATT_FAILURE
}