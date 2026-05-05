/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink on core 1 when hanling TCP
* Socket on core 2. Implemention mDNS for autodetection and starting
* ESP32Wrover as an AP (Access Point)
* Auther      : Alternatives Solutions
* Modification: 2026/05/04
**********************************************************************/
#include <ESPmDNS.h>
#include <WiFi.h>
#include "esp_camera.h" 

//Freenove doc for the Cam Configuration
//https://docs.freenove.com/projects/fnk0047/en/latest/fnk0047/codes/C/35_Camera_Tcp_Server.html
// Select the camera model for Freenove (the WROVER model)
#define CAMERA_MODEL_WROVER_KIT
#include "camera_pins.h"

#define LED_BUILTIN  2
#define DELAY_TIME   1000

//update the ssid and password with your own for this code to run
const char* ssid_ap = "ESP32-KDO_Cam";
const char* password_ap = "3346DundasStreetW";

// Instead of a client connecting to an IP, we start a Server on port 8080
const uint16_t serverPort = 8080;
WiFiServer server(serverPort);

// Creating a "Handle" (an ID card for our task). It is optional for now
TaskHandle_t MyTaskHandle;

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(115200);
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);

  if (initCamera()) {
    Serial.println("Camera configuration complete!");
  }

  //WiFi.begin(ssid, password);
  //while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  //Serial.println("\nWiFi Connected!");  
  WiFi.softAP(ssid_ap, password_ap);
  Serial.print("ESP32 AP IP Address: ");
  //Serial.println(WiFi.localIP());
  Serial.println(WiFi.softAPIP()); 

  setup_mDNS();

  server.begin(); // Start listening for the phone

  // create a task that will be executed in the 
  // TCPTask() function, with priority 1 
  // and executed on core 0
  xTaskCreatePinnedToCore(
        TCPTask,       /* Task function. */
        "TCP_VM",      /* name of task. */
        10000,         /* Stack size of task */
        NULL,          /* parameter of the task */
        1,             /* priority of the task */
        &MyTaskHandle, /* Task handle to keep track of created task */
        0              /* pin task to core 0 */
  );
    
  // Task 2: The Blinker (Core 1)
  xTaskCreatePinnedToCore(BlinkTask, "Blink", 1000, NULL, 1, NULL, 1);

  Serial.println("System initialized, up and running... ");
}

// the loop function runs over and over again forever
void loop() {
  vTaskDelete(NULL);  // We use tasks, so loop is empty
}


void setup_mDNS() {
  if (!MDNS.begin("esp32-KDO")) {
    Serial.println("Error setting up MDNS responder!");
  } else {
    Serial.println("mDNS responder started at esp32-KDO.local");
    // Advertise the TCP service on port 8080
    MDNS.addService("arduino", "tcp", 8080);
  }
}


bool initCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  //freenove seeting here is   config.xclk_freq_hz = 10000000;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG; // We want JPEGs
  //config.pixel_format = PIXFORMAT_RGB565; // for face detection/recognition

  // Set resolution (QVGA is good for testing TCP)

  //psramFound();  //freenove uses this here.
  config.frame_size = FRAMESIZE_QVGA;
  //freenove setting here is   config.jpeg_quality = 10;
  config.jpeg_quality = 12;
  config.fb_count = 1;

  // camera init
  esp_err_t err = esp_camera_init(&config);
  if (ESP_OK != err) {
    Serial.printf("Camera init failed with error 0x%x", err);
  }

  return (err == ESP_OK);
}


void BlinkTask(void * pvParameters) {
  for(;;) {
    digitalWrite(LED_BUILTIN, HIGH);        // turn the LED off (HIGH is the voltage level)
//    Serial.println("LED OFF");
    vTaskDelay(500 / portTICK_PERIOD_MS);   // wait for a Defined delay time
    digitalWrite(LED_BUILTIN, LOW);         // turn the LED ON, Active LOW
//    Serial.println("LED ON");
    vTaskDelay(500 / portTICK_PERIOD_MS);   // wait for a Defined delay time
  }
}

// Function for our Task on Core 0
void TCPTask(void * pvParameters) {
  Serial.print("Task1 running on core ");
  Serial.println(xPortGetCoreID());

  for(;;) { // Workers need their own infinite loop
    WiFiClient client = server.available(); // Wait for phone to connect
    if (client) {
      Serial.println("Phone Connected!");
      
      while (client.connected()) {
        // 1. Capture a frame
        camera_fb_t* fb = esp_camera_fb_get();
        if (!fb) {
          Serial.println("Camera capture failed");
          continue;
        }
        // 2. Send the SIZE of the image first (The Header)
        uint32_t size = fb->len;
        client.write((const uint8_t*)&size, sizeof(size));
        // 3. Send the actual IMAGE data (The Payload)
        client.write(fb->buf, fb->len);
        Serial.printf("Sent Image: %d bytes\n", fb->len);
        // 4. Return the frame buffer to the system or clear the buffer
        esp_camera_fb_return(fb);
      }
      client.stop();
      Serial.println("Phone Disconnected");
    }
    vTaskDelay(10 / portTICK_PERIOD_MS); // Small delay to prevent watchdog issues  
  }
}
