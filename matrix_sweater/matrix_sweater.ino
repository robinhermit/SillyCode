 /*
Matrix: Kapton + Copper
A simple pressure sensor matrix made from two Kapton film sheets with
7×7 copper tape traces and a piece of Velostat or Eeonyx piezoresistive
material in between.
parsing through this grid by switching individual rows/columns to be
HIGH, LOW or INPUT (high impedance) to detect location and pressure.
>> http://howtogetwhatyouwant.at/
*/

#define numRows 15
#define numCols 14
#define sensorPoints numRows*numCols

int rows[] = {A0, A1, A2,A3,A4,A5,A6,A7,A8,A9,A10,A11,A12,A13,A14};
int cols[] = {39,40,41,42,43,44,45,46,47,48,49,50,51,52};
int incomingValues[sensorPoints] = {};

void setup() {
// set all rows and columns to INPUT (high impedance):
for (int i = 0; i < numRows; i++) { 
  pinMode(rows[i], INPUT_PULLUP); 
  }
for (int i = 0; i < numCols; i++) { 
  pinMode(cols[i], INPUT); 
  } 
  Serial.begin(115200); 
  } 
  
void loop() { 
  for (int colCount = 0; colCount < numCols; colCount++) { 
    
    pinMode(cols[colCount], OUTPUT); // set as OUTPUT 
    digitalWrite(cols[colCount], LOW); // set LOW 
    for (int rowCount = 0; rowCount < numRows; rowCount++) { 
      
      incomingValues[colCount * numRows + rowCount] = analogRead(rows[rowCount]); // read INPUT 
      }// end rowCount 
      
      pinMode(cols[colCount], INPUT); // set back to INPUT! 
    }// end colCount 
    
    // Print the incoming values of the grid: 
    for (int i = 0; i < sensorPoints; i++) { 
      Serial.print(incomingValues[i]); 
      if (i < sensorPoints - 1) 
      Serial.print(" "); 
      } 
      Serial.println(); 
      delay(10); 
      } 
