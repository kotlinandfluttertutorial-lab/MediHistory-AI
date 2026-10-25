package com.medihistory.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for MediHistory AI.
 *
 * The [HiltAndroidApp] annotation triggers Hilt's code generation for
 * the application-level dependency injection component.
 */
@HiltAndroidApp
class MediHistoryApplication : Application()
