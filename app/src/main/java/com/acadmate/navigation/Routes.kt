package com.acadmate.navigation

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    object Splash

    @Serializable
    object AuthGraph

    @Serializable
    object Onboarding

    @Serializable
    object RoleSelection

    @Serializable
    object Login

    @Serializable
    data class Otp(val phoneNumber: String)

    @Serializable
    object MainGraph

    @Serializable
    object Home

    @Serializable
    object Attendance

    @Serializable
    data class FacultyAttendance(val classId: String, val hour: Int? = null, val date: String? = null)

    @Serializable
    data class FacultyMarkAttendance(val classId: String)

    @Serializable
    object QrScanner

    @Serializable
    data class MarkAttendance(val classId: String)

    @Serializable
    object ActiveSessionsBrowser

    @Serializable
    object AiSuite

    @Serializable
    data class AiChat(val subject: String? = null, val mode: String = "TUTOR")

    @Serializable
    object Syllabus

    @Serializable
    object SyllabusBrowser

    @Serializable
    object ManageSyllabus

    @Serializable
    object SyllabusResult

    @Serializable
    object MockExamSetup

    @Serializable
    object MockExam

    @Serializable
    object MockResult

    @Serializable
    object Timetable

    @Serializable
    object LeaveApplication

    @Serializable
    object LeaveManagement

    @Serializable
    object Profile

    @Serializable
    object Notifications

    @Serializable
    object ManageNotifications

    @Serializable
    object Privacy

    @Serializable
    object Help

    @Serializable
    object About

    @Serializable
    object FocusMode

    @Serializable
    object LectureNotes

    @Serializable
    object DoubtMarketplace

    @Serializable
    object NoticeBoard

    @Serializable
    object LectureLogs

    @Serializable
    object PerformanceAnalytics

    @Serializable
    object DigitalIdCard

    @Serializable
    object PlacementHub

    @Serializable
    object CampusEvents

    @Serializable
    object HostelManagement

    @Serializable
    object DigitalLibrary

    @Serializable
    object FeePayment

    @Serializable
    object CampusClubs

    @Serializable
    object CommunityGroups

    @Serializable
    object FacultyCommunity

    @Serializable
    object ManageCommunity

    @Serializable
    data class GroupChat(val groupId: String)

    @Serializable
    object AdminGraph

    @Serializable
    object AdminDashboard

    @Serializable
    object CreateUser

    @Serializable
    object CampusSetup

    @Serializable
    object AuditLog

    @Serializable
    object AdminLeaveRequests

    @Serializable
    object AdminEvents

    @Serializable
    object AdminAnalytics

    @Serializable
    object AssignmentGraph

    @Serializable
    object AssignmentList

    @Serializable
    object CreateAssignment

    @Serializable
    data class AssignmentDetail(val assignmentId: String)

    @Serializable
    object Register

    @Serializable
    data class RegisterOtp(val phoneNumber: String)

    @Serializable
    data class RegisterProfile(val phoneNumber: String, val verificationToken: String)

    @Serializable
    data class ResetPassword(val userId: String)

    @Serializable
    object UpdateProfile

    @Serializable
    object Results

    @Serializable
    object Gradebook

    @Serializable
    object ResourceManager

    @Serializable
    object CourseManagement

    @Serializable
    object TimetableManagement

    @Serializable
    object AiTimetableGenerator
}
