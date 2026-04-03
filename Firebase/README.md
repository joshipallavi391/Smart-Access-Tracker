This folder contains Firebase Firestore configuration and data structure used in the Smart Access Tracker project.

### Function
- Recieves real time data from google apps script
- Stores attendance data
- Syncs with Android app

### Note
Sensitive credentials should be replaced with your own Firebase configuration.

### Database Rules
The following rules are configured for Firebase Realtime Database access:

{
  "rules": {
    ".read": "now < 1748736000000",   // Read access until June 1, 2025
    ".write": "now < 1748736000000"   // Write access until June 1, 2025
  }
}

### What these rules mean:
- Read and write access was open to everyone until June 1, 2025.
- After that date, access is restricted by default.
- For testing or re-deployment, update the timestamp or replace
  with proper authentication-based rules.

### Note:
These are Firebase Realtime Database rules, not Firestore rules.
Firestore has a separate rules configuration in the Firebase Console
under Firestore Database > Rules.

### Cost & Subscription
- Firebase Free Tier (Spark Plan) is used for this project.
- No subscription or payment is required.
- Since only one user is accessing the database, the free tier
  is more than sufficient for lifetime usage.

### Important Note on Data & Rules Expiry
- The database rules are set to expire after a certain date
  (see rules above).
- Firestore data may reset or become inaccessible after 30 days
  of inactivity under the free tier.
- To continue using the project after expiry:
  1. Go to Firebase Console
  2. Update the rules with a new expiry timestamp
  3. Re-scan a few RFID cards to refresh the data
