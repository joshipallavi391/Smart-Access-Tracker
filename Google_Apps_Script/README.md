## Google Apps Script - Smart Access Tracker

### Description
Contains the Apps Script code that acts as a bridge between
ESP32 and Google Sheets / Firebase.

### Function
- Receives HTTP POST request from ESP32
- Extracts card UID and timestamp
- Updates Google Sheets with attendance data
- Data is then synced to Firebase Firestore

### Deployment Steps
1. Open script in Google Apps Script
2. Click Deploy → New Deployment
3. Select type: Web App
4. Set access to "Anyone"
5. Click Deploy and copy the Web App URL
6. Paste this URL in ESP32 code

### Note
Every time you make changes to the script,
re-deploy it as a new version to apply updates.
