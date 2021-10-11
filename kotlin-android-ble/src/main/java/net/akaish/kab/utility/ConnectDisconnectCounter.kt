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

import java.util.concurrent.ConcurrentHashMap

/**
 * Utility for checking amount of connection\disconnection calls for each gat facade impl instance
 * Not designed to used in app, just some kind of testing equality of gatt.connect and gat.close calls for each gatt instance
 */
internal object ConnectDisconnectCounter {

    private val map = ConcurrentHashMap<String, StringBuilder>()
    private val logCDC = false

    @Synchronized fun connection(caller: Any) {
        if(!logCDC)
            return
        val instance = caller.toString()
        map[instance] = StringBuilder().append("[+]")
    }

    @Synchronized fun close(caller: Any) {
        if(!logCDC)
            return
        val instance = caller.toString()
        map[instance]?.append("[-]")
    }

    @Suppress("Unused")
    @Synchronized fun toLines() : List<String> {
        if(!logCDC)
            return emptyList()
        val out = mutableListOf<String>()
        out.add("===========================================")
        out.add("Print start: ${System.currentTimeMillis()}")
        out.add("===========================================")
        map.iterator().forEach {
            out.add("${it.key} :: ${it.value}")
        }
        out.add("===========================================")
        out.add("Print end: ${System.currentTimeMillis()}")
        out.add("===========================================")
        return out
    }
}