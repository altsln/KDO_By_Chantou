/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink when printing out heartbeat.
* In this case we will be using the power of the 2 cores esp32 has.
* Auther      : Alternatives Solutions
* Modification: 2026/04/30
**********************************************************************/
#define LED_BUILTIN  2  
#define DELAY_TIME   1000

// Creating a "Handle" (an ID card for our task). It is optional for now
TaskHandle_t MyTaskHandle;

// Function for our Task on Core 0
void MyWorkerFunc(void * pvParameters) {
  Serial.print("Task1 running on core ");
  Serial.println(xPortGetCoreID());

  for(;;) { // Workers need their own infinite loop
    Serial.println("System Heartbeat: Core 0 is Healthy");
    delay(DELAY_TIME);
  }
}


// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(115200);
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);

  // create a task that will be executed in the 
  // MyWorkerFunc() function, with priority 1 
  // and executed on core 0
  xTaskCreatePinnedToCore(
        MyWorkerFunc,  /* Task function. */
        "Task1",       /* name of task. */
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
    Serial.println("LED OFF");
    vTaskDelay(500 / portTICK_PERIOD_MS);   // wait for a Defined delay time
    digitalWrite(LED_BUILTIN, LOW);         // turn the LED ON, Active LOW
    Serial.println("LED ON");
    vTaskDelay(500 / portTICK_PERIOD_MS);   // wait for a Defined delay time
  }
}
