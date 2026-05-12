# AcadMate: The Elite Academic Operating System
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/android)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20Modular-blue.svg)](https://developer.android.com/topic/architecture)
[![Backend](https://img.shields.io/badge/Backend-Firebase-orange.svg)](https://firebase.google.com/)
[![Security](https://img.shields.io/badge/Security-6--Layer%20Anti--Proxy-red.svg)]()

AcadMate is an enterprise-grade academic management ecosystem designed to eliminate institutional friction. It integrates a **proprietary 6-layer anti-proxy attendance protocol** with a **context-aware AI Learning Suite**, providing a unified infrastructure for Administrators, Faculty, and Students.

---

## 📱 Download Latest APK
You can download the latest production-ready APK from the link below:

[**⬇️ Download AcadMate v1.1.0-Premium**](https://github.com/aakashdeepyadav/AcadMate/releases/download/v1.1.0/AcadMate-v1.1.0.apk)

---

## 🔐 The 6-Layer Anti-Proxy Protocol
AcadMate solves the "Proxy Problem" in Indian colleges through a mandatory 6-layer verification stack that makes marking attendance for a friend statistically and practically impossible.

1.  **Hardware-Level Binding (Layer 1)**: Each account is cryptographically locked to a single `ANDROID_ID`. "Phone passing" is impossible as you cannot log into your account on another device without Admin approval.
2.  **Acoustic Proximity (Layer 2)**: Uses near-ultrasonic handshakes (18.5kHz) to prove the device is physically located within the classroom's four walls (Solves the "Indoor GPS" accuracy problem).
3.  **ML Face Liveness (Layer 3)**: Uses Google ML Kit to detect natural blinks and micro-movements, blocking static photo, video, or synthetic deepfake spoofing.
4.  **Campus Geofencing (Layer 4)**: High-precision GPS validation using a Ray-casting polygon algorithm ensures the user is within the registered institutional boundaries.
5.  **Continuous Presence Monitoring (Layer 5)**: A foreground service maintains a secure connection and location heartbeats throughout the lecture duration, flagging devices that leave the area early.
6.  **Biometric Re-authentication (Layer 6)**: Randomized fingerprint or FaceID checks during long sessions (triggered every 30 mins) ensure the student hasn't left their device in the classroom while they are physically absent.

---

## 🏛️ Comprehensive Role-Based Ecosystem

AcadMate adapts its entire interface and permission set based on the institutional role, ensuring that every user has exactly what they need to succeed.

### 1. Institutional Administrator (The Command Center)
The Admin portal is designed for high-level oversight and system integrity.
*   **Dynamic User Governance**: Full lifecycle management (CRUD) of the institutional database. Admins pre-authorize students and faculty, preventing unauthorized account creation.
*   **Course Catalog Architecture**: Define subjects, subject codes, and credit hours. Build the foundational academic map of the institution.
*   **Faculty-Subject Binding**: Real-time assignment of professors to specific courses. Changes propagate instantly across the network to faculty and student schedules.
*   **Institutional Pulse**: A live dashboard displaying campus-wide average attendance, active teaching sessions, and user growth metrics using real-time Firestore analytics.
*   **Global Announcements**: A high-priority broadcast system to communicate urgent notices (holidays, exam schedules) to the entire campus instantly.

### 2. Faculty (The Classroom Orchestrator)
A suite of tools designed to reduce administrative load and maximize teaching time.
*   **Acoustic Session Broadcasting**: One-tap initiation of secure attendance sessions using high-frequency (18.5kHz) **Acoustic Tokens** and BLE beacons.
*   **Smart Gradebook & Auto-Grader**: A centralized hub to view student submissions. Features an automated grading engine that maps marks to institutional grades (A+ to F).
*   **Live Attendance Monitoring**: Real-time view of students entering the "Secure Zone" during a lecture, with the ability to manually override or audit records.
*   **Automated Teaching Schedule**: A dynamic view of the day's teaching commitments fetched directly from the institutional timetable.
*   **Task Management**: Create, distribute, and track assignments with integrated deadline reminders for students.

### 3. Student (The Intelligent Learner)
A personal companion that handles the "mechanics" of college life, allowing the student to focus on learning.
*   **6-Layer Secure Attendance**: A frictionless, 10-second verification process that proves presence within the classroom walls.
*   **Contextual AI Professor (Gemini 3 Flash)**: An AI tutor that knows your real assignments, attendance history, and specific syllabus gaps to provide personalized help.
*   **Academic Transcript & Visualized Performance**: Real-time tracking of CGPA/SGPA with interactive health rings for attendance percentages per subject.
*   **Smart Timetable & Auto-Alarms**: Automated class reminders with a built-in alarm scheduler (Android AlarmManager integration) and "Focus Mode" to block distractions.
*   **Syllabus Gap Analysis**: AI-powered analysis of uploaded syllabus vs. covered topics, identifying weak areas and suggesting resources.

---

## 🛠️ Technology & Architecture

### **Advanced Core Stack**
*   **UI Engine**: 100% Jetpack Compose for a modern, fluid, and declarative user interface.
*   **Dependency Injection**: Hilt (Dagger) for institutional-scale dependency management.
*   **Persistence**: Room DB (Offline-First cache) + Cloud Firestore (Real-time sync).
*   **AI Integration**: Firebase Vertex AI for Gemini-powered tutoring and context.
*   **Security Stack**: Android Biometric API + Google Play Integrity API + Hardware ID Cryptography.

### **9-Module Scalable Architecture**
The project is divided into specialized Gradle modules:
- `:app`: Main entry point and application shell.
- `:auth`: Identity management, 2-step verification (MFA), and phone authentication.
- `:attendance`: The 6-layer verification engine, acoustic processing, and geofencing.
- `:ai`: Learning suite, Gemini tutor integration, and syllabus analysis tools.
- `:dashboard`: Specialized home screens for Student, Faculty, and Admin roles.
- `:admin`: Institutional management tools for users and courses.
- `:assignments`: Grading system, student submissions, and task tracking.
- `:designsystem`: Unified "Academic Precision" UI library.
- `:core`: Shared entities, database layers, and networking utilities.

---

## 🚀 Deployment & Developer Setup

### **Mandatory Configuration**
1.  **Clone the Repository**: `git clone https://github.com/aakashdeepyadav/AcadMate.git`
2.  **API Keys**: Add `GEMINI_API_KEY=your_key` to your `local.properties` file.
3.  **Firebase Setup**: 
    *   Create a Firebase project.
    *   Add `google-services.json` to the `app/` folder (This file is ignored by git for security).
    *   Enable **Phone Authentication** and **Firestore**.
    *   Register your **SHA-256** fingerprint in the Firebase Console.
4.  **Integrity API**: Enable **Play Integrity API** in the Google Cloud Console.
5.  **Storage**: Initialize **Firebase Storage** for profile pictures and assignment file handling.

### **Institutional Test Credentials**
The system enforces **Two-Step Verification (MFA)** for all accounts.

| Role | ID (Username) | Password | MFA Requirement |
| :--- | :--- | :--- | :--- |
| **Student** | `12345678` | `Password@123` | Phone OTP |
| **Faculty** | `100001` | `Password@123` | Phone OTP |
| **Admin** | `9001` | `Password@123` | Phone OTP |


---
## ⚠️ Security Notice
This repository excludes sensitive configuration files such as `google-services.json` and `local.properties`. If you fork this project, you **must** provide your own Firebase configuration to make the app functional.

---
*Built for the next generation of academic excellence. Smart India Hackathon 2025.*
