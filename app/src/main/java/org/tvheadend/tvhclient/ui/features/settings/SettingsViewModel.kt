package org.tvheadend.tvhclient.ui.features.settings

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import org.tvheadend.data.AppRepository
import org.tvheadend.data.entity.Channel
import org.tvheadend.data.entity.Connection
import org.tvheadend.data.entity.ServerProfile
import org.tvheadend.data.entity.ServerStatus
import org.tvheadend.data.source.MiscDataSource
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.common.interfaces.SnackbarMessageInterface
import org.tvheadend.tvhclient.util.livedata.Event
import timber.log.Timber
import javax.inject.Inject

class SettingsViewModel(application: Application) : AndroidViewModel(application), SnackbarMessageInterface {

    @Inject
    lateinit var appRepository: AppRepository

    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)

    var connectionToEdit: Connection
    var connectionIdToBeEdited: Int = -1
    var connectionCountLiveData: LiveData<Int>
    var activeConnectionLiveData: LiveData<Connection>
    var connectionListLiveData: LiveData<List<Connection>>
    var currentServerStatus: ServerStatus
    var currentServerStatusLiveData: LiveData<ServerStatus>

    // Always unlocked — billing has been removed
    val isUnlockedLiveData: LiveData<Boolean> = MutableLiveData(true)
    var isUnlocked = true

    private val navigationMenuIdLiveData = MutableLiveData(Event("default"))

    var snackbarMessageLiveData = MutableLiveData<Event<Intent>>()
        private set

    init {
        inject()
        connectionToEdit = appRepository.connectionData.activeItem
        activeConnectionLiveData = appRepository.connectionData.liveDataActiveItem
        connectionCountLiveData = appRepository.connectionData.getLiveDataItemCount()
        connectionListLiveData = appRepository.connectionData.getLiveDataItems()
        currentServerStatus = appRepository.serverStatusData.activeItem
        currentServerStatusLiveData = appRepository.serverStatusData.liveDataActiveItem
    }

    private fun inject() {
        MainApplication.component.inject(this)
    }

    fun getNavigationMenuId(): LiveData<Event<String>> = navigationMenuIdLiveData

    fun setNavigationMenuId(id: String) {
        Timber.d("Received new navigation id $id")
        navigationMenuIdLiveData.value = Event(id)
    }

    fun getChannelList(): List<Channel> {
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun setSyncRequiredForActiveConnection() {
        Timber.d("Updating active connection to request a full sync")
        appRepository.connectionData.setSyncRequiredForActiveConnection()
    }

    fun clearDatabase(callback: MiscDataSource.DatabaseClearedCallback) {
        appRepository.miscData.clearDatabase(callback)
    }

    fun updateServerStatus(serverStatus: ServerStatus) {
        appRepository.serverStatusData.updateItem(serverStatus)
    }

    fun getHtspProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.htspPlaybackServerProfileId)
    fun getHtspProfiles(): List<ServerProfile> = appRepository.serverProfileData.htspPlaybackProfiles
    fun getHttpProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.httpPlaybackServerProfileId)
    fun getHttpProfiles(): List<ServerProfile> = appRepository.serverProfileData.httpPlaybackProfiles
    fun getRecordingProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.recordingServerProfileId)
    fun getSeriesRecordingProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.seriesRecordingServerProfileId)
    fun getTimerRecordingProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.timerRecordingServerProfileId)
    fun getRecordingProfiles(): List<ServerProfile> = appRepository.serverProfileData.recordingProfiles
    fun getCastingProfile(): ServerProfile? = appRepository.serverProfileData.getItemById(currentServerStatus.castingServerProfileId)

    fun addConnection() = appRepository.connectionData.addItem(connectionToEdit)
    fun loadConnectionById(id: Int) { connectionToEdit = appRepository.connectionData.getItemById(id) ?: Connection() }
    fun updateConnection(connection: Connection) = appRepository.connectionData.updateItem(connection)
    fun updateConnection() = appRepository.connectionData.updateItem(connectionToEdit)
    fun removeConnection(connection: Connection) = appRepository.connectionData.removeItem(connection)

    override fun setSnackbarMessage(intent: Intent) {
        snackbarMessageLiveData.value = Event(intent)
    }
}
