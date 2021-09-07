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

import androidx.annotation.IntRange

interface ILogger {

    companion object {
        const val INFO = 0
        const val DEBUG = 1
        const val WARNING = 2
        const val ERROR = 4
    }

    fun setEnabled(isEnabled: Boolean)
    fun setLogLevel(@IntRange(from = INFO.toLong(), to = ERROR.toLong()) level: Int)

    fun i(message: String)
    fun i(message: String, tr: Throwable?)

    fun d(message: String)
    fun d(message: String, tr: Throwable?)

    fun w(message: String)
    fun w(message: String, tr: Throwable?)

    fun e(message: String)
    fun e(message: String, tr: Throwable?)
}