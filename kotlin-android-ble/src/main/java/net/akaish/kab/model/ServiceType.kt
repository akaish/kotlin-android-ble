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
package net.akaish.kab.model

import android.bluetooth.BluetoothGattCharacteristic

sealed class ServiceType(private val property: Int) {
    object Read : ServiceType(BluetoothGattCharacteristic.PROPERTY_READ)
    object Write : ServiceType(BluetoothGattCharacteristic.PROPERTY_WRITE)
    object Notify : ServiceType(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
    object WriteNoResponse : ServiceType(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun supportedByCharacteristic(properties: Int) = properties.and(property) > 0
}