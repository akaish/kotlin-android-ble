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
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.GATT
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.akaish.kab.BleConstants.Companion.MIN_RSSI_UPDATE_PERIOD
import net.akaish.kab.BleConstants.Companion.MTU_TIMEOUT
import net.akaish.kab.BleConstants.Companion.READ_TIMEOUT
import net.akaish.kab.BleConstants.Companion.RSSI_TIMEOUT
import net.akaish.kab.BleConstants.Companion.SUBSCRIPTION_TIMEOUT
import net.akaish.kab.BleConstants.Companion.WRITE_TIMEOUT
import net.akaish.kab.model.BleConnectionState
import net.akaish.kab.model.ServiceType
import net.akaish.kab.model.TargetCharacteristic
import net.akaish.kab.result.MTUResult
import net.akaish.kab.result.MTUResult.MTUSuccess
import net.akaish.kab.result.ReadResult
import net.akaish.kab.result.SubscriptionResult
import net.akaish.kab.result.WriteResult
import net.akaish.kab.utility.BleLogger
import net.akaish.kab.utility.ILogger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
abstract class AbstractBleDevice(override val disableExceptions: AtomicBoolean,
                                 override val rssiUpdatePeriod: Long? = null,
                                 override val desiredMTU: Int? = null,
                                 override val autoSubscription: Boolean = true,
                                 override val l: ILogger? = BleLogger("Ble"),
                                 override val onBleDeviceDisconnected: OnBleDeviceDisconnected? = null) : IBleScopedDevice {

    /**
     * Instance of GattCallback instance where all magic of flattening callbacks into raw coroutine
     * code is happened.
     */
    private lateinit var facadeImpl: IGattFacade

    private val connectionRequested = AtomicBoolean(false)

    //----------------------------------------------------------------------------------------------
    // Abstract methods
    //----------------------------------------------------------------------------------------------
    /**
     * Do something when connection established (io operations etc) to prepare device for application
     * usage.
     */
    protected abstract suspend fun onReady()

    //----------------------------------------------------------------------------------------------
    // IBleDevice partial implementation
    //----------------------------------------------------------------------------------------------
    // Connection routine
    @Synchronized override fun connect(device: BluetoothDevice, context: Context, transport: Int) {
        check(connectionRequested.compareAndSet(false, true)) { "Duplicate connection request!" }
        check(!isConnected(context)) { "Already connected" }
        l?.d("Connection request (${device.name} @ ${device.address}) [$this]")
        scope = CoroutineScope(job)
        scope.launch(coroutineContext) {
            try {
                facadeImpl = GattFacadeImpl(
                    device = device,
                    l = l,
                    applicationServices = applicationCharacteristics,
                    disableExceptions = disableExceptions,
                    phyLe = desiredPhyLe)
                facadeImpl.connect(context, false, transport)
                facadeImpl.deviceState.collect {
                    if (it.bleConnectionState is BleConnectionState.Disconnected) {
                        withContext(NonCancellable) {
                            facadeImpl.close()
                            delay(200)
                            onBleDeviceDisconnected?.onDeviceDisconnected(this@AbstractBleDevice)
                            job.cancel()
                        }
                        return@collect
                    }
                    if (it.bleConnectionState is BleConnectionState.ConnectionStateError) {
                        withContext(NonCancellable) {
                            facadeImpl.close()
                            delay(200)
                            onBleDeviceDisconnected?.onDeviceDisconnected(this@AbstractBleDevice)
                            job.cancel()
                        }
                        return@collect
                    }
                    if (it.bleConnectionState is BleConnectionState.ServicesSupported) {
                        //------------------------------------------------------------------------------
                        // Subscribing what should be subscribed
                        //------------------------------------------------------------------------------
                        if (autoSubscription) {
                            applicationCharacteristics.forEach { service ->
                                if (service.serviceTypes.contains(ServiceType.Notify)) {
                                    l?.d("Subscribing to [${service.id}] ${service.characteristic}...")
                                    when(val result = facadeImpl.subscribe(service.id, SUBSCRIPTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                                        is SubscriptionResult.SubscriptionSuccess -> l?.i(result.toString())
                                        is SubscriptionResult.OperationException -> l?.e(result.toString(), result.origin)
                                        else -> l?.e(result.toString())
                                    }
                                }
                            }
                        }
                        //------------------------------------------------------------------------------
                        // Setting RSSI update period if necessary
                        //------------------------------------------------------------------------------
                        rssiUpdatePeriod?.let { updatePeriod ->
                            val period = if (updatePeriod > MIN_RSSI_UPDATE_PERIOD) {
                                updatePeriod
                            } else {
                                l?.e("Defined RSSI update period is too short ($updatePeriod), setting min value instead ($MIN_RSSI_UPDATE_PERIOD)")
                                MIN_RSSI_UPDATE_PERIOD
                            }
                            launch {
                                l?.d("Starting RSSI request loop with $period ms delay between iterations...")
                                while (true) {
                                    facadeImpl.readRemoteRSSI(RSSI_TIMEOUT, TimeUnit.MILLISECONDS)
                                    delay(period)
                                }
                            }
                        }
                        //------------------------------------------------------------------------------
                        // Firing MTU request if necessary
                        //------------------------------------------------------------------------------
                        desiredMTU?.let { mtu ->
                            l?.d("Requesting MTU (target value $mtu)...")
                            when(val mtuResult = facadeImpl.requestMTU(mtu, MTU_TIMEOUT, TimeUnit.MILLISECONDS)) {
                                is MTUSuccess -> l?.i(mtuResult.toString())
                                is MTUResult.OperationException -> l?.e(mtuResult.toString(), mtuResult.origin)
                                else -> l?.e(mtuResult.toString())
                            }
                        }
                        onReady()
                        facadeImpl.onReady()
                    }
                }
            } catch (tr: Throwable) {
                tr.printStackTrace()
            }
        }
    }

    @Synchronized override fun disconnect() = facadeImpl.disconnect()

    @Synchronized override fun release() {
        facadeImpl.disconnect()
        facadeImpl.close()
        job.cancel()
    }

    @Synchronized override fun isConnected(context: Context) : Boolean {
        if (this::facadeImpl.isInitialized) {
            facadeImpl.getGatt()?.let { gatt ->
                val androidBleManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                var connected = false
                androidBleManager.getConnectedDevices(GATT).forEach {
                    if (it.address == gatt.device.address)
                        connected = true
                }
                return connected
            } ?: run {
                return false
            }
        } else {
            return false
        }
    }
    // Device communication
    override fun notificationChannel() = facadeImpl.notificationChannel

    override suspend fun write(target: Long, bytes: ByteArray, timeoutMS: Long?) =
        facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)

    override suspend fun write(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?) =
        facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)

    override fun writeBlocking(target: Long, bytes: ByteArray, timeoutMS: Long?) : WriteResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    override fun writeBlocking(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?) : WriteResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    override suspend fun read(target: Long, timeoutMS: Long?) = facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)

    override suspend fun read(target: TargetCharacteristic, timeoutMS: Long?) = facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)

    override fun readBlocking(target: Long, timeoutMS: Long?) : ReadResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    override fun readBlocking(target: TargetCharacteristic, timeoutMS: Long?) : ReadResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    override fun gattFacade(): IGattFacade = facadeImpl
    //----------------------------------------------------------------------------------------------
    // Scoped device implementation
    //----------------------------------------------------------------------------------------------
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
    override lateinit var scope: CoroutineScope

    override fun deviceReady() = this::facadeImpl.isInitialized
}