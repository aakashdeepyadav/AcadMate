package com.acadmate.core.db

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    private val timetableDao: TimetableDao
) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getTimetableForDay(day: Int): Flow<List<TimetableEntity>> {
        return timetableDao.getTimetableForDay(day)
    }

    suspend fun getTimetableForDaySync(day: Int): List<TimetableEntity> {
        return timetableDao.getTimetableForDaySync(day)
    }

    suspend fun updateAlarmStatus(id: String, isAlarmSet: Boolean, userId: String? = null) {
        timetableDao.updateAlarmStatus(id, isAlarmSet)
        
        userId?.let { uid ->
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("timetable")
                    .document(id)
                    .update("isAlarmSet", isAlarmSet)
                    .await()
            } catch (e: Exception) {
                // Handle or log sync error
            }
        }
    }

    suspend fun insertTimetable(items: List<TimetableEntity>) {
        timetableDao.insertTimetable(items)
    }

    suspend fun initializeSixthSemTimetable() {
        val slots = mutableListOf<TimetableEntity>()
        
        // Monday
        slots.add(TimetableEntity("MON_09", 1, "CSE225: Android Apps", "", "09:00", "10:00", "B-401", 0xFF4A90E2.toInt()))
        slots.add(TimetableEntity("MON_10", 1, "CSE225: Android Apps", "", "10:00", "11:00", "B-401", 0xFF4A90E2.toInt()))
        slots.add(TimetableEntity("MON_11", 1, "CSE332: Industry Ethics", "", "11:00", "12:00", "B-401", 0xFF50E3C2.toInt()))
        slots.add(TimetableEntity("MON_13", 1, "PES319: Soft Skills-II", "", "13:00", "14:00", "C-102", 0xFF9B59B6.toInt()))
        slots.add(TimetableEntity("MON_14", 1, "PES319: Soft Skills-II", "", "14:00", "15:00", "C-102", 0xFF9B59B6.toInt()))

        // Tuesday
        slots.add(TimetableEntity("TUE_09", 2, "CSE357: Combinatorial Studies", "", "09:00", "10:00", "B-402", 0xFFF5A623.toInt()))
        slots.add(TimetableEntity("TUE_10", 2, "CSE357: Combinatorial Studies", "", "10:00", "11:00", "B-402", 0xFFF5A623.toInt()))
        slots.add(TimetableEntity("TUE_11", 2, "INT345: Computer Vision", "", "11:00", "12:00", "Lab-2", 0xFFBD10E0.toInt()))
        slots.add(TimetableEntity("TUE_13", 2, "INT345: Computer Vision", "", "13:00", "14:00", "Lab-2", 0xFFBD10E0.toInt()))
        slots.add(TimetableEntity("TUE_14", 2, "CSE332: Industry Ethics", "", "14:00", "15:00", "B-401", 0xFF50E3C2.toInt()))

        // Wednesday
        slots.add(TimetableEntity("WED_09", 3, "CSE225: Android Lab", "", "09:00", "10:00", "Lab-5", 0xFF4A90E2.toInt()))
        slots.add(TimetableEntity("WED_10", 3, "CSE225: Android Lab", "", "10:00", "11:00", "Lab-5", 0xFF4A90E2.toInt()))
        slots.add(TimetableEntity("WED_11", 3, "INT345: CV Lab", "", "11:00", "12:00", "Lab-2", 0xFFBD10E0.toInt()))
        slots.add(TimetableEntity("WED_13", 3, "INT345: CV Lab", "", "13:00", "14:00", "Lab-2", 0xFFBD10E0.toInt()))
        slots.add(TimetableEntity("WED_14", 3, "PES319: Soft Skills-II", "", "14:00", "15:00", "C-102", 0xFF9B59B6.toInt()))

        // Thursday
        slots.add(TimetableEntity("THU_09", 4, "CSE357: Comb. Lab", "", "09:00", "10:00", "Lab-3", 0xFFF5A623.toInt()))
        slots.add(TimetableEntity("THU_10", 4, "CSE357: Comb. Lab", "", "10:00", "11:00", "Lab-3", 0xFFF5A623.toInt()))
        slots.add(TimetableEntity("THU_11", 4, "INT345: Computer Vision", "", "11:00", "12:00", "B-401", 0xFFBD10E0.toInt()))

        // Friday
        slots.add(TimetableEntity("FRI_09", 5, "CSE225: Android Apps", "", "09:00", "10:00", "B-401", 0xFF4A90E2.toInt()))
        slots.add(TimetableEntity("FRI_10", 5, "CSE357: Combinatorial Studies", "", "10:00", "11:00", "B-402", 0xFFF5A623.toInt()))

        timetableDao.clearTimetable()
        timetableDao.insertTimetable(slots)
    }

    suspend fun syncGlobalTimetable() {
        try {
            val snapshot = firestore.collection("global_timetable")
                .get()
                .await()

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                TimetableEntity(
                    id = doc.id,
                    dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: 1,
                    subject = doc.getString("subject") ?: "",
                    faculty = doc.getString("faculty") ?: "",
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: "",
                    room = doc.getString("room") ?: "",
                    color = doc.getLong("color")?.toInt() ?: 0xFF4A90E2.toInt(),
                    isAlarmSet = false // Alarms are local to the user
                )
            }

            if (remoteItems.isNotEmpty()) {
                timetableDao.clearTimetable()
                timetableDao.insertTimetable(remoteItems)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun syncTimetable(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("timetable")
                .get()
                .await()

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                TimetableEntity(
                    id = doc.id,
                    dayOfWeek = doc.getLong("dayOfWeek")?.toInt() ?: 1,
                    subject = doc.getString("subject") ?: "",
                    faculty = doc.getString("faculty") ?: "",
                    startTime = doc.getString("startTime") ?: "",
                    endTime = doc.getString("endTime") ?: "",
                    room = doc.getString("room") ?: "",
                    color = doc.getLong("color")?.toInt() ?: 0,
                    isAlarmSet = doc.getBoolean("isAlarmSet") ?: false
                )
            }

            if (remoteItems.isNotEmpty()) {
                timetableDao.clearTimetable()
                timetableDao.insertTimetable(remoteItems)
            }
        } catch (e: Exception) {
            // Log error or handle offline state
        }
    }
}
