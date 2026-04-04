package org.tvheadend.tvhclientng.ui.features.information

import android.os.Bundle
import android.view.View
import org.tvheadend.tvhclientng.R

class PrivacyPolicyFragment : WebViewFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        website = "privacy_policy"

        toolbarInterface.setTitle(getString(R.string.pref_privacy_policy))
        toolbarInterface.setSubtitle("")
    }
}