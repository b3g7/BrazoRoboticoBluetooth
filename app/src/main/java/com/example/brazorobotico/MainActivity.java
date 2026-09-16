package com.example.brazorobotico;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ_BT = 20;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream output;
    private Spinner deviceSpinner;
    private TextView status;
    private boolean connected;
    private int servo1 = 0, servo2 = 0, servo3 = 0, servo4 = 0;
    private final Handler handler = new Handler();
    private final Runnable transmitter = new Runnable() {
        @Override public void run() {
            if (connected) sendCommand();
            handler.postDelayed(this, 60);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildInterface();
        requestBluetoothPermissions();
        handler.post(transmitter);
    }

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView label(String text, float size) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(Color.rgb(55,71,79));
        t.setGravity(Gravity.CENTER); t.setPadding(0, dp(3), 0, dp(3));
        return t;
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(Color.rgb(247,248,250));

        TextView title = label("BRAZO ROBÓTICO", 25);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(44)));

        deviceSpinner = new Spinner(this);
        root.addView(deviceSpinner, new LinearLayout.LayoutParams(-1, dp(46)));

        Button connect = new Button(this);
        connect.setText("CONECTAR BLUETOOTH");
        root.addView(connect, new LinearLayout.LayoutParams(-1, dp(50)));
        connect.setOnClickListener(v -> connectBluetooth());

        status = label("Desconectado", 16);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(34)));

        LinearLayout joysticks = new LinearLayout(this);
        joysticks.setOrientation(LinearLayout.HORIZONTAL);
        joysticks.setGravity(Gravity.CENTER);
        joysticks.setWeightSum(2f);

        joysticks.addView(createJoystickColumn(1), new LinearLayout.LayoutParams(0, -1, 1f));
        joysticks.addView(createJoystickColumn(2), new LinearLayout.LayoutParams(0, -1, 1f));
        root.addView(joysticks, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button stop = new Button(this);
        stop.setText("🛑  PARADA"); stop.setTextSize(16);
        root.addView(stop, new LinearLayout.LayoutParams(-1, dp(54)));
        stop.setOnClickListener(v -> { servo1=servo2=servo3=servo4=0; sendCommand(); });

        setContentView(root);
        loadPairedDevices();
    }

    private LinearLayout createJoystickColumn(int number) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.setPadding(dp(3), dp(2), dp(3), dp(2));

        TextView heading = label("JOYSTICK " + number, 17);
        heading.setTypeface(null, 1);
        column.addView(heading, new LinearLayout.LayoutParams(-1, dp(30)));

        JoystickView joystick = new JoystickView(this);
        LinearLayout.LayoutParams joystickParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        joystickParams.setMargins(dp(2), dp(2), dp(2), dp(2));
        column.addView(joystick, joystickParams);

        TextView axes = label(number == 1 ? "X → BASE\nY → BRAZO" : "X → PINZA\nY → BRAZO", 14);
        column.addView(axes, new LinearLayout.LayoutParams(-1, dp(50)));

        if (number == 1) {
            joystick.setListener(new JoystickView.Listener() {
                @Override public void move(float x, float y) { servo1=direction(x); servo2=direction(-y); }
                @Override public void release() { servo1=servo2=0; sendCommand(); }
            });
        } else {
            joystick.setListener(new JoystickView.Listener() {
                @Override public void move(float x, float y) { servo4=direction(x); servo3=direction(-y); }
                @Override public void release() { servo3=servo4=0; sendCommand(); }
            });
        }
        return column;
    }

    private int direction(float value) { return Math.abs(value) < 0.18f ? 0 : (value > 0 ? 1 : -1); }

    private boolean hasBluetoothPermission() {
        return android.os.Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 31 && !hasBluetoothPermission()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQ_BT);
        }
    }

    private void loadPairedDevices() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !hasBluetoothPermission()) return;
            ArrayList<String> names = new ArrayList<>();
            ArrayList<BluetoothDevice> devices = new ArrayList<>();
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice d : paired) {
                names.add((d.getName() == null ? "Sin nombre" : d.getName()) + "\n" + d.getAddress());
                devices.add(d);
            }
            if (names.isEmpty()) names.add("Empareja primero el HC-06 en Ajustes Bluetooth");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            deviceSpinner.setAdapter(adapter);
            deviceSpinner.setTag(devices);
        } catch (Exception ignored) {}
    }

    private void connectBluetooth() {
        try {
            if (!hasBluetoothPermission()) { requestBluetoothPermissions(); return; }
            if (bluetoothAdapter == null) { toast("Bluetooth no disponible"); return; }
            if (!bluetoothAdapter.isEnabled()) { toast("Activa Bluetooth"); return; }
            Object tag = deviceSpinner.getTag();
            if (!(tag instanceof ArrayList) || ((ArrayList<?>)tag).isEmpty()) { toast("Empareja primero el HC-06"); return; }
            BluetoothDevice device = (BluetoothDevice)((ArrayList<?>)tag).get(deviceSpinner.getSelectedItemPosition());
            status.setText("Conectando...");
            new Thread(() -> {
                try {
                    BluetoothSocket s = device.createRfcommSocketToServiceRecord(SPP_UUID);
                    s.connect();
                    OutputStream o = s.getOutputStream();
                    socket=s; output=o; connected=true;
                    runOnUiThread(() -> status.setText("Conectado: " + safeName(device)));
                } catch (Exception e) {
                    connected=false;
                    runOnUiThread(() -> status.setText("Error de conexión"));
                }
            }).start();
        } catch (Exception e) { toast(e.getMessage() == null ? "Error de conexión" : e.getMessage()); }
    }

    private String safeName(BluetoothDevice device) {
        try { return device.getName() == null ? "HC-06" : device.getName(); } catch (SecurityException e) { return "HC-06"; }
    }

    private void sendCommand() {
        if (!connected || output == null) return;
        try {
            String command = "M," + servo1 + "," + servo2 + "," + servo3 + "," + servo4 + "\n";
            output.write(command.getBytes()); output.flush();
        } catch (Exception e) { connected=false; }
    }

    private void toast(String message) { runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()); }

    @Override protected void onDestroy() {
        handler.removeCallbacks(transmitter);
        try { if (output != null) output.close(); if (socket != null) socket.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
