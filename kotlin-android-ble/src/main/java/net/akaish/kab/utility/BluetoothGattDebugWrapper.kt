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

/**
 * Gatt wrapper that does same things as [BluetoothGatt] via reflection but returns more helpful errors on fail.
 * Requires something like [https://github.com/ChickenHook/RestrictionBypass](https://github.com/ChickenHook/RestrictionBypass)
 * or free reflection library.
 */
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

            /*
             * It just not working
             */
            //val getDeviceMethod = service::class.java.getDeclaredMethod("getDevice")
            //val deviceReference = getDeviceMethod.invoke(service) ?: return -5

            val deviceReference = gatt.device ?: return -5


            val mDeviceBusyLockField =
                BluetoothGatt::class.java.getDeclaredField("mDeviceBusyLock").apply {
                    isAccessible = true
                }
            val mDeviceLockReference = mDeviceBusyLockField.get(gatt) ?: return -6
            val mDeviceBusyField = BluetoothGatt::class.java.getDeclaredField("mDeviceBusy").apply {
                isAccessible = true
            }

            synchronized(mDeviceLockReference) {
                val mDeviceBusyValue = mDeviceBusyField.get(gatt)
                if (mDeviceBusyValue as Boolean) return -7
                mDeviceBusyField.set(gatt, true)
            }

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
                    (deviceReference as BluetoothDevice).address,
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

    /**
     * Writes a given characteristic and its values to the associated remote device.
     * This method runs all routine of [BluetoothGatt.writeCharacteristic] via reflection
     * with main idea to provide better logging of real fail reason
     *
     * <p>Once the write operation has been completed, the
     * {@link BluetoothGattCallback#onCharacteristicWrite} callback is invoked,
     * reporting the result of the operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return 1, if the read operation was initiated successfully else value from -1 to -A
     */
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) : Int {
        if(
            (characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) &&
            (characteristic.properties.and(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0)
        ) return  -1

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
            if (characteristic.value == null) return -5

            /*
             * It just not working
             */
//            val getDeviceMethod = service::class.java.getDeclaredMethod("getDevice")
//            val deviceReference = getDeviceMethod.invoke(service) ?: return -6
            val deviceReference = gatt.device ?: return -6

            val mDeviceBusyLockField =
                BluetoothGatt::class.java.getDeclaredField("mDeviceBusyLock").apply {
                    isAccessible = true
                }
            val mDeviceLockReference = mDeviceBusyLockField.get(gatt) ?: return -7
            val mDeviceBusyField = BluetoothGatt::class.java.getDeclaredField("mDeviceBusy").apply {
                isAccessible = true
            }

            synchronized(mDeviceLockReference) {
                val mDeviceBusyValue = mDeviceBusyField.get(gatt)
                if (mDeviceBusyValue as Boolean) return -8
                mDeviceBusyField.set(gatt, true)
            }

            try {
                val mServiceWriteCharacteristicMethod =
                    mServiceFieldReference::class.java.getDeclaredMethod(
                        "writeCharacteristic",
                        Int::class.java, // mClientIf
                        String::class.java, // device address
                        Int::class.java, // instance id
                        Int::class.java, // write type
                        Int::class.java, // auth type
                        ByteArray::class.java // char value
                    ).apply {
                        isAccessible = true
                    }
                mServiceWriteCharacteristicMethod.invoke(
                    mServiceFieldReference,
                    mClientIfValue,
                    (deviceReference as BluetoothDevice).address,
                    characteristic.instanceId,
                    characteristic.writeType,
                    AUTHENTICATION_NONE,
                    characteristic.value
                )
            } catch (tr: RemoteException) {
                mDeviceBusyField.set(gatt, true)
                Log.e(TAG, "writeCharacteristic : Remote exception", tr)
                return -9
            }

            return 1
        } catch (tr: Throwable) {
            Log.e(TAG, "writeCharacteristic : throwable caught", tr)
            return -0xA
        }
    }

}