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

[**⬇️ Download AcadMate v1.1.0-Premium**](https://github.com/aakashdeepyadav/AcadMate/releases/download/v1.1.0/AcadMate-v1.1.0.apk)

---

## 🔐 The 5-Layer Anti-Proxy Protocol
AcadMate solves the "Proxy Problem" in Indian colleges through a mandatory 5-layer verification stack that makes marking attendance for a friend statistically and practically impossible.

1.  **Hardware-Level Binding (Layer 1)**: Each account is cryptographically locked to a single `ANDROID_ID`. "Phone passing" is impossible without Admin approval.
2.  **Acoustic Proximity (Layer 2)**: Uses near-ultrasonic handshakes (18.5kHz) to prove the device is physically located within the classroom's four walls.
3.  **ML Face Liveness (Layer 3)**: Uses Google ML Kit to detect natural blinks and micro-movements, blocking static photo, video, or deepfake spoofing.
4.  **Campus Geofencing (Layer 4)**: High-precision GPS validation using a Ray-casting polygon algorithm ensures the user is within the registered institutional boundaries.
5.  **Continuous Presence Monitoring (Layer 5)**: A foreground service maintains a secure connection and location heartbeats throughout the lecture duration.

---

## 🏛️ Comprehensive Role-Based Ecosystem

AcadMate adapts its entire interface and permission set based on the institutional role. All roles are unified under a single **Registration Number (regNo)** identity system + **OTP Phone Authentication** for 100% data consistency.

### 1. Institutional Administrator (The Command Center)
The Admin portal is designed for high-level oversight and system integrity.
*   **User Governance**: Full CRUD lifecycle management. Admins create all user profiles (Students, Faculty) and assign dynamic roles.
*   **Course & Campus Architecture**: Define subjects, subject codes, credit hours, and configure geographical boundaries for campus geofencing.
*   **Schedule Management**: Create and modify the global institutional timetable. Changes propagate instantly to all Faculty and Student dashboards.
*   **Institutional Pulse**: A live dashboard displaying campus-wide active sessions, average attendance, and pending anomalies.
*   **Audit Logs & Notice Board**: Real-time security feed of all administrative actions, plus a high-priority broadcast system for campus-wide notices.

### 2. Faculty (The Classroom Orchestrator)
A suite of tools designed to reduce administrative load and maximize teaching time.
*   **Classroom Management**: Initiate secure attendance sessions, view real-time live attendance logs with instant UI updates, and manage manual overrides.
*   **Smart Gradebook & Assignments**: Create dynamic assignments and grade student submissions directly within a unified Gradebook tied to Firestore.
*   **Syllabus Builder**: Publish dynamic curricula and track course progression. Data pushes directly to the students' Syllabus Analyzer.
*   **Resource Manager**: Upload and manage study materials and lecture notes.
*   **Leave Management**: Approve or deny digital leave applications from students.

### 3. Student (The Intelligent Learner)
A personal companion that handles the "mechanics" of college life, allowing the student to focus on learning.
*   **Smart Dashboard**: "Luminous Scholar" deep-indigo aesthetic featuring a visual Attendance Hero, daily schedule highlights, and Quick Action hubs.
*   **Academic Hub**: Instant access to real-time Timetables, Assignments, Study Materials, and Campus Notices.
*   **Results & GPA Simulator**: View detailed semester-wise grades and use the interactive GPA Simulator to predict future CGPA requirements.
*   **5-Layer Secure Attendance**: Frictionless verification powered by Google Play Integrity, including a live **Active Sessions Browser** for students to search and join running classes in real-time.
*   **Leave Applications**: Apply for institutional leave digitally with status tracking.

### 4. AI Powerhouse Suite (Cross-Role)
*   **AI Lecture Summarizer**: Record audio during lectures and instantly convert it into structured Markdown notes using Speech-to-Text and Gemini AI.
*   **AI Tutor & Exam Simulator**: Context-aware chatbot that quizzes students based on their specific syllabus gaps.
*   **Tech Interview Prep**: Practice technical interviews with an AI bot tailored specifically for CSE students, receiving real-time feedback on coding concepts and communication.

---

## 🛠️ Technology & Architecture

### **Advanced Core Stack**
*   **UI Engine**: 100% Jetpack Compose using Material Design 3 guidelines. Features a custom "Luminous Scholar" (Deep Indigo) premium aesthetic with mesh gradients and glassmorphism.
*   **Dependency Injection**: Hilt (Dagger) for institutional-scale dependency management.
*   **Persistence**: Cloud Firestore (Real-time sync) with offline-caching capabilities.
*   **State Management**: MVVM Architecture with Kotlin Flow/StateFlow for reactive UI updates.
*   **AI Integration**: Firebase Vertex AI / Gemini API for intelligent tutoring.
*   **Security Stack**: Android Biometric API + Google Play Integrity API.

### **9-Module Scalable Architecture**
The project is strictly modularized to reduce build times and enforce separation of concerns:
- `:app`: Main entry point, navigation graphs, and application shell.
- `:auth`: Identity management, OTP Verification, and User creation flows.
- `:attendance`: The 6-layer verification engine, acoustic processing, and geofencing.
- `:ai`: Learning suite, Gemini tutor integration, and audio-to-text summarization.
- `:dashboard`: Specialized Home and Profile screens for Student, Faculty, and Admin roles.
- `:admin`: Institutional management tools (Timetables, Courses, Logs).
- `:assignments`: Grading system, Gradebook, student submissions, and Resource Manager.
- `:designsystem`: Unified "Academic Precision" UI library (Buttons, Cards, Theme).
- `:core`: Shared entities, database repositories, Alarms, and networking utilities.

---

## 🚀 Deployment & Developer Setup

### **Mandatory Configuration**
1.  **Clone the Repository**: `git clone https://github.com/aakashdeepyadav/AcadMate.git`
2.  **API Keys**: Add `GEMINI_API_KEY=your_gemini_key` to your `local.properties` file.
3.  **Firebase Setup**: 
    *   Create a Firebase project.
    *   Add your `google-services.json` to the `app/` folder.
    *   Enable **Phone Authentication** (OTP) and **Firestore Database**.
    *   Register your **SHA-1 and SHA-256** fingerprints in the Firebase Console.
4.  **Integrity API**: Enable **Play Integrity API** in the Google Cloud Console for Anti-Proxy features.
5.  **Storage**: Initialize **Firebase Storage** for profile pictures and Resource Manager.

### **Institutional Test Credentials**
The system enforces **Two-Step Verification (Password + OTP)** for all accounts. To test the app, ensure the phone numbers are whitelisted in your Firebase Authentication console for testing.

| Role | ID (Registration No.) | Password | MFA Requirement |
| :--- | :--- | :--- | :--- |
| **Student** | `12345678` | `Password@123` | Phone OTP |
| **Faculty** | `100001` | `Password@123` | Phone OTP |
| **Admin** | `9001` | `Password@123` | Phone OTP |

---
## ⚠️ Security Notice
This repository excludes sensitive configuration files such as `google-services.json` and `local.properties`. If you fork this project, you **must** provide your own Firebase configuration to make the app functional.

---
*Built for the next generation of academic excellence. Smart India Hackathon 2025.*
