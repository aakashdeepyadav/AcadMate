# AcadMate: The Elite Academic Operating System

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/android)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-blue.svg)](https://developer.android.com/topic/architecture)
[![Backend](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![Security](https://img.shields.io/badge/Security-5--Layer%20Anti--Proxy-red.svg)]()

AcadMate is an enterprise-grade academic management ecosystem designed to eliminate institutional friction. It integrates a **proprietary 5-layer anti-proxy attendance protocol** with a **context-aware AI Learning Suite**, providing a unified, production-ready infrastructure for Administrators, Faculty, and Students. 

This multi-module Android application handles the end-to-end lifecycle of a higher education institution, from deep curriculum building to automated grading, all driven by a real-time Firestore backend.

---

## 📱 Download Latest APK
You can download the latest production-ready APK from the link below:

[**⬇️ Download AcadMate v1.2.0-Elite**](https://github.com/aakashdeepyadav/AcadMate/releases/download/v1.1.0/AcadMate-v1.1.0.apk)

---

## 🔐 The 5-Layer Anti-Proxy Protocol
AcadMate solves the "Proxy Problem" in Indian colleges through a mandatory 5-layer verification stack that makes marking attendance for a friend statistically and practically impossible.

1.  **Hardware-Level Binding (Layer 1)**: Each account is cryptographically locked to a single `ANDROID_ID`. "Phone passing" is impossible without Admin approval.
2.  **Acoustic Proximity (Layer 2)**: Uses **continuous infinite-looping** near-ultrasonic handshakes (18.5kHz) to prove the device is physically located within the classroom.
3.  **ML Face Liveness (Layer 3)**: Uses Google ML Kit to detect natural blinks and micro-movements. **Mandatory Profile Photo** enrollment ensures biometric integrity.
4.  **Campus Geofencing (Layer 4)**: High-precision GPS validation with real-time distance feedback (e.g., "Off-campus: 120m away") ensures the user is within institutional boundaries.
5.  **Continuous Presence Monitoring (Layer 5)**: A foreground service maintains a secure connection and location heartbeats throughout the lecture duration.

---

## 🏛️ Comprehensive Role-Based Ecosystem

AcadMate adapts its entire interface and permission set based on the institutional role. All roles are unified under a single **Registration Number (regNo)** identity system + **OTP Phone Authentication** (with smart-lookup fallback) for 100% data consistency.

### 1. Institutional Administrator (The Command Center)
The Admin portal is designed for high-level oversight and system integrity.
*   **User Governance**: Full CRUD lifecycle management including **Edit User Info** capabilities. Admins create profiles (Students, Faculty) and assign dynamic roles.
*   **Centralized Syllabus Management**: Define the master curriculum with multi-unit structures and specific topics. Changes propagate instantly to all roles.
*   **Smart AI Scheduler**: Generate optimized timetables using **Gemini AI** with support for **Natural Language Suggestions** (e.g., "No labs on Monday") and **Room Inventory** management.
*   **Institutional Pulse**: A real-time dashboard displaying campus-wide active sessions, **actual average attendance**, and pending security anomalies.

### 2. Faculty (The Classroom Orchestrator)
A suite of tools designed to reduce administrative load and maximize teaching time.
*   **Live Attendance Pulse**: Initiate secure sessions and view a **real-time presence list** of students as they mark themselves present.
*   **Manual Override**: Faculty can manually toggle attendance for any student, providing a reliable fallback for technical or environmental edge cases.
*   **Teaching Schedule**: A personalized daily timetable showing exactly when and where their next classes are scheduled.
*   **Dynamic Dashboard**: Real-time analytics on personal teaching load, average session attendance, and active assignment counts.

### 3. Student (The Intelligent Learner)
A personal companion that handles the "mechanics" of college life, allowing the student to focus on learning.
*   **Smart Auto-Alarm**: Automatically schedules a high-priority system alarm **1 hour before the first class** of the day.
*   **Alarm Customization**: Students can modify alarm volume, vibration, and tones within the Manage Notifications suite.
*   **Academic Hub**: Instant access to real-time Timetables, Assignments, **Syllabus Progress**, and Campus Notices.
*   **Luminous Scholar Aesthetic**: Deep-indigo premium interface featuring a visual Attendance Hero and interactive GPA Simulator.

---

## 🛠️ Technology & Architecture

### **Advanced Core Stack**
*   **UI Engine**: 100% Jetpack Compose using Material Design 3 guidelines.
*   **AI Integration**: Firebase Vertex AI / Gemini Pro API for **Natural Language Scheduling** and Intelligent Tutoring.
*   **Security Stack**: Android Biometric API + Google Play Integrity API + Acoustic Signal Processing.
*   **State Management**: MVVM Architecture with Kotlin Flow/StateFlow for reactive UI updates.

### **9-Module Scalable Architecture**
- `:app`: Main entry point, role-aware navigation graphs, and application shell.
- `:auth`: Identity management, OTP Verification (Smart-Lookup), and User creation flows.
- `:attendance`: The 5-layer verification engine, infinite acoustic processing, and geofencing.
- `:ai`: Learning suite, Gemini-driven scheduling, and audio-to-text summarization.
- `:dashboard`: Specialized Home, Profile, and **Timetable** screens for all roles.
- `:admin`: Institutional management tools (Timetables, **Master Syllabus**, Courses).
- `:assignments`: Grading system, Gradebook, student submissions, and Resource Manager.
- `:designsystem`: Unified "Academic Precision" UI library (Buttons, Cards, Themes).
- `:core`: Shared entities, database repositories, **Alarm Schedulers**, and networking utilities.

---

## 🚀 Deployment & Developer Setup

### **Mandatory Configuration**
1.  **Clone the Repository**: `git clone https://github.com/aakashdeepyadav/AcadMate.git`
2.  **API Keys**: Add `GEMINI_API_KEY=your_gemini_key` to your `local.properties` file.
3.  **Firebase Setup**: 
    *   Add your `google-services.json` to the `app/` folder.
    *   Enable **Phone Authentication** (OTP) and **Firestore Database**.
4.  **Integrity API**: Enable **Play Integrity API** in the Google Cloud Console for Anti-Proxy features.

### **Institutional Test Credentials**
The system enforces **Two-Step Verification (Password + OTP)** for all accounts.

| Role | ID (Registration No.) | Password | MFA Requirement |
| :--- | :--- | :--- | :--- |
| **Student** | `12345678` | `Password@123` | Phone OTP |
| **Faculty** | `100001` | `Password@123` | Phone OTP |
| **Admin** | `9001` | `Password@123` | Phone OTP |

---
## ⚠️ Security Notice
This repository excludes sensitive configuration files such as `google-services.json` and `local.properties`. You **must** provide your own Firebase configuration to make the app functional.

---
*Built for the next generation of academic excellence. Smart India Hackathon 2025.*
