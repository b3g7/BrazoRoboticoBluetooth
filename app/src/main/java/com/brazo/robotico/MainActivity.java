package com.brazo.robotico;

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
import android.view.ViewGroup;
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
    private BluetoothAdapter adapter; private BluetoothSocket socket; private OutputStream out;
    private Spinner devices; private TextView status; private boolean connected=false;
    private int m1=0,m2=0,m3=0,m4=0; private final Handler h=new Handler();
    private final Runnable tx = new Runnable(){ @Override public void run(){ if(connected) send(); h.postDelayed(this,60); }};

    @Override public void onCreate(Bundle b){ super.onCreate(b); crearInterfaz(); pedirPermisos(); h.post(tx); }

    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private TextView label(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(55,71,79)); t.setGravity(Gravity.CENTER); t.setPadding(0,dp(4),0,dp(4)); return t; }

    private void crearInterfaz(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(10),dp(16),dp(10)); root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title=label("BRAZO ROBÓTICO",26); title.setTextColor(Color.rgb(45,55,60)); root.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
        devices=new Spinner(this); root.addView(devices,new LinearLayout.LayoutParams(-1,dp(48))); cargarDispositivos();
        Button connect=new Button(this); connect.setText("CONECTAR BLUETOOTH"); root.addView(connect,new LinearLayout.LayoutParams(-1,dp(52))); connect.setOnClickListener(v->conectar());
        status=label("Desconectado",17); root.addView(status,new LinearLayout.LayoutParams(-1,dp(38)));

        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER); row.setWeightSum(2);
        LinearLayout left=col("JOYSTICK 1","X → BASE\nY → BRAZO"); LinearLayout right=col("JOYSTICK 2","X → PINZA\nY → BRAZO");
        row.addView(left,new LinearLayout.LayoutParams(0,0,1)); row.addView(right,new LinearLayout.LayoutParams(0,0,1));
        root.addView(row,new LinearLayout.LayoutParams(-1,0,1));

        Button stop=new Button(this); stop.setText("🛑  PARADA"); stop.setTextSize(16); root.addView(stop,new LinearLayout.LayoutParams(-1,dp(56))); stop.setOnClickListener(v->{m1=m2=m3=m4=0; send();});
        setContentView(root);
    }

    private LinearLayout col(String head,String sub){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setGravity(Gravity.CENTER); c.setPadding(dp(4),0,dp(4),0);
        TextView h1=label(head,17); h1.setTypeface(null,1); c.addView(h1,new LinearLayout.LayoutParams(-1,dp(30)));
        JoystickView j=new JoystickView(this); c.addView(j,new LinearLayout.LayoutParams(-1,dp(210)));
        TextView s=label(sub,14); c.addView(s,new LinearLayout.LayoutParams(-1,dp(55)));
        if(head.contains("1")) j.setListener(new JoystickView.Listener(){ public void move(float x,float y){m1=dir(x);m2=dir(-y);} public void release(){m1=m2=0;send();}});
        else j.setListener(new JoystickView.Listener(){ public void move(float x,float y){m4=dir(x);m3=dir(-y);} public void release(){m3=m4=0;send();}});
        return c;
    }
    private int dir(float v){ return Math.abs(v)<0.18f?0:(v>0?1:-1); }

    private void pedirPermisos(){ if(android.os.Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},REQ_BT); }
    private boolean ok(){ return android.os.Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED; }
    private void cargarDispositivos(){ try{ adapter=BluetoothAdapter.getDefaultAdapter(); if(adapter==null)return; ArrayList<String> names=new ArrayList<>(); ArrayList<BluetoothDevice> list=new ArrayList<>(); if(!ok())return; Set<BluetoothDevice> paired=adapter.getBondedDevices(); for(BluetoothDevice d:paired){ names.add((d.getName()==null?"Sin nombre":d.getName())+"\n"+d.getAddress()); list.add(d);} if(names.isEmpty())names.add("Primero empareja el HC-05 en Ajustes Bluetooth"); ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names); a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); devices.setAdapter(a); devices.setTag(list); }catch(Exception e){}}
    private void conectar(){ try{ if(!ok()){pedirPermisos();return;} if(adapter==null){toast("Bluetooth no disponible");return;} if(!adapter.isEnabled()){toast("Activa Bluetooth");return;} Object tag=devices.getTag(); if(!(tag instanceof ArrayList) || ((ArrayList<?>)tag).isEmpty()){toast("Empareja primero el módulo HC-05");return;} BluetoothDevice d=(BluetoothDevice)((ArrayList<?>)tag).get(devices.getSelectedItemPosition()); status.setText("Conectando..."); new Thread(()->{try{ socket=d.createRfcommSocketToServiceRecord(SPP_UUID); socket.connect(); out=socket.getOutputStream(); connected=true; runOnUiThread(()->status.setText("Conectado: "+d.getName())); }catch(Exception e){connected=false;runOnUiThread(()->status.setText("Error de conexión"));}}).start(); }catch(Exception e){toast(e.getMessage());}}
    private void send(){ if(!connected||out==null)return; try{String s="M,"+m1+","+m2+","+m3+","+m4+"\n";out.write(s.getBytes());out.flush();}catch(Exception e){connected=false;}}
    private void toast(String s){runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
    @Override protected void onDestroy(){h.removeCallbacks(tx);try{if(out!=null)out.close();if(socket!=null)socket.close();}catch(Exception ignored){}super.onDestroy();}
}
