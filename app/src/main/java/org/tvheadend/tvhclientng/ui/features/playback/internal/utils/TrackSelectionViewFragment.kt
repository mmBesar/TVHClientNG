package org.tvheadend.tvhclientng.ui.features.playback.internal.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

/**
 * Simplified track selection fragment for Media3.
 * TrackSelectionView was removed in Media3 — track selection is now
 * handled via DefaultTrackSelector parameters directly.
 */
class TrackSelectionViewFragment : Fragment() {

    private lateinit var mappedTrackInfo: androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo
    private var rendererIndex = 0

    var isDisabled = false
    var overrides: List<DefaultTrackSelector.SelectionOverride> = emptyList()

    init {
        retainInstance = true
    }

    fun init(
        mappedTrackInfo: androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo,
        rendererIndex: Int,
        initialIsDisabled: Boolean,
        initialOverride: DefaultTrackSelector.SelectionOverride?
    ) {
        this.mappedTrackInfo = mappedTrackInfo
        this.rendererIndex = rendererIndex
        this.isDisabled = initialIsDisabled
        this.overrides = initialOverride?.let { listOf(it) } ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // No UI needed — track selection handled by TrackSelectionDialog
        return null
    }
}
