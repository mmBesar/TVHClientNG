package org.tvheadend.tvhclientng.ui.features.playback.internal.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.media3.common.C
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector

class TrackSelectionViewFragment : Fragment() {

    private lateinit var mappedTrackInfo: MappingTrackSelector.MappedTrackInfo
    private var rendererIndex = 0
    private var selectedTrackGroup = -1
    private var selectedTrackIndex = -1

    var isDisabled = false
    var overrides: List<DefaultTrackSelector.SelectionOverride> = emptyList()

    init {
        retainInstance = true
    }

    fun init(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        rendererIndex: Int,
        initialIsDisabled: Boolean,
        initialOverride: DefaultTrackSelector.SelectionOverride?
    ) {
        this.mappedTrackInfo = mappedTrackInfo
        this.rendererIndex = rendererIndex
        this.isDisabled = initialIsDisabled
        if (initialOverride != null) {
            selectedTrackGroup = initialOverride.groupIndex
            selectedTrackIndex = initialOverride.trackIndex
            this.overrides = listOf(initialOverride)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = android.widget.ScrollView(requireContext())
        scrollView.setPadding(0, 8, 0, 8)

        val mainLayout = LinearLayout(requireContext())
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)

        if (trackGroupArray.length == 0) {
            val emptyText = TextView(requireContext())
            emptyText.text = "No tracks available"
            emptyText.setPadding(32, 16, 32, 16)
            mainLayout.addView(emptyText)
        } else {
            val radioGroup = RadioGroup(requireContext())
            radioGroup.orientation = RadioGroup.VERTICAL
            radioGroup.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            var checkedId = -1
            var index = 0
            for (groupIdx in 0 until trackGroupArray.length) {
                val trackGroup = trackGroupArray.get(groupIdx)
                for (trackIdx in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(trackIdx)
                    val trackName = "  ${ExoPlayerUtils.buildTrackName(format)}"
                    val radioButton = RadioButton(requireContext())
                    radioButton.text = trackName
                    radioButton.id = View.generateViewId()
                    radioButton.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    radioButton.isChecked = (groupIdx == selectedTrackGroup && trackIdx == selectedTrackIndex)
                    if (radioButton.isChecked) {
                        checkedId = radioButton.id
                    }
                    val finalGroupIdx = groupIdx
                    val finalTrackIdx = trackIdx
                    radioButton.setOnClickListener {
                        selectedTrackGroup = finalGroupIdx
                        selectedTrackIndex = finalTrackIdx
                        isDisabled = false
                        overrides = listOf(
                            DefaultTrackSelector.SelectionOverride(finalGroupIdx, finalTrackIdx)
                        )
                    }
                    radioGroup.addView(radioButton)
                    index++
                }
            }

            if (checkedId >= 0) {
                radioGroup.check(checkedId)
            }
            mainLayout.addView(radioGroup)
        }

        scrollView.addView(mainLayout)
        return scrollView
    }
}