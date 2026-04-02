package org.tvheadend.tvhclient.ui.features.playback.internal

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.common.MediaItem
import org.tvheadend.api.AuthenticationStateResult
import org.tvheadend.api.ConnectionStateResult
import org.tvheadend.api.ServerConnectionStateListener
import org.tvheadend.data.entity.Channel
import org.tvheadend.htsp.*
import org.tvheadend.tvhclient.BuildConfig
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.CustomEventLogger
import org.tvheadend.tvhclient.ui.features.playback.internal.utils.VideoAspect
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.math.max

class PlayerViewModel(application: Application) : BaseViewModel(application), ServerConnectionStateListener, Player.Listener {

    private var channelId: Int = 0
    private val channelList: List<Channel>

    private val execService: ScheduledExecutorService = Executors.newScheduledThreadPool(10)
    private val htspConnection: HtspConnection
    private var htspSubscriptionDataSourceFactory: HtspSubscriptionDataSource.Factory? = null
    private var htspFileInputStreamDataSourceFactory: HtspFileInputStreamDataSource.Factory? = null
    private var dataSource: HtspDataSourceInterface? = null

    val player: ExoPlayer
    val trackSelector: DefaultTrackSelector

    val videoAspectRatio: MutableLiveData<VideoAspect> = MutableLiveData()
    var playerState: MutableLiveData<Int> = MutableLiveData()
    var playerIsPlaying: MutableLiveData<Boolean> = MutableLiveData()
    var liveTvIsPlaying: MutableLiveData<Boolean> = MutableLiveData()
    var isConnected: MutableLiveData<Boolean> = MutableLiveData()
    var channelIcon: MutableLiveData<String> = MutableLiveData()
    var channelName: MutableLiveData<String> = MutableLiveData()
    var title: MutableLiveData<String> = MutableLiveData()
    var subtitle: MutableLiveData<String> = MutableLiveData()
    var nextTitle: MutableLiveData<String> = MutableLiveData()
    var elapsedTime: MutableLiveData<String> = MutableLiveData()
    var remainingTime: MutableLiveData<String> = MutableLiveData()

    private lateinit var playbackInformation: PlaybackInformation
    private lateinit var timeUpdateRunnable: Runnable
    private val timeUpdateHandler = Handler(Looper.getMainLooper())

    var pipModeActive: Boolean = false

    private val defaultForceAspectRatio = application.applicationContext.resources.getBoolean(R.bool.pref_default_force_aspect_ratio_for_sd_content_enabled)
    private val defaultChannelSortOrder = application.applicationContext.resources.getString(R.string.pref_default_channel_sort_order)
    private val defaultAudioTunnelingEnabled = application.applicationContext.resources.getBoolean(R.bool.pref_default_audio_tunneling_enabled)
    private val defaultConnectionTimeout = application.resources.getString(R.string.pref_default_connection_timeout)

