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
 * Gatt characteristic write result: [net.akaish.kab.GattFacadeImpl.write] method
 */
sealed class WriteResult : BLEResult {

    /**
     * Device running other gatt operation
     */
    object DeviceIsBusy : WriteResult() {
        override fun toThrowable(): BleException? = BleDeviceIsBusyException(toString(), this)

        override fun toString() : String = "WriteResult: Device is busy: other gatt operation in progress!"
    }

    /**
     * Read failure
     */
    class WriteInitiateFailure(private val code: Int,
                               private val tr: Throwable?) : WriteResult() {
        override fun toThrowable(): BleException? = BleOperationInitializationFailure(toString(), this)

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("WriteResult: operation failed; code = $code!")
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
    object OperationCallbackAlreadyExists : WriteResult() {
        override fun toThrowable(): BleException? = BleCallbackAlreadyExistsException(toString(), this)

        override fun toString() : String = "WriteResult: operation callback already exists: other gatt operation in progress (app layer)!"
    }

    /**
     * Write error while executing [net.akaish.kab.GattFacadeImpl.write]
     */
    class WriteError(
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
        val status: Int) : WriteResult() {

        override fun toThrowable(): BleException? = BleErrorException(toString(), this)

        override fun toString() : String = "WriteResult: GATT error ${
            GattStatusToString.gattStatusToHumanReadableString(
                status
            )
        } [$status DEC 0x${status.toString(16)} HEX]!"
    }

    /**
     * Wrapper for exception thrown during [net.akaish.kab.GattFacadeImpl.write]
     */
    class OperationException(
        /**
         * Original throwable
         */
        @Suppress("Unused")
        val tr: Throwable) : WriteResult() {

        override fun toThrowable(): BleException? = BleOperationException(toString(), this)

        override fun toString() : String = "WriteResult: exception caught while executing: ${tr.localizedMessage}!"
    }

    /**
     * Operation timeout during running [net.akaish.kab.GattFacadeImpl.write]
     */
    class OperationTimeout(
        /**
         * Timeout value in ms
         */
        @Suppress("Unused")
        val timeoutMs: Long) : WriteResult() {

        override fun toThrowable(): BleException? = BleTimeoutException(toString(), this)

        override fun toString() : String = "WriteResult: timeout ($timeoutMs ms)!"
    }


    /**
     * Characteristic write success
     */
    object WriteSuccess : WriteResult() {

        override fun toThrowable(): BleException? = null

        override fun toString() = "WriteResult: success"
    }

    /**
     * Internal error
     */
    object WrongCharacteristicCallback : WriteResult() {

        override fun toThrowable(): BleException? = BleInternalErrorException(toString(), this)

        override fun toString() = "WriteResult: WrongCharacteristicCallback internal error"
    }
}