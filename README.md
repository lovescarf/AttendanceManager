# Attendance Manager 📱

A native Android application built for college students to track, manage, and monitor academic attendance efficiently and entirely offline. Developed as part of a Mobile Application Development (MAD) lab project.

## 🚀 Features
* **Subject & Course Management:** Add and manage custom academic theory subjects and practical labs.
* **Granular Attendance Tracking:** Mark classes as **Present**, **Absent**, or **Cancelled** with real-time state updates.
* **Smart Percentage Calculation:** Automatically computes attendance percentages and visually alerts users when attendance drops below the critical **75% threshold**.
* **Persistent Local Storage:** Powered by **Room Database** (backed by SQLite and KSP) to ensure data is safely stored locally and persists across app sessions.
* **Modern Material 3 UI & Dark Mode:** Designed with a clean peach-themed interface, rounded material cards, and full native support for both Light and Dark themes.
* **Asynchronous Operations:** Built using **Kotlin Coroutines** and **LiveData** for non-blocking database queries and reactive UI updates.

## 🛠️ Tech Stack
* **Language:** Kotlin
* **UI Toolkit:** XML Layouts (ConstraintLayout, LinearLayout, Material Design 3)
* **Local Database:** Room Persistence Library, KSP (Kotlin Symbol Processing)
* **Architecture / Concurrency:** MVVM-inspired structure with Kotlin Coroutines & LiveData
* **Environment:** Android Studio

## 📥 Installation & Setup
1. Clone the repository or download the source code:
   ```bash
   git clone [https://github.com/your-username/attendance-manager.git](https://github.com/your-username/attendance-manager.git)
