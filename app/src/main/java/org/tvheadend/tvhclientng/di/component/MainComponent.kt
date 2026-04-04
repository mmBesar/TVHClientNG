package org.tvheadend.tvhclientng.di.component

import dagger.Component
import org.tvheadend.data.di.FeatureScope
import org.tvheadend.tvhclientng.MainApplication
import org.tvheadend.tvhclientng.di.module.ContextModule
import org.tvheadend.tvhclientng.di.module.SharedPreferencesModule
import org.tvheadend.tvhclientng.service.ConnectionIntentService
import org.tvheadend.tvhclientng.service.ConnectionService
import org.tvheadend.tvhclientng.ui.base.BaseFragment
import org.tvheadend.tvhclientng.ui.base.BaseViewModel
import org.tvheadend.tvhclientng.ui.features.settings.SettingsActivity
import org.tvheadend.tvhclientng.ui.features.settings.SettingsViewModel
import org.tvheadend.tvhclientng.ui.features.startup.StartupViewModel


@Component(
        modules = [ContextModule::class, SharedPreferencesModule::class],
        dependencies = [org.tvheadend.data.di.RepositoryComponent::class])
@FeatureScope
interface MainComponent {

    fun inject(mainApplication: MainApplication)
    fun inject(connectionService: ConnectionService)
    fun inject(connectionIntentService: ConnectionIntentService)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(baseFragment: BaseFragment)
    fun inject(baseViewModel: BaseViewModel)
    fun inject(settingsViewModel: SettingsViewModel)
    fun inject(startupViewModel: StartupViewModel)
}