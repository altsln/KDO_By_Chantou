/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink on core 1 when hanling TCP
* Socket on core 2. Implemention mDNS for autodetection and starting
* ESP32Wrover as an AP (Access Point), and Camera upgrade to OV5640
* Auther      : Alternatives Solutions
* Modification: 2026/05/06
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
  //old  config.xclk_freq_hz = 20000000;
  
  // OV5640 is much more stable at 12MHz or 16MHz than 20MHz.
  // This value works perfectly for both sensors.
  config.xclk_freq_hz = 12000000;

  config.pixel_format = PIXFORMAT_JPEG; // We want JPEGs
  //config.pixel_format = PIXFORMAT_RGB565; // for face detection/recognition

  // Set resolution (QVGA is good for testing TCP)

  //psramFound();  //freenove uses this here.
  /* old
  config.frame_size = FRAMESIZE_QVGA;
  //freenove seeting here is   config.jpeg_quality = 10;
  config.jpeg_quality = 12;
  */
  // Use SVGA (800x600) for a "High Def" salon look. 
  // Both sensors support this easily.
  config.frame_size = FRAMESIZE_SVGA;
  config.jpeg_quality = 12;


  //old  config.fb_count = 1;

  // IMPORTANT: Increase to 2 buffers. 
  // This prevents the Android "skia" warnings because the ESP32 can
  // capture the next frame while the current one is still being sent.
  config.fb_count = 2;


  // Init camera
  esp_err_t err = esp_camera_init(&config);
  if (ESP_OK != err) {
    Serial.printf("Camera init failed with error 0x%x", err);
    return false;
  }

  // SENSOR SPECIFIC TUNING
  sensor_t* s = esp_camera_sensor_get();
  if (s->id.PID == OV5640_PID) {
    Serial.println("OV5640 detected! Enabling Salon Pro features...");

    // Most recent library versions use these commands:
    s->set_vflip(s, 1);    // Match your salon mounting
    s->set_hmirror(s, 1);

    // Use the driver-specific call
    // High-Detail Mode
    s->set_sharpness(s, 2); 
    s->set_brightness(s, 1);


    // TRIGGER AUTOFOCUS
    // On many ESP32-S3 and high-end boards, this is the standard call:
    esp_err_t af_err = s->set_reg(s, 0xFF, 0xFF, 0x01); // Standard trigger for many AF drivers
    
    Serial.println("Autofocus triggered.");

/*
//    s->set_af_mode(s, 1); // 1 = Continuous
//    s->set_af_trigger(s, 1);*/
  } else if (s->id.PID == OV3660_PID) {
    Serial.println("OV3660 detected. Using standard settings.");
  }

  return true;
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
