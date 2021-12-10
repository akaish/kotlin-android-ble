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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.RemoteException
import android.util.Log

class BluetoothGattDebugWrapper(val gatt: BluetoothGatt) {

    companion object {
        const val AUTHENTICATION_NONE = 0
        const val TAG = "BGDW"
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     * This method runs all routine of [BluetoothGatt.readCharacteristic] via reflection
     * with main idea to provide better logging of real fail reason
     *
     * <p>This is an asynchronous operation. The result of the read operation
     * is reported by the {@link BluetoothGattCallback#onCharacteristicRead}
     * callback.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to read from the remote device
     * @return 1, if the read operation was initiated successfully else value from -1 to -9
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) : Int {
        if((characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_READ)) == 0) {
            return  -1
        }

        try {
            val mServiceField = BluetoothGatt::class.java.getDeclaredField("mService").apply {
                isAccessible = true
            }
            val mClientIfField = BluetoothGatt::class.java.getDeclaredField("mClientIf").apply {
                isAccessible = true
            }
            val mServiceFieldReference = mServiceField.get(gatt) ?: return -2
            val mClientIfValue = mClientIfField.getInt(gatt)
            if (mClientIfValue == 0) return -3

            val service = characteristic.service ?: return -4

            if(gatt.device == null) {
                return -5
            }
            // blacklisted access
//            val getDeviceMethod = service::class.java.getDeclaredMethod("getDevice")
//            val deviceReference = getDeviceMethod.invoke(service) ?: return -5
//            2021-12-10 08:31:14.097 8198-8198/ru.ikey.express E/BGDW: readCharacteristic : throwable caught
//            java.lang.NoSuchMethodException: android.bluetooth.BluetoothGattService.getDevice []
//            at java.lang.Class.getMethod(Class.java:2072)
//            at java.lang.Class.getDeclaredMethod(Class.java:2050)
//            at net.akaish.kab.utility.BluetoothGattDebugWrapper.readCharacteristic(BluetoothGattDebugWrapper.kt:71)
//            at net.akaish.kab.GattFacadeImpl$read$$inlined$suspendCancellableCoroutine$lambda$1.run(GattFacadeImpl.kt:547)
//            at android.os.Handler.handleCallback(Handler.java:938)
//            at android.os.Handler.dispatchMessage(Handler.java:99)
//            at android.os.Looper.loop(Looper.java:263)
//            at android.app.ActivityThread.main(ActivityThread.java:8283)
//            at java.lang.reflect.Method.invoke(Native Method)
//            at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:612)
//            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1006)


            // For android 11 it is blacklisted for reflection access

//            val mDeviceBusyLockField =
//                BluetoothGatt::class.java.getDeclaredField("mDeviceBusyLock").apply {
//                    isAccessible = true
//                }
            //val mDeviceLockReference = mDeviceBusyLockField.get(gatt) ?: return -6
            //so sync on this reference is unavailable
            //see https://dl.google.com/developers/android/rvc/non-sdk/hiddenapi-flags.csv line 34416

            val mDeviceBusyField = BluetoothGatt::class.java.getDeclaredField("mDeviceBusy").apply {
                isAccessible = true
            }

            //synchronized(mDeviceLockReference) {
            Log.e("111111", "${mDeviceBusyField.get(gatt).javaClass.simpleName}")

//            java.lang.IllegalArgumentException: Not a primitive field: java.lang.Boolean android.bluetooth.BluetoothGatt.mDeviceBusy
//            at java.lang.reflect.Field.getBoolean(Native Method)
//            at net.akaish.kab.utility.BluetoothGattDebugWrapper.readCharacteristic(BluetoothGattDebugWrapper.kt:107)
//            at net.akaish.kab.GattFacadeImpl$read$$inlined$suspendCancellableCoroutine$lambda$1.run(GattFacadeImpl.kt:547)
//            at android.os.Handler.handleCallback(Handler.java:938)
//            at android.os.Handler.dispatchMessage(Handler.java:99)
//            at android.os.Looper.loop(Looper.java:263)
//            at android.app.ActivityThread.main(ActivityThread.java:8283)
//            at java.lang.reflect.Method.invoke(Native Method)
//            at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:612)
//            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1006)
            val mDeviceBusyValue = mDeviceBusyField.get(gatt)
            if (mDeviceBusyValue as Boolean) return -7
            mDeviceBusyField.set(gatt, true)
            //}

            try {
                val mServiceReadCharacteristicMethod =
                    mServiceFieldReference::class.java.getDeclaredMethod(
                        "readCharacteristic",
                        Int::class.java,
                        String::class.java,
                        Int::class.java,
                        Int::class.java
                    ).apply {
                        isAccessible = true
                    }
                mServiceReadCharacteristicMethod.invoke(
                    mServiceFieldReference,
                    mClientIfValue,
                    gatt.device.address,
                    characteristic.instanceId,
                    AUTHENTICATION_NONE
                )
            } catch (tr: RemoteException) {
                mDeviceBusyField.set(gatt, true)
                Log.e(TAG, "readCharacteristic : Remote exception", tr)
                return -8
            }

            return 1
        } catch (tr: Throwable) {
            Log.e(TAG, "readCharacteristic : throwable caught", tr)
            return -9
        }
    }
}