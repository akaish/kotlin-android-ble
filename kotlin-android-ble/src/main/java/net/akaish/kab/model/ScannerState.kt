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

import android.bluetooth.le.ScanCallback.SCAN_FAILED_ALREADY_STARTED

sealed class ScannerState {
    open fun isScanning() = false

    /**
     * During scanner start request (or some time after) error raised
     * Possible errors:
     * 1 - SCAN_FAILED_ALREADY_STARTED - Fails to start scan as BLE scan with the same settings is already started by the app.
     * 2 - SCAN_FAILED_APPLICATION_REGISTRATION_FAILED - Fails to start scan as app cannot be registered.
     * 3 - SCAN_FAILED_INTERNAL_ERROR - Fails to start scan due an internal error.
     * 4 - SCAN_FAILED_FEATURE_UNSUPPORTED - Fails to start power optimized scan as this feature is not supported.
     */
    class ScannerError(@Suppress("Unused") val errorId: Int) : ScannerState() {
        override fun isScanning(): Boolean {
            return when (errorId) {
                SCAN_FAILED_ALREADY_STARTED -> true
                else -> false
            }
        }
    }

    @Suppress("Unused")
    class ScannerException(val tr: Throwable) : ScannerState()

    object Scanning : ScannerState() {
        override fun isScanning() : Boolean = true
    }

    object Idle : ScannerState()
}