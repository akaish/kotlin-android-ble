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

import net.akaish.kab.utility.StringSplitIterable
import java.util.*

data class TargetCharacteristic(
    val serviceUUID: UUID,
    val characteristicUUID: UUID) {

    companion object {

        @Suppress("Unused")
        /**
         * Creates target characteristic from string in following format:
         * "SERVICE-UUID-STRING-REPRESENTATION@CHARACTERISTIC-UUID-STRING-REPRESENTATION",
         * for example, string 00002902-0000-1000-8000-00805f9b34fb@00002902-0000-1000-8000-00805f9b34fb is
         * good argument.
         *
         * If argument in not expected format, exception would be raised.
         *
         * @param target target characteristic in following format string:
         * "SERVICE-UUID-STRING-REPRESENTATION@CHARACTERISTIC-UUID-STRING-REPRESENTATION"
         */
        @JvmStatic fun fromString(target: String) : TargetCharacteristic {
            val uuidIterator = StringSplitIterable(target, '@').iterator()
            val service = UUID.fromString(uuidIterator.next())
            val characteristic = UUID.fromString(uuidIterator.next())
            require(!uuidIterator.hasNext()) {
                "Wrong target characteristic format!"
            }
            return TargetCharacteristic(service, characteristic)
        }
    }

    override fun toString() = "$serviceUUID@$characteristicUUID"
}