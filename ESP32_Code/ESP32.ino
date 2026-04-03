#include <WiFi.h>
#include <SPI.h>
#include <MFRC522.h>
#include <HTTPClient.h>

// ===== RFID Pins =====
#define SS_PIN     5      // SDA connected to GPIO 5
#define RST_PIN    22     // RST connected to GPIO 22

MFRC522 rfid(SS_PIN, RST_PIN);

// ===== LED + Buzzer =====
#define LED_PIN    2
#define BUZZER_PIN 4

// ===== WiFi Credentials =====
const char* ssid = "MyPhone";
const char* password = "My123456";

// ===== Google Apps Script URL =====
String googleScriptURL = "https://script.google.com/macros/s/AKfycbws9Gs65imSqbL10Xmw-IgUuI-qWxmnqjlTHaAWVQVgwp4jXj7O-69ML9bUa3kLxUZzew/exec";

// ===== Firebase Realtime Database URL & Secret =====
String firebaseURL = "https://mart-access-tracker-default-rtdb.firebaseio.com/attendance";
String firebaseSecret = "Dgswtx7B10r8jMcPz2QDHXQk9vCPUJ5epwHC9W3d";

// ===== Timing =====
unsigned long lastScanTime = 0;
const unsigned long scanInterval = 2000; // 2 seconds between scans

// ===== Duplicate Scan Prevention =====
String lastUID = "";
bool cardPresent = false;

// ===== Student Name Mapping =====
struct Student {
  String name;
  int presents;
  int absents;
};

#include <map>
std::map<String, Student> students = {
  {"B332FF3E", {"STUDENT 1", 0, 0}},
  {"21D83D02", {"STUDENT 2", 0, 0}},
  {"B16C3502", {"STUDENT 3", 0, 0}},
  {"A2BD3D02", {"STUDENT 4", 0, 0}},
  {"BA034102", {"STUDENT 5", 0, 0}},
  {"3767B302", {"STUDENT 6", 0, 0}},
  {"6DA9B402", {"STUDENT 7", 0, 0}},
  {"FAAAB402", {"STUDENT 8", 0, 0}},
  {"E49F3502", {"STUDENT 9", 0, 0}},
  {"DD4A3402", {"STUDENT 10", 0, 0}},
  {"2747ACB5", {"STUDENT 11", 0, 0}}
};

void setup() {
  Serial.begin(115200);
  delay(1500);

  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  SPI.begin(18, 19, 23, SS_PIN);
  rfid.PCD_Init();
  delay(100);

  Serial.println("\nESP32 + RC522 + Google Sheets + Firebase Attendance System");
  Serial.println("Place your RFID card near the reader...");

  connectWiFi();
}

void loop() {
  // Reconnect WiFi if disconnected
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
    delay(1000);
    return;
  }

  // Non-blocking scan interval
  if (millis() - lastScanTime < scanInterval) return;

  // Check for new card
  if (!rfid.PICC_IsNewCardPresent()) {
    cardPresent = false; // reset if no card
    return;
  }
  if (!rfid.PICC_ReadCardSerial()) return;

  String uid = getUID(rfid.uid);

  // Prevent duplicate scans
  if (cardPresent && uid == lastUID) return;
  cardPresent = true;
  lastUID = uid;

  Serial.println("Card UID: " + uid);

  flashLEDandBuzzer(300);

  // Update student attendance
  String name = "UNKNOWN";
  int presents = 0;
  int absents = 0;

  if (students.count(uid) > 0) {
    name = students[uid].name;
    students[uid].presents += 1;
    presents = students[uid].presents;
    absents = students[uid].absents; // currently static
  }

  float percentage = (presents + absents > 0) ? ((float)presents / (presents + absents) * 100.0) : 0.0;

  // Send to Google Sheets
  sendUIDToGoogleSheets(uid);

  // Send to Firebase
  sendUIDToFirebase(uid, name, presents, absents, percentage);

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  lastScanTime = millis();
}

// ===== Helper Functions =====
String getUID(MFRC522::Uid uid) {
  String uidStr = "";
  for (byte i = 0; i < uid.size; i++) {
    if (uid.uidByte[i] < 0x10) uidStr += "0";
    uidStr += String(uid.uidByte[i], HEX);
  }
  uidStr.toUpperCase(); // modifies uidStr in place
  return uidStr;
}

void flashLEDandBuzzer(int duration) {
  digitalWrite(LED_PIN, HIGH);
  digitalWrite(BUZZER_PIN, HIGH);
  delay(duration);
  digitalWrite(LED_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);
}

void connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;

  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - startAttemptTime < 10000) {
    delay(500);
    Serial.print(".");
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi Connected!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nFailed to connect to WiFi");
  }
}

void sendUIDToGoogleSheets(String uid) {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  String requestURL = googleScriptURL + "?uid=" + uid;

  http.begin(requestURL);
  int httpCode = http.GET();

  if (httpCode > 0) {
    Serial.println("✔ Google Sheets Response: " + http.getString());
  } else {
    Serial.println("❌ Failed to send to Google Sheets: " + http.errorToString(httpCode));
  }

  http.end();
}

void sendUIDToFirebase(String uid, String name, int presents, int absents, float percentage) {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  String requestURL = firebaseURL + "/" + uid + ".json?auth=" + firebaseSecret;

  String payload = "{";
  payload += "\"uid\":\"" + uid + "\",";
  payload += "\"name\":\"" + name + "\",";
  payload += "\"status\":\"PRESENT\",";
  payload += "\"presents\":" + String(presents) + ",";
  payload += "\"absents\":" + String(absents) + ",";
  payload += "\"percentage\":\"" + String(percentage, 2) + "%\",";
  payload += "\"timestamp\":\"" + String(millis()) + "\"";
  payload += "}";

  http.begin(requestURL);
  http.addHeader("Content-Type", "application/json");
  int httpCode = http.PUT(payload);

  if (httpCode > 0) {
    Serial.println("✔ Firebase Response: " + http.getString());
  } else {
    Serial.println("❌ Failed to send to Firebase: " + http.errorToString(httpCode));
  }

  http.end();
}