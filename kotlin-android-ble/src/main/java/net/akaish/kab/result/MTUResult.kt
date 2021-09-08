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

import net.akaish.kab.throwable.*
import net.akaish.kab.utility.GattStatusToString

/**
 * MTU result: sealed class used as result for [net.akaish.kab.GattFacadeImpl.requestMTU] method
 */
sealed class MTUResult : BLEResult {

    /**
     * Device running other gatt operation
     */
    object DeviceIsBusy : MTUResult() {
        override fun toThrowable(): BleException? = BleDeviceIsBusyException(toString(), this)

        override fun toString() : String = "MTUResult: Device is busy: other gatt operation in progress!"
    }

    /**
     * Wrapper for exception thrown during [net.akaish.kab.GattFacadeImpl.requestMTU]
     */
    class OperationException(
        /**
         * Original throwable
         */
        @Suppress("Unused")
        val origin: Throwable) : MTUResult() {

            override fun toThrowable(): BleException? = BleOperationException(toString(), this)

            override fun toString() : String = "MTUResult: exception caught while executing: ${origin.localizedMessage}!"
    }

    /**
     * Operation timeout during running [net.akaish.kab.GattFacadeImpl.requestMTU]
     */
    class OperationTimeout(
        /**
         * Timeout value in ms
         */
        @Suppress("Unused")
        val timeoutMs: Long) : MTUResult() {

        override fun toThrowable(): BleException? = BleTimeoutException(toString(), this)

        override fun toString() : String = "MTUResult: timeout ($timeoutMs ms)!"
    }

    /**
     * Subscription error during running [net.akaish.kab.GattFacadeImpl.requestMTU]
     */
    class MTUError(
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
        val status: Int) : MTUResult() {

        override fun toThrowable(): BleException? = BleErrorException(toString(), this)

        override fun toString() : String = "MTUResult: GATT error ${
            GattStatusToString.gattStatusToHumanReadableString(
                status
            )
        } [$status DEC 0x${status.toString(16)} HEX]!"
    }

    /**
     * Mtu request success result. Note: success doesn't mean that peripheral and server set desired
     * mtu, that means that peripheral and server set mtu = [MTUSuccess.mtu]
     */
    class MTUSuccess(
        /**
         * Current MTU value
         */
        @Suppress("Unused")
        val mtu: Int) : MTUResult() {

        override fun toThrowable() : BleException? = null

        override fun toString() : String = "MTUResult: Success, new MTU is set to $mtu!"
    }
}