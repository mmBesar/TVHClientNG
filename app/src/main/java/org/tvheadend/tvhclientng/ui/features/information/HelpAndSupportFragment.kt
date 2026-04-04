package org.tvheadend.tvhclientng.ui.features.information

import android.os.Bundle
import android.view.View
import org.tvheadend.tvhclientng.R

class HelpAndSupportFragment : WebViewFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        website = "help_and_support"

        toolbarInterface.setTitle(getString(R.string.help_and_support))
        toolbarInterface.setSubtitle("")
    }
}