package org.tvheadend.tvhclient.ui.features.playback.internal.utils

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.media3.common.C
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo
import com.google.android.material.tabs.TabLayout
import org.tvheadend.tvhclient.R
import java.util.ArrayList

class TrackSelectionDialog : DialogFragment() {

    private val tabFragments: SparseArray<TrackSelectionViewFragment> = SparseArray()
    private val tabTrackTypes: ArrayList<Int> = ArrayList()
    private var titleId = 0
    private lateinit var onClickListener: DialogInterface.OnClickListener

    private fun init(
        mappedTrackInfo: MappedTrackInfo,
        initialParameters: DefaultTrackSelector.Parameters,
        onClickListener: DialogInterface.OnClickListener
    ) {
        this.titleId = R.string.track_selection_title
        this.onClickListener = onClickListener

        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (showTabForRenderer(mappedTrackInfo, i)) {
                val trackType = mappedTrackInfo.getRendererType(i)
                val trackGroupArray = mappedTrackInfo.getTrackGroups(i)
                val tabFragment = TrackSelectionViewFragment()
                tabFragment.init(
                    mappedTrackInfo, i,
                    initialParameters.getRendererDisabled(i),
                    initialParameters.getSelectionOverride(i, trackGroupArray)
                )
                tabFragments.put(i, tabFragment)
                tabTrackTypes.add(trackType)
            }
        }
    }

    private fun getIsDisabled(rendererIndex: Int): Boolean {
        return tabFragments[rendererIndex]?.isDisabled ?: false
    }

    private fun getOverrides(rendererIndex: Int): List<DefaultTrackSelector.SelectionOverride> {
        return tabFragments[rendererIndex]?.overrides ?: emptyList()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AppCompatDialog(activity, R.style.TrackSelectionDialogThemeOverlay)
        dialog.setTitle(titleId)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dialogView = inflater.inflate(R.layout.track_selection_dialog, container, false)
        val tabLayout: TabLayout = dialogView.findViewById(R.id.track_selection_dialog_tab_layout)
        val viewPager: ViewPager = dialogView.findViewById(R.id.track_selection_dialog_view_pager)
        val cancelButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_cancel_button)
        val okButton = dialogView.findViewById<Button>(R.id.track_selection_dialog_ok_button)

        viewPager.adapter = FragmentAdapter(childFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.visibility = if (tabFragments.size() > 1) View.VISIBLE else View.GONE
        cancelButton.setOnClickListener { dismiss() }
        okButton.setOnClickListener {
            onClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
            dismiss()
        }
        return dialogView
    }

    private inner class FragmentAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment = tabFragments.valueAt(position)
        override fun getCount(): Int = tabFragments.size()
        override fun getPageTitle(position: Int): CharSequence =
            getTrackTypeString(resources, tabTrackTypes[position])
    }

    companion object {

        fun willHaveContent(trackSelector: DefaultTrackSelector): Boolean {
            val mappedTrackInfo = trackSelector.currentMappedTrackInfo
            return mappedTrackInfo != null && willHaveContent(mappedTrackInfo)
        }

        private fun willHaveContent(mappedTrackInfo: MappedTrackInfo): Boolean {
            for (i in 0 until mappedTrackInfo.rendererCount) {
                if (showTabForRenderer(mappedTrackInfo, i)) return true
            }
            return false
        }

        fun createForTrackSelector(trackSelector: DefaultTrackSelector): TrackSelectionDialog {
            val mappedTrackInfo = checkNotNull(trackSelector.currentMappedTrackInfo)
            val parameters = trackSelector.parameters
            val dialog = TrackSelectionDialog()

            dialog.init(mappedTrackInfo, parameters) { _: DialogInterface?, _: Int ->
                val builder = parameters.buildUpon()
                for (i in 0 until mappedTrackInfo.rendererCount) {
                    builder.clearSelectionOverrides(i)
                        .setRendererDisabled(i, dialog.getIsDisabled(i))
                    val overrides = dialog.getOverrides(i)
                    if (overrides.isNotEmpty()) {
                        builder.setSelectionOverride(
                            i,
                            mappedTrackInfo.getTrackGroups(i),
                            overrides[0]
                        )
                    }
                }
                trackSelector.setParameters(builder)
            }
            return dialog
        }

        private fun showTabForRenderer(mappedTrackInfo: MappedTrackInfo, rendererIndex: Int): Boolean {
            val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
            if (trackGroupArray.length == 0) return false
            return isSupportedTrackType(mappedTrackInfo.getRendererType(rendererIndex))
        }

        private fun isSupportedTrackType(trackType: Int): Boolean {
            return trackType == C.TRACK_TYPE_VIDEO ||
                    trackType == C.TRACK_TYPE_AUDIO ||
                    trackType == C.TRACK_TYPE_TEXT
        }

        private fun getTrackTypeString(resources: Resources, trackType: Int): String {
            return when (trackType) {
                C.TRACK_TYPE_VIDEO -> resources.getString(R.string.exo_track_selection_title_video)
                C.TRACK_TYPE_AUDIO -> resources.getString(R.string.exo_track_selection_title_audio)
                C.TRACK_TYPE_TEXT -> resources.getString(R.string.exo_track_selection_title_text)
                else -> "unknown"
            }
        }
    }

    init {
        retainInstance = true
    }
}
