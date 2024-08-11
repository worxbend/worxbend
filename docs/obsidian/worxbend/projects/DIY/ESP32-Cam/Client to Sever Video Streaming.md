
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

## Streaming MJPEG Video

The ESP32-CAM does not have built-in hardware support for H.264 encoding, but it can handle MJPEG (Motion JPEG) compression

MJPEG (Motion JPEG): The ESP32-CAM can capture and encode video frames in MJPEG format. 
This format compresses each frame as a separate JPEG image, which is straightforward to implement and can be streamed over HTTP

ESP32-CAM Code Example (MJPEG Streaming):

```
#include <WiFi.h>
#include <ESP32WebServer.h>
#include <esp_camera.h>

const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";

ESP32WebServer server(80);

void setup() {
    Serial.begin(115200);
    camera_config_t config;
    // Camera configuration here
    esp_camera_init(&config);
    
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
        delay(1000);
        Serial.print(".");
    }
    Serial.println("Connected to WiFi");
    
    server.on("/stream", HTTP_GET, []() {
        camera_fb_t *fb = esp_camera_fb_get();
        if (!fb) {
            server.send(500, "text/plain", "Failed to capture image");
            return;
        }
        server.sendHeader("Access-Control-Allow-Origin", "*");
        server.send_P(200, "multipart/x-mixed-replace; boundary=boundary","--boundary\r\nContent-Type: image/jpeg\r\nContent-Length: " + String(fb->len) + "\r\n\r\n" + String((char*)fb->buf) + "\r\n--boundary--");
        esp_camera_fb_return(fb);
    });

    server.begin();
}

void loop() {
    server.handleClient();
}
```
For an ESP32-CAM to act as a client sending frames to a remote server, you’ll typically use HTTP POST requests to transmit each frame.  


```
#include <WiFi.h>
#include <esp_camera.h>
#include <HTTPClient.h>

const char* ssid = "your_SSID";
const char* password = "your_PASSWORD";
const char* serverUrl = "http://yourserver.com/upload"; // Server endpoint for receiving frames

void setup() {
  Serial.begin(115200);
  camera_config_t config;
  // Camera configuration here
  esp_camera_init(&config);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }
  Serial.println("Connected to WiFi");
}

void loop() {
  camera_fb_t *fb = esp_camera_fb_get();
  if (!fb) {
    Serial.println("Failed to capture image");
    return;
  }

  HTTPClient http;
  http.begin(serverUrl);
  http.addHeader("Content-Type", "image/jpeg");
  
  int httpResponseCode = http.POST(fb->buf, fb->len);
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.println("Server Response: " + response);
  } else {
    Serial.println("Error on HTTP request");
  }

  http.end();
  esp_camera_fb_return(fb);

  delay(1000); // Adjust delay as needed
}

```

Configure your server to handle MJPEG streams. A simple HTTP server or a more robust media server can be used to receive and process the MJPEG stream.

---

Use FFmpeg to Stream to YouTube/Twitch

FFmpeg can read images from a directory and convert them into a video stream. You can use a script to start FFmpeg and continuously convert the saved images into an H.264 stream.

FFmpeg Command:

```bash
ffmpeg -framerate 30 -pattern_type glob -i '/path/to/upload/folder/*.jpg' -c:v libx264 -pix_fmt yuv420p -preset fast -f flv 'rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY'
```

```bash
ffmpeg -re -loop 1 -i '/path/to/upload/folder/current_frame.jpg' -c:v libx264 -pix_fmt yuv420p -preset fast -f flv 'rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY'

```
- re: Read input at the native frame rate, useful for streaming.
- loop 1: Loop the input file (in this case, the single JPEG) to continuously process the latest frame. 
- framerate 30: Set the frame rate of the output video.
- pattern_type glob -i '/path/to/upload/folder/*.jpg': Read all JPEG images from the specified directory.
- c:v libx264: Use the H.264 codec.
- pix_fmt yuv420p: Set the pixel format to yuv420p for compatibility.
- preset fast: Choose a balance between encoding speed and compression efficiency.
- f flv 'rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY': Stream the output to YouTube using RTMP.

```shell
#!/bin/bash

IMAGE_PATH="/path/to/upload/folder/current_frame.jpg"
YOUTUBE_URL="rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY"

while true; do
  ffmpeg -re -loop 1 -i "$IMAGE_PATH" -c:v libx264 -pix_fmt yuv420p -preset fast -f flv "$YOUTUBE_URL"
  sleep 1
done

```

Handling file access and concurrent writing issues is crucial in such setups to ensure a smooth streaming experience.
If FFmpeg attempts to read a file while it is being written and the file is partially written or corrupted, we
need to handle concurrent file access and avoid issues.
**Use Atomic File Operations**
Ensure that the file is fully written before it’s accessible to FFmpeg. 
This can be achieved using atomic file operations, where the server writes to a temporary file and then renames it to the target file name.

```python
import os
import tempfile

@app.route('/upload', methods=['POST'])
def upload_image():
    if 'image/jpeg' in request.content_type:
        # Create a temporary file
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix='.jpg')
        temp_file.write(request.data)
        temp_file.close()
        
        # Rename the temporary file to the target filename
        final_path = os.path.join(UPLOAD_FOLDER, 'current_frame.jpg')
        os.rename(temp_file.name, final_path)
        return 'Image received', 200
    return 'Unsupported Media Type', 415

```




