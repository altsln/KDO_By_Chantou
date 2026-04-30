/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink when printing out heartbeat.
* In this case we will be using the power of the 2 cores esp32 has.
* Auther      : Alternatives Solutions
* Modification: 2026/04/30
**********************************************************************/
#define LED_BUILTIN  2  
#define DELAY_TIME   1000

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(115200);
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.println("System initialized, up and running... ");
}

// the loop function runs over and over again forever
void loop() {
  digitalWrite(LED_BUILTIN, HIGH);   // turn the LED off (HIGH is the voltage level)
  Serial.println("LED OFF");
  delay(DELAY_TIME);                 // wait for a Defined delay time
  digitalWrite(LED_BUILTIN, LOW);    // turn the LED ON, Active LOW
  Serial.println("LED ON");
  delay(DELAY_TIME);                 // wait for a Defined delay time
}
