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

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import net.akaish.kab.BleConstants.Companion.CLIENT_CHARACTERISTIC_CONFIG_UUID
import net.akaish.kab.BleConstants.Companion.MTU_DEFAULT
import net.akaish.kab.BleConstants.Companion.MTU_MAX
import net.akaish.kab.BleConstants.Companion.MTU_MIN
import net.akaish.kab.BleConstants.Companion.RSSI_UNKNOWN
import net.akaish.kab.IGattFacade.Companion.CONNECTED_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.CONNECTING_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.DISCONNECTED_STATE_TIMEOUT_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.DISCONNECT_STATE_DELAY_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.SERVICE_DISCOVERY_DELAY_DEFAULTS
import net.akaish.kab.IGattFacade.Companion.SERVICE_DISCOVERY_TIMEOUT_DEFAULTS
import net.akaish.kab.model.*
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_CONNECTED
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_CONNECTING
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_CONNECTION_STAGE_TIMEOUT
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_DISCONNECTED
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_DISCONNECTING
import net.akaish.kab.model.BleConnectionState.Companion.B_STATE_SERVICES_DISCOVERY_ERROR
import net.akaish.kab.result.*
import net.akaish.kab.utility.ConnectDisconnectCounter
import net.akaish.kab.utility.GattCode
import net.akaish.kab.utility.GattCode.GATT_SUCCESS
import net.akaish.kab.utility.Hex
import net.akaish.kab.utility.ILogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.HashMap
import kotlin.coroutines.resume

