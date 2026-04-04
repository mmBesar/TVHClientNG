package org.tvheadend.tvhclientng.ui.common.interfaces

import android.view.View

interface RecyclerViewClickInterface {
    fun onClick(view: View, position: Int)
    fun onLongClick(view: View, position: Int): Boolean
}
