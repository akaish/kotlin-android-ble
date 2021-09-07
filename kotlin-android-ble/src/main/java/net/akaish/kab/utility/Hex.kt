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

object Hex {
    private val hexArray = "0123456789ABCDEF".toCharArray()
    private const val BYTE_DELIMITER = '-'

    /**
     * Returns hex string representation of provided byte array with default
     * delimiter [.BYTE_DELIMITER]
     *
     * @param bytes bytes to be converted to string
     * @return string representation of byte array
     */
    fun toPrettyHexString(bytes: ByteArray?): String {
        return if (bytes == null) "" else toPrettyHexString(bytes, BYTE_DELIMITER)
    }

    /**
     * Returns hex string representation of provided byte array
     *
     * @param bytes bytes to be converted to string
     * @return string representation of byte array
     */
    fun toHexString(bytes: ByteArray?): String {
        return if (bytes == null) "" else toPrettyHexString(bytes, null)
    }

    /**
     * Returns hex string representation of provided byte array
     *
     * @param bytes     string representation of byte array
     * @param delimiter byte pair delimiter
     * @return string representation of byte array
     */
    fun toPrettyHexString(bytes: ByteArray?, delimiter: Char?): String {
        if (bytes == null) return ""
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v: Int = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        val sb = StringBuilder()
        var i = 0
        while (i < hexChars.size) {
            sb.append(hexChars[i])
            sb.append(hexChars[i + 1])
            if (i + 2 != hexChars.size && delimiter != null) sb.append(delimiter)
            i += 2
        }
        return sb.toString()
    }

    /**
     * Converts provided String 0123...CEF to byte array
     * if % 2 != 0 than 0 would be added at the end of provided string
     *
     * @param str string to be converted
     * @return byte representation of 0123...CEF string
     */
    fun hexStringToByteArray(str: String): ByteArray? {
        if (str.length % 2 != 0) {
            val sb = StringBuilder()
            sb.append(str.substring(0, str.length - 1))
            sb.append('0')
            sb.append(str[str.length - 1])
            return try {
                hexStrToByteArray(sb.toString())
            } catch (tr: Throwable) {
                tr.printStackTrace()
                null
            }
        }
        return try {
            hexStrToByteArray(str)
        } catch (tr: Throwable) {
            tr.printStackTrace()
            null
        }
    }

    private fun hexStrToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}