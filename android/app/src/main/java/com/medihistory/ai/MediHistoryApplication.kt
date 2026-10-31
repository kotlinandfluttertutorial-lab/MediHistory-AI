package com.medihistory.ai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for MediHistory AI.
 *
 * The [HiltAndroidApp] annotation triggers Hilt's code generation for
 * the application-level dependency injection component.
 */
@HiltAndroidApp
class MediHistoryApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(
                object : Timber.Tree() {
                    override fun log(
                        priority: Int,
                        tag: String?,
                        message: String,
                        t: Throwable?,
                    ) {
                        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                            return
                        }
                        android.util.Log.println(priority, tag ?: "MediHistoryAI", message)
                    }
                },
            )
        }
    }
}
