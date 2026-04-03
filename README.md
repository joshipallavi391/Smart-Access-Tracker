# Smart Access Tracker

An RFID-based real-time attendance tracking system using ESP32,
Google Sheets, Firebase, and an Android App.

---

## Working Flow

RFID Card → RC522 Reader → ESP32 → WiFi → Google Apps Script
→ Google Sheets → Firebase Firestore → Android App

---

## Features

- Contactless attendance using RFID cards
- Real-time data sync to cloud
- Mobile app for viewing attendance records
- Audio (buzzer) and visual (LED) feedback on card scan
- Daily and monthly attendance reports

---

## Hardware Used

- ESP32 microcontroller
- RC522 RFID Reader
- RFID Cards
- Buzzer
- LED

---

## Software & Tools Used

- Arduino IDE (ESP32 programming)
- Google Apps Script (HTTP request handling)
- Google Sheets (intermediate data storage)
- Firebase Firestore (cloud database)
- Android Studio (mobile app development)

---

## Project Structure

Smart-Access-Tracker/
│
├── Android_App/        → Android app source code + APK link
├── ESP32_Code/         → Arduino code for ESP32
├── Firebase/           → Firestore structure + configuration
├── Google_Apps_Script/ → Apps Script code + deployment steps

---

## Setup Instructions

### Step 1 – ESP32
1. Open .ino file in Arduino IDE
2. Enter your WiFi credentials (SSID & Password)
3. Paste your Google Apps Script Web App URL
4. Upload code to ESP32

### Step 2 – Google Apps Script
1. Open Code.gs in Google Apps Script
2. Click Deploy → New Deployment → Web App
3. Set access to "Anyone"
4. Copy the Web App URL
5. Paste it in ESP32 code

### Step 3 – Firebase
1. Create a Firebase project
2. Enable Firestore Database
3. Replace google-services.json with your own config
4. Update database rules if expired

### Step 4 – Android App
1. Download APK from the link in Android_App/README.md
2. Install on Android device
3. Connect to internet and open the app

---

## APK Download

See Android_App/README.md for the download link.

---

## Institution

Banasthali Vidyapith
School of Automation
2025-26
