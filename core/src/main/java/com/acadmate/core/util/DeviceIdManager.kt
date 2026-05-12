package com.acadmate.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Gets a unique ID for the hardware. 
     * Using ANDROID_ID as a stable identifier for device binding.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    }
}
