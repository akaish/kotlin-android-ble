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
package net.akaish.kab

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import androidx.annotation.IntRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.StateFlow
import net.akaish.kab.model.ApplicationCharacteristic
import net.akaish.kab.model.BleConnection
import net.akaish.kab.model.TargetCharacteristic
import net.akaish.kab.result.*
import net.akaish.kab.utility.ILogger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalCoroutinesApi
interface IGattFacade {

    val device: BluetoothDevice

    fun connect(context: Context, autoConnection: Boolean, transport: Int)

    fun disconnect()

    fun close()

    fun getGatt() : BluetoothGatt?

    /**
     * When true, no exceptions would be raised, all errors would be returned as results
     */
    val disableExceptions : AtomicBoolean

    /**
     * Logger instance, when null - no logging
     */
    val l: ILogger?

    val applicationServices: List<ApplicationCharacteristic>

    /**
     * [android.bluetooth.BluetoothGattCallback] implementation ready to work with facade
     */
    val bluetoothGattCallback : BluetoothGattCallback

    /**
     * Device RSSI state flow
     */
    val rssi : StateFlow<Int>

    /**
     * MTU state flow
     */
    val mtu : StateFlow<Int>

    /**
     * Device state
     */
    val deviceState : StateFlow<BleConnection>

    /**
     * PHY value, one of [BluetoothDevice.PHY_LE_1M], [BluetoothDevice.PHY_LE_2M] etc
     * Default implementation value is BluetoothDevice.PHY_LE_1M without constant link (e.g. 1)
     */
    val phyLe : Int

    /**
     * This void should be called when BleDevice implementation done all connection routine and preparations
     */
    fun onReady()

    /**
     * Request remote RSSI for device
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return operation result [net.akaish.kab.result.RSSIResult]
     */
    suspend fun readRemoteRSSI(timeout: Long, timeUnit: TimeUnit) : RSSIResult

    /**
     * Request MTU exchange, on devices with API level lower than [Build.VERSION_CODES.LOLLIPOP] does nothing,
     * just returns default MTU which value is [MTU_MIN].
     * @param desiredMTU desired mtu value in range [23..517]
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return operation result [net.akaish.kab.result.MTUResult]
     */
    suspend fun requestMTU(@IntRange(from = 23L, to = 517L) desiredMTU: Int, timeout: Long, timeUnit: TimeUnit) : MTUResult

    /**
     * Notification broadcast channel that emits broadcast of pair of ble gat characteristic and byte array
     * that contains data received from device
     */
    val notificationChannel : BroadcastChannel<Pair<BluetoothGattCharacteristic, ByteArray>>

    /**
     * Request characteristic subscription, in case of operation success characteristic changes would be
     * emitted from [notificationChannel]
     * @param target id for characteristic from registered application characteristics
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return operation result [net.akaish.kab.result.SubscriptionResult]
     */
    suspend fun subscribe(target: Long, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult

    /**
     * Request characteristic subscription, in case of operation success characteristic changes would be
     * emitted from [notificationChannel]
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return operation result [net.akaish.kab.result.SubscriptionResult]
     */
    suspend fun subscribe(target: TargetCharacteristic, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult

    /**
     * Request characteristic read
     * @param target id for characteristic from registered application characteristics
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    suspend fun read(target: Long, timeout: Long, timeUnit: TimeUnit) : ReadResult

    /**
     * Request characteristic read
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    suspend fun read(target: TargetCharacteristic, timeout: Long, timeUnit: TimeUnit) : ReadResult

    /**
     * Request characteristic write
     * @param target id for characteristic from registered application characteristics
     * @param bytes bytes to write
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    suspend fun write(target: Long, bytes: ByteArray, timeout: Long, timeUnit: TimeUnit) : WriteResult

    /**
     * Request characteristic write
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param bytes bytes to write
     * @param timeout timeout for operation
     * @param timeUnit time unit for timeout parameter
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    suspend fun write(target: TargetCharacteristic, bytes: ByteArray, timeout: Long, timeUnit: TimeUnit) : WriteResult
}