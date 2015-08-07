int const numFires = 5;
//each fire has 3 pins to denote its intensity
int const pins1[numFires] = {26,27,28,29,30};
int const pins2[numFires] = {36,37,38,39,40};
int const pins3[numFires] = {46,47,48,49,50};

char inData[numFires] = {0,0,0,0,0};

void serialReadManyChars()
{
  byte index = 0; // Index into array
  while(index < numFires){
      inData[index] = serialRead();
      index++;
  }
}

char serialRead()
{
  char in;
  // Loop until input is not -1 (which means no input was available)
  while ((in = Serial.read()) == -1) {}
  return in;
}

// the setup function runs once when you press reset or power the board
void setup() {
  Serial.begin(9600);
  Serial.flush();
  //for each fire, set all three pins according to its intensity
  for(int i = 0; i < numFires; i++){
    pinMode(pins1[i], OUTPUT);
    pinMode(pins2[i], OUTPUT);
    pinMode(pins3[i], OUTPUT);
  }
}

// the loop function runs over and over again forever
void loop() {
  while(Serial.available() > 0){
    serialReadManyChars();
    for(int i = 0; i < numFires; i++){
      //if the fire intensity is at level 3, set all 3 pins for this fire to high (all three LEDs will light up)
      if(inData[i] == '3'){
        digitalWrite(pins1[i], HIGH);
        digitalWrite(pins2[i], HIGH);
        digitalWrite(pins3[i], HIGH);
      }
      //if the fire intensity is at level 2, set 2 pins to high (two LEDs will light up)
      if(inData[i] == '2'){
        digitalWrite(pins1[i], HIGH);
        digitalWrite(pins2[i], HIGH);
        digitalWrite(pins3[i], LOW);
      }
      //if the fire intensity is at level 1, set 1 pin to high (only one LED will light up)
      if(inData[i] == '1'){
        digitalWrite(pins1[i], HIGH);
        digitalWrite(pins2[i], LOW);
        digitalWrite(pins3[i], LOW);
      }
      //if the fire does not exist or is burned out, set all pins to low (none of the LEDs will light up)
      if(inData[i] == '0'){
        digitalWrite(pins1[i], LOW);
        digitalWrite(pins2[i], LOW);
        digitalWrite(pins3[i], LOW);
      }
    }
  }
}
