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
package net.akaish.kab.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.collect
import net.akaish.kab.model.FoundBleDevice
import net.akaish.kab.model.ScanResult
import net.akaish.kab.model.ScannerState
import net.akaish.kab.scanner.IBleScanner.Companion.DEVICE_AUTO_REMOVE_PERIOD_MS
import net.akaish.kab.scanner.IBleScanner.Companion.DEVICE_FORGET_TIMEOUT_MS
import net.akaish.kab.scanner.IBleScanner.Companion.defaultPrefix
import net.akaish.kab.utility.BleAddressUtil.generateDeviceId
import net.akaish.kab.utility.ILogger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
@Suppress("Unused")
class BleScanner(
    @IntRange(from = 0, to = Long.MAX_VALUE)
    override val deviceForgetTimeoutMs: Long = DEVICE_FORGET_TIMEOUT_MS,
    @IntRange(from = 0, to = Long.MAX_VALUE)
    override val deviceAutoRemovePeriodMs: Long = DEVICE_AUTO_REMOVE_PERIOD_MS,
    override val devicePrefixMap: Map<String, ByteArray> = HashMap(),
    override val l: ILogger? = null,
    @IntRange(from = Byte.MIN_VALUE.toLong(), to = Byte.MAX_VALUE.toLong())
    override val emissionBackPressure: Long = TimeUnit.SECONDS.toMillis(1)) : IBleScanner {

    init {
        devicePrefixMap.entries.forEach { entry ->
            require(entry.value.size == 2) { "Prefix for ${entry.key} must be 2 bytes, but provided prefix has ${entry.value.size} bytes length!" }
        }
    }

    companion object {

        fun toLeFilters(filters: List<BleScanFilter>) : List<ScanFilter> = ArrayList<ScanFilter>().apply {
            filters.forEach {
                add(it.asScanFilter())
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // IBleScanner implementation
    //----------------------------------------------------------------------------------------------
    override fun resetIgnoredAddresses() {
        ignoredDevices.clear()
    }

    override fun addIgnoredAddress(address: String) : Boolean {
        if(ignoredDevices.contains(address))
            return false
        ignoredDevices.add(address)
        val out = results.removeAll { ignoredDevices.contains(it.address) }
        emitResults()
        return out
    }

    override fun addIgnoredAddresses(addresses: List<String>) : Boolean {
        var updateRequired = false
        addresses.forEach {
            if(!ignoredDevices.contains(it)) {
                ignoredDevices.add(it)
                updateRequired = true
            }
        }
        if(!updateRequired)
            return false
        val out = results.removeAll { ignoredDevices.contains(it.address) }
        emitResults()
        return out
    }

    override val scanResultsChannel = BroadcastChannel<ScanResult>(1)

    override val rawScanResultChannel = BroadcastChannel<FoundBleDevice>(1)

    override val isScanning = MutableStateFlow<ScannerState>(ScannerState.Idle)

    override fun setScanResultListener(scanResultListener: OnScanResult) = apply {
        this.onScanResultListener = scanResultListener
    }

    override fun setRawScanResultListener(rawScanResultListener: OnRawScanResult) = apply {
        this.onRawScanResultListener = rawScanResultListener
    }

    //----------------------------------------------------------------------------------------------
    // Callbacks
    //----------------------------------------------------------------------------------------------
    private var minRssiToEmit = -127

    private fun generateId(name: String?, address: String) : Long {
        val prefix = if(name == null) {
            defaultPrefix
        } else {
            devicePrefixMap[name] ?: defaultPrefix
        }
        return generateDeviceId(prefix, address)
    }

    internal inner class CompatScanCallback(private val scanFilters: List<BleScanFilter>) : BluetoothAdapter.LeScanCallback{
        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            device?.let { bleDevice ->
                scanFilters.forEach { filter ->
                    try {
                        if (filter.filter(bleDevice)) {
                            if(rssi >= minRssiToEmit) {
                                val foundItem = FoundBleDevice(
                                    id = generateId(bleDevice.name, bleDevice.address),
                                    name = bleDevice.name,
                                    address = bleDevice.address,
                                    rssi = rssi,
                                    timestamp = System.currentTimeMillis(),
                                    remoteDevice = bleDevice
                                )
                                rawScanResultChannel.offer(foundItem)
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
    internal inner class LEScanCallback : ScanCallback() {

        init {
            l?.i("LEScanCallback created")
        }

        override fun onScanResult(callbackType: Int, scanResult: android.bluetooth.le.ScanResult?) {
            scanResult?.device?.let { bleDevice ->
                try {
                    if(scanResult.rssi >= minRssiToEmit) {
                        val foundItem = FoundBleDevice(
                            id = generateId(bleDevice.name, bleDevice.address),
                            name = bleDevice.name,
                            address = bleDevice.address,
                            rssi = scanResult.rssi,
                            timestamp = System.currentTimeMillis(),
                            remoteDevice = bleDevice
                        )
                        rawScanResultChannel.offer(foundItem)
                        onRawScanResultListener?.onRawScanResult(foundItem)
                    } else Unit
                } catch (tr: Throwable) {
                    l?.e("Exception caught", tr)
                }
            }
        }

        override fun onBatchScanResults(results: List<android.bluetooth.le.ScanResult?>?) {
            results?.forEach { scanResult ->
                scanResult?.device?.let { bleDevice ->
                    try {
                        if(scanResult.rssi >= minRssiToEmit) {
                            val foundItem = FoundBleDevice(
                                id = generateId(bleDevice.name, bleDevice.address),
                                name = bleDevice.name,
                                address = bleDevice.address,
                                rssi = scanResult.rssi,
                                timestamp = System.currentTimeMillis(),
                                remoteDevice = bleDevice
                            )
                            rawScanResultChannel.offer(foundItem)
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
            if(errorCode != 0) {
                isScanning.value = ScannerState.ScannerError(errorCode)
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // Batching results and working with coroutines
    //----------------------------------------------------------------------------------------------
    private val results = ConcurrentLinkedQueue<FoundBleDevice>()
    private val ignoredDevices = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    @Volatile private var onScanResultListener: OnScanResult? = null
    @Volatile private var onRawScanResultListener: OnRawScanResult? = null

    private fun emitResults() {
        try {
            val result =
                    ScanResult(System.currentTimeMillis(), results.toList().sortedBy { it.rssi })
            onScanResultListener?.onScanResult(result)
            scanResultsChannel.offer(result)
        } catch (tr: Throwable) {
            l?.i("emitResults() : skipping emission", tr)
        }
    }

    private fun removeTimedOutItems() {
        results.removeAll { existingItem ->
            existingItem.timestamp + deviceForgetTimeoutMs < System.currentTimeMillis()
                    || ignoredDevices.contains(existingItem.address)
        }
        emitResults()
    }

    private lateinit var job: Job
    private lateinit var coroutineContext: CoroutineContext
    private lateinit var receiveBleDeviceChannel: ReceiveChannel<FoundBleDevice>

    private fun startResultsEmission() {
        job = SupervisorJob()
        coroutineContext = job + Dispatchers.IO
        try {
            val scope = CoroutineScope(coroutineContext)
            scope.launch(coroutineContext) {
                while (true) {
                    delay(deviceAutoRemovePeriodMs)
                    removeTimedOutItems()
                }
            }
            scope.launch(coroutineContext) {
                if(this@BleScanner::receiveBleDeviceChannel.isInitialized)
                    receiveBleDeviceChannel.cancel()
                receiveBleDeviceChannel = rawScanResultChannel.openSubscription()
                receiveBleDeviceChannel.receiveAsFlow().onEach { newItem ->
                    results.removeAll { existingItem ->
                        (newItem.address == existingItem.address && newItem.name == existingItem.name)
                    }
                    results.add(newItem)
                    results.removeAll { existingItem ->
                        existingItem.timestamp + deviceForgetTimeoutMs < System.currentTimeMillis()
                                || ignoredDevices.contains(newItem.address)
                    }
                    l?.i("Found device: ${newItem.name} @ ${newItem.address}")
                }.buffer(1, BufferOverflow.DROP_OLDEST).collect {
                    emitResults()
                    delay(emissionBackPressure)
                }
            }
            isScanning.value = ScannerState.Scanning
        } catch (tr: Throwable) {
            stopResultsEmission()
        }
    }

    private fun stopResultsEmission() {
        isScanning.value = ScannerState.Idle
        if(this::job.isInitialized)
            job.cancel()
    }

    //----------------------------------------------------------------------------------------------
    // Start\Stop scan implementation
    //----------------------------------------------------------------------------------------------
    private var compatCallback: CompatScanCallback? = null
    private var leCallback: LEScanCallback? = null
    private lateinit var adapter: BluetoothAdapter

    @UiThread
    @Synchronized override fun startScan(context: Context,
                                         scanFilters: List<BleScanFilter>,
                                         scanSettings: ScanSettings?,
                                         minRssiToEmit: Int) : Boolean {
        this.minRssiToEmit = minRssiToEmit
        try {
            adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
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
                    scanner.stopScan(it)
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
        }
    }

    private fun stopScannerNotSynchronized() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                compatCallback?.let {
                    @Suppress("Deprecation")
                    adapter.stopLeScan(it)
                    stopResultsEmission()
                }
                l?.d("Scanner compat callback instance does not exist.")
                compatCallback = null
            } else {
                leCallback?.let {
                    stopResultsEmission()
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.stopScan(it)
                }
                l?.d("Scanner le callback instance does not exist.")
                leCallback = null
            }
        } catch (tr: Throwable) {
            l?.e("Exception caught while stopping le scan", tr)
        }
    }

    @UiThread
    @Synchronized  override fun stopScan() : Boolean {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                compatCallback?.let {
                    stopResultsEmission()
                    @Suppress("Deprecation")
                    adapter.stopLeScan(it)
                }
                l?.d("Scanner compat callback instance does not exist.")
                compatCallback = null
            } else {
                leCallback?.let {
                    stopResultsEmission()
                    val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner
                    scanner.stopScan(it)
                }
                l?.d("Scanner le callback instance does not exist.")
                leCallback = null
            }
        } catch (tr: Throwable) {
            l?.e("Exception caught while stopping le scan", tr)
        } finally {
            return true
        }
    }
}