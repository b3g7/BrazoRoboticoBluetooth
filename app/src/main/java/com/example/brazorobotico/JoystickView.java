package com.example.brazorobotico;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    public interface Listener { void move(float x, float y); void release(); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float cx, cy, knobX, knobY, radius;
    private Listener listener;
    private boolean active;

    public JoystickView(Context context) { super(context); init(); }
    public JoystickView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public JoystickView(Context context, AttributeSet attrs, int style) { super(context, attrs, style); init(); }

    private void init() { setFocusable(true); setClickable(true); }
    public void setListener(Listener listener) { this.listener = listener; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cx = w / 2f; cy = h / 2f;
        radius = Math.max(1f, Math.min(w, h) * 0.32f);
        knobX = cx; knobY = cy;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float outer = radius * 1.35f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFFE0E5E8);
        canvas.drawCircle(cx, cy, outer, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4f); paint.setColor(0xFF607D8B);
        canvas.drawCircle(cx, cy, outer, paint);
        paint.setStrokeWidth(2f); paint.setColor(0xFFB0BEC5);
        canvas.drawLine(cx-radius, cy, cx+radius, cy, paint);
        canvas.drawLine(cx, cy-radius, cx, cy+radius, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(active ? 0xFF263238 : 0xFF455A64);
        canvas.drawCircle(knobX, knobY, radius * 0.42f, paint);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(knobX, knobY, radius * 0.12f, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            active = true;
            float dx = event.getX() - cx;
            float dy = event.getY() - cy;
            float distance = (float)Math.hypot(dx, dy);
            if (distance > radius) {
                dx = dx / distance * radius;
                dy = dy / distance * radius;
            }
            knobX = cx + dx; knobY = cy + dy; invalidate();
            if (listener != null) listener.move(dx / radius, dy / radius);
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            active = false; knobX = cx; knobY = cy; invalidate();
            if (listener != null) listener.release();
            performClick();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
