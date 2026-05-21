package com.example.readmebot.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.readmebot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SakuraFallingView extends View {

    private static final int PETAL_COUNT = 15;
    private final List<Petal> petals = new ArrayList<>();
    private final Random random = new Random();
    private Drawable petalDrawable;

    public SakuraFallingView(Context context) {
        super(context);
        init();
    }

    public SakuraFallingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        petalDrawable = ContextCompat.getDrawable(getContext(), R.drawable.sakura_petal);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        petals.clear();
        for (int i = 0; i < PETAL_COUNT; i++) {
            petals.add(new Petal(w, h));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Petal petal : petals) {
            petal.update(getWidth(), getHeight());
            petal.draw(canvas, petalDrawable);
        }
        invalidate(); // Redraw for animation
    }

    private class Petal {
        float x, y, speed, angle, rotationSpeed;
        int size;

        Petal(int width, int height) {
            reset(width, height, true);
        }

        void reset(int width, int height, boolean initial) {
            x = random.nextInt(width);
            y = initial ? random.nextInt(height) : -50;
            speed = 2 + random.nextFloat() * 3;
            angle = random.nextInt(360);
            rotationSpeed = 1 + random.nextFloat() * 2;
            size = 20 + random.nextInt(30);
        }

        void update(int width, int height) {
            y += speed;
            x += Math.sin(y / 50f) * 2; // Swerving effect
            angle += rotationSpeed;
            if (y > height) {
                reset(width, height, false);
            }
        }

        void draw(Canvas canvas, Drawable drawable) {
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(angle);
            drawable.setBounds(0, 0, size, (int) (size * 0.7f));
            drawable.draw(canvas);
            canvas.restore();
        }
    }
}
