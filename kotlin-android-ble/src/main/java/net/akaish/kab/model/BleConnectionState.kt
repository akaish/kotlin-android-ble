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
import net.akaish.kab.utility.GattCode.APP_GATT_CONNECTION_STAGE_TIMEOUT
import net.akaish.kab.utility.GattCode.APP_GATT_SERVICES_ACQUIRE_FAILURE

sealed class BleConnectionState(val stateId: Int) {

    companion object {
        const val B_STATE_DISCONNECTED = BluetoothGatt.STATE_DISCONNECTED
        const val B_STATE_CONNECTING = BluetoothGatt.STATE_CONNECTING
        const val B_STATE_DISCONNECTING = BluetoothGatt.STATE_DISCONNECTING
        const val B_STATE_CONNECTED = BluetoothGatt.STATE_CONNECTED

        const val B_STATE_CONNECTION_OBJECT_CREATED = 0x991
        const val B_STATE_SERVICES_DISCOVERY_STARTED = 0x992
        const val B_STATE_SERVICES_DISCOVERY_ERROR = 0x993
        const val B_STATE_SERVICES_DISCOVERED = 0x994
        const val B_STATE_CONNECTION_STAGE_TIMEOUT = 0x995
        const val B_STATE_CONNECTION_READY = 0x996
    }

    object Started : BleConnectionState(B_STATE_CONNECTION_OBJECT_CREATED)

    object Disconnected : BleConnectionState(B_STATE_DISCONNECTED)
    object Connecting : BleConnectionState(B_STATE_CONNECTING)
    object Connected : BleConnectionState(B_STATE_CONNECTED)
    object Disconnecting : BleConnectionState(B_STATE_DISCONNECTING)
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
    class ServicesDiscoveryError(val status: Int = APP_GATT_SERVICES_ACQUIRE_FAILURE) : BleConnectionState(B_STATE_SERVICES_DISCOVERY_ERROR)
    object ServicesDiscoveryStarted : BleConnectionState(B_STATE_SERVICES_DISCOVERY_STARTED)
    class ServicesDiscoveryTimeout(val status: Int = APP_GATT_CONNECTION_STAGE_TIMEOUT) : BleConnectionState(B_STATE_SERVICES_DISCOVERY_ERROR)
    object ServicesDiscovered : BleConnectionState(B_STATE_SERVICES_DISCOVERED)
    object ConnectionReady : BleConnectionState(B_STATE_CONNECTION_READY)

    class ConnectionStateTimeout(val status: Int = APP_GATT_CONNECTION_STAGE_TIMEOUT) : BleConnectionState(B_STATE_CONNECTION_STAGE_TIMEOUT)
    object DisconnectedStateTimeout : BleConnectionState(B_STATE_CONNECTION_STAGE_TIMEOUT)
}