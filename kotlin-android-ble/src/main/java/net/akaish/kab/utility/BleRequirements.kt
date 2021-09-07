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

import android.bluetooth.BluetoothAdapter.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class BleRequirements(private val context: Context) {

    data class State(val bleOn: Boolean, val locOn: Boolean)

    @Suppress("Unused")
    lateinit var locationServicesEnabled: MutableStateFlow<Boolean>
    @Suppress("Unused")
    lateinit var bluetoothEnabled: MutableStateFlow<Int>
    val unmetRequirements = MutableStateFlow(State(bleOn = false, locOn = false))

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { intentnn ->
                val action = intentnn.action
                action?.let {
                    if(it == ACTION_STATE_CHANGED) {
                        bluetoothEnabled.value = intentnn.getIntExtra(EXTRA_STATE, ERROR)
                    }
                }
            }
        }
    }

    private lateinit var job: Job
    private val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    fun startWatching() {
        job = SupervisorJob()
        locationServicesEnabled = MutableStateFlow(isLocationServicesEnabled(context))
        bluetoothEnabled = MutableStateFlow(getDefaultAdapter().state)
        val scope = CoroutineScope(coroutineContext)
        scope.launch(coroutineContext) {
            launch {
                locationServicesEnabled.collect {
                    val reqSet = unmetRequirements.value
                    unmetRequirements.value = reqSet.copy(locOn = it)
                }
            }
            launch {
                bluetoothEnabled.collect {
                    val reqSet = unmetRequirements.value
                    unmetRequirements.value = reqSet.copy(bleOn = it == STATE_ON)
                }
            }
            while (true) {
                locationServicesEnabled.value = isLocationServicesEnabled(context)
                delay(2000L)
            }
        }
        context.registerReceiver(bluetoothStateReceiver, IntentFilter(ACTION_STATE_CHANGED))
    }

    fun terminate() {
        context.unregisterReceiver(bluetoothStateReceiver)
        if(this::job.isInitialized)
            job.cancel()
    }

    private fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            false.also { Log.d("BleRequirements", "BleRequirements#isLocationServicesEnabled exception", ex) }
        }

        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            false.also { Log.d("BleRequirements", "BleRequirements#isLocationServicesEnabled exception", ex) }
        }

        return gpsEnabled || networkEnabled
    }
}