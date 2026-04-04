package org.tvheadend.tvhclientng.ui.common.interfaces

interface SearchRequestInterface {

    fun getQueryHint(): String

    fun onSearchRequested(query: String)

    fun onSearchResultsCleared()
}
