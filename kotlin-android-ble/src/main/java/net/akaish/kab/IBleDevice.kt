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
import android.content.Context
import net.akaish.kab.model.ApplicationCharacteristic

interface IBleDevice {

    /**
     * Connect device and prepare for application usage
     */
    fun connect(device: BluetoothDevice, context: Context)

    /**
     * @return true if device connected via bluetooth
     */
    fun isConnected() : Boolean

    /**
     * @return ble device name.
     */
    fun bleDeviceName() : String

    /**
     * List of application characteristics to communicate with. You can assign long ids for them, also
     * all of characteristics with notification service type in this list would be auto subscribed
     * after connection, when "subscribe automatically" is set to true;
     */
    val applicationCharacteristics : MutableList<ApplicationCharacteristic>
}