/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink on core 1 when hanling TCP
* Socket on core 2. Sending Strings to the phone.
* Auther      : Alternatives Solutions
* Modification: 2026/05/02
**********************************************************************/
#include <WiFi.h>

#define LED_BUILTIN  2
#define DELAY_TIME   1000

//update the ssid and password with your own for this code to run
const char* ssid = "YOUR_WIFI_NAME";
const char* password = "YOUR_WIFI_PASSWORD";

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

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  Serial.println("\nWiFi Connected!");
  Serial.print("ESP32 IP Address: ");
  Serial.println(WiFi.localIP()); // YOU WILL NEED THIS FOR THE ANDROID APP

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
      int count = 0;
      String str = "";
      while (client.connected()) {
        count %= 4;
        switch(count) {
          case 0:
            str = "ESP32 Wrover & Android Phone";
            break;
          case 1:
            str = "DATA_READY: Waiting for camera...";
            break;
          case 2:
            str = "KDO by Chantou";
            break;
          case 3:
            str = "STATUS_OK: System is running";
            break;
        }
        count++;
        client.println(str);
        Serial.printf("Sent String: %s\n", str.c_str());
        vTaskDelay(1000 / portTICK_PERIOD_MS);
      }
      client.stop();
      Serial.println("Phone Disconnected");
    }
    vTaskDelay(10 / portTICK_PERIOD_MS); // Small delay to prevent watchdog issues  
  }
}
