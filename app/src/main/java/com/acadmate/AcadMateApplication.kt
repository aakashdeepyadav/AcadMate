package com.acadmate

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.acadmate.core.alarms.AutoAlarmWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AcadMateApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        
        try {
            // 1. Initialize Firebase first
            Firebase.initialize(context = this)

            val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

            if (isDebug) {
                // FORCE a specific debug token so we don't have to search Logcat
                System.setProperty("firebase.appcheck.debug.token", "8f7b3a21-9e12-4c5d-b8a1-f3a2b1c0d9e8")
                Log.d("AppCheckSetup", "Forcing Debug Token: 8f7b3a21-9e12-4c5d-b8a1-f3a2b1c0d9e8")
            }

            // 2. Setup App Check Factory
            val factory = if (isDebug) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                Log.d("AppCheckSetup", "Running in Release mode - Using Play Integrity")
                // Use Play Integrity for invisible verification in production.
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            Firebase.appCheck.installAppCheckProviderFactory(factory)
            
            if (isDebug) {
                Log.d("AppCheckDebugToken", "************************************************************")
                Log.d("AppCheckDebugToken", "CHECK LOGCAT FOR 'Enter this debug token into the Firebase Console'")
                Log.d("AppCheckDebugToken", "************************************************************")
            }

            setupAutoAlarms()
        } catch (e: UnsupportedOperationException) {
            Log.e("AppCheckSetup", "Play Integrity not supported (possibly no Google account): ${e.message}")
        } catch (e: Exception) {
            Log.e("AppCheckSetup", "Error installing App Check: ${e.message}", e)
        }
    }

    private fun setupAutoAlarms() {
        val workRequest = PeriodicWorkRequestBuilder<AutoAlarmWorker>(
            6, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoAlarmWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
