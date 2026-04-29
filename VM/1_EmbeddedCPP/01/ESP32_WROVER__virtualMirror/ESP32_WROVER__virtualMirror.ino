/**********************************************************************
* Filename    : ESP32_WROVER__virtualMirror
* Description : Make builtin led to blink.
* Auther      : Alternatives Solutions
* Modification: 2026/04/28
**********************************************************************/
#define LED_BUILTIN  2  
#define DELAY_TIME   1000

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
}

// the loop function runs over and over again forever
void loop() {
  digitalWrite(LED_BUILTIN, HIGH);   // turn the LED off (HIGH is the voltage level)
  delay(DELAY_TIME);                 // wait for a Defined delay time
  digitalWrite(LED_BUILTIN, LOW);    // turn the LED ON, Active LOW
  delay(DELAY_TIME);                 // wait for a Defined delay time
}
