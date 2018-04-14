//#include <SoftwareSerial.h>

char data = 0;                //Variable for storing received data
//SoftwareSerial mySerial(0, 1); // RX, TX

void setup() 
{
    Serial.begin(9600);         //Sets the data rate in bits per second (baud) for serial data transmission
    pinMode(LED_BUILTIN, OUTPUT);      
}

void loop()
{
  if(Serial.available() > 0)  // Send data only when you receive data:
  {
    data = Serial.read();      //Read the incoming data and store it into variable data
    Serial.print(data);        //Print Value inside data in Serial monitor
    Serial.print("\n");        //New line 
    if(data == '1')            //Checks whether value of data is equal to 1 
      digitalWrite(LED_BUILTIN, HIGH);  //If value is 1 then LED turns ON
    else if(data == '0')       //Checks whether value of data is equal to 0
      digitalWrite(LED_BUILTIN, LOW);   //If value is 0 then LED turns OFF
  }/*else{
    digitalWrite(LED_BUILTIN, HIGH);   // turn the LED on (HIGH is the voltage level)
    delay(1000);                       // wait for a second
    digitalWrite(LED_BUILTIN, LOW);    // turn the LED off by making the voltage LOW
    delay(1000);                             
  }*/
 
}
