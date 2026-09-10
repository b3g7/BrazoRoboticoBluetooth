package com.example.brazorobotico;
import android.Manifest; import android.app.*; import android.bluetooth.*; import android.content.*; import android.content.pm.PackageManager; import android.os.*; import android.provider.Settings; import android.view.*; import android.widget.*; import java.io.*; import java.util.*;
public class MainActivity extends Activity {
 BluetoothAdapter adapter; BluetoothSocket socket; OutputStream out; Spinner devices; TextView status; Handler h=new Handler(Looper.getMainLooper());
 volatile int s1=0,s2=0,s3=0,s4=0; boolean connected=false;
 Runnable tx=()->{if(connected)send();h.postDelayed(tx,60);};
 public void onCreate(Bundle b){super.onCreate(b);setContentView(ui());adapter=BluetoothAdapter.getDefaultAdapter();if(adapter==null){status.setText("Bluetooth no disponible");return;}requestBt();load();h.post(tx);}
 LinearLayout ui(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,16,16,16);
  TextView title=new TextView(this);title.setText("BRAZO ROBÓTICO");title.setTextSize(24);title.setGravity(17);root.addView(title,new LinearLayout.LayoutParams(-1,60));
  devices=new Spinner(this);root.addView(devices,new LinearLayout.LayoutParams(-1,60));
  Button con=new Button(this);con.setText("CONECTAR BLUETOOTH");root.addView(con,new LinearLayout.LayoutParams(-1,60));
  status=new TextView(this);status.setText("Desconectado");status.setGravity(17);root.addView(status,new LinearLayout.LayoutParams(-1,45));
  LinearLayout js=new LinearLayout(this);js.setOrientation(LinearLayout.HORIZONTAL);js.setGravity(17);JoystickView j1=new JoystickView(this,null),j2=new JoystickView(this,null);
  js.addView(j1,new LinearLayout.LayoutParams(0,0,1));js.addView(j2,new LinearLayout.LayoutParams(0,0,1));LinearLayout.LayoutParams jp=new LinearLayout.LayoutParams(-1,0,1);root.addView(js,jp);
  LinearLayout labels=new LinearLayout(this);TextView a=new TextView(this),c=new TextView(this);a.setText("JOYSTICK 1\nX BASE | Y BRAZO");a.setGravity(17);c.setText("JOYSTICK 2\nX PINZA | Y BRAZO");c.setGravity(17);labels.addView(a,new LinearLayout.LayoutParams(0,60,1));labels.addView(c,new LinearLayout.LayoutParams(0,60,1));root.addView(labels);
  Button stop=new Button(this);stop.setText("PARADA");root.addView(stop,new LinearLayout.LayoutParams(-1,60));
  con.setOnClickListener(v->connect());stop.setOnClickListener(v->{s1=s2=s3=s4=0;send();});
  j1.setListener(new JoystickView.Listener(){public void move(float x,float y){s1=dir(x);s2=dir(y);}public void release(){s1=0;s2=0;send();}});
  j2.setListener(new JoystickView.Listener(){public void move(float x,float y){s4=dir(x);s3=dir(y);}public void release(){s3=0;s4=0;send();}});
  return root;
 }
 int dir(float v){return Math.abs(v)<.18?0:(v>0?1:-1);}
 void requestBt(){if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},10);}
 void load(){if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return;ArrayList<String> a=new ArrayList<>();for(BluetoothDevice d:adapter.getBondedDevices())a.add(d.getName()+"\n"+d.getAddress());if(a.isEmpty())a.add("Empareja HC-05/HC-06 en Ajustes Bluetooth");devices.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));}
 void connect(){if(adapter==null)return;if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestBt();return;}Set<BluetoothDevice> ds=adapter.getBondedDevices();if(ds.isEmpty()){startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));return;}String sel=devices.getSelectedItem().toString();BluetoothDevice chosen=null;for(BluetoothDevice d:ds)if(sel.contains(d.getAddress()))chosen=d;if(chosen==null)chosen=ds.iterator().next();BluetoothDevice d=chosen;
  new Thread(()->{try{BluetoothSocket s=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));adapter.cancelDiscovery();s.connect();socket=s;out=s.getOutputStream();connected=true;runOnUiThread(()->status.setText("CONECTADO: "+d.getName()));}catch(Exception e){connected=false;runOnUiThread(()->status.setText("No se pudo conectar"));}}).start();
 }
 synchronized void send(){if(!connected||out==null)return;try{out.write(("M,"+s1+","+s2+","+s3+","+s4+"\n").getBytes());}catch(Exception e){connected=false;}}
 protected void onDestroy(){h.removeCallbacks(tx);try{if(out!=null)out.close();if(socket!=null)socket.close();}catch(Exception e){}super.onDestroy();}
}