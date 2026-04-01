package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import androidx.media3.common.Tracks
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import timber.log.Timber

class CustomEventLogger(private val trackSelector: MappingTrackSelector) : AnalyticsListener {

    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: Tracks) {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        if (mappedTrackInfo == null) {
            Timber.d("No media tracks available")
            return
        }

        Timber.d("Available media tracks:")

        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
            val rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex)
            if (rendererTrackGroups.length > 0) {
                Timber.d("  Renderer:$rendererIndex [${getTrackTypeString(mappedTrackInfo.getRendererType(rendererIndex))}]")
                for (groupIndex in 0 until rendererTrackGroups.length) {
                    val trackGroup = rendererTrackGroups[groupIndex]
                    Timber.d("    Group:$groupIndex")
                    for (trackIndex in 0 until trackGroup.length) {
                        val isSelected = tracks.isTrackSelected(trackGroup, trackIndex)
                        val format = trackGroup.getFormat(trackIndex)
                        val support = mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)
                        val isSupported = RendererCapabilities.isFormatSupported(support)
                        Timber.d("      Track:$trackIndex selected=$isSelected mimeType=${format.sampleMimeType} supported=$isSupported")
                    }
                }
            }
        }
    }

    private fun getTrackTypeString(trackType: Int): String {
        return when (trackType) {
            androidx.media3.common.C.TRACK_TYPE_VIDEO -> "video"
            androidx.media3.common.C.TRACK_TYPE_AUDIO -> "audio"
            androidx.media3.common.C.TRACK_TYPE_TEXT -> "text"
            else -> "unknown"
        }
    }
}
