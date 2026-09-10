package com.brazo.robotico;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    public interface Listener { void move(float x, float y); void release(); }
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cx, cy, knobX, knobY, radius;
    private Listener listener;
    private boolean active;

    public JoystickView(Context c) { super(c); init(); }
    public JoystickView(Context c, AttributeSet a) { super(c,a); init(); }
    public JoystickView(Context c, AttributeSet a, int s) { super(c,a,s); init(); }

    private void init() { setFocusable(true); p.setStrokeWidth(3f); setBackgroundColor(0x00000000); }
    public void setListener(Listener l) { listener = l; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx = w / 2f; cy = h / 2f; radius = Math.min(w,h) * 0.34f; knobX = cx; knobY = cy;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        p.setStyle(Paint.Style.FILL); p.setColor(0xFFECEFF1);
        c.drawCircle(cx, cy, radius * 1.38f, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(0xFFB0BEC5);
        c.drawCircle(cx, cy, radius * 1.38f, p);
        p.setStrokeWidth(2); p.setColor(0xFFCFD8DC);
        c.drawLine(cx-radius, cy, cx+radius, cy, p); c.drawLine(cx, cy-radius, cx, cy+radius, p);
        p.setStyle(Paint.Style.FILL); p.setColor(active ? 0xFF455A64 : 0xFF607D8B);
        c.drawCircle(knobX, knobY, radius * 0.42f, p);
        p.setColor(0xFFFFFFFF); c.drawCircle(knobX, knobY, radius * 0.15f, p);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_MOVE) {
            active = true; float dx=e.getX()-cx, dy=e.getY()-cy; float d=(float)Math.hypot(dx,dy);
            float max=radius; if(d>max){dx=dx/d*max; dy=dy/d*max;}
            knobX=cx+dx; knobY=cy+dy; invalidate();
            if(listener!=null) listener.move(dx/max, dy/max); return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) {
            active=false; knobX=cx; knobY=cy; invalidate(); if(listener!=null) listener.release(); return true;
        }
        return true;
    }
}
