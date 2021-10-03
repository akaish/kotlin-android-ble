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

import android.bluetooth.le.ScanSettings
import androidx.annotation.IntRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.StateFlow
import net.akaish.kab.model.FoundBleDevice
import net.akaish.kab.model.ScanResult
import net.akaish.kab.model.ScannerState
import net.akaish.kab.utility.ILogger

@ExperimentalCoroutinesApi
interface IBleScanner {

    companion object {
        const val DEVICE_FORGET_TIMEOUT_MS = 8_000L
        const val DEVICE_AUTO_REMOVE_PERIOD_MS = 2_000L
        internal val defaultPrefix = byteArrayOf(0x00, 0x00)
    }

    //----------------------------------------------------------------------------------------------
    // Configuration
    //----------------------------------------------------------------------------------------------
    val deviceForgetTimeoutMs : Long

    val deviceAutoRemovePeriodMs : Long

    val devicePrefixMap : Map<String, ByteArray>?

    val l : ILogger?

    val emissionBackPressure : Long

    fun addIgnoredAddress(address: String) : Boolean

    fun addIgnoredAddresses(addresses: List<String>) : Boolean

    fun resetIgnoredAddresses()


    //----------------------------------------------------------------------------------------------
    // Scanner methods
    //----------------------------------------------------------------------------------------------
    /**
     * Scan results channel that emits batch scan results
     */
    val scanResultsChannel : BroadcastChannel<ScanResult>

    /**
     * Raw scan results channel that emits each found device one by one
     */
    val rawScanResultChannel : BroadcastChannel<FoundBleDevice>

    /**
     * State flow that emits scanner state
     */
    val isScanning : StateFlow<ScannerState>

    /**
     * Sets callback for receiving scan results in callback way
     */
    fun setScanResultListener(scanResultListener: OnScanResult) : IBleScanner

    /**
     * Sets callback for receiving raw scan results in callback way
     */
    fun setRawScanResultListener(rawScanResultListener: OnRawScanResult) : IBleScanner

    /**
     * Starts scanner
     * @return true if scanner started
     */
    fun startScan(scanFilters: List<BleScanFilter> = listOf(),
                  scanSettings: ScanSettings? = null,
                  @IntRange(from = Byte.MIN_VALUE.toLong(), to = Byte.MAX_VALUE.toLong())
                  minRssiToEmit: Int = -127) : Boolean

    /**
     * Stops scanning
     * @return always true
     */
    fun stopScan() : Boolean
}