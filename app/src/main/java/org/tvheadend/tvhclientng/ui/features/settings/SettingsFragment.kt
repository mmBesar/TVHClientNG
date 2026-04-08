package org.tvheadend.tvhclientng.ui.features.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import org.tvheadend.tvhclientng.R
import org.tvheadend.tvhclientng.ui.common.interfaces.ToolbarInterface
import org.tvheadend.tvhclientng.ui.features.MainActivity
import org.tvheadend.tvhclientng.util.extensions.sendSnackbarMessage
import timber.log.Timber

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, ActivityCompat.OnRequestPermissionsResultCallback {

    lateinit var sharedPreferences: SharedPreferences
    lateinit var settingsViewModel: SettingsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        PreferenceManager.setDefaultValues(activity, R.xml.preferences, false)
        settingsViewModel = ViewModelProvider(activity as SettingsActivity)[SettingsViewModel::class.java]

        (activity as ToolbarInterface).let {
            it.setTitle(getString(R.string.settings))
            it.setSubtitle("")
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        findPreference<Preference>("list_connections")?.onPreferenceClickListener = this
        findPreference<Preference>("user_interface")?.onPreferenceClickListener = this
        findPreference<Preference>("profiles")?.onPreferenceClickListener = this
        findPreference<Preference>("playback")?.onPreferenceClickListener = this
        findPreference<Preference>("advanced")?.onPreferenceClickListener = this
        findPreference<Preference>("changelog")?.onPreferenceClickListener = this
        findPreference<Preference>("language")?.onPreferenceClickListener = this
        findPreference<Preference>("selected_theme")?.onPreferenceClickListener = this
        findPreference<Preference>("information")?.onPreferenceClickListener = this
        findPreference<Preference>("privacy_policy")?.onPreferenceClickListener = this
        findPreference<Preference>("download_directory")?.onPreferenceClickListener = this

        // Hide the unlocker option since all features are free
        findPreference<Preference>("unlocker")?.isVisible = false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateDownloadDirSummary()
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun updateDownloadDirSummary() {
        Timber.d("Updating download directory summary")
        val path = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            sharedPreferences.getString("download_directory", Environment.DIRECTORY_DOWNLOADS)
        } else {
            Environment.DIRECTORY_DOWNLOADS
        }
        findPreference<Preference>("download_directory")?.summary = getString(R.string.pref_download_directory_sum, path)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && permissions.isNotEmpty()
                && permissions[0] == "android.permission.READ_EXTERNAL_STORAGE") {
            Handler(Looper.getMainLooper()).postDelayed({
                activity?.let { showFolderSelectionDialog(it) }
            }, 200)
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        when (key) {
            "selected_theme" -> handlePreferenceThemeChanged()
            "language" -> {
                activity?.let {
                    val intent = Intent(it, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    it.startActivity(intent)
                }
            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "profiles" -> handlePreferenceProfilesSelected()
            "playback" -> settingsViewModel.setNavigationMenuId("playback")
            "download_directory" -> handlePreferenceDownloadDirectorySelected()
            else -> settingsViewModel.setNavigationMenuId(preference.key)
        }
        return true
    }

    private fun handlePreferenceThemeChanged() {
        activity?.let {
            TaskStackBuilder.create(it)
                    .addNextIntent(Intent(it, MainActivity::class.java))
                    .addNextIntent(it.intent)
                    .startActivities()
        }
    }

    private fun handlePreferenceProfilesSelected() {
        if (view != null) {
            if (settingsViewModel.currentServerStatus.htspVersion < 16) {
                context?.sendSnackbarMessage(R.string.feature_not_supported_by_server)
            } else {
                settingsViewModel.setNavigationMenuId("profiles")
            }
        }
    }

    private fun handlePreferenceDownloadDirectorySelected() {
        activity?.let {
            if (isReadPermissionGranted(it)) {
                showFolderSelectionDialog(it)
            }
        }
    }

    private fun showFolderSelectionDialog(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            MaterialDialog(context).show {
                folderChooser(context) { _, file ->
                    val strippedPath = file.absolutePath.replace(Environment.getExternalStorageDirectory().absolutePath, "")
                    sharedPreferences.edit().putString("download_directory", strippedPath).apply()
                    updateDownloadDirSummary()
                }
            }
        } else {
            context.sendSnackbarMessage("On Android 10 and higher devices the download folder is always ${Environment.DIRECTORY_DOWNLOADS}")
        }
    }

    private fun isReadPermissionGranted(activity: FragmentActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                false
            }
        } else {
            true
        }
    }
}
