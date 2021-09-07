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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StringSplitIterableTests {
    @Test fun checkStringSplitIterator() {
        val iterator1 = StringSplitIterable("sss@ss@@@", '@', true).iterator()
        assertEquals(iterator1.next(), "sss")
        assertEquals(iterator1.next(), "ss")
        assertEquals(iterator1.next(), "")
        assertEquals(iterator1.next(), "")
        assertTrue(!iterator1.hasNext())

        val iterator2 = StringSplitIterable("sss@ss@@@", '@', false).iterator()
        assertEquals(iterator2.next(), "sss")
        assertEquals(iterator2.next(), "ss")
        assertTrue(!iterator2.hasNext())

        val iterator3 = StringSplitIterable("@", '@', true).iterator()
        assertEquals(iterator3.next(), "")
        assertTrue(!iterator3.hasNext())

        val iterator4 = StringSplitIterable("@", '@', false).iterator()
        assertTrue(!iterator4.hasNext())

        val iterator5 = StringSplitIterable("", '@', true).iterator()
        assertEquals(iterator5.next(), "")
        assertTrue(!iterator5.hasNext())

        val iterator6 = StringSplitIterable("", '@', false).iterator()
        assertTrue(!iterator6.hasNext())
    }
}