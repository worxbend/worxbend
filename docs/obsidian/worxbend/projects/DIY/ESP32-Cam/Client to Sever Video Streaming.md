
# Client

## Connect to the Wi-Fi Network

### SSL (HTTPS)
- Basic HTTPS Client using the `ESP8266HTTPClient` library: **ESP8266HTTPClient** > **[BasicHttpsClient](https://github.com/esp8266/Arduino/blob/master/libraries/ESP8266HTTPClient/examples/BasicHttpsClient/BasicHttpsClient.ino)**
- Basic HTTPS Client using `WiFiClientSecure` library: **ESP8266WiFi** > **[HTTPSRequest](https://github.com/esp8266/Arduino/tree/master/libraries/ESP8266WiFi/examples/HTTPSRequest)**


### Check connectivity and network speed

**TO BE ADDED**

## Reconnect to Wi-Fi Network After Lost Connection

To reconnect to Wi-Fi after a connection is lost, you can use WiFi.reconnect() to try to reconnect to the previously connected access point:

```c
WiFi.reconnect()
```

Or, you can call WiFi.disconnect() followed by WiFi.begin(ssid,password).

```c
WiFi.disconnect();
WiFi.begin(ssid, password);
```

Alternatively, you can also try to restart the ESP32 with ESP.restart() when the connection is lost.

You can add something like the snippet below to the loop() that checks once in a while if the board is connected and tries to reconnect if it has lost connection.

```c
unsigned long currentMillis = millis();
// if WiFi is down, try reconnecting
if ((WiFi.status() != WL_CONNECTED) && (currentMillis - previousMillis >=interval)) {
  Serial.print(millis());
  Serial.println("Reconnecting to WiFi...");
  WiFi.disconnect();
  WiFi.reconnect();
  previousMillis = currentMillis;
}
```

Full example:
```c
#include <WiFi.h>

const char* ssid = "REPLACE_WITH_YOUR_SSID";
const char* password = "REPLACE_WITH_YOUR_PASSWORD";

unsigned long previousMillis = 0;
unsigned long interval = 30000;

void initWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  Serial.print("Connecting to WiFi ..");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print('.');
    delay(1000);
  }
  Serial.println(WiFi.localIP());
}

void setup() {
  Serial.begin(115200);
  initWiFi();
  Serial.print("RSSI: ");
  Serial.println(WiFi.RSSI());
}

void loop() {
  unsigned long currentMillis = millis();
  // if WiFi is down, try reconnecting every CHECK_WIFI_TIME seconds
  if ((WiFi.status() != WL_CONNECTED) && (currentMillis - previousMillis >=interval)) {
    Serial.print(millis());
    Serial.println("Reconnecting to WiFi...");
    WiFi.disconnect();
    WiFi.reconnect();
    previousMillis = currentMillis;
  }
}
```

### ESP32 Wi-Fi Events

The ESP32 is able to handle different Wi-Fi events. With Wi-Fi Events, you don’t need to be constantly checking the Wi-Fi state. When a certain event happens, it automatically calls the corresponding handling function.

The following events are very useful to detect if the connection was lost or reestablished:

- `ARDUINO_EVENT_WIFI_STA_CONNECTED`: the ESP32 is connected in station mode to an access point/hotspot (your router);
- `ARDUINO_EVENT_WIFI_STA_DISCONNECTED`: the ESP32 station disconnected from the access point.

Go to the next section to see an application example.

####  Reconnect to Wi-Fi Network After Lost Connection (Wi-Fi Events)

Wi-Fi events can be useful to detect that a connection was lost and try to reconnect right after (use the ARDUINO_EVENT_WIFI_STA_DISCONNECTED event). Here’s a sample code:

```c
#include <WiFi.h>
 
const char* ssid = "REPLACE_WITH_YOUR_SSID";
const char* password = "REPLACE_WITH_YOUR_PASSWORD";

void WiFiStationConnected(WiFiEvent_t event, WiFiEventInfo_t info){
  Serial.println("Connected to AP successfully!");
}

void WiFiGotIP(WiFiEvent_t event, WiFiEventInfo_t info){
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void WiFiStationDisconnected(WiFiEvent_t event, WiFiEventInfo_t info){
  Serial.println("Disconnected from WiFi access point");
  Serial.print("WiFi lost connection. Reason: ");
  Serial.println(info.wifi_sta_disconnected.reason);
  Serial.println("Trying to Reconnect");
  WiFi.begin(ssid, password);
}

void setup(){
  Serial.begin(115200);

  // delete old config
  WiFi.disconnect(true);

  delay(1000);

  WiFi.onEvent(WiFiStationConnected, WiFiEvent_t::ARDUINO_EVENT_WIFI_STA_CONNECTED);
  WiFi.onEvent(WiFiGotIP, WiFiEvent_t::ARDUINO_EVENT_WIFI_STA_GOT_IP);
  WiFi.onEvent(WiFiStationDisconnected, WiFiEvent_t::ARDUINO_EVENT_WIFI_STA_DISCONNECTED);

  /* Remove WiFi event
  Serial.print("WiFi Event ID: ");
  Serial.println(eventID);
  WiFi.removeEvent(eventID);*/

  WiFi.begin(ssid, password);
    
  Serial.println();
  Serial.println();
  Serial.println("Wait for WiFi... ");
}

void loop(){
  delay(1000);
}
```

Reference:
- https://randomnerdtutorials.com/solved-reconnect-esp32-to-wifi/


## ESP32 Cam Library

 https://github.com/espressif/esp32-camera
 
 https://github.com/espressif/esp32-camera?tab=readme-ov-file#jpeg-http-stream