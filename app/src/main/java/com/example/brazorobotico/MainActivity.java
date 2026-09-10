package com.example.brazorobotico;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    // =====================================================
    // BLUETOOTH
    // =====================================================

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream out;

    private Spinner devices;
    private TextView status;

    // =====================================================
    // TEMPORIZADOR
    // =====================================================

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    // =====================================================
    // MOVIMIENTO DE LOS SERVOS
    //
    //  1 = una dirección
    //  0 = detenido
    // -1 = dirección contraria
    // =====================================================

    private volatile int servo1Movimiento = 0;
    private volatile int servo2Movimiento = 0;
    private volatile int servo3Movimiento = 0;
    private volatile int servo4Movimiento = 0;

    private volatile boolean connected = false;

    // =====================================================
    // ENVÍO PERIÓDICO
    //
    // IMPORTANTE:
    // Se utiliza "this" y no "tx" dentro de la inicialización.
    // Esto corrige el error:
    // self-reference in initializer
    // =====================================================

    private final Runnable tx = new Runnable() {

        @Override
        public void run() {

            if (connected) {
                enviarComando();
            }

            handler.postDelayed(this, 60);
        }
    };


    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(crearInterfaz());

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {

            status.setText(
                    "Bluetooth no disponible"
            );

            return;
        }

        solicitarPermisosBluetooth();

        cargarDispositivos();

        handler.post(tx);
    }


    // =====================================================
    // CREAR INTERFAZ
    // =====================================================

    private LinearLayout crearInterfaz() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                16,
                16,
                16,
                16
        );


        // =================================================
        // TÍTULO
        // =================================================

        TextView titulo =
                new TextView(this);

        titulo.setText(
                "BRAZO ROBÓTICO"
        );

        titulo.setTextSize(24);

        titulo.setGravity(
                Gravity.CENTER
        );

        root.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );


        // =================================================
        // LISTA BLUETOOTH
        // =================================================

        devices =
                new Spinner(this);

        root.addView(
                devices,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );


        // =================================================
        // BOTÓN CONECTAR
        // =================================================

        Button conectar =
                new Button(this);

        conectar.setText(
                "CONECTAR BLUETOOTH"
        );

        root.addView(
                conectar,
                new LinearLayout.LayoutParams(
                        -1,
                        60
                )
        );


        // =================================================
        // ESTADO
        // =================================================

        status =
                new TextView(this);

        status.setText(
                "Desconectado"
        );

        status.setGravity(
                Gravity.CENTER
        );

        root.addView(
                status,
                new LinearLayout.LayoutParams(
                        -1,
                        50
                )
        );


        // =================================================
        // CONTENEDOR DE JOYSTICKS
        // =================================================

        LinearLayout joysticks =
                new LinearLayout(this);

        joysticks.setOrientation(
                LinearLayout.HORIZONTAL
        );

        joysticks.setGravity(
                Gravity.CENTER
        );


        // =================================================
        // JOYSTICK 1
        // =================================================

        JoystickView joystick1 =
                new JoystickView(
                        this,
                        null
                );


        // =================================================
        // JOYSTICK 2
        // =================================================

        JoystickView joystick2 =
                new JoystickView(
                        this,
                        null
                );


        joysticks.addView(
                joystick1,
                new LinearLayout.LayoutParams(
                        0,
                        0,
                        1
                )
        );

        joysticks.addView(
                joystick2,
                new LinearLayout.LayoutParams(
                        0,
                        0,
                        1
                )
        );


        LinearLayout.LayoutParams parametrosJoystick =
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                );

        root.addView(
                joysticks,
                parametrosJoystick
        );


        // =================================================
        // ETIQUETAS
        // =================================================

        LinearLayout etiquetas =
                new LinearLayout(this);

        etiquetas.setOrientation(
                LinearLayout.HORIZONTAL
        );


        TextView etiqueta1 =
                new TextView(this);

        etiqueta1.setText(
                "JOYSTICK 1\n\n" +
                "X → BASE\n" +
                "Y → BRAZO"
        );

        etiqueta1.setGravity(
                Gravity.CENTER
        );


        TextView etiqueta2 =
                new TextView(this);

        etiqueta2.setText(
                "JOYSTICK 2\n\n" +
                "X → PINZA\n" +
                "Y → BRAZO"
        );

        etiqueta2.setGravity(
                Gravity.CENTER
        );


        etiquetas.addView(
                etiqueta1,
                new LinearLayout.LayoutParams(
                        0,
                        100,
                        1
                )
        );

        etiquetas.addView(
                etiqueta2,
                new LinearLayout.LayoutParams(
                        0,
                        100,
                        1
                )
        );


        root.addView(etiquetas);


        // =================================================
        // BOTÓN PARADA
        // =================================================

        Button parada =
                new Button(this);

        parada.setText(
                "🛑 PARADA"
        );

        root.addView(
                parada,
                new LinearLayout.LayoutParams(
                        -1,
                        70
                )
        );


        // =================================================
        // EVENTO CONECTAR
        // =================================================

        conectar.setOnClickListener(
                v -> conectarBluetooth()
        );


        // =================================================
        // EVENTO PARADA
        // =================================================

        parada.setOnClickListener(
                v -> {

                    servo1Movimiento = 0;
                    servo2Movimiento = 0;
                    servo3Movimiento = 0;
                    servo4Movimiento = 0;

                    enviarComando();
                }
        );


        // =================================================
        // JOYSTICK 1
        //
        // X → SERVO 1
        // Y → SERVO 2
        // =================================================

        joystick1.setListener(
                new JoystickView.Listener() {

                    @Override
                    public void move(
                            float x,
                            float y
                    ) {

                        servo1Movimiento =
                                convertirDireccion(x);

                        servo2Movimiento =
                                convertirDireccion(y);
                    }


                    @Override
                    public void release() {

                        servo1Movimiento = 0;
                        servo2Movimiento = 0;

                        enviarComando();
                    }
                }
        );


        // =================================================
        // JOYSTICK 2
        //
        // X → SERVO 4
        // Y → SERVO 3
        // =================================================

        joystick2.setListener(
                new JoystickView.Listener() {

                    @Override
                    public void move(
                            float x,
                            float y
                    ) {

                        servo4Movimiento =
                                convertirDireccion(x);

                        servo3Movimiento =
                                convertirDireccion(y);
                    }


                    @Override
                    public void release() {

                        servo3Movimiento = 0;
                        servo4Movimiento = 0;

                        enviarComando();
                    }
                }
        );


        return root;
    }


    // =====================================================
    // CONVERTIR DIRECCIÓN
    // =====================================================

    private int convertirDireccion(
            float valor
    ) {

        // Zona muerta del joystick

        if (Math.abs(valor) < 0.18f) {

            return 0;
        }


        if (valor > 0) {

            return 1;

        } else {

            return -1;
        }
    }


    // =====================================================
    // SOLICITAR PERMISOS BLUETOOTH
    // =====================================================

    private void solicitarPermisosBluetooth() {

        if (Build.VERSION.SDK_INT >= 31) {

            if (
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                    != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        100
                );
            }
        }
    }


    // =====================================================
    // CARGAR DISPOSITIVOS EMPAREJADOS
    // =====================================================

    private void cargarDispositivos() {

        if (adapter == null) {
            return;
        }


        if (Build.VERSION.SDK_INT >= 31) {

            if (
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                    != PackageManager.PERMISSION_GRANTED
            ) {

                return;
            }
        }


        ArrayList<String> lista =
                new ArrayList<>();


        Set<BluetoothDevice> dispositivos =
                adapter.getBondedDevices();


        for (
                BluetoothDevice dispositivo :
                dispositivos
        ) {

            lista.add(
                    dispositivo.getName()
                            + "\n"
                            + dispositivo.getAddress()
            );
        }


        if (lista.isEmpty()) {

            lista.add(
                    "Empareja primero el HC-05/HC-06"
            );
        }


        ArrayAdapter<String> adaptador =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_dropdown_item,
                        lista
                );


        devices.setAdapter(adaptador);
    }


    // =====================================================
    // CONECTAR BLUETOOTH
    // =====================================================

    private void conectarBluetooth() {

        if (adapter == null) {

            return;
        }


        if (Build.VERSION.SDK_INT >= 31) {

            if (
                    checkSelfPermission(
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                    != PackageManager.PERMISSION_GRANTED
            ) {

                solicitarPermisosBluetooth();

                return;
            }
        }


        Set<BluetoothDevice> dispositivos =
                adapter.getBondedDevices();


        if (dispositivos.isEmpty()) {

            status.setText(
                    "Primero empareja el HC-05/HC-06"
            );


            startActivity(
                    new Intent(
                            Settings.ACTION_BLUETOOTH_SETTINGS
                    )
            );

            return;
        }


        String seleccionado =
                devices
                        .getSelectedItem()
                        .toString();


        BluetoothDevice elegido = null;


        for (
                BluetoothDevice dispositivo :
                dispositivos
        ) {

            if (
                    seleccionado.contains(
                            dispositivo.getAddress()
                    )
            ) {

                elegido = dispositivo;

                break;
            }
        }


        if (elegido == null) {

            elegido =
                    dispositivos
                            .iterator()
                            .next();
        }


        final BluetoothDevice dispositivoFinal =
                elegido;


        new Thread(
                () -> {

                    try {

                        UUID uuid =
                                UUID.fromString(
                                        "00001101-0000-1000-8000-00805F9B34FB"
                                );


                        BluetoothSocket nuevoSocket =
                                dispositivoFinal
                                        .createRfcommSocketToServiceRecord(
                                                uuid
                                        );


                        adapter.cancelDiscovery();


                        nuevoSocket.connect();


                        socket =
                                nuevoSocket;


                        out =
                                nuevoSocket
                                        .getOutputStream();


                        connected = true;


                        runOnUiThread(
                                () -> status.setText(
                                        "CONECTADO: "
                                                + dispositivoFinal
                                                .getName()
                                )
                        );


                    } catch (Exception e) {

                        connected = false;


                        runOnUiThread(
                                () -> status.setText(
                                        "No se pudo conectar"
                                )
                        );
                    }

                }
        ).start();
    }


    // =====================================================
    // ENVIAR COMANDO
    //
    // FORMATO:
    //
    // M,S1,S2,S3,S4
    //
    // Ejemplo:
    //
    // M,1,0,-1,0
    // =====================================================

    private synchronized void enviarComando() {

        if (
                !connected ||
                out == null
        ) {

            return;
        }


        String mensaje =
                "M,"
                        + servo1Movimiento
                        + ","
                        + servo2Movimiento
                        + ","
                        + servo3Movimiento
                        + ","
                        + servo4Movimiento
                        + "\n";


        try {

            out.write(
                    mensaje.getBytes()
            );


        } catch (IOException e) {

            connected = false;


            runOnUiThread(
                    () -> status.setText(
                            "Bluetooth desconectado"
                    )
            );
        }
    }


    // =====================================================
    // CERRAR
    // =====================================================

    @Override
    protected void onDestroy() {

        handler.removeCallbacks(tx);


        try {

            if (out != null) {
                out.close();
            }


            if (socket != null) {
                socket.close();
            }

        } catch (Exception ignored) {
        }


        super.onDestroy();
    }
}
