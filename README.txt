BRAZO ROBOTICO - VERSION CORREGIDA

IMPORTANTE: para evitar que queden archivos viejos en GitHub, reemplaza TODO el contenido del repositorio con este proyecto.

El error anterior:
package R does not exist

se producia porque habia un JoystickView viejo en:
app/src/main/java/com/example/brazorobotico/

La version correcta usa:
app/src/main/java/com/brazo/robotico/

El workflow tambien elimina automaticamente la carpeta com/example si quedara en el repositorio.

Los joysticks son visibles y tactiles.
Bluetooth: HC-06 / SPP.

Controles:
Joystick 1 X -> Servo 1 BASE
Joystick 1 Y -> Servo 2 BRAZO
Joystick 2 X -> Servo 4 PINZA
Joystick 2 Y -> Servo 3 BRAZO
Al soltar: detiene movimiento y conserva posicion.
