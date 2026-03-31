package org.tvheadend.tvhclient

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Simplified repository — all features are permanently unlocked.
 * Billing and Google Play dependencies have been removed.
 */
class MainRepository {

    fun isPurchased(sku: String): Flow<Boolean> = flowOf(true)

    fun canPurchase(sku: String): Flow<Boolean> = flowOf(false)

    companion object {
        const val UNLOCKER = "unlocker"
        val INAPP_SKUS = arrayOf(UNLOCKER)
    }
}
