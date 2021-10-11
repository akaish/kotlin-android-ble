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
 */
object ConnectDisconnectCounter {

    private val map = ConcurrentHashMap<String, StringBuilder>()

    @Synchronized fun connection(caller: Any) {
        val instance = caller.toString()
        map[instance] = StringBuilder().append("[+]")
    }

    @Synchronized fun close(caller: Any) {
        val instance = caller.toString()
        map[instance]?.append("[-]")
    }

    @Synchronized fun toLines() : List<String> {
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

//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ffc4363 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@515e1c0 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e2dc41 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@d403022 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5c2d1c1 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@cf824f9 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@96e789d :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@d224c70 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ac5c467 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@78bbe5d :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e7ffc52 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f9df7f0 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f547342 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ad5e092 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@d1bed5a :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@a234718 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@462823b :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@51e6b3c :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@527a891 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@a646964 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@b5bf5f9 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@59b920f :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@81d5292 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@3acaa86 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@cec59c6 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@81bdba3 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ee5c1c :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@99a4592 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@bfee7f5 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@8c382a7 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@313f7d0 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@b83cfe0 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f227952 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@13e1b9b :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@367a2a2 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5824ba :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@696458a :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f964008 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@15a87c1 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@368440b :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4658b7a :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5cbfb76 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e0555f7 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@34dff6d :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@1c988ac :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@7670f74 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@d311b41 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@b862bde :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ca34a80 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5835a6a :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e36137 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@469109b :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@8543947 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@13fe8ec :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@a1a1695 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@54ae541 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e5b9d0f :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@8da5d98 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4c89a74 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e354ab4 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@31d35f3 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@afb4e0b :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@1dd4fe0 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@d162bfc :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@82261d9 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@c04c387 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@1b7d488 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f48c4bf :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@904e882 :: [+][-][-][-]
//2021-10-10 03:36:54.301 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@64c0897 :: [+][-][-][-]
//2021-10-10 03:36:54.302 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@988b262 :: [+][-][-][-]
//2021-10-10 03:36:54.302 25200-25400/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e63a3c :: [+][-][-][-]

//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@b4a80b7 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@52871cb :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5e18503 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e0c1e6d :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@100b892 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@6458713 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@a594441 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@6189237 :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@82645fe :: [+][-][-][-]
//2021-10-10 03:48:47.725 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@db206bf :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@582b2f :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ccfbd0e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ea5ab1f :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@3488cd0 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@13bcb2d :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@fac525a :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@2b2117f :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5f4f9b3 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@9af12c1 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@43c15d2 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@fe2eb6f :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5da5844 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@572882d :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@f0d8d7e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@9062774 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@83c2be :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@cc97f21 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4f8f8c1 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@329f7f3 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5673932 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@8baaf5c :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@9378611 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@bd13e79 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@15016cd :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@580b017 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@5db6e60 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@75c43a6 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@89c9751 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@432db17 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@17cf0ac :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@bdc37ac :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4b2f0bf :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@c97e526 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@2b4407e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@945633 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@fc37f66 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@a7a413a :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@51a264 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4763db :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@444d00e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@2471718 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@9a12172 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@98b651d :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@ae7a5a7 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@7bca03d :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@db0520e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@cea31e5 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@74e67d3 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@71464e4 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@c01022e :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@48b6081 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@aeff76c :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@e571a35 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@6740d62 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@b7ab81f :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@1e38f83 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@4e03fb3 :: [+][-][-][-]
//2021-10-10 03:48:47.726 26682-26753/ru.ikey.express E/AAA: net.akaish.kab.GattFacadeImpl@6ddb32b :: [+][-][-][-]