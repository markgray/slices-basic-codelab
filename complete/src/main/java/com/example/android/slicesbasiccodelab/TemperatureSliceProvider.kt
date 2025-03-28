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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.ListBuilderDsl
import androidx.slice.builders.SliceAction
import androidx.slice.builders.header
import androidx.slice.builders.list

import com.example.android.slicesbasiccodelab.MainActivity.Companion.getTemperature
import com.example.android.slicesbasiccodelab.MainActivity.Companion.getTemperatureString
import com.example.android.slicesbasiccodelab.TemperatureBroadcastReceiver.Companion.ACTION_CHANGE_TEMPERATURE
import com.example.android.slicesbasiccodelab.TemperatureBroadcastReceiver.Companion.EXTRA_TEMPERATURE_VALUE

/**
 * Creates a temperature control [Slice] that mirrors the main activity. The slice displays the
 * current temperature and allows the user to adjust the temperature up and down from the [Slice]
 * without launching the activity. NOTE: The main action still allows the user to launch the main
 * activity if they choose. It is named as a `ContentProvider` by a `provider` element in our
 * AndroidManifest.xml file, with an `intent-filter` element whose `action` is [Intent.ACTION_VIEW],
 * and whose `category` is "android.app.slice.category.SLICE". Its [getUri] method is called by the
 * `updateTemperature` method of [MainActivity].
 */
@SuppressLint("Slices") // TODO: Implement SliceProvider#onMapIntentToUri to handle the intents defined on your slice <provider> in your manifest
class TemperatureSliceProvider : SliceProvider() {

    /**
     * The is the [Context] this provider is running in. It is initialize our our [onCreateSliceProvider]
     * override and guaranteed to not be `null` (once [onCreateSliceProvider] returns `true` indicating
     * that the provider was successfully loaded).
     */
    private lateinit var contextNonNull: Context

    /**
     * Implement this to initialize your slice provider on startup. This method is called for all
     * registered slice providers on the application main thread at application launch time.  It
     * must not perform lengthy operations, or application startup will be delayed. We initialize
     * our [Context] field [contextNonNull] to the [Context] this provider is running in, and if
     * that is `null` we return `false` to indicate we failed to load successfully. If is it
     * non-`null` we return `true` to indicate we were successfully loaded.
     *
     * @return `true` if the provider was successfully loaded, `false` otherwise
     */
    override fun onCreateSliceProvider(): Boolean {
        // Step 2.3, Review non-nullable Context variable.
        contextNonNull = context ?: return false
        return true
    }

    /**
     * Each [Slice] has an associated URI. The standard format is package name + path for each
     * [Slice]. In our case, we only have one slice mapped to the 'temperature' path
     * ('com.example.android.slicesbasiccodelab/temperature').
     *
     * When a surface wants to display a [Slice], it sends a binding request to your app with this
     * URI via this method and you build out the slice to return. The surface can then display the
     * [Slice] when appropriate.
     *
     * Note: You can make your slices interactive by adding actions. (We do this for our
     * temperature up/down buttons.)
     *
     * First we log the [Uri] parameter [sliceUri] that was sent to us in the binding request of
     * the "slice-viewer.apk" package. (This [Uri] is the same one sent to the `sliceviewer` app
     * from the [MainActivity.launchSliceViewerApplication] method as the Intent data URI of the
     * [Intent] used to launch it). Then when the `path` of [sliceUri] is "/temperature" we return
     * the [Slice] created by our [createTemperatureSlice] method. (If our app defined more [Slice]s
     * each would have a different `path` which we could switch on here).
     *
     * If the `path` of [sliceUri] is not "/temperature" we return `null`.
     *
     * @param sliceUri the [Uri] used by the surface when it sent a binding request to our app
     * @return a [Slice] that can be displayed in the app that sent us a binding request.
     */
    override fun onBindSlice(sliceUri: Uri): Slice? {
        Log.d(TAG, "onBindSlice(): $sliceUri")

        // Step 2.4, Define a slice path.
        when (sliceUri.path) {
            "/temperature" -> return createTemperatureSlice(sliceUri)
        }
        return null
    }

