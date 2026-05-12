package com.acadmate.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.acadmate.core.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val selectedRoleKey = stringPreferencesKey("selected_role")
    private val appPinKey = stringPreferencesKey("app_pin")
    private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled")
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val isOnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val autoAlarmsEnabledKey = booleanPreferencesKey("auto_alarms_enabled")
    private val attendanceAlertsEnabledKey = booleanPreferencesKey("attendance_alerts_enabled")
    private val assignmentRemindersEnabledKey = booleanPreferencesKey("assignment_reminders_enabled")
    private val examNotificationsEnabledKey = booleanPreferencesKey("exam_notifications_enabled")
    private val smartInsightsEnabledKey = booleanPreferencesKey("smart_insights_enabled")

    val selectedRole: Flow<UserRole?> = context.dataStore.data.map { preferences ->
        preferences[selectedRoleKey]?.let { UserRole.fromString(it) }
    }

    val appPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[appPinKey]
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[appLockEnabledKey] ?: false
    }

    val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[darkModeKey]
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isOnboardingCompletedKey] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isOnboardingCompletedKey] = completed
        }
    }

    val autoAlarmsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[autoAlarmsEnabledKey] ?: false
    }

    val attendanceAlertsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[attendanceAlertsEnabledKey] ?: true
    }

    val assignmentRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[assignmentRemindersEnabledKey] ?: true
    }

    val examNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[examNotificationsEnabledKey] ?: true
    }

    val smartInsightsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[smartInsightsEnabledKey] ?: false
    }

    suspend fun setAutoAlarmsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoAlarmsEnabledKey] = enabled
        }
    }

    suspend fun setAttendanceAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[attendanceAlertsEnabledKey] = enabled
        }
    }

    suspend fun setAssignmentRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[assignmentRemindersEnabledKey] = enabled
        }
    }

    suspend fun setExamNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[examNotificationsEnabledKey] = enabled
        }
    }

    suspend fun setSmartInsightsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[smartInsightsEnabledKey] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkModeKey] = enabled
        }
    }

    suspend fun saveSelectedRole(role: UserRole) {
        context.dataStore.edit { preferences ->
            preferences[selectedRoleKey] = role.name
        }
    }

    suspend fun saveAppPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[appPinKey] = pin
            preferences[appLockEnabledKey] = true
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[appLockEnabledKey] = enabled
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.remove(selectedRoleKey)
            preferences.remove(isOnboardingCompletedKey)
            preferences.remove(appPinKey)
            preferences.remove(appLockEnabledKey)
        }
    }

    suspend fun clearSelectedRole() {
        context.dataStore.edit { preferences ->
            preferences.remove(selectedRoleKey)
        }
    }
}
