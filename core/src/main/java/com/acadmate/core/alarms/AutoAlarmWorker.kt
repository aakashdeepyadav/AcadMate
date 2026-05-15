package com.acadmate.core.alarms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.acadmate.core.db.TimetableRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.tasks.await
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

import kotlinx.coroutines.flow.first

@HiltWorker
class AutoAlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TimetableRepository,
    private val userRepository: com.acadmate.core.db.UserRepository,
    private val alarmScheduler: AndroidAlarmScheduler,
    private val onboardingDataStore: com.acadmate.core.datastore.OnboardingDataStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            
            // Skip if not logged in
            val currentUserId = auth.currentUser?.uid ?: return Result.success()
            val user = userRepository.getUserById(currentUserId) 
                ?: userRepository.getUserFromFirestore(currentUserId).getOrNull()
                ?: return Result.success()
            
            // This functionality is for students and teachers only
            if (user.role != com.acadmate.core.model.UserRole.STUDENT && 
                user.role != com.acadmate.core.model.UserRole.FACULTY) {
                return Result.success()
            }

            android.util.Log.d("AutoAlarmWorker", "Starting alarm scheduling for ${user.role}: $currentUserId")

            val autoAlarmsEnabled = onboardingDataStore.autoAlarmsEnabled.first()
            val alarmMinutesBefore = onboardingDataStore.alarmMinutesBefore.first()
            if (!autoAlarmsEnabled) return Result.success()

            val now = LocalDateTime.now()

            // 1. SCHEDULE CLASS REMINDERS (Next 48 Hours)
            val calendar = java.util.Calendar.getInstance()
            val todayDayOfWeek = when(calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> 1
                java.util.Calendar.TUESDAY -> 2
                java.util.Calendar.WEDNESDAY -> 3
                java.util.Calendar.THURSDAY -> 4
                java.util.Calendar.FRIDAY -> 5
                java.util.Calendar.SATURDAY -> 6
                java.util.Calendar.SUNDAY -> 7
                else -> 1
            }
            
            val tomorrowDayOfWeek = if (todayDayOfWeek == 7) 1 else todayDayOfWeek + 1

            // Schedule for today and tomorrow
            val dayOffsets = listOf(0, 1)

            dayOffsets.forEach { offset ->
                val targetCalendar = java.util.Calendar.getInstance().apply { 
                    add(java.util.Calendar.DAY_OF_YEAR, offset) 
                }
                val dayIndex = when(targetCalendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> 1
                    java.util.Calendar.TUESDAY -> 2
                    java.util.Calendar.WEDNESDAY -> 3
                    java.util.Calendar.THURSDAY -> 4
                    java.util.Calendar.FRIDAY -> 5
                    java.util.Calendar.SATURDAY -> 6
                    java.util.Calendar.SUNDAY -> 7
                    else -> 1
                }
                
                val allDayClasses = repository.getTimetableForDaySync(dayIndex)
                
                // Filter based on role
                val dayClasses = if (user.role == com.acadmate.core.model.UserRole.FACULTY) {
                    allDayClasses.filter { it.faculty.contains(user.name, ignoreCase = true) }
                } else {
                    allDayClasses // Students see all classes for their section
                }
                
                // User wants 1 hour before alarm specifically for the FIRST relevant class of the day
                val firstClass = dayClasses.minByOrNull { it.startTime }
                
                dayClasses.forEach { classInfo ->
                    try {
                        val isFirstClass = classInfo.id == firstClass?.id
                        val shouldSchedule = classInfo.isAlarmSet || isFirstClass

                        if (!shouldSchedule) return@forEach

                        val startTimeParts = classInfo.startTime.split(":")
                        if (startTimeParts.size == 2) {
                            val hour = startTimeParts[0].toInt()
                            val minute = startTimeParts[1].toInt()
                            
                            val classDateTime = LocalDateTime.of(
                                targetCalendar.get(java.util.Calendar.YEAR),
                                targetCalendar.get(java.util.Calendar.MONTH) + 1,
                                targetCalendar.get(java.util.Calendar.DAY_OF_MONTH),
                                hour,
                                minute
                            )
                            
                            val dateTag = "${targetCalendar.get(java.util.Calendar.DAY_OF_YEAR)}"
                            
                            // Dynamic Before Class Alarm
                            val alarmTimeDynamic = classDateTime.minusMinutes(alarmMinutesBefore.toLong())
                            if (alarmTimeDynamic.isAfter(now)) {
                                alarmScheduler.schedule(AlarmItem(
                                    id = "CLASS_DYN_${classInfo.id}_$dateTag",
                                    time = alarmTimeDynamic,
                                    title = if (isFirstClass) "First Class in ${if (alarmMinutesBefore >= 60) "${alarmMinutesBefore/60}h" else "${alarmMinutesBefore}m"}" else "Class in ${alarmMinutesBefore}m: ${classInfo.subject}",
                                    message = "Your session starts at ${classInfo.startTime} in ${classInfo.room}",
                                    type = "CLASS"
                                ))
                            }

                            // 10 Min Before Quick Reminder (Only if explicitly set or it's the first class)
                            val alarmTimeQuick = classDateTime.minusMinutes(10)
                            if (alarmTimeQuick.isAfter(now)) {
                                alarmScheduler.schedule(AlarmItem(
                                    id = "CLASS_10M_${classInfo.id}_$dateTag",
                                    time = alarmTimeQuick,
                                    title = "Upcoming Class: ${classInfo.subject}",
                                    message = "Starting soon at ${classInfo.startTime}",
                                    type = "CLASS"
                                ))
                            }
                        }
                    } catch (e: Exception) { }
                }
            }

            // 2. SCHEDULE ASSIGNMENT DEADLINES
            try {
                val assignmentsSnapshot = firestore.collection("assignments").get().await()
                assignmentsSnapshot.documents.forEach { doc ->
                    val dueDate = doc.getLong("dueDate") ?: 0L
                    val assignmentTitle = doc.getString("title") ?: "Assignment"
                    if (dueDate > 0) {
                        val dueDateTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(dueDate),
                            java.time.ZoneId.systemDefault()
                        )
                        
                        // 24 Hours Before Reminder
                        val alarmTimeOneDay = dueDateTime.minusDays(1)
                        if (alarmTimeOneDay.isAfter(now) && alarmTimeOneDay.isBefore(now.plusDays(1))) {
                            alarmScheduler.schedule(AlarmItem(
                                id = "ASSIGN_24H_${doc.id}",
                                time = alarmTimeOneDay,
                                title = "Assignment Deadline Tomorrow",
                                message = "Submission for $assignmentTitle is due in 24 hours.",
                                type = "ASSIGNMENT"
                            ))
                        }

                        // 1 Hour Before Final Alarm
                        val alarmTimeOneHour = dueDateTime.minusHours(1)
                        if (alarmTimeOneHour.isAfter(now) && alarmTimeOneHour.isBefore(now.plusDays(1))) {
                            alarmScheduler.schedule(AlarmItem(
                                id = "ASSIGN_1H_${doc.id}",
                                time = alarmTimeOneHour,
                                title = "Assignment Due in 1 Hour",
                                message = "Don't forget to submit $assignmentTitle!",
                                type = "ASSIGNMENT"
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-critical
            }

            // 3. SCHEDULE IMPORTANT ANNOUNCEMENTS / SESSIONS
            try {
                // Fetch recent announcements
                val noticeSnapshot = firestore.collection("announcements")
                    .whereGreaterThan("timestamp", System.currentTimeMillis() - 86400000)
                    .get()
                    .await()
                
                noticeSnapshot.documents.forEach { doc ->
                    val noticeTitle = doc.getString("title") ?: "Important Notice"
                    val content = doc.getString("content") ?: ""
                    
                    // If it looks like a session or has a time mentioned, we could try to parse it, 
                    // but for now, we'll send a general reminder for new announcements if they were posted recently.
                    if (content.contains("session", ignoreCase = true) || content.contains("meeting", ignoreCase = true)) {
                         android.util.Log.d("AutoAlarmWorker", "Found session-related notice: $noticeTitle")
                    }
                }
            } catch (e: Exception) { }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
