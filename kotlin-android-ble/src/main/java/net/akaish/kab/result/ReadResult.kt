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
import net.akaish.kab.utility.Hex

/**
 * Gatt characteristic read result: [net.akaish.kab.GattFacadeImpl.read] method
 */
sealed class ReadResult : BLEResult {

    /**
     * Device running other gatt operation
     */
    object DeviceIsBusy : ReadResult() {
        override fun toThrowable(): BleException? = BleDeviceIsBusyException(toString(), this)

        override fun toString() : String = "ReadResult: Device is busy: other gatt operation in progress!"
    }

    /**
     * Read failure
     */
    class ReadInitiateFailure(private val code: Int,
                              private val tr: Throwable?) : ReadResult() {
        override fun toThrowable(): BleException? = BleOperationInitializationFailure(toString(), this)

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("ReadResult: operation failed; code = $code!")
            tr?.let {
                sb.append('\n')
                sb.append(it.javaClass.canonicalName)
                sb.append('\n')
                sb.append(it.stackTraceToString())
            }
            return sb.toString()
        }
    }

    /**
     * Device callback already exists
     */
    object OperationCallbackAlreadyExists : ReadResult() {
        override fun toThrowable(): BleException? = BleCallbackAlreadyExistsException(toString(), this)

        override fun toString() : String = "ReadResult: operation callback already exists: other gatt operation in progress (app layer)!"
    }

    /**
     * Wrapper for exception thrown during [net.akaish.kab.GattFacadeImpl.read]
     */
    class OperationException(
        /**
         * Original throwable
         */
        @Suppress("Unused")
        val origin: Throwable) : ReadResult() {

        override fun toThrowable(): BleException? = BleOperationException(toString(), this)

        override fun toString() : String = "ReadResult: exception caught while executing: ${origin.localizedMessage}!"
    }

    /**
     * Operation timeout during running [net.akaish.kab.GattFacadeImpl.read]
     */
    class OperationTimeout(
        /**
         * Timeout value in ms
         */
        @Suppress("Unused")
        val timeoutMs: Long) : ReadResult() {

        override fun toThrowable(): BleException? = BleTimeoutException(toString(), this)

        override fun toString() : String = "ReadResult: timeout ($timeoutMs ms)!"
    }

    /**
     * Read error while executing [net.akaish.kab.GattFacadeImpl.read]
     */
    class ReadError(
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
        val status: Int) : ReadResult() {

        override fun toThrowable(): BleException? = BleErrorException(toString(), this)

        override fun toString() : String = "ReadResult: GATT error ${
            GattStatusToString.gattStatusToHumanReadableString(
                status
            )
        } [$status DEC 0x${status.toString(16)} HEX]!"
    }

    /**
     * Internal error
     */
    object WrongCharacteristicCallback : ReadResult() {

        override fun toThrowable(): BleException? = BleInternalErrorException(toString(), this)

        override fun toString() = "ReadResult: WrongCharacteristicCallback internal error"
    }

    /**
     * Characteristic read success
     */
    class ReadSuccess(
        /**
         * Bytes read from characteristic
         */
        @Suppress("Unused")
        val bytes: ByteArray) : ReadResult() {

        override fun toThrowable() : BleException? = null

        override fun toString() = "ReadResult: ${Hex.toPrettyHexString(bytes)}"
    }
}