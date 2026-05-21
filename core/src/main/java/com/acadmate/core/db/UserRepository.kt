package com.acadmate.core.db

import com.acadmate.core.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val timetableDao: TimetableDao,
    private val syllabusGapDao: SyllabusGapDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getCurrentUser(): Flow<UserEntity?> {
        return userDao.getCurrentUser()
    }

    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUser(userId).firstOrNull()
    }

    suspend fun saveUserProfile(
        userId: String,
        name: String,
        email: String,
        phoneNumber: String,
        enrollmentNumber: String? = null,
        department: String? = null,
        section: String? = null,
        role: String,
        profilePictureUrl: String? = null,
        address: String? = null
    ): Result<Unit> {
        return try {
            val userEntity = UserEntity(
                id = userId,
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                regNo = enrollmentNumber,
                department = department,
                section = section,
                role = UserRole.fromString(role),
                profilePictureUrl = profilePictureUrl,
                address = address
            )

            // Save to local database
            userDao.insertUser(userEntity)

            // Save to Firestore
            val userMap = hashMapOf(
                "id" to userId,
                "name" to name,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "regNo" to enrollmentNumber,
                "department" to department,
                "section" to section,
                "role" to role,
                "profilePictureUrl" to profilePictureUrl,
                "address" to address,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(userId).set(userMap).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePicture(userId: String, profilePictureUrl: String): Result<Unit> {
        return try {
            // Update local database
            val currentUser = userDao.getUser(userId).firstOrNull()
                ?: userDao.getCurrentUser().firstOrNull()
                
            currentUser?.let {
                val updatedUser = it.copy(
                    profilePictureUrl = profilePictureUrl,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(updatedUser) // Use insertUser (REPLACE) to be safe
            }

            // Update Firestore - Find the document by the 'id' field first
            val query = firestore.collection("users").whereEqualTo("id", userId).get().await()
            if (!query.isEmpty) {
                val docId = query.documents[0].id
                firestore.collection("users").document(docId)
                    .update("profilePictureUrl", profilePictureUrl, "updatedAt", System.currentTimeMillis())
                    .await()
            } else {
                // Fallback to updating by document ID directly if no document has this 'id' field
                firestore.collection("users").document(userId)
                    .update("profilePictureUrl", profilePictureUrl, "updatedAt", System.currentTimeMillis())
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAddress(userId: String, address: String): Result<Unit> {
        return try {
            val currentUser = userDao.getUser(userId).firstOrNull()
                ?: userDao.getCurrentUser().firstOrNull()
                
            currentUser?.let {
                val updatedUser = it.copy(
                    address = address,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(updatedUser)
            }

            val query = firestore.collection("users").whereEqualTo("id", userId).get().await()
            val docId = if (!query.isEmpty) query.documents[0].id else userId

            firestore.collection("users").document(docId)
                .update("address", address, "updatedAt", System.currentTimeMillis())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, name: String, email: String, address: String): Result<Unit> {
        return try {
            val currentUser = userDao.getUser(userId).firstOrNull()
                ?: userDao.getCurrentUser().firstOrNull()
                
            currentUser?.let {
                val updatedUser = it.copy(
                    name = name,
                    email = email,
                    address = address,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.insertUser(updatedUser)
            }

            val query = firestore.collection("users").whereEqualTo("id", userId).get().await()
            val docId = if (!query.isEmpty) query.documents[0].id else userId

            firestore.collection("users").document(docId)
                .update(
                    mapOf(
                        "name" to name,
                        "email" to email,
                        "address" to address,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFromFirestore(userId: String): Result<UserEntity> {
        return try {
            var document: com.google.firebase.firestore.DocumentSnapshot? = null
            
            // First try to find by `id` field and prefer documents that have a 'role' (real documents)
            val query = firestore.collection("users").whereEqualTo("id", userId).get().await()
            if (!query.isEmpty) {
                document = query.documents.find { it.contains("role") } ?: query.documents[0]
            }

            // If not found by `id`, fallback to checking document ID directly
            if (document == null || !document.exists()) {
                val directDoc = firestore.collection("users").document(userId).get().await()
                if (directDoc.exists()) {
                    document = directDoc
                }
            }
            
            // If still not found, try finding by email or phone (for admin-created users)
            if (document == null || !document.exists()) {
                val email = auth.currentUser?.email
                val phone = auth.currentUser?.phoneNumber

                if (!email.isNullOrBlank()) {
                    val emailQuery = firestore.collection("users").whereEqualTo("email", email).get().await()
                    if (!emailQuery.isEmpty) {
                        document = emailQuery.documents.find { it.contains("role") } ?: emailQuery.documents[0]
                    }
                }

                if ((document == null || !document.exists()) && !phone.isNullOrBlank()) {
                    // Try exact match
                    val phoneQuery = firestore.collection("users").whereEqualTo("phoneNumber", phone).get().await()
                    if (!phoneQuery.isEmpty) {
                        document = phoneQuery.documents.find { it.contains("role") } ?: phoneQuery.documents[0]
                    } else {
                        // Try without country code or with country code if saved differently
                        val phoneWithoutPrefix = phone.removePrefix("+91")
                        val altPhoneQuery = firestore.collection("users").whereEqualTo("phoneNumber", phoneWithoutPrefix).get().await()
                        if (!altPhoneQuery.isEmpty) {
                            document = altPhoneQuery.documents.find { it.contains("role") } ?: altPhoneQuery.documents[0]
                        }
                    }
                }
            }

            if (document != null && document.exists()) {
                val data = document.data
                val user = UserEntity(
                    id = data?.get("id") as? String ?: userId,
                    name = data?.get("name") as? String ?: "",
                    email = data?.get("email") as? String ?: "",
                    phoneNumber = data?.get("phoneNumber") as? String ?: "",
                    regNo = data?.get("regNo") as? String ?: data?.get("enrollmentNumber") as? String,
                    department = data?.get("department") as? String,
                    section = data?.get("section") as? String,
                    role = UserRole.fromString(data?.get("role") as? String),
                    profilePictureUrl = data?.get("profilePictureUrl") as? String,
                    address = data?.get("address") as? String,
                    isFirstLogin = data?.get("isFirstLogin") as? Boolean ?: false,
                    createdAt = data?.get("createdAt") as? Long ?: System.currentTimeMillis(),
                    updatedAt = data?.get("updatedAt") as? Long ?: System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserData(userId: String) {
        try {
            val firestoreUser = getUserFromFirestore(userId)
            firestoreUser.getOrNull()?.let { user ->
                userDao.insertUser(user)
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Sync failed: ${e.message}")
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    suspend fun clearLocalData() {
        userDao.clearAllUsers()
        timetableDao.clearTimetable()
        syllabusGapDao.clearAllGaps()
        // Clear persistence for a clean logout
        try {
            firestore.clearPersistence().await()
        } catch (e: Exception) {
            // Silently fail if firestore is not initialized or other errors
        }
    }
}
