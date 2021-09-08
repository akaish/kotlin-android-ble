package net.akaish.kab

import net.akaish.kab.model.ApplicationCharacteristic
import net.akaish.kab.model.ServiceType
import java.util.*

class BleConstants {

    companion object {
        @Suppress("Unused")
        const val BATTERY_ID = 0x1001L

        @JvmStatic val BATTERY_SERVICE : UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")

        @JvmStatic val BATTERY_CHARACTERISTIC : UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        @Suppress("Unused")
        @JvmStatic val APPLICATION_BATTERY_SERVICE = ApplicationCharacteristic(
            BATTERY_SERVICE,
            BATTERY_CHARACTERISTIC,
            mutableListOf(ServiceType.Read),
            BATTERY_ID
        )

        const val MIN_RSSI_UPDATE_PERIOD = 500L

        const val SUBSCRIPTION_TIMEOUT = 1000L
        const val MTU_TIMEOUT = 1000L
        const val RSSI_TIMEOUT = 1000L
        const val WRITE_TIMEOUT = 1000L
        const val READ_TIMEOUT = 1000L

        /**
         * Default value for RSSI
         */
        const val RSSI_UNKNOWN = -0xFFF

        /**
         * Min possible MTU
         */
        const val MTU_MIN = 23

        /**
         * Max possible MTU
         */
        const val MTU_MAX = 517

        /**
         * DEFAULT MTU
         */
        const val MTU_DEFAULT = MTU_MIN

        @JvmStatic val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}