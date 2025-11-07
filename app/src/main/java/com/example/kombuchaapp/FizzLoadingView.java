package com.example.kombuchaapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class FizzLoadingView extends View {

    private static class Bubble {
        float x, y, radius, vy, life, wobblePhase, wobbleAmp;
        int color;
    }

    private final List<Bubble> bubbles = new ArrayList<>();
    private final Random rnd = new Random();

    private Paint bubblePaint;
    private Paint bubbleRimPaint;
    private Paint foamPaint;
    private Paint glassEdgePaint;

    private long lastNanos = 0L;
    private ValueAnimator ticker;

    private int bubbleColor = 0xFF9AD7FF;
    private int foamColor = 0x88FFFFFF;
    private float spawnPerSecond = 26f;
    private float minRadiusDp = 4f;
    private float maxRadiusDp = 11f;
    private float minSpeedDp = 40f;
    private float maxSpeedDp = 120f;

    private boolean highContrast = true;
    private float foamHeightFactor = 0.08f;

    private float density = 1f;

    public FizzLoadingView(Context ctx) { super(ctx); init(ctx); }
    public FizzLoadingView(Context ctx, @Nullable AttributeSet attrs) { super(ctx, attrs); init(ctx); }
    public FizzLoadingView(Context ctx, @Nullable AttributeSet attrs, int defStyleAttr) { super(ctx, attrs, defStyleAttr); init(ctx); }

    private void init(Context ctx) {
        setWillNotDraw(false);
        density = ctx.getResources().getDisplayMetrics().density;

        bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubblePaint.setStyle(Paint.Style.FILL);

        bubbleRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubbleRimPaint.setStyle(Paint.Style.STROKE);
        bubbleRimPaint.setStrokeWidth(1.25f * density);
        bubbleRimPaint.setColor(0xCCFFFFFF);

        foamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foamPaint.setStyle(Paint.Style.FILL);
        foamPaint.setColor(foamColor);

        glassEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassEdgePaint.setStyle(Paint.Style.STROKE);
        glassEdgePaint.setStrokeWidth(2f * density);
        glassEdgePaint.setColor(0x22FFFFFF);

        ticker = ValueAnimator.ofFloat(0f, 1f);
        ticker.setRepeatCount(ValueAnimator.INFINITE);
        ticker.setDuration(1000);
        ticker.addUpdateListener(a -> {
            long now = System.nanoTime();
            if (lastNanos == 0L) lastNanos = now;
            float dt = (now - lastNanos) / 1_000_000_000f;
            lastNanos = now;
            step(dt);
            invalidate();
        });
    }

    public void start() {
        if (!ticker.isStarted()) {
            bubbles.clear();
            lastNanos = 0L;
            ticker.start();
            setVisibility(VISIBLE);
        }
    }

    public void stop() {
        if (ticker.isStarted()) ticker.cancel();
        setVisibility(GONE);
    }

    public void setBubbleColor(int argb) { bubbleColor = argb; }
    public void setSpawnPerSecond(float s) { spawnPerSecond = Math.max(1f, s); }
    public void setBubbleSizeRangeDp(float minDp, float maxDp) {
        minRadiusDp = Math.max(1f, Math.min(minDp, maxDp));
        maxRadiusDp = Math.max(minRadiusDp + 1f, maxDp);
    }
    public void setHighContrast(boolean enabled) { highContrast = enabled; }
    public void setFoamHeightFactor(float f) { foamHeightFactor = Math.max(0.02f, Math.min(0.2f, f)); }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (ticker != null && ticker.isStarted()) ticker.cancel();
    }

    private void step(float dt) {
        if (dt <= 0f) return;

        float expected = spawnPerSecond * dt;
        int n = (int) expected;
        if (rnd.nextFloat() < (expected - n)) n++;
        for (int i = 0; i < n; i++) spawnBubble();

        float w = getWidth(), h = getHeight();
        Iterator<Bubble> it = bubbles.iterator();
        while (it.hasNext()) {
            Bubble b = it.next();
            b.y -= b.vy * dt;
            b.x += (float) Math.sin(b.wobblePhase) * b.wobbleAmp * dt * 60f;
            b.wobblePhase += dt * (2f + rnd.nextFloat() * 3f);
            b.life = 1f - Math.max(0f, b.y) / Math.max(1f, h);

            if (b.y + b.radius < -10f || b.x < -20f || b.x > w + 20f) it.remove();
        }
    }

    private void spawnBubble() {
        float w = Math.max(1f, getWidth());
        float h = Math.max(1f, getHeight());

        Bubble b = new Bubble();
        b.radius = dp(rndRange(minRadiusDp, maxRadiusDp));
        b.x = rndRange(b.radius, w - b.radius);
        float startBand = h * 0.20f;
        b.y = rndRange(h - startBand, h - b.radius);
        b.vy = dp(rndRange(minSpeedDp, maxSpeedDp));
        b.life = 0f;
        b.wobblePhase = rndRange(0f, (float) (2 * Math.PI));
        b.wobbleAmp = dp(rndRange(0.2f, 1.4f)) * (1f + (b.radius / dp(maxRadiusDp)));
        b.color = bubbleColor;

        bubbles.add(b);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        c.drawRoundRect(new RectF(0, 0, w, h), 12f * density, 12f * density, glassEdgePaint);

        float foamH = Math.max(h * foamHeightFactor, 8f * density);
        Path foamPath = new Path();
        foamPath.addRoundRect(new RectF(0, 0, w, foamH), foamH, foamH, Path.Direction.CW);
        c.save();
        c.clipPath(foamPath);
        c.drawRect(0, 0, w, foamH, foamPaint);
        c.restore();

        for (Bubble b : bubbles) {
            int baseAlpha = (int) ((highContrast ? 235 : 200) * (1f - b.life));
            baseAlpha = Math.max(highContrast ? 60 : 30, Math.min(245, baseAlpha));
            int color = (baseAlpha << 24) | (b.color & 0x00FFFFFF);

            RadialGradient grad = new RadialGradient(
                    b.x - b.radius * 0.4f,
                    b.y - b.radius * 0.4f,
                    b.radius * 1.2f,
                    new int[]{0xFFFFFFFF & color, color},
                    new float[]{0.2f, 1f},
                    Shader.TileMode.CLAMP
            );
            bubblePaint.setShader(grad);
            c.drawCircle(b.x, b.y, b.radius, bubblePaint);
            bubblePaint.setShader(null);

            c.drawCircle(b.x, b.y, b.radius, bubbleRimPaint);
        }
    }

    private float dp(float v) { return v * density; }
    private float rndRange(float a, float b) { return a + rnd.nextFloat() * (b - a); }
}
