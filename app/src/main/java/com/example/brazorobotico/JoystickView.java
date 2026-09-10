package com.example.brazorobotico;
import android.content.*; import android.graphics.*; import android.util.*; import android.view.*;
public class JoystickView extends View {
 public interface Listener { void move(float x,float y); void release(); }
 Paint p=new Paint(1); float cx,cy,kx,ky,r,kr; Listener listener;
 public JoystickView(Context c,AttributeSet a){super(c,a);setBackgroundResource(R.drawable.joystick_bg);}
 public void setListener(Listener l){listener=l;}
 protected void onSizeChanged(int w,int h,int ow,int oh){cx=w/2f;cy=h/2f;r=Math.min(w,h)*.42f;kr=Math.min(w,h)*.16f;kx=cx;ky=cy;}
 protected void onDraw(Canvas c){super.onDraw(c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.GRAY);c.drawLine(cx-r,cy,cx+r,cy,p);c.drawLine(cx,cy-r,cx,cy+r,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(21,101,192));c.drawCircle(kx,ky,kr,p);}
 public boolean onTouchEvent(MotionEvent e){float dx=e.getX()-cx,dy=e.getY()-cy,d=(float)Math.hypot(dx,dy);
  if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){kx=cx;ky=cy;invalidate();if(listener!=null)listener.release();return true;}
  if(d>r){dx=dx/d*r;dy=dy/d*r;} kx=cx+dx;ky=cy+dy;invalidate();if(listener!=null)listener.move(dx/r,-dy/r);return true;}
}