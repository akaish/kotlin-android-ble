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

object GattStatusToString {
    fun gattStatusToHumanReadableString(status: Int) : String = when(status) {
        BluetoothGatt.GATT_SUCCESS -> "GATT SUCCESS"
        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT READ NOT PERMITTED"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT WRITE NOT PERMITTED"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT INSUFFICIENT AUTHENTICATION"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT REQUEST NOT SUPPORTED"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT INSUFFICIENT ENCRYPTION"
        BluetoothGatt.GATT_INVALID_OFFSET -> "GATT INVALID OFFSET"
        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT INVALID ATTRIBUTE LENGTH"
        BluetoothGatt.GATT_CONNECTION_CONGESTED -> "GATT CONNECTION CONGESTED"
        BluetoothGatt.GATT_FAILURE -> "GATT FAILURE"
        else -> "GATT UNKNOWN STATE"
    }
}