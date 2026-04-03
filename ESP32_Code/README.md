## ESP32 Code - Smart Access Tracker

### Description
Contains Arduino code for ESP32 that reads RFID card data
and sends it to Google Apps Script via WiFi.

### Hardware Used
- ESP32
- RC522 RFID Reader
- RFID Cards
- Buzzer
- LED

### Working
1. RFID card is scanned on RC522 reader
2. ESP32 reads the card UID
3. ESP32 sends HTTP request to Google Apps Script URL
4. Buzzer and LED give feedback on successful scan

### Setup
1. Open .ino file in Arduino IDE
2. Replace WiFi credentials (SSID and Password)
3. Replace Google Apps Script Web App URL
4. Upload code to ESP32
