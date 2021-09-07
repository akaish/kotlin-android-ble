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

import android.util.Log
import androidx.annotation.IntRange
import net.akaish.kab.utility.ILogger.Companion.DEBUG
import net.akaish.kab.utility.ILogger.Companion.ERROR
import net.akaish.kab.utility.ILogger.Companion.INFO
import net.akaish.kab.utility.ILogger.Companion.WARNING

class BleLogger(private val logTag: String) : ILogger {

    private var enabled = true
    override fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
    }

    private var level: Int = INFO
    override fun setLogLevel(
            @IntRange(from = INFO.toLong(), to = ERROR.toLong()) level: Int) {
        require(level in INFO..ERROR)
        this.level = level
    }

    override fun i(message: String) {
        if(level <= INFO && enabled)
            Log.i(logTag, message)
    }

    override fun i(message: String, tr: Throwable?) {
        if(level <= INFO && enabled)
            Log.i(logTag, message, tr)
    }

    override fun d(message: String) {
        if(level <= DEBUG && enabled)
            Log.d(logTag, message)
    }

    override fun d(message: String, tr: Throwable?) {
        if(level <= DEBUG && enabled)
            Log.d(logTag, message, tr)
    }

    override fun w(message: String) {
        if(level <= WARNING && enabled)
            Log.w(logTag, message)
    }

    override fun w(message: String, tr: Throwable?) {
        if(level <= WARNING && enabled)
            Log.w(logTag, message, tr)
    }

    override fun e(message: String) {
        if(level <= ERROR && enabled)
            Log.e(logTag, message)
    }

    override fun e(message: String, tr: Throwable?) {
        if(level <= ERROR && enabled)
            Log.e(logTag, message, tr)
    }
}