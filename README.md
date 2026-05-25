# 🎓 AcadMate: The Ultimate AI-Powered Campus ERP

**Current Version: v1.3.0**

AcadMate is a cutting-edge, comprehensive institutional management system designed to streamline college operations through AI and real-time data. Built with a modern tech stack (Kotlin, Jetpack Compose, Firebase), it provides specialized portals for Administrators, Faculty, and Students.

---

## 🚀 Key Modules & Features

### 🏛️ Institutional Admin Portal
*   **System Command Center**: Centralized dashboard with real-time stats on students, faculty, and system health.
*   **Campus Configuration**: 
    *   **Geofencing**: Set precise GPS coordinates and radius for secure, location-based attendance.
    *   **Academic Cycle**: One-click management of current semesters (S1-S8).
    *   **Structural Setup**: Manage departments, sections, and subject assignments.
*   **Community Management**: Create moderated class groups and monitor institutional activity.
*   **Audit Logs**: Comprehensive tracking of all administrative actions for transparency.

### 🍎 Faculty Portal (The Teaching Suite)
*   **Smart Attendance**: Start ultrasonic or QR-based beacons for secure, proxy-proof attendance.
*   **Lecture Logs**: Digital "Daily Diary" to record topics covered and unit-wise progress.
*   **Student Analytics**: Identify "At-Risk" students (low attendance/scores) for early intervention.
*   **Moderated Communication**: Administer class groups and appoint up to 2 Class Representatives (CRs) for message moderation.
*   **Resource Manager**: Upload and manage study materials, assignments, and gradebooks.

### 🎓 Student Portal (The Learning Hub)
*   **AI Tutor**: 24/7 academic assistance with Markdown and code-block support (powered by Gemini).
*   **Digital ID Card**: Premium identity card with a dynamic QR code for campus services.
*   **Campus Life Hub**:
    *   **Hostel & Mess**: View real-time mess menus and apply for digital outpasses.
    *   **Placement Hub**: Browse job/internship listings with package (LPA) and location details.
    *   **Digital Library**: Search books, track issued items, and monitor due dates.
*   **Academic Dashboard**: Track attendance percentages, upcoming deadlines, and CGPA estimates.
*   **Moderated Groups**: Engage in class communities where content is filtered by CRs/Faculty for quality.

---

## 🛠️ Technical Architecture

### Tech Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (100%)
*   **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
*   **Dependency Injection**: Hilt (Dagger)
*   **Database**: 
    *   **Remote**: Firebase Firestore (Real-time sync)
    *   **Local**: Room DB (Offline support)
*   **Authentication**: Firebase Auth (Phone + OTP, Email/Password)
*   **AI Engine**: Google Generative AI (Gemini 3.1 Flash)
*   **Image Loading**: Coil
*   **Networking**: Ktor

### Security Features
*   **Package Visibility**: Fully compliant with Android 11+ requirements.
*   **App Check**: Integrated Play Integrity for secure backend communication.
*   **Moderation Engine**: Multi-tier approval flow for student-generated content in community groups.

---

## 📂 Project Structure
```text
├── admin/            # Institutional management & configuration
├── ai/               # AI Tutor, Syllabus parser, and Exam simulation
├── app/              # Navigation, Splash, and Application core
├── assignments/      # Task management and Resource sharing
├── attendance/       # Secure geofenced attendance engine
├── auth/             # Multi-role authentication & profile setup
├── core/             # Shared data models, database, and utilities
├── dashboard/        # Role-specific Home screens & Campus services
└── designsystem/     # Reusable UI components, themes, and animations
```

---

## ⚙️ Setup & Installation

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/yourusername/AcadMate.git
    ```
2.  **Firebase Configuration**:
    *   Add your `google-services.json` to the `/app` directory.
    *   Enable Firestore, Auth (Phone/Email), and Storage.
3.  **Local Configuration**:
    *   Add your `GEMINI_API_KEY` to `local.properties`.
4.  **SHA-1 Registration**:
    *   Generate your SHA-1 using `./gradlew signingReport`.
    *   Register the fingerprint in your Firebase Console to enable OTP and App Check.

---

## 🗺️ Roadmap
- [ ] **Fee Payment Integration**: Stripe/Razorpay SDK implementation.
- [ ] **Clubs & Communities**: Dedicated pages for college societies.
- [ ] **Push Notifications**: Real-time alerts for attendance and announcements.
- [ ] **Offline Mode**: Enhanced Room caching for low-connectivity campus areas.

---

**AcadMate** is more than an app; it's a digital transformation for your institution. 🚀