    init {
        Timber.d("Initializing view model")

        isConnected.postValue(false)
        playerIsPlaying.postValue(false)
        playerState.postValue(Player.STATE_IDLE)

        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder) ?: defaultChannelSortOrder)
        channelList = appRepository.channelData.getChannels(channelSortOrder)

        val connection = appRepository.connectionData.activeItem
        val connectionTimeout = Integer.valueOf(sharedPreferences.getString("connection_timeout", defaultConnectionTimeout)!!) * 1000

        val htspConnectionData = HtspConnectionData(
            connection.username,
            connection.password,
            connection.serverUrl,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            connectionTimeout
        )
        htspConnection = HtspConnection(htspConnectionData, this, null)

        execService.execute {
            htspConnection.openConnection()
            htspConnection.authenticate()
        }

        trackSelector = DefaultTrackSelector(application.applicationContext, AdaptiveTrackSelection.Factory())
        trackSelector.buildUponParameters().setRendererDisabled(C.TRACK_TYPE_TEXT, true)
        if (sharedPreferences.getBoolean("audio_tunneling_enabled", defaultAudioTunnelingEnabled)) {
            trackSelector.buildUponParameters().setTunnelingEnabled(true)
        }

        val bufferTime = Integer.valueOf(sharedPreferences.getString("buffer_playback_ms", application.applicationContext.resources.getString(R.string.pref_default_buffer_playback_ms))!!)
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                bufferTime,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(C.DEFAULT_BUFFER_SEGMENT_SIZE)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val rendererFactory = DefaultRenderersFactory(application.applicationContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        player = ExoPlayer.Builder(application.applicationContext, rendererFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        player.addListener(this)
        player.addAnalyticsListener(CustomEventLogger(trackSelector))

        timeUpdateRunnable = Runnable {
            Timber.d("Updating elapsed and remaining times")
            remainingTime.postValue(playbackInformation.remainingTime)
            elapsedTime.postValue(playbackInformation.elapsedTime)
            timeUpdateHandler.postDelayed(timeUpdateRunnable, 1000)
        }
    }

    fun isPlaybackProfileSelected(bundle: Bundle?): Boolean {
        val channelId = bundle?.getInt("channelId", 0) ?: 0
        if (channelId > 0) {
            val serverStatus = appRepository.serverStatusData.activeItem
            val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
            if (serverProfile != null && !serverProfile.name.isNullOrEmpty() && serverProfile.name != "None") {
                return true
            }
        }
        return false
    }

    fun loadMediaSource(context: Context, bundle: Bundle?) {
        Timber.d("Loading new media source")
        releaseMediaSource()

        channelId = bundle?.getInt("channelId", 0) ?: 0
        val dvrId = bundle?.getInt("dvrId", 0) ?: 0
        val localUri = bundle?.getString("uri", "") ?: ""

        when {
            channelId > 0 -> loadMediaSourceForChannel(context, channelId)
            dvrId > 0 -> loadMediaSourceForRecording(dvrId)
            localUri.isNotEmpty() -> loadMediaSourceForLocalUri(context, localUri)
        }

        channelIcon.postValue(playbackInformation.channelIcon)
        channelName.postValue(playbackInformation.channelName)
        title.postValue(playbackInformation.title)
        subtitle.postValue(playbackInformation.subtitle)
        nextTitle.postValue(playbackInformation.nextTitle)
    }

    private fun loadMediaSourceForChannel(context: Context, channelId: Int) {
        Timber.d("Loading media source for channel id $channelId")
        playbackInformation = PlaybackInformation(appRepository.channelData.getItemByIdWithPrograms(channelId, Date().time))
        val serverStatus = appRepository.serverStatusData.activeItem
        val serverProfile = appRepository.serverProfileData.getItemById(serverStatus.htspPlaybackServerProfileId)
        htspSubscriptionDataSourceFactory = HtspSubscriptionDataSource.Factory(context, htspConnection, serverProfile?.name)
        dataSource = htspSubscriptionDataSourceFactory?.currentDataSource

        val mediaSource = ProgressiveMediaSource.Factory(
            htspSubscriptionDataSourceFactory!!,
            TvheadendExtractorsFactory()
        ).createMediaSource(MediaItem.fromUri(Uri.parse("htsp://channel/$channelId")))

        player.setMediaSource(mediaSource)
        player.prepare()
        liveTvIsPlaying.value = true
        player.playWhenReady = true
    }

    private fun loadMediaSourceForRecording(recordingId: Int) {
        Timber.d("Loading media source for recording id $recordingId")
        playbackInformation = PlaybackInformation(appRepository.recordingData.getItemById(recordingId))
        htspFileInputStreamDataSourceFactory = HtspFileInputStreamDataSource.Factory(htspConnection)
        dataSource = htspFileInputStreamDataSourceFactory?.currentDataSource

        val mediaSource = ProgressiveMediaSource.Factory(
            htspFileInputStreamDataSourceFactory!!,
            TvheadendExtractorsFactory()
        ).createMediaSource(MediaItem.fromUri(Uri.parse("htsp://dvrfile/$recordingId")))

        player.setMediaSource(mediaSource)
        player.prepare()
        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun loadMediaSourceForLocalUri(context: Context, localUri: String) {
        Timber.d("Preparing player with local media source '$localUri'")
        playbackInformation = PlaybackInformation()

        val mediaSource = ProgressiveMediaSource.Factory(
            DefaultDataSource.Factory(context)
        ).createMediaSource(MediaItem.fromUri(Uri.parse(localUri)))

        player.setMediaSource(mediaSource)
        player.prepare()
        liveTvIsPlaying.value = false
        player.playWhenReady = true
    }

    private fun releaseMediaSource() {
        Timber.d("Releasing previous media source")
        player.stop()
        trackSelector.buildUponParameters().clearSelectionOverrides()
        htspSubscriptionDataSourceFactory?.releaseCurrentDataSource()
        htspFileInputStreamDataSourceFactory?.releaseCurrentDataSource()
    }

    fun setVideoAspectRatio(rational: VideoAspect) {
        if (videoAspectRatio.value != null && videoAspectRatio.value != rational) {
            Timber.d("Updating selected video aspect ratio")
            videoAspectRatio.postValue(rational)
        }
    }

    override fun onAuthenticationStateChange(result: AuthenticationStateResult) {
        when (result) {
            is AuthenticationStateResult.Idle -> {}
            is AuthenticationStateResult.Authenticating -> Timber.d("Authenticating")
            is AuthenticationStateResult.Authenticated -> {
                Timber.d("Authenticated, starting player")
                isConnected.postValue(true)
            }
            is AuthenticationStateResult.Failed -> {
                Timber.d("Authorization failed")
                isConnected.postValue(false)
            }
        }
    }

    override fun onConnectionStateChange(result: ConnectionStateResult) {
        when (result) {
            is ConnectionStateResult.Failed -> {
                Timber.d("Connection failed")
                isConnected.postValue(false)
            }
            else -> Timber.d("Connected, initializing or idle")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Clearing view model")
        stopPlaybackAndReleaseMediaSource()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        Timber.d("Video size changed to width ${videoSize.width}, height ${videoSize.height}")
        var pixelWidthHeightRatio = videoSize.pixelWidthHeightRatio
        val width = videoSize.width
        val height = videoSize.height

        val forceAspectRatio = sharedPreferences.getBoolean("force_aspect_ratio_for_sd_content_enabled", defaultForceAspectRatio)
        if (forceAspectRatio) {
            val aspectRatio = DecimalFormat("#.##").format(width.toFloat() / height.toFloat())
            if (aspectRatio == "1,25") {
                pixelWidthHeightRatio = ((16f / 9f) * height.toFloat()) / width.toFloat()
                Timber.d("Video aspect ratio is 5:4, updating pixel aspect ratio to $pixelWidthHeightRatio")
            }
        }
        videoAspectRatio.postValue(VideoAspect((width * pixelWidthHeightRatio).toInt(), height))
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        playerState.postValue(playbackState)
        if (player.playWhenReady && playbackState == Player.STATE_READY) {
            Timber.d("Media is playing")
            playerIsPlaying.postValue(true)
            timeUpdateHandler.post(timeUpdateRunnable)
        } else if (!player.playWhenReady) {
            Timber.d("Player is paused")
            playerIsPlaying.postValue(false)
            timeUpdateHandler.removeCallbacks(timeUpdateRunnable)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Timber.d("Player error occurred: ${error.message}, errorCode: ${error.errorCode}")
    }

    fun stopPlaybackAndReleaseMediaSource() {
        Timber.d("Stopping playback, releasing media source")
        releaseMediaSource()
        player.release()
        Timber.d("Closing connection")
        execService.shutdown()
        htspConnection.closeConnection()
    }

    fun pause() {
        if (!pipModeActive) {
            player.playWhenReady = false
            dataSource?.pause()
        }
    }

    fun play() {
        player.playWhenReady = true
        dataSource?.resume()
    }

    fun seekBackward() {
        val time = getSeekPosition(-5000)
        Timber.d("Seeking backward to $time")
        player.seekTo(time)
    }

    fun seekForward() {
        val time = getSeekPosition(5000)
        Timber.d("Seeking forward to $time")
        player.seekTo(time)
    }

    private fun getSeekPosition(offset: Int): Long {
        val timeshiftStartTime = dataSource?.timeshiftStartTime ?: 0
        val timeshiftStartPts = dataSource?.timeshiftStartPts ?: 0
        val timeshiftOffsetPts = dataSource?.timeshiftOffsetPts ?: 0

        val startTime = if (timeshiftStartTime != Long.MIN_VALUE) (timeshiftStartTime / 1000) else 0
        val currentTime = if (timeshiftOffsetPts != Long.MIN_VALUE)
            System.currentTimeMillis() + timeshiftOffsetPts / 1000 else player.currentPosition

        val time = max(currentTime + offset, startTime)
        val seekPts = time * 1000 - timeshiftStartTime
        return max(seekPts, timeshiftStartPts) / 1000
    }

    fun playNextChannel(context: Context) {
        var newChannelId = channelId
        channelList.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index + 1 < channelList.size) channelList[index + 1].id
                else channelList.first().id
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(context, bundle)
    }

    fun playPreviousChannel(context: Context) {
        var newChannelId = channelId
        channelList.forEachIndexed { index, channel ->
            if (channel.id == channelId) {
                newChannelId = if (index - 1 > 0) channelList[index - 1].id
                else channelList.last().id
            }
        }
        val bundle = Bundle()
        bundle.putInt("channelId", newChannelId)
        loadMediaSource(context, bundle)
    }
}