@ExperimentalCoroutinesApi
class GattFacadeImpl(override val device: BluetoothDevice,
                     override val disableExceptions: AtomicBoolean,
                     override val l: ILogger? = null,
                     override val applicationServices: MutableList<ApplicationCharacteristic> = mutableListOf(),
                     private val uuid: UUID = UUID.randomUUID(),
                     override val phyLe: Int = 1,
                     // timeouts
                     override val serviceDiscoveryTimeout: Long? = SERVICE_DISCOVERY_TIMEOUT_DEFAULTS,
                     override val connectingTimeout: Long? = CONNECTING_STATE_TIMEOUT_DEFAULTS,
                     override val connectedTimeout: Long? = CONNECTED_STATE_TIMEOUT_DEFAULTS,
                     override val disconnectedTimeout: Long? = DISCONNECTED_STATE_TIMEOUT_DEFAULTS,
                     override val disconnectedEventDelay: Long? = DISCONNECT_STATE_DELAY_DEFAULTS,
                     override val serviceDiscoveryStartTimeout: Long? = SERVICE_DISCOVERY_DELAY_DEFAULTS,
                     @IntRange(from = 1, to = Int.MAX_VALUE.toLong())
                     override val retryGattOperationsTime: Int = 1) : IGattFacade {

    private lateinit var gatt: BluetoothGatt
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val bgThread = HandlerThread("GattFacadeImpl").apply { start() }
    private val bgThreadHandler = Handler(bgThread.looper)

    override fun equals(other: Any?): Boolean {
        if(other == null) return false
        if(other !is GattFacadeImpl) return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    private val connectedOnce = AtomicBoolean(false)

    @Synchronized  override fun connect(context: Context, autoConnection: Boolean, transport: Int) {
        if(connectedOnce.compareAndSet(false, true)) {
            mainThreadHandler.post {
                gatt = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        device.connectGatt(
                                context,
                                false,
                                this.bluetoothGattCallback,
                                transport,
                                phyLe,
                                mainThreadHandler
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        device.connectGatt(context, false, this.bluetoothGattCallback, transport)
                    }
                    else -> {
                        device.connectGatt(
                                context,
                                false,
                                this.bluetoothGattCallback
                        )
                    }
                }
                ConnectDisconnectCounter.connection(uuid)
            }
        }
    }

    private val disconnectedOnce = AtomicBoolean(false)

    @Synchronized override fun disconnect() {
        if(this::gatt.isInitialized)
            if(disconnectedOnce.compareAndSet(false, true)) {
                mainThreadHandler.post {
                    gatt.disconnect()
                }
            }
    }

    private val closedOnce = AtomicBoolean(false)

    @Synchronized override fun close() {
        if(this::gatt.isInitialized) {
            if(closedOnce.compareAndSet(false, true)) {
                mainThreadHandler.post {
                    ConnectDisconnectCounter.close(uuid)
                    gatt.close()
                }
            }
        }
    }

    override fun getGatt(): BluetoothGatt? {
        return if(this::gatt.isInitialized)
            gatt
        else null
    }

    //----------------------------------------------------------------------------------------------
    // Gatt facade implementation
    //----------------------------------------------------------------------------------------------
    override val bluetoothGattCallback: BluetoothGattCallback by lazy {
        GattCallbacks(
            connectingTimeout = connectingTimeout,
            connectedTimeout = connectedTimeout,
            disconnectedTimeout = disconnectedTimeout,
            serviceDiscoveredTimeout = serviceDiscoveryTimeout,
            disconnectedEventTimeout = disconnectedEventDelay,
            serviceDiscoveryStartTimeout = serviceDiscoveryStartTimeout).startWatchDog()
    }

    override val rssi = MutableStateFlow(RSSI_UNKNOWN)

    override val mtuFlow = MutableStateFlow(MTU_MIN)

    override val deviceState = MutableStateFlow(BleConnection(
        deviceName = device.name,
        deviceUid = device.address))

    override fun onReady() {
        l?.i("${deviceTag()} device ready to use!")
        deviceState.value.let { previous ->
            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ConnectionReady)
        }
    }

    override suspend fun readRemoteRSSI(timeout: Long, timeUnit: TimeUnit) : RSSIResult {
        val timeoutMs = timeUnit.toMillis(timeout)
        val result: RSSIResult
        withTimeout(timeoutMs) {
            result = readRemoteRSSI(timeoutMs)
        }
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun requestMTU(@IntRange(from = 23L, to = 517L) desiredMTU: Int, timeout: Long, timeUnit: TimeUnit) : MTUResult {
        val mtuParam = when {
            desiredMTU > MTU_MAX -> MTU_MAX
            desiredMTU < MTU_MIN -> MTU_MIN
            else -> desiredMTU
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            l?.w("${deviceTag()}  MTU request is unsupported on Android version ${Build.VERSION.SDK_INT}, returning default MTU value.")
            this.mtuFlow.value = MTU_DEFAULT
            return MTUResult.MTUSuccess(MTU_DEFAULT)
        }
        if(deviceState.value.bleConnectionState.stateId != BluetoothAdapter.STATE_CONNECTED) {
            l?.e("${deviceTag()} UNABLE TO EXECUTE MTU REQUEST: DEVICE IS NOT CONNECTED!")
            return MTUResult.OperationException(IllegalStateException("${deviceTag()} UNABLE TO EXECUTE MTU REQUEST: DEVICE IS NOT CONNECTED!"))
        }
        val timeoutMs = timeUnit.toMillis(timeout)
        val result: MTUResult
        withTimeout(timeoutMs) {
            result = requestMTU(mtuParam, timeoutMs)
        }
        result.throwException(disableExceptions.get())
        return result
    }

    override val notificationChannel = BroadcastChannel<Pair<BluetoothGattCharacteristic, ByteArray>>(32)

    override suspend fun subscribe(target: Long, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult {
        val result = try {
            subscribe(characteristics[target.toString()]!!, timeout, timeUnit)
        } catch (tr: Throwable) {
            SubscriptionResult.OperationException(tr)
        }
        if(result !is SubscriptionResult.SubscriptionSuccess) {
            val previous = deviceState.value.bleConnectionState
            val newState = BleConnectionState.ConnectionStateError(previous.stateId, GattCode.GATT_ERROR)
            deviceState.value = deviceState.value.copy(bleConnectionState = newState)
        }
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun subscribe(target: TargetCharacteristic, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult {
        val result = try {
            subscribe(characteristics[target.toString()]!!, timeout, timeUnit)
        } catch (tr: Throwable) {
            SubscriptionResult.OperationException(tr)
        }
        if(result !is SubscriptionResult.SubscriptionSuccess) {
            val previous = deviceState.value.bleConnectionState
            val newState = BleConnectionState.ConnectionStateError(previous.stateId, GattCode.GATT_ERROR)
            deviceState.value = deviceState.value.copy(bleConnectionState = newState)
        }
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun read(target: Long, timeout: Long, timeUnit: TimeUnit) : ReadResult {
        val result = read(characteristics[target.toString()]!!, timeout, timeUnit)
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun read(target: TargetCharacteristic, timeout: Long, timeUnit: TimeUnit) : ReadResult {
        val result = read(characteristics[target.toString()]!!, timeout, timeUnit)
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun write(target: Long, bytes: ByteArray, timeout: Long, timeUnit: TimeUnit) : WriteResult {
        val result = write(characteristics[target.toString()]!!, bytes, timeout, timeUnit)
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun write(target: TargetCharacteristic, bytes: ByteArray, timeout: Long, timeUnit: TimeUnit) : WriteResult {
        val result = write(characteristics[target.toString()]!!, bytes, timeout, timeUnit)
        result.throwException(disableExceptions.get())
        return result
    }

    //----------------------------------------------------------------------------------------------
    // Internal implementation
    //----------------------------------------------------------------------------------------------
    private fun deviceTag() = "${deviceState.value.deviceName} @ ${deviceState.value.deviceUid}"

    private val characteristics = HashMap<String, BluetoothGattCharacteristic>()

    //----------------------------------------------------------------------------------------------
    // RSSI
    //----------------------------------------------------------------------------------------------
    private interface OnReadRemoteRSSI {
        fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int)
    }

    @Volatile private var onReadRSSICallback: OnReadRemoteRSSI? = null

    private suspend fun readRemoteRSSI(timeoutMs: Long)
        : RSSIResult = suspendCancellableCoroutine { continuation ->
        bgThreadHandler.post {
            try {
                // For some reason using SAM on OnReadRemoteRSSI fun iface cause exception
                onReadRSSICallback = object : OnReadRemoteRSSI {
                    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                        onReadRSSICallback = null
                        if (status == GATT_SUCCESS) {
                            this@GattFacadeImpl.rssi.value = rssi
                            continuation.resume(RSSIResult.RSSISuccess(rssi))
                        } else {
                            l?.e("${deviceTag()} FAILED TO READ RSSI: STATUS CODE $status")
                            continuation.resume(RSSIResult.RSSIError(status))
                        }
                    }

                }
                var tryCounter = 0
                while (tryCounter != retryGattOperationsTime) {
                    tryCounter++
                    if (!gatt.readRemoteRssi()) {
                        if(tryCounter == retryGattOperationsTime) {
                            onReadRSSICallback = null
                            l?.e("${deviceTag()} FAILED TO READ RSSI: DEVICE IS BUSY")
                            continuation.resume(RSSIResult.DeviceIsBusy)
                            return@post
                        } else {
                            l?.w("${deviceTag()} FAILED TO READ RSSI: DEVICE IS BUSY, NEXT TRY")
                            Thread.sleep(5)
                        }
                    } else break
                }
            } catch (tr: Throwable) {
                onReadRSSICallback = null
                if (tr is TimeoutCancellationException) {
                    l?.e("${deviceTag()} FAILED TO READ RSSI: OPERATION TIMEOUT $timeoutMs")
                    continuation.resume(RSSIResult.OperationTimeout(timeoutMs))
                    return@post
                } else {
                    l?.e("${deviceTag()} FAILED TO READ RSSI: OPERATION EXCEPTION", tr)
                    continuation.resume(RSSIResult.OperationException(tr))
                    tr.printStackTrace()
                    return@post
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Notification
    //----------------------------------------------------------------------------------------------
    private interface SubscriptionDescriptorCallback {
        fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int)
    }

    @Volatile private var subscriptionCallback: SubscriptionDescriptorCallback? = null

    private suspend fun subscribe(target: BluetoothGattCharacteristic, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult{
        val timeoutMs = timeUnit.toMillis(timeout)
        val result: SubscriptionResult
        withTimeout(timeoutMs) {
            result = subscribe(target, timeoutMs)
        }
        return result
    }

    private suspend fun subscribe(characteristic: BluetoothGattCharacteristic, timeoutMs: Long)
            : SubscriptionResult = suspendCancellableCoroutine { continuation ->
        mainThreadHandler.post {
            var tryCounter = 0
            while (tryCounter != retryGattOperationsTime) {
                tryCounter++
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    if(tryCounter == retryGattOperationsTime) {
                        l?.e("${deviceTag()} FAILED TO SET SUBSCRIPTION FLAG TO ${characteristic.uuid}!")
                        continuation.resume(SubscriptionResult.SubscriptionError(-228))
                        return@post
                    } else {
                        l?.w("${deviceTag()} FAILED TO SET SUBSCRIPTION FLAG TO ${characteristic.uuid}, NEXT TRY!")
                        Thread.sleep(5)
                    }
                } else break
            }
            try {
                val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                subscriptionCallback = object : SubscriptionDescriptorCallback {
                    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                        subscriptionCallback = null
                        if (status == GATT_SUCCESS) {
                            l?.i("${deviceTag()} subscribed to ${characteristic.uuid}")
                            continuation.resume(SubscriptionResult.SubscriptionSuccess)
                        } else {
                            l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid} ; STATUS CODE: $status!")
                            continuation.resume(SubscriptionResult.SubscriptionError(status))
                        }
                    }
                }
                if (!gatt.writeDescriptor(descriptor)) {
                    subscriptionCallback = null
                    l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid} ; DEVICE IS BUSY!")
                    continuation.resume(SubscriptionResult.DeviceIsBusy)
                    return@post
                }
            } catch (tr: Throwable) {
                subscriptionCallback = null
                if (tr is TimeoutCancellationException) {
                    l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid}: OPERATION TIMEOUT $timeoutMs")
                    continuation.resume(SubscriptionResult.OperationTimeout(timeoutMs))
                    return@post
                } else {
                    l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid}: OPERATION EXCEPTION", tr)
                    continuation.resume(SubscriptionResult.OperationException(tr))
                    return@post
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // MTU
    //----------------------------------------------------------------------------------------------
    private interface OnMTUChanged {
        fun onMTUChanged(gatt: BluetoothGatt, mtu: Int, status: Int)
    }

    @Volatile private var mtuCallback : OnMTUChanged? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private suspend fun requestMTU(desiredMTU: Int, timeoutMs: Long)
            : MTUResult = suspendCancellableCoroutine { continuation ->
        bgThreadHandler.post {
            try {
                require(desiredMTU in MTU_MIN..MTU_MAX)
                mtuCallback = object : OnMTUChanged {
                    override fun onMTUChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                        mtuCallback = null
                        if (status == GATT_SUCCESS) {
                            mtuFlow.value = mtu
                            l?.i("${deviceTag()} MTU callback invoked (value $mtu)")
                            continuation.resume(MTUResult.MTUSuccess(mtu))
                        } else {
                            l?.e("${deviceTag()} MTU ERROR : STATUS CODE $status")
                            continuation.resume(MTUResult.MTUError(status))
                        }
                    }
                }
                var tryCounter = 0
                while (tryCounter != retryGattOperationsTime) {
                    tryCounter++
                    if (!gatt.requestMtu(desiredMTU)) {
                        if(tryCounter == retryGattOperationsTime) {
                            mtuCallback = null
                            l?.e("${deviceTag()} MTU ERROR : DEVICE IS BUSY")
                            continuation.resume(MTUResult.DeviceIsBusy)
                            return@post
                        } else {
                            l?.w("${deviceTag()} MTU ERROR : DEVICE IS BUSY, NEXT TRY")
                            Thread.sleep(5)
                        }
                    } else break
                }
            } catch (tr: Throwable) {
                if (tr is TimeoutCancellationException) {
                    mtuCallback = null
                    l?.e("${deviceTag()} FAILED TO EXECUTE MTU REQUEST: OPERATION TIMEOUT $timeoutMs")
                    continuation.resume(MTUResult.OperationTimeout(timeoutMs))
                    return@post
                } else {
                    mtuCallback = null
                    l?.e("${deviceTag()} FAILED TO EXECUTE MTU REQUEST: OPERATION EXCEPTION", tr)
                    continuation.resume(MTUResult.OperationException(tr))
                    return@post
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // RW callbacks map
    //----------------------------------------------------------------------------------------------
    private interface RWCallback
    private val callbacks = ConcurrentHashMap<BluetoothGattCharacteristic, RWCallback>()

    //----------------------------------------------------------------------------------------------
    // Read something
    //----------------------------------------------------------------------------------------------
    private interface OnCharacteristicRead : RWCallback {
        fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int)
    }

    private suspend fun read(target: BluetoothGattCharacteristic, timeout: Long, timeUnit: TimeUnit) : ReadResult {
        val timeoutMs = timeUnit.toMillis(timeout)
        val result: ReadResult
        withTimeout(timeoutMs) {
            result = read(target, timeoutMs)
        }
        return result
    }

    private suspend fun read(target: BluetoothGattCharacteristic, timeoutMs: Long)
            : ReadResult = suspendCancellableCoroutine { continuation ->
        mainThreadHandler.post {
            try {
                val callback = object : OnCharacteristicRead {
                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        callbacks.remove(target)
                        if (target.uuid == characteristic.uuid) {
                            if (status == GATT_SUCCESS) {
                                val value: ByteArray? = characteristic.value
                                if (value == null || value.isEmpty()) {
                                    l?.w("${deviceTag()} Read ${characteristic.uuid} result is null or empty")
                                    continuation.resume(ReadResult.ReadSuccess(byteArrayOf()))
                                } else {
                                    l?.i("${deviceTag()} Read ${characteristic.uuid}: ${Hex.toPrettyHexString(value)}")
                                    continuation.resume(ReadResult.ReadSuccess(value))
                                }
                            } else {
                                l?.e("${deviceTag()} READ ERROR : STATUS CODE $status; CHAR ${target.uuid}")
                                continuation.resume(ReadResult.ReadError(status))
                            }
                        } else {
                            l?.e("${deviceTag()} READ ERROR : WRONG CALLBACK; CHAR ${target.uuid}")
                            continuation.resume(ReadResult.WrongCharacteristicCallback)
                        }
                    }
                }
                callbacks[target]?.let {
                    l?.e("${deviceTag()} READ ERROR : DEVICE IS BUSY; CHAR ${target.uuid}")
                    continuation.resume(ReadResult.DeviceIsBusy)
                    return@post
                }
                callbacks[target] = callback
                var tryCounter = 0
                while (tryCounter != retryGattOperationsTime) {
                    tryCounter++
                    if (!gatt.readCharacteristic(target)) {
                        if(tryCounter == retryGattOperationsTime) {
                            callbacks.remove(target)
                            l?.e("${deviceTag()} READ ERROR : DEVICE IS BUSY; CHAR ${target.uuid}")
                            continuation.resume(ReadResult.DeviceIsBusy)
                            return@post
                        } else {
                            l?.w("${deviceTag()} READ ERROR : DEVICE IS BUSY; CHAR ${target.uuid}, NEXT TRY")
                            Thread.sleep(5)
                        }
                    } else break
                }
            } catch (tr: Throwable) {
                callbacks.remove(target)
                if (tr is TimeoutCancellationException) {
                    l?.e("${deviceTag()} READ ERROR: OPERATION TIMEOUT $timeoutMs; CHAR ${target.uuid}")
                    continuation.resume(ReadResult.OperationTimeout(timeoutMs))
                    return@post
                } else {
                    callbacks.remove(target)
                    l?.e("${deviceTag()} READ ERROR: OPERATION EXCEPTION; CHAR ${target.uuid}", tr)
                    continuation.resume(ReadResult.OperationException(tr))
                    return@post
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Write something
    //----------------------------------------------------------------------------------------------
    private interface OnCharacteristicWrite : RWCallback {
        fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int)
    }

    private suspend fun write(target: BluetoothGattCharacteristic, bytes: ByteArray, timeout: Long, timeUnit: TimeUnit) : WriteResult {
        val timeoutMs = timeUnit.toMillis(timeout)
        val result: WriteResult
        withTimeout(timeoutMs) {
            result = write(target, bytes, timeoutMs)
        }
        return result
    }

    private suspend fun write(target: BluetoothGattCharacteristic, bytes: ByteArray, timeoutMs: Long)
            : WriteResult = suspendCancellableCoroutine { continuation ->
        mainThreadHandler.post {
            try {
                val writeCallback = object : OnCharacteristicWrite {
                    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        callbacks.remove(target)
                        if (target.uuid == characteristic.uuid) {
                            if (status == GATT_SUCCESS) {
                                l?.i("${deviceTag()} Write ${characteristic.uuid}: ${Hex.toPrettyHexString(bytes)}")
                                continuation.resume(WriteResult.WriteSuccess)
                            } else {
                                l?.e("${deviceTag()} WRITE ERROR : STATUS CODE $status; CHAR ${target.uuid}")
                                continuation.resume(WriteResult.WriteError(status))
                            }
                        } else {
                            l?.e("${deviceTag()} WRITE ERROR : WRONG CALLBACK; CHAR ${target.uuid}")
                            continuation.resume(WriteResult.WrongCharacteristicCallback)
                        }
                    }
                }
                callbacks[target]?.let {
                    continuation.resume(WriteResult.DeviceIsBusy)
                    return@post
                }
                callbacks[target] = writeCallback
                target.value = bytes
                var tryCounter = 0
                while (tryCounter != retryGattOperationsTime) {
                    tryCounter++
                    if (!gatt.writeCharacteristic(target)) {
                        if(tryCounter == retryGattOperationsTime) {
                            l?.e("${deviceTag()} WRITE ERROR : DEVICE IS BUSY; CHAR ${target.uuid}")
                            callbacks.remove(target)
                            continuation.resume(WriteResult.DeviceIsBusy)
                            return@post
                        } else {
                            l?.w("${deviceTag()} WRITE ERROR : DEVICE IS BUSY; CHAR ${target.uuid}, NEXT TRY")
                            Thread.sleep(5)
                        }
                    } else break
                }
            } catch (tr: Throwable) {
                if (tr is TimeoutCancellationException) {
                    callbacks.remove(target)
                    continuation.resume(WriteResult.OperationTimeout(timeoutMs))
                    l?.e("${deviceTag()} WRITE ERROR: OPERATION TIMEOUT $timeoutMs; CHAR ${target.uuid}")
                    return@post
                } else {
                    callbacks.remove(target)
                    l?.e("${deviceTag()} WRITE ERROR: OPERATION EXCEPTION; CHAR ${target.uuid}", tr)
                    continuation.resume(WriteResult.OperationException(tr))
                    return@post
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Internal GattCallback class that hides all callback routine behind scene
    //----------------------------------------------------------------------------------------------
    private inner class GattCallbacks(val connectingTimeout: Long?,
                                      val connectedTimeout: Long?,
                                      val disconnectedTimeout: Long?,
                                      val serviceDiscoveredTimeout: Long?,
                                      val disconnectedEventTimeout: Long?,
                                      val serviceDiscoveryStartTimeout: Long?) : BluetoothGattCallback() {

        private val alreadyConnected = AtomicBoolean(false)
        private val alreadyConnecting = AtomicBoolean(false)
        private val alreadyDisconnecting = AtomicBoolean(false)
        private val alreadyDisconnected = AtomicBoolean(false)

        private val timer = AtomicReference<ConnectionStateTimer?>(null)

        fun startWatchDog() : GattCallbacks {
            connectingTimeout?.let {
                timer.set(newTimer(it))
            }
            return this
        }

        private fun disposeTimer() {
            timer.get()?.dismiss()
        }

        /**
         * Returns new scheduled timer
         */
        private fun newTimer(stageTimeout: Long) = ConnectionStateTimer(stageTimeout) {
            onConnectionStateChange(gatt, GATT_SUCCESS, B_STATE_CONNECTION_STAGE_TIMEOUT)
        }.schedule()

        private fun serviceDiscoveryTimer(stateTimeout: Long) = ConnectionStateTimer(stateTimeout) {
            onConnectionStateChange(gatt, GATT_SUCCESS, B_STATE_SERVICES_DISCOVERY_ERROR)
        }.schedule()

        @Synchronized override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            bgThreadHandler.post {
                deviceState.value.let { previous ->
                    if (status == GATT_SUCCESS) {
                        when (newState) {
                            B_STATE_CONNECTION_STAGE_TIMEOUT -> {
                                l?.i("${deviceTag()} timeout (device not responding).")
                                deviceState.value =
                                        previous.copy(bleConnectionState = BleConnectionState.ConnectionStateTimeout())
                                disposeTimer()
                            }
                            B_STATE_SERVICES_DISCOVERY_ERROR -> {
                                l?.i("${deviceTag()} service discovery timeout (device not responding).")
                                deviceState.value =
                                        previous.copy(bleConnectionState = BleConnectionState.ServicesDiscoveryTimeout())
                                disposeTimer()
                            }
                            B_STATE_DISCONNECTED -> {
                                if (alreadyDisconnected.compareAndSet(false, true)) {
                                    l?.i("${deviceTag()} disconnected.")
                                    disposeTimer()
                                    close()
                                    disconnectedEventTimeout?.let {
                                        Thread.sleep(it)
                                    }
                                    l?.e("Disconnection, closing gatt...")
                                    rssi.value = RSSI_UNKNOWN
                                    deviceState.value =
                                            previous.copy(bleConnectionState = BleConnectionState.Disconnected)
                                } else {
                                    l?.e("STATE_DISCONNECTED CALLED TWICE")
                                }
                            }
                            B_STATE_CONNECTING -> {
                                if (alreadyConnecting.compareAndSet(false, true)) {
                                    disposeTimer()
                                    connectedTimeout?.let {
                                        timer.set(newTimer(it))
                                    }
                                    l?.i("${deviceTag()} connecting...")
                                    deviceState.value =
                                            previous.copy(bleConnectionState = BleConnectionState.Connecting)
                                } else {
                                    l?.e("STATE_CONNECTING CALLED TWICE")
                                }
                            }
                            B_STATE_CONNECTED -> {
                                if (alreadyConnected.compareAndSet(false, true)) {
                                    disposeTimer()
                                    deviceState.value =
                                            previous.copy(bleConnectionState = BleConnectionState.Connected)
                                    serviceDiscoveryStartTimeout?.let {
                                        l?.d("${deviceTag()} connected, waiting some time before starting services discovery [$it ms]")
                                        Thread.sleep(it)
                                    }
                                    l?.i("${deviceTag()} discovering services...")
                                    mainThreadHandler.post {
                                        serviceDiscoveredTimeout?.let {
                                            timer.set(serviceDiscoveryTimer(it))
                                        }
                                        gatt.discoverServices()
                                        deviceState.value =
                                                previous.copy(bleConnectionState = BleConnectionState.ServicesDiscoveryStarted)
                                    }
                                } else {
                                    l?.e("STATE_CONNECTED CALLED TWICE")
                                }
                            }
                            B_STATE_DISCONNECTING -> {
                                if (alreadyDisconnecting.compareAndSet(false, true)) {
                                    disposeTimer()
                                    disconnectedTimeout?.let {
                                        timer.set(newTimer(it))
                                    }
                                    l?.i("${deviceTag()} disconnecting...")
                                    deviceState.value =
                                            previous.copy(bleConnectionState = BleConnectionState.Disconnecting)
                                } else {
                                    l?.e("STATE_DISCONNECTING CALLED TWICE")
                                }
                            }
                            else -> {
                                disposeTimer()
                                l?.e("${deviceTag()} : UNKNOWN STATE: $newState")
                                deviceState.value = previous.copy(bleConnectionState = BleConnectionState.UnknownState(newState))
                                close()
                            }
                        }
                    } else {
                        l?.e("${deviceTag()} : CONNECTION ERROR: STATE = $newState ; STATUS CODE = $status")
                        deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ConnectionStateError(newState, status))
                        disposeTimer()
                        close()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            disposeTimer()
            bgThreadHandler.post {
                deviceState.value.let { previous ->
                    if(status != GATT_SUCCESS) {
                        l?.e("${deviceTag()} : FAILED TO DISCOVER SERVICES : STATUS CODE = $status")
                        deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesDiscoveryError())
                        close()
                        return@post
                    }

                    if(applicationServices.isEmpty()) {
                        registerAllServices(gatt, previous)
                    } else {
                        registerRequiredServices(gatt, previous)
                    }
                }
            }
        }

        private fun registerAllServices(gatt: BluetoothGatt, previous: BleConnection) {
            var id = 1L
            gatt.services.forEach { gattService ->
                l?.d("${deviceTag()} service found: ${gattService.uuid}")
                gattService.characteristics.forEach { deviceCharacteristic ->
                    l?.d("${deviceTag()} characteristic found: ${deviceCharacteristic.uuid}")
                    val read = ServiceType.Read.supportedByCharacteristic(deviceCharacteristic.properties)
                    val write = ServiceType.Write.supportedByCharacteristic(deviceCharacteristic.properties)
                    val notify = ServiceType.Notify.supportedByCharacteristic(deviceCharacteristic.properties)
                    val writeNoResponse = ServiceType.WriteNoResponse.supportedByCharacteristic(deviceCharacteristic.properties)
                    l?.d("${deviceCharacteristic.uuid} : Read enabled = $read")
                    l?.d("${deviceCharacteristic.uuid} : Write enabled = $write")
                    l?.d("${deviceCharacteristic.uuid} : Notifications enabled = $notify")
                    l?.d("${deviceCharacteristic.uuid} : Write no response enabled = $writeNoResponse")

                    characteristics[id.toString()] = deviceCharacteristic
                    val target = TargetCharacteristic(gattService.uuid, deviceCharacteristic.uuid)
                    characteristics[target.toString()] = deviceCharacteristic
                    id++
                }
            }
            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesDiscovered)
        }

        private fun registerRequiredServices(gatt: BluetoothGatt, previous: BleConnection) {
            val servicesMap = RequiredServiceRegistry().apply {
                setFromRequiredServices(applicationServices)
            }

            var featureCount = 0
            var requiredFeatureAmount = 0
            gatt.services.forEach { gattService ->
                l?.d("${deviceTag()} service found: ${gattService.uuid}")
                servicesMap.getCharacteristics(gattService.uuid)?.let { requiredService ->
                    gattService.characteristics.forEach { deviceCharacteristic ->
                        l?.d("${deviceTag()} characteristic found: ${deviceCharacteristic.uuid}")
                        requiredService[deviceCharacteristic.uuid]?.let { requiredCharacteristic ->
                            requiredCharacteristic.types.forEach { serviceType ->
                                requiredFeatureAmount++
                                if(serviceType.supportedByCharacteristic(deviceCharacteristic.properties)) {
                                    l?.d("${deviceTag()} characteristic ${deviceCharacteristic.uuid} supports ${serviceType.javaClass.simpleName}!")
                                    featureCount++
                                } else {
                                    l?.e("${deviceTag()} characteristic ${deviceCharacteristic.uuid} does not support ${serviceType.javaClass.simpleName}!")
                                }
                                characteristics[requiredCharacteristic.id.toString()] = deviceCharacteristic
                                val target = TargetCharacteristic(gattService.uuid, requiredCharacteristic.uuid)
                                characteristics[target.toString()] = deviceCharacteristic
                            }
                        }
                    }
                }
            }
            if(featureCount != requiredFeatureAmount) {
                l?.e("${deviceTag()} NOT ALL SERVICES/CHARACTERISTICS SUPPORTED: REQUIRED FEATURES: ${applicationServices.size}; SUPPORTED FEATURES: $featureCount!")
                l?.e("${deviceTag()} DISCONNECTING!")
                deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesDiscoveryError())
                disconnect()
            } else {
                deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesDiscovered)
            }
        }

        // If it should be added other descriptor writes, this callback should be redesigned
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            l?.i("${deviceTag()} descriptor write callback invoked")
            subscriptionCallback?.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            l?.d("${deviceTag()} onCharacteristicWrite callback : ${characteristic.uuid} : status code = $status")
            callbacks[characteristic]?.let {
                if(it is OnCharacteristicWrite)
                    it.onCharacteristicWrite(gatt, characteristic, status)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            l?.d("${deviceTag()} onCharacteristicRead callback : ${characteristic.uuid} : status code = $status")
            callbacks[characteristic]?.let {
                if(it is OnCharacteristicRead) {
                    it.onCharacteristicRead(gatt, characteristic, status)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            l?.i("${deviceTag()} RSSI update $rssi")
            onReadRSSICallback?.onReadRemoteRssi(gatt, rssi, status)
        }

        @Synchronized override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val value = if(characteristic.value == null) byteArrayOf() else characteristic.value
            l?.i("${deviceTag()} received notification from ${characteristic.uuid} : [${value.size}] ${Hex.toPrettyHexString(value)}")
            l?.e("I AM OM ${Thread.currentThread().name}")
            notificationChannel.sendBlocking(Pair(characteristic, value))
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            l?.d("${deviceTag()} OnMtuChanged callback : mtu = $mtu ; status code = $status")
            mtuCallback?.onMTUChanged(gatt, mtu, status)
        }
    }

    private inner class ConnectionStateTimer(val waitStateTimeoutMs: Long, onStateTimeout: () -> Unit) {
        private val timer = Timer()
        private val timerTask = object : TimerTask() {
            override fun run() = onStateTimeout.invoke()
        }

        fun schedule() : ConnectionStateTimer {
            timer.schedule(timerTask, waitStateTimeoutMs)
            return this
        }

        fun dismiss() = timer.cancel()
    }
}