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
package net.akaish.kab.utility

import java.nio.ByteBuffer

internal object BleAddressUtil {
    private const val HEX_ONLY_PATTERN = "[^0-9a-fA-F]"
    private const val UID_LENGTH = 6
    private const val UID_PREFIX_LENGTH = 2

    /**
     * Just little function that makes long from 6 bytes ble mac, may throw illegal argument exception
     * if uidPrefixBytes length not 2 or deviceUid after byte conversion length not 6
     * @param uidPrefixBytes 2 bytes of uid prefix
     * @param deviceUid device uid string
     * @return long getId assembled from device tag bytes and device 6-bytes uid
     */
    fun generateDeviceId(uidPrefixBytes: ByteArray, deviceUid: String): Long {
        val deviceUidHexOnly = deviceUid.replace(HEX_ONLY_PATTERN.toRegex(), "")
        val uidBytes = hexStringToByteArray(deviceUidHexOnly)
        require(uidBytes.size == UID_LENGTH) { "Bad uid: $deviceUid" }
        require(uidPrefixBytes.size == UID_PREFIX_LENGTH) {
            "Bad prefix bytes array, " +
                    "length should be equal 2, but actual length is " + uidPrefixBytes.size
        }
        val uidBleUidBytes = ByteArray(8)
        System.arraycopy(uidPrefixBytes, 0, uidBleUidBytes, 0, uidPrefixBytes.size)
        System.arraycopy(uidBytes, 0, uidBleUidBytes, 2, uidBytes.size)
        // LOL, Long.BYTES available only since API version 24
        val buffer = ByteBuffer.allocate(UID_LENGTH + UID_PREFIX_LENGTH)
        buffer.put(uidBleUidBytes)
        buffer.flip()
        return buffer.long
    }

    /**
     * Converts provided String 0123...CEF to byte array
     * if % 2 != 0 than 0 would be added at the end of provided string
     *
     * @param str string to be converted
     * @return byte representation of 0123...CEF string
     */
    private fun hexStringToByteArray(str: String): ByteArray {
        if (str.length % 2 != 0) {
            val sb = StringBuilder()
            sb.append(str.substring(0, str.length - 1))
            sb.append('0')
            sb.append(str[str.length - 1])
            return hexStringToBytes(str)
        }
        return hexStringToBytes(str)
    }

    private fun hexStringToBytes(str: String): ByteArray {
        val value = ByteArray(str.length / 2)
        for (i in value.indices) {
            value[i] = str.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return value
    }
}