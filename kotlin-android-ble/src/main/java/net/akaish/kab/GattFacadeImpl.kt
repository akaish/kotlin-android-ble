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
import android.bluetooth.BluetoothGatt.*
import android.os.Build
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
import net.akaish.kab.model.*
import net.akaish.kab.result.*
import net.akaish.kab.utility.Hex
import net.akaish.kab.utility.ILogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap
import kotlin.coroutines.resume

@ExperimentalCoroutinesApi
class GattFacadeImpl(device: BluetoothDevice,
                     override val disableExceptions: AtomicBoolean,
                     override val l: ILogger? = null,
                     override val applicationServices: MutableList<ApplicationCharacteristic> = mutableListOf()) : IGattFacade {

    //----------------------------------------------------------------------------------------------
    // Gatt facade implementation
    //----------------------------------------------------------------------------------------------
    override val bluetoothGattCallback: BluetoothGattCallback = GattCallbacks()

    override val rssi = MutableStateFlow(RSSI_UNKNOWN)

    override val mtu = MutableStateFlow(MTU_MIN)

    override val deviceState = MutableStateFlow(BleConnection(
        deviceName = device.name,
        deviceUid = device.address))

    override fun onReady() {
        l?.i("${deviceTag()} device ready to use!")
        deviceState.value.let { previous ->
            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.Ready(previous.bleConnectionState.stateId, GATT_SUCCESS))
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
            this.mtu.value = MTU_DEFAULT
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

    override val notificationChannel = BroadcastChannel<Pair<BluetoothGattCharacteristic, ByteArray>>(1)

    override suspend fun subscribe(target: Long, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult {
        val result = subscribe(characteristics[target.toString()]!!, timeout, timeUnit)
        result.throwException(disableExceptions.get())
        return result
    }

    override suspend fun subscribe(target: TargetCharacteristic, timeout: Long, timeUnit: TimeUnit) : SubscriptionResult {
        val result = subscribe(characteristics[target.toString()]!!, timeout, timeUnit)
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
    private lateinit var gatt: BluetoothGatt

    private fun deviceTag() = "${deviceState.value.deviceName} @ ${deviceState.value.deviceUid}"

    private val characteristics = HashMap<String, BluetoothGattCharacteristic>()

    private fun registerRequiredServices(gatt: BluetoothGatt, status: Int, previous: BleConnection) {
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
            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServiceDiscoveryError(previous.bleConnectionState.stateId, status))
            gatt.disconnect()
        } else {
            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesSupported(previous.bleConnectionState.stateId, status))
        }
    }

    private fun registerAllServices(gatt: BluetoothGatt, status: Int, previous: BleConnection) {
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
        deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServicesSupported(previous.bleConnectionState.stateId, status))
    }

    //----------------------------------------------------------------------------------------------
    // RSSI
    //----------------------------------------------------------------------------------------------
    private interface OnReadRemoteRSSI {
        fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int)
    }

    @Volatile private var onReadRSSICallback: OnReadRemoteRSSI? = null

    private suspend fun readRemoteRSSI(timeoutMs: Long)
        : RSSIResult = suspendCancellableCoroutine { continuation ->
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
            if(!gatt.readRemoteRssi()) {
                onReadRSSICallback = null
                l?.e("${deviceTag()} FAILED TO READ RSSI: DEVICE IS BUSY")
                continuation.resume(RSSIResult.DeviceIsBusy)
                return@suspendCancellableCoroutine
            }
        } catch (tr: Throwable) {
            onReadRSSICallback = null
            if (tr is TimeoutCancellationException) {
                l?.e("${deviceTag()} FAILED TO READ RSSI: OPERATION TIMEOUT $timeoutMs")
                continuation.resume(RSSIResult.OperationTimeout(timeoutMs))
                return@suspendCancellableCoroutine
            } else {
                l?.e("${deviceTag()} FAILED TO READ RSSI: OPERATION EXCEPTION", tr)
                continuation.resume(RSSIResult.OperationException(tr))
                tr.printStackTrace()
                return@suspendCancellableCoroutine
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
        if (!gatt.setCharacteristicNotification(characteristic, true)) {
            l?.e("${deviceTag()} FAILED TO SET SUBSCRIPTION FLAG TO ${characteristic.uuid}!")
            continuation.resume(SubscriptionResult.SubscriptionError(-228))
            return@suspendCancellableCoroutine
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
                return@suspendCancellableCoroutine
            }
        } catch (tr: Throwable) {
            subscriptionCallback = null
            if (tr is TimeoutCancellationException) {
                l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid}: OPERATION TIMEOUT $timeoutMs")
                continuation.resume(SubscriptionResult.OperationTimeout(timeoutMs))
                return@suspendCancellableCoroutine
            } else {
                l?.e("${deviceTag()} FAILED TO SUBSCRIBE TO ${characteristic.uuid}: OPERATION EXCEPTION", tr)
                continuation.resume(SubscriptionResult.OperationException(tr))
                return@suspendCancellableCoroutine
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
        try {
            require(desiredMTU in MTU_MIN..MTU_MAX)
            mtuCallback = object : OnMTUChanged {
                override fun onMTUChanged(gatt: BluetoothGatt, mtu1: Int, status: Int) {
                    mtuCallback = null
                    if (status == GATT_SUCCESS) {
                        mtu.value = mtu1
                        l?.i("${deviceTag()} MTU callback invoked (value $mtu1)")
                        continuation.resume(MTUResult.MTUSuccess(mtu1))
                    } else {
                        l?.e("${deviceTag()} MTU ERROR : STATUS CODE $status")
                        continuation.resume(MTUResult.MTUError(status))
                    }
                }
            }
            if(!gatt.requestMtu(desiredMTU)) {
                mtuCallback = null
                l?.e("${deviceTag()} MTU ERROR : DEVICE IS BUSY")
                continuation.resume(MTUResult.DeviceIsBusy)
                return@suspendCancellableCoroutine
            }
        } catch (tr: Throwable) {
            if(tr is TimeoutCancellationException) {
                mtuCallback = null
                l?.e("${deviceTag()} FAILED TO EXECUTE MTU REQUEST: OPERATION TIMEOUT $timeoutMs")
                continuation.resume(MTUResult.OperationTimeout(timeoutMs))
                return@suspendCancellableCoroutine
            } else {
                mtuCallback = null
                l?.e("${deviceTag()} FAILED TO EXECUTE MTU REQUEST: OPERATION EXCEPTION", tr)
                continuation.resume(MTUResult.OperationException(tr))
                return@suspendCancellableCoroutine
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
                return@suspendCancellableCoroutine
            }
            callbacks[target] = callback
            if(!gatt.readCharacteristic(target)) {
                callbacks.remove(target)
                l?.e("${deviceTag()} READ ERROR : DEVICE IS BUSY; CHAR ${target.uuid}")
                continuation.resume(ReadResult.DeviceIsBusy)
                return@suspendCancellableCoroutine
            }
        } catch (tr: Throwable) {
            callbacks.remove(target)
            if(tr is TimeoutCancellationException) {
                l?.e("${deviceTag()} READ ERROR: OPERATION TIMEOUT $timeoutMs; CHAR ${target.uuid}")
                continuation.resume(ReadResult.OperationTimeout(timeoutMs))
                return@suspendCancellableCoroutine
            } else {
                callbacks.remove(target)
                l?.e("${deviceTag()} READ ERROR: OPERATION EXCEPTION; CHAR ${target.uuid}", tr)
                continuation.resume(ReadResult.OperationException(tr))
                return@suspendCancellableCoroutine
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
                return@suspendCancellableCoroutine
            }
            callbacks[target] = writeCallback
            target.value = bytes
            if(!gatt.writeCharacteristic(target)) {
                l?.e("${deviceTag()} WRITE ERROR : DEVICE IS BUSY; CHAR ${target.uuid}")
                callbacks.remove(target)
                continuation.resume(WriteResult.DeviceIsBusy)
                return@suspendCancellableCoroutine
            }
        } catch (tr: Throwable) {
            if(tr is TimeoutCancellationException) {
                callbacks.remove(target)
                continuation.resume(WriteResult.OperationTimeout(timeoutMs))
                l?.e("${deviceTag()} WRITE ERROR: OPERATION TIMEOUT $timeoutMs; CHAR ${target.uuid}")
                return@suspendCancellableCoroutine
            } else {
                callbacks.remove(target)
                l?.e("${deviceTag()} WRITE ERROR: OPERATION EXCEPTION; CHAR ${target.uuid}", tr)
                continuation.resume(WriteResult.OperationException(tr))
                return@suspendCancellableCoroutine
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Internal GattCallback class that hides all callback routine behind scene
    //----------------------------------------------------------------------------------------------
    private inner class GattCallbacks : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(!this@GattFacadeImpl::gatt.isInitialized)
                this@GattFacadeImpl.gatt = gatt
            deviceState.value.let { previous ->
                if(status == GATT_SUCCESS) {
                    when (newState) {
                        STATE_DISCONNECTED -> {
                            l?.i("${deviceTag()} disconnected.")
                            gatt.close()
                            Thread.sleep(600)
                            l?.e("Disconnection, closing gatt...")
                            rssi.value = RSSI_UNKNOWN
                            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.Disconnected)
                        }
                        STATE_CONNECTING -> {
                            l?.i("${deviceTag()} connecting...")
                            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.Connecting)
                        }
                        STATE_CONNECTED -> {
                            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.Connected)
                            l?.d("${deviceTag()} connected, waiting some time before starting services discovery")
                            Thread.sleep(400L)
                            l?.i("${deviceTag()} discovering services...")
                            gatt.discoverServices()
                        }
                        STATE_DISCONNECTING -> {
                            l?.i("${deviceTag()} disconnecting...")
                            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.Disconnecting)
                        }
                        else -> {
                            l?.e("${deviceTag()} : UNKNOWN STATE: $newState")
                            deviceState.value = previous.copy(bleConnectionState = BleConnectionState.UnknownState(newState))
                            gatt.disconnect()
                        }
                    }
                } else {
                    l?.e("${deviceTag()} : CONNECTION ERROR: STATE = $newState ; STATUS CODE = $status")
                    deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ConnectionStateError(newState, status))
                    gatt.disconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            deviceState.value.let { previous ->
                if(status != GATT_SUCCESS) {
                    l?.e("${deviceTag()} : FAILED TO DISCOVER SERVICES : STATUS CODE = $status")
                    deviceState.value = previous.copy(bleConnectionState = BleConnectionState.ServiceDiscoveryError(-1, status))
                    gatt.disconnect()
                    return
                }

                if(applicationServices.isEmpty()) {
                    registerAllServices(gatt, status, previous)
                } else {
                    registerRequiredServices(gatt, status, previous)
                }
            }
        }

        // If it should be added other descriptor writes, this callback should be redesigned
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            l?.i("${deviceTag()} descriptor write callback invoked")
            subscriptionCallback?.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            l?.d("${deviceTag()} onCharacteristicWrite callback : ${characteristic.uuid} : status code = $status")
            callbacks[characteristic]?.let {
                if(it is OnCharacteristicWrite)
                    it.onCharacteristicWrite(gatt, characteristic, status)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
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

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = if(characteristic.value == null) byteArrayOf() else characteristic.value
            l?.d("${deviceTag()} received notification from ${characteristic.uuid} : [${value.size}] ${Hex.toPrettyHexString(value)}")
            notificationChannel.sendBlocking(Pair(characteristic, value))
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            l?.d("${deviceTag()} OnMtuChanged callback : mtu = $mtu ; status code = $status")
            mtuCallback?.onMTUChanged(gatt, mtu, status)
        }
    }
}