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
package net.akaish.kab.model

import android.bluetooth.BluetoothGatt

sealed class BleConnectionState(val stateId: Int) {
    object Started : BleConnectionState(BluetoothGatt.STATE_DISCONNECTED)
    object Disconnected : BleConnectionState(BluetoothGatt.STATE_DISCONNECTED)
    object Connecting : BleConnectionState(BluetoothGatt.STATE_CONNECTING)
    object Connected : BleConnectionState(BluetoothGatt.STATE_CONNECTED)
    object Disconnecting : BleConnectionState(BluetoothGatt.STATE_DISCONNECTING)
    class UnknownState(stateId: Int) : BleConnectionState(stateId)

//        Possible errors:
//        /** GATT read operation is not permitted  */
//        val GATT_READ_NOT_PERMITTED = 0x2
//        /** GATT write operation is not permitted  */
//        val GATT_WRITE_NOT_PERMITTED = 0x3
//        /** Insufficient authentication for a given operation  */
//        val GATT_INSUFFICIENT_AUTHENTICATION = 0x5
//        /** The given request is not supported  */
//        val GATT_REQUEST_NOT_SUPPORTED = 0x6
//        /** Insufficient encryption for a given operation  */
//        val GATT_INSUFFICIENT_ENCRYPTION = 0xf
//        /** A read or write operation was requested with an invalid offset  */
//        val GATT_INVALID_OFFSET = 0x7
//        /** A write operation exceeds the maximum length of the attribute  */
//        val GATT_INVALID_ATTRIBUTE_LENGTH = 0xd
//        /** A remote device connection is congested.  */
//        val GATT_CONNECTION_CONGESTED = 0x8f
//        /** A GATT operation failed, errors other than the above  */
//        val GATT_FAILURE = 0x101

    class ConnectionStateError(stateId: Int, val status: Int) : BleConnectionState(stateId)
    class ServiceDiscoveryError(stateId: Int, val status: Int) : BleConnectionState(stateId)
    class ServicesSupported(stateId: Int, val status: Int) : BleConnectionState(stateId)
    class Ready(stateId: Int, val status: Int) : BleConnectionState(stateId)
}