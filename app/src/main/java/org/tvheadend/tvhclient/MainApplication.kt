package org.tvheadend.tvhclient

import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDexApplication
import org.tvheadend.data.AppRepository
import org.tvheadend.data.di.DaggerRepositoryComponent
import org.tvheadend.data.di.RepositoryModule
import org.tvheadend.tvhclient.di.component.DaggerMainComponent
import org.tvheadend.tvhclient.di.component.MainComponent
import org.tvheadend.tvhclient.di.module.ContextModule
import org.tvheadend.tvhclient.di.module.SharedPreferencesModule
import org.tvheadend.tvhclient.ui.common.onAttach
import org.tvheadend.tvhclient.util.MigrateUtils
import org.tvheadend.tvhclient.util.logging.DebugTree
import org.tvheadend.tvhclient.util.logging.FileLoggingTree
import timber.log.Timber
import javax.inject.Inject

class MainApplication : MultiDexApplication() {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    inner class AppContainer {
        val mainRepository = MainRepository()
    }

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()

        val repositoryComponent = DaggerRepositoryComponent
            .builder()
            .repositoryModule(RepositoryModule(applicationContext))
            .build()

        component = DaggerMainComponent
            .builder()
            .contextModule(ContextModule(applicationContext))
            .sharedPreferencesModule(SharedPreferencesModule())
            .repositoryComponent(repositoryComponent)
            .build()
        component.inject(this)

        instance = this

        Timber.plant(DebugTree())

        if (!BuildConfig.DEBUG && sharedPreferences.getBoolean("debug_mode_enabled", resources.getBoolean(R.bool.pref_default_debug_mode_enabled))) {
            Timber.plant(FileLoggingTree(applicationContext))
        }

        appContainer = AppContainer()
        Timber.d("Application build time is ${BuildConfig.BUILD_TIME}, git commit hash is ${BuildConfig.GIT_SHA}")

        MigrateUtils(applicationContext, appRepository, sharedPreferences).doMigrate()
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(onAttach(context))
    }

    companion object {
        lateinit var instance: MainApplication
        lateinit var component: MainComponent
    }
}
