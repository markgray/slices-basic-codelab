/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.slicesbasiccodelab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.example.android.slicesbasiccodelab.MainActivity.Companion.getTemperature
import com.example.android.slicesbasiccodelab.MainActivity.Companion.updateTemperature

/**
 * Updates temperature (triggered by [TemperatureSliceProvider] and [MainActivity]). It is named as
 * a [BroadcastReceiver] by a `receiver` element in our AndroidManifest.xml file.
 */
class TemperatureBroadcastReceiver : BroadcastReceiver() {

    /**
     * This method is called when the [BroadcastReceiver] is receiving an [Intent] broadcast. This
     * method is always called within the main thread of its process, unless you explicitly asked
     * for it to be scheduled on a different thread using [Context.registerReceiver]. If the action
     * of our [Intent] parameter [intent] is [ACTION_CHANGE_TEMPERATURE] we initialize our [Int]
     * variable `val newValue` to the value stored under the key [EXTRA_TEMPERATURE_VALUE] in the
     * extras of [intent] using the current value of temperature returned by the [getTemperature]
     * method as the default. Then we call the [updateTemperature] method with our [Context]
     * parameter [context] and `newValue` to have it update the temperature.
     *
     * @param context The [Context] in which the receiver is running.
     * @param intent The [Intent] being received.
     */
    override fun onReceive(context: Context, intent: Intent) {

        if (ACTION_CHANGE_TEMPERATURE == intent.action) {
            val newValue: Int =
                intent.extras?.getInt(EXTRA_TEMPERATURE_VALUE, getTemperature()) ?: return
            updateTemperature(context, newValue)
        }
    }

    companion object {
        /**
         * Our package name, which we use as the namespace for [ACTION_CHANGE_TEMPERATURE] and
         * [EXTRA_TEMPERATURE_VALUE].
         */
        private const val PACKAGE_NAME = "com.example.android.slicesbasiccodelab"

        /**
         * The [Intent] action that our [onReceive] override understands.
         */
        const val ACTION_CHANGE_TEMPERATURE: String = "$PACKAGE_NAME.action.CHANGE_TEMPERATURE"

        /**
         * The key under which the new temperature is stored as an extra in the broadcast [Intent]
         * which our [onReceive] override receives.
         */
        const val EXTRA_TEMPERATURE_VALUE: String = "$PACKAGE_NAME.extra.TEMPERATURE_VALUE"
    }
}
