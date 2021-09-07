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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import net.akaish.kab.utility.BleAddressUtil.generateDeviceId
import net.akaish.kab.utility.ILogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class BleScanner(private val deviceForgetTimeoutMs: Long? = null,
                 private val deviceAutoRemovePeriodMs: Long? = null,
                 private val devicePrefixMap: Map<String, ByteArray>? = null,
                 private val l: ILogger? = null,
                 private val emissionBackPressure: Long = TimeUnit.SECONDS.toMillis(1)) {

    init {
        deviceForgetTimeoutMs?.let {
            require(it > 0) { "Device forget timeout can not be negative" }
        }
        deviceAutoRemovePeriodMs?.let {
            require(it > 0) { "Device auto remove period can not be negative" }
        }
        devicePrefixMap?.let {
            it.entries.forEach { entry ->
                require(entry.value.size == 2) { "Prefix for ${entry.key} must be 2 bytes, but provided prefix has ${entry.value.size} bytes length!" }
            }
        }
    }

    companion object {

        const val DEVICE_FORGET_TIMEOUT_MS = 8_000L
        const val DEVICE_AUTO_REMOVE_PERIOD_MS = 2_000L
        private val defaultPrefix = byteArrayOf(0x00, 0x00)

        fun toLeFilters(filters: List<Filter>) : List<ScanFilter> = ArrayList<ScanFilter>().apply {
            filters.forEach {
                add(it.asScanFilter())
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Filter adapter
    //----------------------------------------------------------------------------------------------
    sealed class Filter {

        abstract fun filter(device: BluetoothDevice) : Boolean
        abstract fun asScanFilter() : ScanFilter

        data class NameFilter(val name: String, val ignoreCase: Boolean = false) : Filter() {

            override fun filter(device: BluetoothDevice) = name.equals(device.name, ignoreCase)

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun asScanFilter() : ScanFilter = ScanFilter.Builder().setDeviceName(name).build()
        }

        @Suppress("Unused")
        data class AddressFilter(val address: String) : Filter() {

            override fun filter(device: BluetoothDevice) = address == device.address

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun asScanFilter() : ScanFilter = ScanFilter.Builder().setDeviceAddress(address).build()
        }

        @Suppress("Unused")
        data class UUIDFilter(val uuidList: List<UUID>) : Filter() {

            override fun filter(device: BluetoothDevice) : Boolean {
                var implementationCount = 0
                uuidList.forEach { requestedUUID ->
                    device.uuids.forEach next@{ deviceUUID ->
                        if(deviceUUID.uuid == requestedUUID) {
                            implementationCount++
                            return@next
                        }
                    }
                }
                return implementationCount == uuidList.size
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun asScanFilter() = throw NotImplementedError("Not implemented")
        }
    }

    //----------------------------------------------------------------------------------------------
    // Callbacks
    //----------------------------------------------------------------------------------------------
    private var minRssiToEmit = -127

    private fun generateId(name: String, address: String)  = if(devicePrefixMap != null) {
        generateDeviceId(devicePrefixMap[name] ?: defaultPrefix, address)
    } else {
        generateDeviceId(defaultPrefix, address)
    }

    inner class CompatScanCallback(private val scanFilters: List<Filter>) : BluetoothAdapter.LeScanCallback{
        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            device?.let { bleDevice ->
                scanFilters.forEach { filter ->
                    try {
                        if (filter.filter(bleDevice)) {
                            if(rssi >= minRssiToEmit) {
                                val foundItem = FoundDevice(
                                        id = generateId(bleDevice.name, bleDevice.address),
                                        name = bleDevice.name,
                                        address = bleDevice.address,
                                        rssi = rssi,
                                        timestamp = System.currentTimeMillis()
                                )
                                deviceChannel.offer(foundItem)
                                onRawScanResultListener?.onRawScanResult(foundItem)
                            }
                        }
                    } catch (tr: Throwable) {
                        l?.e("Exception caught", tr)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    inner class LEScanCallback : ScanCallback() {

        init {
            l?.i("LEScanCallback created")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { bleDevice ->
                try {
                    if(result.rssi >= minRssiToEmit) {
                        val foundItem = FoundDevice(
                                id = generateId(bleDevice.name, bleDevice.address),
                                name = bleDevice.name,
                                address = bleDevice.address,
                                rssi = result.rssi,
                                timestamp = System.currentTimeMillis()
                        )
                        deviceChannel.offer(foundItem)
                        onRawScanResultListener?.onRawScanResult(foundItem)
                    } else Unit
                } catch (tr: Throwable) {
                    l?.e("Exception caught", tr)
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            results?.forEach { scanResult ->
                scanResult?.device?.let { bleDevice ->
                    try {
                        if(scanResult.rssi >= minRssiToEmit) {
                            val foundItem = FoundDevice(
                                    id = generateId(bleDevice.name, bleDevice.address),
                                    name = bleDevice.name,
                                    address = bleDevice.address,
                                    rssi = scanResult.rssi,
                                    timestamp = System.currentTimeMillis()
                            )
                            deviceChannel.offer(foundItem)
                            onRawScanResultListener?.onRawScanResult(foundItem)
                        }
                    } catch (tr: Throwable) {
                        l?.e("Exception caught", tr)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            l?.e("Failed to start scanning: error code is $errorCode!")
            if(errorCode != 0)
                throw IllegalStateException("LeScanner failed with code $errorCode!")
        }
    }

    //----------------------------------------------------------------------------------------------
    // Batching results and working with coroutines
    //----------------------------------------------------------------------------------------------
    data class FoundDevice(val id: Long,
                           val name: String,
                           val address: String,
                           val rssi: Int,
                           val timestamp: Long)

    data class Result(val timestamp: Long, val foundDevices: List<FoundDevice>)

    private val results = ConcurrentLinkedQueue<FoundDevice>()
    private val ignoredDevices = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    interface OnScanResult {
        fun onScanResult(result: Result)
    }

    interface OnRawScanResult {
        fun onRawScanResult(device: FoundDevice)
    }

    @Volatile private var onScanResultListener: OnScanResult? = null
    @Volatile private var onRawScanResultListener: OnRawScanResult? = null

    fun setScanResultListener(scanResultListener: OnScanResult) = apply {
        this.onScanResultListener = scanResultListener
    }

    fun setRawScanResultListener(rawScanResultListener: OnRawScanResult) = apply {
        this.onRawScanResultListener = rawScanResultListener
    }

    @Suppress("Unused")
    val scanResultsChannel = BroadcastChannel<Result>(1)
    @Suppress("Unused")
    val isRunning = MutableStateFlow(false)

    private fun emitResults() {
        val result = Result(System.currentTimeMillis(), results.toList().sortedBy { it.rssi })
        onScanResultListener?.onScanResult(result)
        scanResultsChannel.offer(result)
    }

    private fun removeTimedOutItems() {
        results.removeAll { existingItem ->
            existingItem.timestamp + (deviceForgetTimeoutMs ?: DEVICE_FORGET_TIMEOUT_MS) < System.currentTimeMillis()
                    || ignoredDevices.contains(existingItem.address)
        }
        emitResults()
    }

    fun resetIgnoredAddress() = ignoredDevices.clear()

    fun addIgnoredAddress(address: String) : Boolean {
        ignoredDevices.add(address)
        val out = results.removeAll { ignoredDevices.contains(it.address) }
        emitResults()
        return out
    }

    private lateinit var job: Job

    private lateinit var coroutineContext: CoroutineContext
    private lateinit var receiveDeviceChannel: ReceiveChannel<FoundDevice>

    private fun startResultsEmission() {
        job = SupervisorJob()
        coroutineContext = job + Dispatchers.IO
        try {
            val scope = CoroutineScope(coroutineContext)
            scope.launch(coroutineContext) {
                while (true) {
                    delay(deviceAutoRemovePeriodMs ?: DEVICE_AUTO_REMOVE_PERIOD_MS)
                    removeTimedOutItems()
                }
            }
            scope.launch(coroutineContext) {
                if(this@BleScanner::receiveDeviceChannel.isInitialized)
                    receiveDeviceChannel.cancel()
                receiveDeviceChannel = deviceChannel.openSubscription()
                receiveDeviceChannel.receiveAsFlow().conflate().collect { newItem ->
                    results.removeAll { existingItem ->
                        existingItem.timestamp + (deviceForgetTimeoutMs ?: DEVICE_FORGET_TIMEOUT_MS) < System.currentTimeMillis()
                                || (newItem.address == existingItem.address && newItem.name == existingItem.name)
                                || ignoredDevices.contains(newItem.address)
                    }
                    l?.d("Found device: ${newItem.name} @ ${newItem.address}")
                    results.add(newItem)
                    emitResults()
                    delay(emissionBackPressure)
                }
            }
            isRunning.value = true
        } catch (tr: Throwable) {
            stopResultsEmission()
        }
    }

    private fun stopResultsEmission() {
        isRunning.value = false
        if(this::job.isInitialized)
            job.cancel()
    }

    //----------------------------------------------------------------------------------------------
    // Implementation
    //----------------------------------------------------------------------------------------------
    val deviceChannel = BroadcastChannel<FoundDevice>(1)
    private val mutex = Semaphore(1)
    private val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var compatCallback: CompatScanCallback? = null
    private var leCallback: LEScanCallback? = null

    fun startScan(scanFilters: List<Filter>, scanSettings: ScanSettings? = null, minRssiToEmit: Int = -127) : Boolean {
        this.minRssiToEmit = minRssiToEmit
        try {
            mutex.acquire()
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CompatScanCallback(scanFilters).let {
                    compatCallback = it
                    @Suppress("Deprecation")
                    adapter.startLeScan(it)
                }
            } else {
                leCallback?.let {
                    stopResultsEmission()
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.stopScan(LEScanCallback())
                }

                LEScanCallback().let {
                    leCallback = it
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.startScan(toLeFilters(scanFilters), scanSettings,  it)
                }
            }
            startResultsEmission()
            return true
        } catch (tr: Throwable) {
            l?.e("Failed to start LE scan", tr)
            stopScannerNotSynchronized()
            return false
        } finally {
            mutex.release()
        }
    }

    private fun stopScannerNotSynchronized() {
        try {
            mutex.acquire()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                compatCallback?.let {
                    @Suppress("Deprecation")
                    adapter.stopLeScan(it)
                    stopResultsEmission()
                }
                l?.d("Scanner compat callback instance does not exist.")
            } else {
                leCallback?.let {
                    stopResultsEmission()
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.stopScan(LEScanCallback())
                }
                l?.d("Scanner le callback instance does not exist.")
            }
        } catch (tr: Throwable) {
            l?.e("Exception caught while stopping le scan", tr)
        }
    }

    fun stopScan() : Boolean {
        try {
            mutex.acquire()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                compatCallback?.let {
                    stopResultsEmission()
                    @Suppress("Deprecation")
                    adapter.stopLeScan(it)
                }
                l?.d("Scanner compat callback instance does not exist.")
            } else {
                leCallback?.let {
                    stopResultsEmission()
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.stopScan(LEScanCallback())
                }
                l?.d("Scanner le callback instance does not exist.")
            }
        } catch (tr: Throwable) {
            l?.e("Exception caught while stopping le scan", tr)
        } finally {
            mutex.release()
            return true
        }
    }
}