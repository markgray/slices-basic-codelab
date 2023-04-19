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

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

/**
 * Displays the current temperature and allows user to adjust it up and down. Any adjustments from
 * the external slice will also change this temperature value.
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var temperatureTextView: TextView
    private lateinit var sliceViewerPackageName: String

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main]. Next we initialize
     * our [TextView] field [temperatureTextView] by finding the view with ID [R.id.temperature] and
     * set our [String] field [sliceViewerPackageName] to the string with the resource ID
     * [R.string.slice_viewer_application_package_name]. Finally we find the [Button]'s with ID's
     * [R.id.increase_temp], [R.id.decrease_temp], and [R.id.launch_slice_viewer_application] and
     * set their [View.OnClickListener] to `this`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        temperatureTextView = findViewById(R.id.temperature)

        // Used for launching external app to preview Slice.
        sliceViewerPackageName = getString(R.string.slice_viewer_application_package_name)

        findViewById<Button>(R.id.increase_temp).setOnClickListener(this)
        findViewById<Button>(R.id.decrease_temp).setOnClickListener(this)

        findViewById<Button>(R.id.launch_slice_viewer_application).setOnClickListener(this)
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause]. This is usually a hint for
     * your activity to start interacting with the user, which is a good indicator that the activity
     * became active and ready to receive input. This sometimes could also be a transit state toward
     * another resting state. For instance, an activity may be relaunched to [onPause] due to
     * configuration changes and the activity was visible, but wasn’t the top-most activity of an
     * activity task. [onResume] is guaranteed to be called before [onPause] in this case which
     * honors the activity lifecycle policy and the activity eventually rests in [onPause]. First
     * we call our super's implementation of `onResume`, then we set the text of our [TextView]
     * field [temperatureTextView] to the string returned by our [getTemperatureString] method.
     */
    public override fun onResume() {
        super.onResume()
        temperatureTextView.text = getTemperatureString(applicationContext)
    }

    /**
     * Called when one of the views that `this` has been set as its [View.OnClickListener] for is
     * clicked. When the [View.getId] method (aka kotlin `id` property) of the [View] parameter
     * [view] is [R.id.increase_temp] we call our method [updateTemperature] to increase the
     * value of our [temperature] field by 1, when it is [R.id.decrease_temp] we call our method
     * [updateTemperature] to decrease the value of our [temperature] field by 1, and it is
     * [R.id.launch_slice_viewer_application] we call our method [launchSliceViewerApplication]
     * to launch the "Slice Viewer Application". For all other ID's we just return. Finally if
     * the ID was handled, we set the text of our [TextView] field [temperatureTextView] to the
     * string returned by our [getTemperatureString] method.
     *
     * @param view the [View] that was clicked.
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.increase_temp -> updateTemperature(applicationContext, temperature + 1)
            R.id.decrease_temp -> updateTemperature(applicationContext, temperature - 1)
            R.id.launch_slice_viewer_application -> launchSliceViewerApplication()
            else -> return
        }
        temperatureTextView.text = getTemperatureString(applicationContext)
    }

    private fun launchSliceViewerApplication() {

        if (isSliceViewerApplicationInstalled() && isSliceViewerApplicationEnabled()) {
            val uri = getString(R.string.uri_specific_for_slice_viewer_application)
            val sliceViewerIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(sliceViewerIntent)
        }
    }

    private fun isSliceViewerApplicationInstalled(): Boolean {

        val packageManager = applicationContext.packageManager

        try {
            @Suppress("DEPRECATION") // TODO: Use getPackageInfo(String, PackageManager.PackageInfoFlags) for SDK 33+
            packageManager.getPackageInfo(sliceViewerPackageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (ignored: PackageManager.NameNotFoundException) {

            val notInstalledToast = Toast.makeText(
                applicationContext,
                getString(R.string.slice_viewer_application_not_installed),
                Toast.LENGTH_LONG)

            notInstalledToast.show()

            Log.e(TAG, getString(R.string.error_message_slice_viewer_APK_missing))
        }

        return false
    }

    private fun isSliceViewerApplicationEnabled(): Boolean {
        var status = false
        try {
            @Suppress("DEPRECATION") // TODO: Use getApplicationInfo(String, PackageManager.ApplicationInfoFlags) for SDK 33+
            val applicationInfo: ApplicationInfo =
                applicationContext.packageManager.getApplicationInfo(sliceViewerPackageName, 0)

            @Suppress("SENSELESS_COMPARISON") // Better safe than sorry
            if (applicationInfo != null) {
                status = applicationInfo.enabled
            }
        } catch (ignored: PackageManager.NameNotFoundException) {

            val notEnabledToast = Toast.makeText(
                applicationContext,
                getString(R.string.slice_viewer_application_not_enabled),
                Toast.LENGTH_LONG)

            notEnabledToast.show()

            Log.e(TAG, getString(R.string.error_message_slice_viewer_APK_disabled))
        }

        return status
    }

    companion object {

        private const val TAG = "MainActivity"

        /* Temperature is in Celsius.
         *
         * NOTE: You should store your data in a more permanent way that doesn't disappear when the
         * app is killed. This drastically simplified sample is focused on learning Slices.
         */
        private var temperature = 16

        /**
         * Returns the [String] that contains the value of our [temperature] field formatted using
         * the [R.string.temperature] format (Temperature: %d\u00B0C).
         *
         * @param context the app's [Context] to use to retrieve resources.
         * @return a formatted [String] displaying the value of our [temperature] field.
         */
        fun getTemperatureString(context: Context): String {
            return context.getString(R.string.temperature, temperature)
        }

        /**
         * Just a getter for our [temperature] field.
         *
         * @return the current value of our [temperature] field.
         */
        fun getTemperature(): Int {
            return temperature
        }

        /**
         * Updates the value of our [temperature] to its parameter[newTemperature] if it has changed
         * and notifies our [TemperatureSliceProvider] about the new value.
         *
         * @param context the [Context] of the app.
         * @param newTemperature the new temperature.
         */
        fun updateTemperature(context: Context, newTemperature: Int) {
            Log.d(TAG, "updateTemperature(): $newTemperature")

            // TODO: Step 2.2, Notify TemperatureSliceProvider the temperature changed.
            if (temperature != newTemperature) {
                temperature = newTemperature

                // Notify slice via URI that the temperature has changed so they can update views.
                // NOTE: TemperatureSliceProvider is derived from ContentProvider, so we are
                // actually assigning a ContentProvider to this authority (URI).
                val uri = TemperatureSliceProvider.getUri(context, "temperature")
                context.contentResolver.notifyChange(uri, null)
            }

        }
    }
}
