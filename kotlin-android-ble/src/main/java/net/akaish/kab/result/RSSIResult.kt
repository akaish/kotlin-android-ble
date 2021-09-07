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
package net.akaish.kab.result

/**
 * RSSI request result: sealed class used as result for [net.akaish.kab.GattFacadeImpl.readRemoteRSSI] method
 */
sealed class RSSIResult {

    /**
     * Device running other gatt operation
     */
    object DeviceIsBusy : RSSIResult()

    /**
     * Wrapper for exception thrown during [net.akaish.kab.GattFacadeImpl.readRemoteRSSI]
     */
    class OperationException(
        /**
         * Original throwable
         */
        @Suppress("Unused")
        val origin: Throwable) : RSSIResult()

    /**
     * Operation timeout during running [net.akaish.kab.GattFacadeImpl.readRemoteRSSI]
     */
    class OperationTimeout(
        /**
         * Timeout value in ms
         */
        @Suppress("Unused")
        val timeoutMs: Long) : RSSIResult()

    /**
     * RSSI request error during running [net.akaish.kab.GattFacadeImpl.readRemoteRSSI]
     */
    class RSSIError(
        /**
         * Error status that differs from [android.bluetooth.BluetoothGatt.GATT_SUCCESS]
         * Possible statuses:
         * [android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED]
         * [android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED]
         * [android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION]
         * [android.bluetooth.BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED]
         * [android.bluetooth.BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION]
         * [android.bluetooth.BluetoothGatt.GATT_INVALID_OFFSET]
         * [android.bluetooth.BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH]
         * [android.bluetooth.BluetoothGatt.GATT_CONNECTION_CONGESTED]
         * [android.bluetooth.BluetoothGatt.GATT_FAILURE]
         */
        @Suppress("Unused")
        val status: Int) : RSSIResult()

    /**
     * RSSI success result
     */
    class RSSISuccess(
        /**
         * Remote device RSSI value
         */
        @Suppress("Unused")
        val rssi: Int) : RSSIResult()
}