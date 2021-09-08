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

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

//----------------------------------------------------------------------------------------------
// Filter adapter
//----------------------------------------------------------------------------------------------
@Suppress("Unused")
sealed class BleScanFilter {

    abstract fun filter(device: BluetoothDevice) : Boolean
    abstract fun asScanFilter() : ScanFilter

    data class NameFilter(val name: String, val ignoreCase: Boolean = false) : BleScanFilter() {

        override fun filter(device: BluetoothDevice) = name.equals(device.name, ignoreCase)

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun asScanFilter() : ScanFilter = ScanFilter.Builder().setDeviceName(name).build()
    }

    data class AddressFilter(val address: String) : BleScanFilter() {

        override fun filter(device: BluetoothDevice) = address == device.address

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun asScanFilter() : ScanFilter = ScanFilter.Builder().setDeviceAddress(address).build()
    }

    // TODO actually reformat it to something compatible with ScanFilter.Builder().setService***
    data class UUIDFilter(val uuidList: List<UUID>) : BleScanFilter() {

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