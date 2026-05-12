package com.acadmate.attendance.di

import android.content.Context
import com.acadmate.attendance.ble.BleScanner
import com.acadmate.attendance.face.FaceLivenessDetector
import com.acadmate.attendance.geo.GeofenceValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AttendanceModule {

    @Provides
    @Singleton
    fun provideBleScanner(@ApplicationContext context: Context): BleScanner {
        return BleScanner(context)
    }

    @Provides
    @Singleton
    fun provideFaceLivenessDetector(@ApplicationContext context: Context): FaceLivenessDetector {
        return FaceLivenessDetector(context)
    }

    @Provides
    @Singleton
    fun provideGeofenceValidator(@ApplicationContext context: Context): GeofenceValidator {
        return GeofenceValidator(context)
    }

    @Provides
    @Singleton
    fun provideAcousticTokenReceiver(): com.acadmate.attendance.audio.AcousticTokenReceiver {
        return com.acadmate.attendance.audio.AcousticTokenReceiver()
    }
}
