package org.tvheadend.tvhclientng.ui.common.interfaces

import org.tvheadend.tvhclientng.ui.common.NetworkStatus

interface NetworkStatusInterface {
    fun setNetworkStatus(status: NetworkStatus)
    fun getNetworkStatus(): NetworkStatus?
}