    /**
     * Creates the actual [Slice] returned from [onBindSlice]. First we log the [Uri] parameter
     * [sliceUri] that we were called with. Then we build and return a [Slice] using a [ListBuilder]
     * by simplifying the verbosity required using the kotlin inline function [list] which constructs
     * a [ListBuilderDsl] using our field [contextNonNull] as the [Context], our [Uri] parameter
     * [sliceUri] as the [Uri] to tag for the slice, and [ListBuilder.INFINITY] as the length in
     * milliseconds that the content in this slice can live for, and then applies our lambda to
     * the [ListBuilder] which:
     *  - Sets the color to use on tintable items within the list builder to the color stored in
     *  our resources with the ID `R.color.slice_accent_color` (a bright Red)
     *  - Sets a header for the list builder whose title is the [String] returned by our
     *  [getTemperatureString] method, whose primary action when clicked is a [SliceAction] that
     *  launches our [MainActivity], whose icon to display has the ID `R.drawable.ic_home`, with
     *  an image mode to display this icon of [ListBuilder.ICON_IMAGE] (Indicates that the image
     *  should be presented as an icon and it can be tinted), and the title for the action is the
     *  [String] "Temperature Controls" (resource ID `R.string.slice_action_primary_title`)
     *  - Next we add [SliceAction] which launches a [PendingIntent] to increase the current value
     *  of temperature returned by the [getTemperature] by 1, whose icon is the resource drawable
     *  with ID `R.drawable.ic_temp_up` (an "UP" arrow), with an image mode to display this icon of
     *  [ListBuilder.ICON_IMAGE], and the title for the action is the [String] "Increase temperature"
     *  (resource ID `R.string.increase_temperature`)
     *  - Finally we add [SliceAction] which launches a [PendingIntent] to decrease the current value
     *  of temperature returned by the [getTemperature] by 1, whose icon is the resource drawable
     *  with ID `R.drawable.ic_temp_down` (a "DOWN" arrow), with an image mode to display this icon
     *  of [ListBuilder.ICON_IMAGE], and the title for the action is the [String] "Decrease temperature"
     *  (resource ID `R.string.decrease_temperature`)
     *
     * @param sliceUri the [Uri] that was passed to [onBindSlice].
     * @return a [Slice] that displays the temperature and allows it to be changed.
     */
    private fun createTemperatureSlice(sliceUri: Uri): Slice {
        Log.d(TAG, "createTemperatureSlice(): $sliceUri")

        /* Slices are constructed by using a ListBuilder (it is the main building block of Slices).
         * ListBuilder allows you to add different types of rows that are displayed in a list.
         * Because we are using the Slice KTX library, we can use the DSL version of ListBuilder,
         * so we just need to use list() and include some general arguments before defining the
         * structure of the Slice.
         */
        // Step 3.1, Review Slice's ListBuilder.
        return list(contextNonNull, sliceUri, ListBuilder.INFINITY) {
            setAccentColor(ContextCompat.getColor(contextNonNull, R.color.slice_accent_color))
            /* The first row of your slice should be a header. The header supports a title,
             * subtitle, and a tappable action (usually used to launch an Activity). A header
             * can also have a summary of the contents of the slice which can be shown when
             * the Slice may be too large to be displayed. In our case, we are also attaching
             * multiple actions to the header row (temp up/down).
             *
             * If we wanted to add additional rows, you can use the RowBuilder or the GridBuilder.
             *
             */
            // Step 3.2, Create a Slice Header (title and primary action).
            header {
                title = getTemperatureString(contextNonNull)
                // Launches the main Activity associated with the Slice.
                primaryAction = SliceAction.create(
                    PendingIntent.getActivity(
                        contextNonNull,
                        sliceUri.hashCode(),
                        Intent(contextNonNull, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    ),
                    IconCompat.createWithResource(contextNonNull, R.drawable.ic_home),
                    ListBuilder.ICON_IMAGE,
                    contextNonNull.getString(R.string.slice_action_primary_title)
                )
            }
            // Step 3.3, Add Temperature Up Slice Action.
            addAction(
                SliceAction.create(
                    createTemperatureChangePendingIntent(getTemperature() + 1),
                    IconCompat.createWithResource(contextNonNull, R.drawable.ic_temp_up),
                    ListBuilder.ICON_IMAGE,
                    contextNonNull.getString(R.string.increase_temperature)
                )
            )
            // Step 3.4, Add Temperature Down Slice Action.
            addAction(
                SliceAction.create(
                    createTemperatureChangePendingIntent(getTemperature() - 1),
                    IconCompat.createWithResource(contextNonNull, R.drawable.ic_temp_down),
                    ListBuilder.ICON_IMAGE,
                    contextNonNull.getString(R.string.decrease_temperature)
                )
            )
        }
    }

    /**
     * Step 3.4, Review Pending Intent Creation. Creates a [PendingIntent] that triggers an increase
     * or decrease in temperature. First we initialize our [Intent] variable `val intent` to a new
     * instance whose action is [ACTION_CHANGE_TEMPERATURE], whose target is [TemperatureBroadcastReceiver]
     * with an [Int] extra of our parameter [value] stored under the key [EXTRA_TEMPERATURE_VALUE].
     * Then we return a broadcast [PendingIntent] using [contextNonNull] as the [Context] in which
     * the [PendingIntent] should perform the broadcast, a request code of [requestCode] (which we
     * post increment for the next time we are called), `intent` as the [Intent] to be broadcast,
     * and [PendingIntent.FLAG_UPDATE_CURRENT] as the flags (Flag indicating that if the described
     * [PendingIntent] already exists, then keep it but replace its extra data with what is in this
     * new [Intent]).
     *
     * @param value the new value for the temperature.
     * @return a [PendingIntent] whose action is [ACTION_CHANGE_TEMPERATURE], whose target is the
     * class [TemperatureBroadcastReceiver], with an extra that stores our [Int] parameter [value]
     * under the key [EXTRA_TEMPERATURE_VALUE]
     */
    private fun createTemperatureChangePendingIntent(value: Int): PendingIntent {
        val intent = Intent(ACTION_CHANGE_TEMPERATURE)
            .setClass(contextNonNull, TemperatureBroadcastReceiver::class.java)
            .putExtra(EXTRA_TEMPERATURE_VALUE, value)

        return PendingIntent.getBroadcast(
            contextNonNull, requestCode++, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "TempSliceProvider"

        /**
         * The request code of the broadcast [PendingIntent] which we send to our class
         * [TemperatureBroadcastReceiver]
         */
        private var requestCode = 0

        /**
         * Convenience function to construct and return a [Uri] whose scheme is "content"
         * ([ContentResolver.SCHEME_CONTENT]), whose authority is the name of this application's
         * package, and whose path is our [String] parameter [path]. In our case this [Uri] is
         * "content://com.example.android.slicesbasiccodelab/temperature"
         *
         * @param context the [Context] to use to fetch the package name, the context of the single,
         * global Application object of the current process when we are called from [MainActivity].
         * @param path the path of the [Uri], always "temperature" in our case.
         * @return a [Uri] that we can use to notify our [Slice] that the temperature has changed.
         */
        fun getUri(context: Context, path: String): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(context.packageName)
                .appendPath(path)
                .build()
        }
    }
}
