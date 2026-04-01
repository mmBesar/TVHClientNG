package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import androidx.media3.common.C
import androidx.media3.common.Format
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
                Timber.d("  Renderer:$rendererIndex")
                for (groupIndex in 0 until rendererTrackGroups.length) {
                    val trackGroup = rendererTrackGroups[groupIndex]
                    val adaptiveSupport = getAdaptiveSupportString(
                        trackGroup.length,
                        mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, false)
                    )
                    Timber.d("    Group:$groupIndex, adaptive streaming supported:$adaptiveSupport")

                    for (trackIndex in 0 until trackGroup.length) {
                        val isEnabled = getTrackStatusString(
                            tracks.isTrackSelected(trackGroup, trackIndex)
                        )
                        val format = trackGroup.getFormat(trackIndex)
                        val formatSupport = getFormatSupportString(
                            mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)
                        )
                        Timber.d("      Track:$trackIndex, selected=$isEnabled, mimeType=${format.sampleMimeType}, supported=$formatSupport")
                    }
                }
            }
        }
    }

    private fun getFormatSupportString(formatSupport: Int): String {
        return when (formatSupport and 0xF) {
            0x4 -> "yes"
            0x3 -> "no, exceeds capabilities"
            0x2 -> "no, unsupported drm"
            0x1 -> "no, unsupported type"
            0x0 -> "no"
            else -> "unknown"
        }
    }

    private fun getAdaptiveSupportString(trackCount: Int, adaptiveSupport: Int): String {
        if (trackCount < 2) return "n/a"
        return when (adaptiveSupport) {
            0x10000 -> "yes"
            0x8000 -> "yes but not seamless"
            0x0 -> "no"
            else -> "unknown"
        }
    }

    private fun getTrackStatusString(enabled: Boolean): String {
        return if (enabled) "yes" else "no"
    }
}
