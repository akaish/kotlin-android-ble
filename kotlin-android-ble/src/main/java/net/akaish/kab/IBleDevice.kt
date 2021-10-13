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
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import net.akaish.kab.IGattFacade.Companion.CONNECTED_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.CONNECTING_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.DISCONNECTED_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.SERVICE_DISCOVERY_TIMEOUT_DEFAULTS
import net.akaish.kab.model.ApplicationCharacteristic
import net.akaish.kab.model.TargetCharacteristic
import net.akaish.kab.result.ReadResult
import net.akaish.kab.result.WriteResult
import net.akaish.kab.utility.ILogger
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalCoroutinesApi
interface IBleDevice {

    //----------------------------------------------------------------------------------------------
    // Configuration
    //----------------------------------------------------------------------------------------------
    /**
     * @return ble device name.
     */
    @Suppress("Unused")
    fun bleDeviceName() : String

    /**
     * List of application characteristics to communicate with. You can assign long ids for them, also
     * all of characteristics with notification service type in this list would be auto subscribed
     * after connection, when "subscribe automatically" is set to true;
     */
    val applicationCharacteristics : MutableList<ApplicationCharacteristic>

    /**
     * Subscribe to application characteristics automatically
     */
    val autoSubscription : Boolean

    /**
     * Logger instance, if null
     */
    val l: ILogger?

    val desiredMTU: Int?

    val rssiUpdatePeriod: Long?

    val disableExceptions: AtomicBoolean

    val desiredPhyLe: Int

    //----------------------------------------------------------------------------------------------
    // Gatt configuration
    //----------------------------------------------------------------------------------------------
    /*
 * Timeouts
 */
    /**
     * Service timeout discovery timeout: after device connection gatt.discoverServices would be called,
     * if onServicesDiscovered would not be invoked before timeout, connection state would be changed to
     * [net.akaish.kab.model.BleConnectionState.ServicesDiscoveryTimeout]
     * App code may ignore this state or disconnect device.
     * Default value in library implementation would be [SERVICE_DISCOVERY_TIMEOUT_DEFAULTS],
     * if value is null then no timeout would be used.
     *
     */
    val serviceDiscoveryTimeout: Long?

    /**
     * Connecting timeout: after gatt.connect method would be called if connecting value callback would
     * not be called within timeout, connection state would be changed to
     * [net.akaish.kab.model.BleConnectionState.ConnectionStateTimeout]
     * App code may ignore this state or disconnect device.
     * Default value in library implementation would be [CONNECTING_STATE_TIMEOUT_DEFAULTS],
     * if value is null then no timeout would be used.
     */
    val connectingTimeout: Long?

    /**
     * Connected timeout: after state connecting received method would be called if connected callback would
     * not be called within timeout, connection state would be changed to
     * [net.akaish.kab.model.BleConnectionState.ConnectionStateTimeout]
     * App code may ignore this state or disconnect device.
     * Default value in library implementation would be [CONNECTED_STATE_TIMEOUT_DEFAULTS],
     * if value is null then no timeout would be used.
     */
    val connectedTimeout: Long?

    /**
     * Disconnected timeout: after disconnected method invoked if connected callback would
     * not be called within timeout, connection state would be changed to
     * [net.akaish.kab.model.BleConnectionState.DisconnectedStateTimeout]
     * App code may ignore this state or disconnect device.
     * Default value in library implementation would be [DISCONNECTED_STATE_TIMEOUT_DEFAULTS],
     * if value is null then no timeout would be used.
     * NOT USED RIGHT NOW
     */
    val disconnectedTimeout: Long?

    /**
     * Delay in ms to change connection state to [net.akaish.kab.model.BleConnectionState.Disconnected]
     * after [BluetoothGatt.STATE_DISCONNECTED] callback received
     */
    val disconnectedEventDelay: Long?

    /**
     * Delay in ms to start gatt.discoverServices after [BluetoothGatt.STATE_CONNECTED] callback received
     */
    val serviceDiscoveryDelay: Long?

    /**
     * Amount of retries for accessing gatt operations (not connection state operations)
     * Only gatt busy retries, default value : 1
     */
    val retryGattOperationsTime: Int

    //----------------------------------------------------------------------------------------------
    // Connection routine
    //----------------------------------------------------------------------------------------------
    /**
     * Connect device and prepare for application usage
     * @param device - remote device to connect
     * @param context - application context for registration etc
     * @param transport - connection transport, depends on API level, see [BluetoothDevice.TRANSPORT_LE], [BluetoothDevice.TRANSPORT_AUTO] etc
     */
    fun connect(device: BluetoothDevice, context: Context, transport: Int = 0)

    /**
     * @return true if device connected via bluetooth
     */
    fun isConnected(context: Context) : Boolean

    /**
     * Disconnects from device
     */
    fun disconnect()

    /**
     * Disconnects from device and cancels any coroutines working in kab scope
     */
    fun release()

    //----------------------------------------------------------------------------------------------
    // Communication with device
    //----------------------------------------------------------------------------------------------
    /**
     * @return gatt facade if ready or throws not initialized exception
     */
    fun gattFacade() : IGattFacade

    /**
     * Write something to target characteristic (suspend write)
     * @param target id for characteristic from registered application characteristics
     * @param bytes bytes to write
     * @param timeoutMS timeout for operation
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    suspend fun write(target: Long, bytes: ByteArray, timeoutMS: Long?) : WriteResult

    /**
     * Write something to target characteristic (suspend write)
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param bytes bytes to write
     * @param timeoutMS timeout for operation
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    suspend fun write(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?)  : WriteResult

    /**
     * Write something to target characteristic (blocking write)
     * @param target id for characteristic from registered application characteristics
     * @param bytes bytes to write
     * @param timeoutMS timeout for operation
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    fun writeBlocking(target: Long, bytes: ByteArray, timeoutMS: Long?) : WriteResult

    /**
     * Write something to target characteristic (blocking write)
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param bytes bytes to write
     * @param timeoutMS timeout for operation
     * @return write operation result [net.akaish.kab.result.WriteResult]
     */
    fun writeBlocking(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?) : WriteResult

    /**
     * Request characteristic read (suspend read)
     * @param target id for characteristic from registered application characteristics
     * @param timeoutMS timeout for operation
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    suspend fun read(target: Long, timeoutMS: Long?) : ReadResult

    /**
     * Request characteristic read (suspend read)
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param timeoutMS timeout for operation
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    suspend fun read(target: TargetCharacteristic, timeoutMS: Long?) : ReadResult

    /**
     * Request characteristic read (blocking read)
     * @param target id for characteristic from registered application characteristics
     * @param timeoutMS timeout for operation
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    fun readBlocking(target: Long, timeoutMS: Long?) : ReadResult

    /**
     * Request characteristic read (blocking read)
     * @param target service's and characteristic's uuid wrapped in instance of [net.akaish.kab.model.TargetCharacteristic]
     * @param timeoutMS timeout for operation
     * @return read operation result [net.akaish.kab.result.ReadResult]
     */
    fun readBlocking(target: TargetCharacteristic, timeoutMS: Long?) : ReadResult

    /**
     * Notification channel, that emits notifications from different sources
     */
    fun notificationChannel() : BroadcastChannel<Pair<BluetoothGattCharacteristic, ByteArray>>
}