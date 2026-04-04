package org.tvheadend.tvhclientng.ui.common

import android.content.SearchRecentSuggestionsProvider

class SuggestionProvider : SearchRecentSuggestionsProvider() {

    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "org.tvheadend.tvhclientng.ui.common.SuggestionProvider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}
