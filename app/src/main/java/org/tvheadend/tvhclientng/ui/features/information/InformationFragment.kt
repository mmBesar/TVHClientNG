package org.tvheadend.tvhclientng.ui.features.information

import android.os.Bundle
import android.view.View
import org.tvheadend.tvhclientng.R

class InformationFragment : WebViewFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        website = "information"

        toolbarInterface.setTitle(getString(R.string.pref_information))
        toolbarInterface.setSubtitle("")
    }
}