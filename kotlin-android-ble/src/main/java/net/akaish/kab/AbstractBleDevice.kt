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
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.GATT
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.akaish.kab.model.BleConnectionState
import net.akaish.kab.model.ApplicationCharacteristic
import net.akaish.kab.model.ServiceType
import net.akaish.kab.model.TargetCharacteristic
import net.akaish.kab.result.MTUResult
import net.akaish.kab.result.MTUResult.MTUSuccess
import net.akaish.kab.result.ReadResult
import net.akaish.kab.result.SubscriptionResult
import net.akaish.kab.result.WriteResult
import net.akaish.kab.utility.BleLogger
import net.akaish.kab.utility.ILogger
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
abstract class AbstractBleDevice(context: Context,
                                 private val rssiUpdatePeriod: Long? = null,
                                 private val desiredMTU: Int? = null,
                                 private val autoSubscription: Boolean = true,
                                 protected val l: ILogger? = BleLogger("Ble")) : IBleDevice {

    private var gatt: BluetoothGatt? = null
    private val androidBleManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

    /**
     * Instance of GattCallback instance where all magic of flattening callbacks into raw coroutine
     * code is happened.
     */
    private lateinit var facadeImpl: IGattFacade

    //----------------------------------------------------------------------------------------------
    // Abstract methods
    //----------------------------------------------------------------------------------------------
    /**
     * Do something when connection established (io operations etc) to prepare device for application
     * usage.
     */
    protected abstract suspend fun onReady()

    @Synchronized override fun connect(device: BluetoothDevice, context: Context) {
        check(!connectionRequested.get()) { "Duplicate connection request!" }
        check(!isConnected()) { "Already connected" }
        connectionRequested.set(true)
        l?.d("Connection request (${device.name} @ ${device.address})")
        scope = CoroutineScope(job)
        scope.launch(coroutineContext) {
            try {
                facadeImpl = GattFacadeImpl(device = device,
                    l = l, applicationServices = applicationCharacteristics)
                gatt = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, facadeImpl.bluetoothGattCallback, TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, facadeImpl.bluetoothGattCallback) // TRANSPORT_AUTO =\
                }
                facadeImpl.deviceState.collect {
                    if (it.bleConnectionState is BleConnectionState.Disconnected) {
                        gatt?.close()
                        withContext(NonCancellable) {
                            onDeviceDisconnected?.let { action ->
                                action.onDeviceDisconnected(this@AbstractBleDevice)
                                onDeviceDisconnected = null
                            }
                        }
                        job.cancel()
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

    //----------------------------------------------------------------------------------------------
    // IBleDevice partial implementation
    //----------------------------------------------------------------------------------------------


    companion object {
        const val BATTERY_ID = 0x1001L
        val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_CHARACTERISTIC = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val APPLICATION_BATTERY_SERVICE = ApplicationCharacteristic(
                BATTERY_SERVICE,
                BATTERY_CHARACTERISTIC,
                mutableListOf(ServiceType.Read),
                BATTERY_ID
        )

        const val MIN_RSSI_UPDATE_PERIOD = 500L

        const val SUBSCRIPTION_TIMEOUT = 1000L
        const val MTU_TIMEOUT = 1000L
        const val RSSI_TIMEOUT = 1000L
        const val WRITE_TIMEOUT = 1000L
        const val READ_TIMEOUT = 1000L
    }

    private val job = SupervisorJob()
    protected val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO
    protected lateinit var scope: CoroutineScope

    protected fun callbackReady() = this::facadeImpl.isInitialized

    interface OnDeviceDisconnected {
        fun onDeviceDisconnected(instance: AbstractBleDevice)
    }

    private var onDeviceDisconnected: OnDeviceDisconnected? = null

    fun setOnDeviceDisconnectedCallback(onDeviceDisconnected: OnDeviceDisconnected?) {
        this.onDeviceDisconnected = onDeviceDisconnected
    }

    private val connectionRequested = AtomicBoolean(false)

    @Synchronized fun disconnect() {
        gatt?.disconnect() ?: run {
            l?.w("No device connected but disconnect request received [gatt.disconnect()]")
        }
        gatt?.close() ?: run {
            l?.w("No device connected but disconnect request received [gatt.close()]")
        }
    }

    @Synchronized fun release() {
        job.cancel()
        gatt?.disconnect()
        gatt?.close()
    }

    @Synchronized override fun isConnected() : Boolean {
        gatt?.let { gatt ->
            androidBleManager.getConnectedDevices(GATT).forEach {
                if(it.address == gatt.device.address)
                    return true
            }
            return false
        } ?: run {
            return false
        }
    }

    //----------------------------------------------------------------------------------------------
    // Device communication
    //----------------------------------------------------------------------------------------------
    suspend fun write(target: Long, bytes: ByteArray, timeoutMS: Long?) =
            facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)

    suspend fun write(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?) =
        facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)

    fun writeBlocking(target: Long, bytes: ByteArray, timeoutMS: Long?) : WriteResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    fun writeBlocking(target: TargetCharacteristic, bytes: ByteArray, timeoutMS: Long?) : WriteResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.write(target, bytes, timeoutMS ?: WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    suspend fun read(target: Long, timeoutMS: Long?) = facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)

    suspend fun read(target: TargetCharacteristic, timeoutMS: Long?) = facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)

    fun readBlocking(target: Long, timeoutMS: Long?) : ReadResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    fun readBlocking(target: TargetCharacteristic, timeoutMS: Long?) : ReadResult {
        return runBlocking(coroutineContext) {
            return@runBlocking facadeImpl.read(target, timeoutMS ?: READ_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    fun notificationChannel() = facadeImpl.notificationChannel
}