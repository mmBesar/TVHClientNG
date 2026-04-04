package org.tvheadend.tvhclientng.ui.features.playback.internal.utils

import androidx.media3.common.C
import androidx.media3.common.Tracks
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
                        val format = trackGroup.getFormat(trackIndex)
                        val support = mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)
                        val isSupported = (support and 0x04) != 0

                        // Use Tracks.Group API to check if track is selected
                        var isSelected = false
                        for (trackGroupIndex in 0 until tracks.groups.size) {
                            val group = tracks.groups[trackGroupIndex]
                            if (group.mediaTrackGroup == trackGroup) {
                                isSelected = group.isTrackSelected(trackIndex)
                                break
                            }
                        }

                        Timber.d("      Track:$trackIndex selected=$isSelected mimeType=${format.sampleMimeType} supported=$isSupported")
                    }
                }
            }
        }
    }

    private fun getTrackTypeString(trackType: Int): String {
        return when (trackType) {
            C.TRACK_TYPE_VIDEO -> "video"
            C.TRACK_TYPE_AUDIO -> "audio"
            C.TRACK_TYPE_TEXT -> "text"
            else -> "unknown"
        }
    }
}
