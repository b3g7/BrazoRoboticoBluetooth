#include <Servo.h>
Servo s1,s2,s3,s4;
const byte P1=5,P2=6,P3=7,P4=8;
int a1=90,a2=90,a3=90,a4=90,d1=0,d2=0,d3=0,d4=0;
const int S1MIN=10,S1MAX=170,S2MIN=20,S2MAX=160,S3MIN=20,S3MAX=160,S4MIN=20,S4MAX=160;
String line="";
void setup(){Serial.begin(9600);s1.attach(P1);s2.attach(P2);s3.attach(P3);s4.attach(P4);s1.write(a1);s2.write(a2);s3.write(a3);s4.write(a4);}
void loop(){while(Serial.available()){char c=Serial.read();if(c=='\n'){parse(line);line="";}else if(c!='\r')line+=c;}
 if(d1)a1=constrain(a1+d1,S1MIN,S1MAX);if(d2)a2=constrain(a2+d2,S2MIN,S2MAX);if(d3)a3=constrain(a3+d3,S3MIN,S3MAX);if(d4)a4=constrain(a4+d4,S4MIN,S4MAX);
 s1.write(a1);s2.write(a2);s3.write(a3);s4.write(a4);delay(20);}
void parse(String c){if(!c.startsWith("M,"))return;int p1=c.indexOf(',',2),p2=c.indexOf(',',p1+1),p3=c.indexOf(',',p2+1);if(p1<0||p2<0||p3<0)return;d1=c.substring(2,p1).toInt();d2=c.substring(p1+1,p2).toInt();d3=c.substring(p2+1,p3).toInt();d4=c.substring(p3+1).toInt();d1=constrain(d1,-1,1);d2=constrain(d2,-1,1);d3=constrain(d3,-1,1);d4=constrain(d4,-1,1);}