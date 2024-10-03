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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.android.slicesbasiccodelab.TemperatureBroadcastReceiver.Companion.EXTRA_TEMPERATURE_VALUE

/**
 * Displays the current temperature and allows user to adjust it up and down. Any adjustments from
 * the external slice will also change this temperature value.
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    /**
     * The [TextView] with ID [R.id.temperature] that we use to display the current temperature
     */
    private lateinit var temperatureTextView: TextView

    /**
     * The package name of the "com.example.android.sliceviewer" package that we use to display our
     * slice in. Its APK (the file slice-viewer.apk) is in the root directory of the codelab, and
     * needs to be installed using the command: adb install -r -t slice-viewer.apk
     */
    private lateinit var sliceViewerPackageName: String

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main] which consists of a
     * `ConstraintLayout` holding a [TextView] with ID [R.id.temperature] which we use to display
     * the current temperature, an "Increase Temperature" [Button] with id [R.id.increase_temp],
     * a 'Decrease Temperature" [Button] with id [R.id.decrease_temp], and an "Launch Slice Viewer"
     * [Button] with id [R.id.launch_slice_viewer_application].
     *
     * Having set our content view we next initialize our [TextView] field [temperatureTextView] by
     * finding the view with ID [R.id.temperature], initialize our [String] field [sliceViewerPackageName]
     * to the string with ID [R.string.slice_viewer_application_package_name] from our resources
     * (its value is "com.example.android.sliceviewer"), and set the [View.OnClickListener] of all
     * three of our [Button]s to `this` (our [onClick] override will branch on the ID of the [View]
     * that was clicked in order to decide what needs to be done when the user clicks a [Button]).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val rootView = findViewById<ConstraintLayout>(R.id.root_view)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        temperatureTextView = findViewById(R.id.temperature)

        // Used for launching external app to preview Slice.
        sliceViewerPackageName = getString(R.string.slice_viewer_application_package_name)

        findViewById<Button>(R.id.increase_temp).setOnClickListener(this)
        findViewById<Button>(R.id.decrease_temp).setOnClickListener(this)

        findViewById<Button>(R.id.launch_slice_viewer_application).setOnClickListener(this)
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for your activity to start
     * interacting with the user. This is an indicator that the activity became active and ready to
     * receive input. It is on top of an activity stack and visible to user. First we call our
     * super's implementation of `onResume`, then we set the text of our [TextView] field
     * [temperatureTextView] to the formatted [String] returned by our [getTemperatureString] method.
     */
    public override fun onResume() {
        super.onResume()
        temperatureTextView.text = getTemperatureString(applicationContext)
    }

    /**
     * Called when a view has been clicked. We branch on the ID of the [View] parameter [view]:
     *  - [R.id.increase_temp] (the "Increase Temperature" [Button]) we call our [updateTemperature]
     *  method to have it increment our static field [temperature] by 1 and notify our slice about
     *  the new temperature.
     *  - [R.id.decrease_temp] (the "Decrease Temperature" [Button]) we call our [updateTemperature]
     *  method to have it decrement our static field [temperature] by 1 and notify our slice about
     *  the new temperature.
     *  - [R.id.launch_slice_viewer_application] (the "Launch Slice Viewer" [Button]) we call our
     *  [launchSliceViewerApplication] method to have it start our slice viewer app slice-viewer.apk
     *  - For any other ID we return having done nothing.
     *
     * Finally we set the text of our [TextView] field [temperatureTextView] to the formatted [String]
     * returned by our [getTemperatureString] method.
     *
     * @param view The [View] that was clicked.
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

    /**
     * Called to launch the external package "com.example.android.sliceviewer" to preview our Slice.
     * If our [isSliceViewerApplicationInstalled] method returns `true` (indicating that the
     * PackageManager was able to retrieve package information about the package named
     * [sliceViewerPackageName] indicating that it is installed on the system) and our
     * [isSliceViewerApplicationEnabled] method returns `true` (indicating that the [ApplicationInfo]
     * for the package named [sliceViewerPackageName] has its `enabled` property set indicating that
     * the package is enabled) we proceed to:
     *  - Initialize our [String] variable `val uri` to the resource string with the ID
     *  [R.string.uri_specific_for_slice_viewer_application], which is:
     *  "slice-content://com.example.android.slicesbasiccodelab/temperature"
     *  - Initialize our [Intent] variable `val sliceViewerIntent` to an instance whose action is
     *  [Intent.ACTION_VIEW] (Display the data to the user) and whose Intent data URI is the [Uri]
     *  that the [Uri.parse] method creates by parsing our [String] variable `uri`.
     *  - Finally we launch the activity specified by the [Intent] variable `sliceViewerIntent`.
     */
    private fun launchSliceViewerApplication() {
        if (isSliceViewerApplicationInstalled() && isSliceViewerApplicationEnabled()) {
            val uri: String = getString(R.string.uri_specific_for_slice_viewer_application)
            val sliceViewerIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(sliceViewerIntent)
        }
    }

    /**
     * Queries the [PackageManager] for the [PackageInfo] of the package named [sliceViewerPackageName]
     * and returns `true` iff the [PackageManager] was able to find that package installed on the
     * system. First we initialize our [PackageManager] variable `val packageManager` to a
     * [PackageManager] instance for finding global package information. Then wrapped in a `try`
     * block intended to catch [PackageManager.NameNotFoundException] we call the
     * [PackageManager.getPackageInfo] method of `packageManager` to retrieve the [PackageInfo] for
     * the package with the name [sliceViewerPackageName] using the [PackageManager.GET_ACTIVITIES]
     * flag to have it return information about activities in the package. If this succeeds without
     * throwing [PackageManager.NameNotFoundException] we return `true` to the caller. If it does
     * throw [PackageManager.NameNotFoundException] we make and show a [Toast] informing the user
     * that the "Slice Viewer Application is not installed...", log the error and return `false` to
     * the caller.
     *
     * @return `true` if the [PackageManager] determined that the package named [sliceViewerPackageName]
     * is installed on the system.
     */
    private fun isSliceViewerApplicationInstalled(): Boolean {

        val packageManager: PackageManager = applicationContext.packageManager

        try {
            packageManager.getPackageInfo(sliceViewerPackageName, PackageManager.GET_ACTIVITIES)
            return true
        } catch (ignored: PackageManager.NameNotFoundException) {

            val notInstalledToast = Toast.makeText(
                applicationContext,
                getString(R.string.slice_viewer_application_not_installed),
                Toast.LENGTH_LONG
            )

            notInstalledToast.show()

            Log.e(TAG, getString(R.string.error_message_slice_viewer_APK_missing))
        }

        return false
    }

    /**
     * Queries the [PackageManager] for the [ApplicationInfo] of the package [sliceViewerPackageName]
     * and returns `true` if the `enabled` property of that [ApplicationInfo] is `true`. First we
     * initialize our [Boolean] variable `var status` to `false`, then wrapped in a `try` block
     * intended to catch [PackageManager.NameNotFoundException] we initialize our [ApplicationInfo]
     * variable `val applicationInfo` to the [ApplicationInfo] that the method
     * [PackageManager.getApplicationInfo] returns for the package named [sliceViewerPackageName].
     * If this is not `null` we set our `status` variable to the value of the [ApplicationInfo.enabled]
     * property of `applicationInfo`. If we catch [PackageManager.NameNotFoundException] we make and
     * show a [Toast] informing the user that the "Slice Viewer Application is not enabled..." and
     * log the error. Finally we return `status` to the caller.
     *
     * @return `true` if the `enabled` property of the [ApplicationInfo] of the package named
     * [sliceViewerPackageName] is `true`.
     */
    private fun isSliceViewerApplicationEnabled(): Boolean {
        var status = false
        try {
            // TODO: Use getApplicationInfo(String, PackageManager.ApplicationInfoFlags) for SDK 33+
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

        /**
         * TAG used for logging.
         */
        private const val TAG = "MainActivity"

        /**
         * Temperature is in Celsius.
         *
         * NOTE: You should store your data in a more permanent way that doesn't disappear when the
         * app is killed. This drastically simplified sample is focused on learning Slices.
         */
        private var temperature = 16

        /**
         * Returns a [String] which displays our [temperature] field formatted using the format in
         * [R.string.temperature].
         *
         * @return [String] which displays our [temperature] field formatted using the format in
         * [R.string.temperature].
         */
        fun getTemperatureString(context: Context): String {
            return context.getString(R.string.temperature, temperature)
        }

        /**
         * Getter for our [temperature] field that is used by [TemperatureBroadcastReceiver] and
         * [TemperatureSliceProvider]. The `onReceive` override of [TemperatureBroadcastReceiver]
         * uses the value returned as the default when it retrieves the new value that is stored
         * under the key [EXTRA_TEMPERATURE_VALUE] in the extras of the broadcast [Intent] that it
         * receives, and [TemperatureSliceProvider] uses the value returned in creating both the
         * "Temperature Up Slice Action", and the "Temperature Down Slice Action" incrementing and
         * decrementing the value respectively when creating the [PendingIntent] that is invoked for
         * the actions.
         *
         * @return the current value of our [Int] field [temperature].
         */
        fun getTemperature(): Int {
            return temperature
        }

        /**
         * If our [Int] parameter [newTemperature] is different than our [Int] field [temperature]
         * we update [temperature] to [newTemperature] and then notify our slice via URI that the
         * temperature has changed so that they can update their views.
         *
         * @param context either the [Context] in which the receiver is running when called form the
         * `onReceive` override of [TemperatureBroadcastReceiver] or the the [Context] of the single
         * global Application object of the current process when called from the [onClick] of this
         * [MainActivity].
         * @param newTemperature the new temperature to set our [temperature] field to.
         */
        fun updateTemperature(context: Context, newTemperature: Int) {
            Log.d(TAG, "updateTemperature(): $newTemperature")

            // Step 2.2, Notify TemperatureSliceProvider the temperature changed.
            if (temperature != newTemperature) {
                temperature = newTemperature

                // Notify slice via URI that the temperature has changed so they can update views.
                // NOTE: TemperatureSliceProvider inherits from ContentProvider, so we are
                // actually assigning a ContentProvider to this authority (URI).
                val uri = TemperatureSliceProvider.getUri(context, "temperature")
                context.contentResolver.notifyChange(uri, null)
            }
        }
    }
}
