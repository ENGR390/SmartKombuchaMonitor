package com.example.kombuchaapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class FizzLoaderAdapter {

    public interface Controller {
        @MainThread void show();
        @MainThread void hide();
        @MainThread void setBlockTouches(boolean block);
        @MainThread void setBubbleColor(int argb);
        @MainThread void setShowDelayMs(long ms);
        @MainThread void setMinVisibleMs(long ms);
        @MainThread void setFadeMs(long ms);
        @MainThread void setDimAmount(float amt01);
        @MainThread void setLoaderSizeDp(int dp);
        @MainThread void setBlurRadiusDp(float dp);
        @MainThread void setBlurDownscale(float factor01);
        @MainThread void setDesaturate(boolean enabled);
        @MainThread boolean isShowing();
        @Nullable View getView();
    }

    private FizzLoaderAdapter() {}

    @NonNull
    public static Controller attach(@NonNull Activity activity) {
        Window w = activity.getWindow();
        ViewGroup root = w.findViewById(android.R.id.content);
        return attach(root);
    }

    @NonNull
    public static Controller attach(@NonNull ViewGroup parent) {
        return new C(parent);
    }

    private static final class C implements Controller {
        private final ViewGroup parent;

        private FrameLayout overlay;
        private ImageView blurredBg;
        private FizzLoadingView fizz;

        private boolean block = true;
        private long showDelayMs = 120;
        private long minVisibleMs = 500;
        private long fadeMs = 180;
        private float dimAmount = 0.35f;
        private int loaderSizeDp = 200;

        private float blurRadiusDp = 20f;
        private float blurDownscale = 0.22f;
        private boolean desaturate = true;

        private boolean requested = false;
        private boolean actuallyShowing = false;
        private long actuallyShownUptime = 0L;

        private View snapshotTarget;

        private final Runnable showRunnable;
        private final Runnable hideRunnable;

        C(@NonNull ViewGroup parent) {
            this.parent = parent;

            this.showRunnable = new Runnable() {
                @Override public void run() {
                    if (!requested) return;
                    ensureOverlay();
                    buildBlurredBackground();

                    if (overlay.getParent() == null) C.this.parent.addView(overlay);
                    overlay.setClickable(block);
                    overlay.setFocusable(block);

                    overlay.setAlpha(0f);
                    overlay.animate().cancel();
                    overlay.animate().alpha(1f).setDuration(fadeMs).start();

                    fizz.setVisibility(View.VISIBLE);
                    fizz.start();

                    actuallyShowing = true;
                    actuallyShownUptime = SystemClock.uptimeMillis();
                }
            };

            this.hideRunnable = new Runnable() {
                @Override public void run() {
                    if (!actuallyShowing) return;
                    overlay.animate().cancel();
                    overlay.animate().alpha(0f).setDuration(fadeMs).withEndAction(() -> {
                        if (fizz != null) fizz.stop();
                        if (overlay != null && overlay.getParent() == C.this.parent) C.this.parent.removeView(overlay);
                        overlay.setAlpha(1f);
                        actuallyShowing = false;
                        actuallyShownUptime = 0L;
                        if (blurredBg != null) blurredBg.setImageDrawable(null);
                    }).start();
                }
            };
        }

        @Override public void show() {
            requested = true;
            parent.removeCallbacks(showRunnable);
            parent.postDelayed(showRunnable, showDelayMs);
        }

        @Override public void hide() {
            requested = false;
            parent.removeCallbacks(showRunnable);
            if (!actuallyShowing) return;
            long elapsed = SystemClock.uptimeMillis() - actuallyShownUptime;
            long delay = Math.max(0L, minVisibleMs - elapsed);
            parent.removeCallbacks(hideRunnable);
            if (delay == 0L) parent.post(hideRunnable);
            else parent.postDelayed(hideRunnable, delay);
        }

        @Override public void setBlockTouches(boolean b) {
            block = b;
            if (overlay != null) {
                overlay.setClickable(block);
                overlay.setFocusable(block);
                overlay.setBackgroundColor(block ? dimColor() : Color.TRANSPARENT);
            }
        }

        @Override public void setBubbleColor(int argb) { if (fizz != null) fizz.setBubbleColor(argb); }
        @Override public void setShowDelayMs(long ms) { showDelayMs = Math.max(0, ms); }
        @Override public void setMinVisibleMs(long ms) { minVisibleMs = Math.max(0, ms); }
        @Override public void setFadeMs(long ms) { fadeMs = Math.max(0, ms); }

        @Override public void setDimAmount(float amt01) {
            dimAmount = clamp01(amt01);
            if (overlay != null && block) overlay.setBackgroundColor(dimColor());
        }

        @Override public void setLoaderSizeDp(int dp) {
            loaderSizeDp = Math.max(120, dp);
            if (fizz != null && fizz.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                int px = dpToPx(loaderSizeDp);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fizz.getLayoutParams();
                lp.width = px; lp.height = px;
                fizz.setLayoutParams(lp);
            }
        }

        @Override public void setBlurRadiusDp(float dp) { blurRadiusDp = Math.max(2f, Math.min(50f, dp)); }
        @Override public void setBlurDownscale(float factor01) { blurDownscale = Math.max(0.1f, Math.min(0.6f, factor01)); }
        @Override public void setDesaturate(boolean enabled) { desaturate = enabled; }
        @Override public boolean isShowing() { return actuallyShowing; }
        @Override public @Nullable View getView() { return overlay; }

        private void ensureOverlay() {
            if (overlay != null) return;

            snapshotTarget = findContentChild(parent);

            overlay = new FrameLayout(parent.getContext());
            overlay.setClickable(true);
            overlay.setFocusable(true);
            overlay.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            overlay.setBackgroundColor(block ? dimColor() : Color.TRANSPARENT);

            blurredBg = new ImageView(parent.getContext());
            blurredBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            overlay.addView(blurredBg, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            fizz = new FizzLoadingView(parent.getContext());
            int size = dpToPx(loaderSizeDp);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.gravity = Gravity.CENTER;
            fizz.setLayoutParams(lp);
            overlay.addView(fizz);
        }

        private View findContentChild(ViewGroup root) {
            return (root.getChildCount() > 0) ? root.getChildAt(0) : null;
        }

        private void buildBlurredBackground() {
            if (snapshotTarget == null) { blurredBg.setImageDrawable(null); return; }

            int w = snapshotTarget.getWidth();
            int h = snapshotTarget.getHeight();
            if (w <= 0 || h <= 0) { blurredBg.setImageDrawable(null); return; }

            Bitmap full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(full);
            snapshotTarget.draw(c);

            float scale = blurDownscale;
            int sw = Math.max(1, Math.round(w * scale));
            int sh = Math.max(1, Math.round(h * scale));
            Bitmap small = Bitmap.createScaledBitmap(full, sw, sh, true);
            full.recycle();

            int pxRadius = Math.max(1, Math.round(dpToPxF(blurRadiusDp) * scale));
            Bitmap blurred = small.copy(Bitmap.Config.ARGB_8888, true);
            fastBoxBlur(blurred, pxRadius, 3);
            small.recycle();

            if (desaturate) {
                ColorMatrix m = new ColorMatrix();
                m.setSaturation(0f);
                Paint p = new Paint();
                p.setColorFilter(new ColorMatrixColorFilter(m));
                Bitmap gray = Bitmap.createBitmap(blurred.getWidth(), blurred.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas cc = new Canvas(gray);
                cc.drawBitmap(blurred, 0, 0, p);
                blurred.recycle();
                blurred = gray;
            }

            Bitmap bg = Bitmap.createScaledBitmap(blurred, w, h, true);
            if (bg != blurred) blurred.recycle();

            blurredBg.setImageBitmap(bg);
        }

        private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

        private int dimColor() {
            int alpha = (int) (clamp01(dimAmount) * 0x99) & 0xFF;
            return (alpha << 24);
        }

        private int dpToPx(int dp) {
            float d = parent.getResources().getDisplayMetrics().density;
            return Math.round(dp * d);
        }
        private float dpToPxF(float dp) {
            float d = parent.getResources().getDisplayMetrics().density;
            return dp * d;
        }

        private void fastBoxBlur(Bitmap bmp, int radius, int iterations) {
            if (radius <= 0 || iterations <= 0) return;
            int w = bmp.getWidth(), h = bmp.getHeight();
            int[] src = new int[w * h];
            int[] tmp = new int[w * h];
            bmp.getPixels(src, 0, w, 0, 0, w, h);
            for (int i = 0; i < iterations; i++) {
                boxBlurHorizontal(src, tmp, w, h, radius);
                boxBlurVertical(tmp, src, w, h, radius);
            }
            bmp.setPixels(src, 0, w, 0, 0, w, h);
        }

        private void boxBlurHorizontal(int[] in, int[] out, int w, int h, int r) {
            int div = r * 2 + 1;
            for (int y = 0; y < h; y++) {
                int ti = y * w, li = ti, ri = ti + r;
                int ta = 0, tr = 0, tg = 0, tb = 0;

                for (int i = -r; i <= r; i++) {
                    int idx = clampIndex(ti + clampX(i, w), 0, in.length - 1);
                    int c = in[idx];
                    ta += (c >>> 24); tr += (c >> 16) & 0xFF; tg += (c >> 8) & 0xFF; tb += c & 0xFF;
                }

                for (int x = 0; x < w; x++) {
                    out[ti + x] = ((ta / div) << 24) | ((tr / div) << 16) | ((tg / div) << 8) | (tb / div);

                    int liIdx = clampIndex(li, 0, in.length - 1);
                    int riIdx = clampIndex(ri, 0, in.length - 1);
                    int lc = in[liIdx], rc = in[riIdx];

                    ta += ((rc >>> 24) - (lc >>> 24));
                    tr += (((rc >> 16) & 0xFF) - ((lc >> 16) & 0xFF));
                    tg += (((rc >> 8) & 0xFF) - ((lc >> 8) & 0xFF));
                    tb += (((rc) & 0xFF) - ((lc) & 0xFF));

                    li++; ri++;
                }
            }
        }

        private void boxBlurVertical(int[] in, int[] out, int w, int h, int r) {
            int div = r * 2 + 1;
            for (int x = 0; x < w; x++) {
                int ti = x, li = ti, ri = ti + r * w;
                int ta = 0, tr = 0, tg = 0, tb = 0;

                for (int i = -r; i <= r; i++) {
                    int idx = clampIndex(x + clampY(i, h) * w, 0, in.length - 1);
                    int c = in[idx];
                    ta += (c >>> 24); tr += (c >> 16) & 0xFF; tg += (c >> 8) & 0xFF; tb += c & 0xFF;
                }

                for (int y = 0; y < h; y++) {
                    out[y * w + x] = ((ta / div) << 24) | ((tr / div) << 16) | ((tg / div) << 8) | (tb / div);

                    int liIdx = clampIndex(li, 0, in.length - 1);
                    int riIdx = clampIndex(ri, 0, in.length - 1);
                    int lc = in[liIdx], rc = in[riIdx];

                    ta += ((rc >>> 24) - (lc >>> 24));
                    tr += (((rc >> 16) & 0xFF) - ((lc >> 16) & 0xFF));
                    tg += (((rc >> 8) & 0xFF) - ((lc >> 8) & 0xFF));
                    tb += (((rc) & 0xFF) - ((lc) & 0xFF));

                    li += w; ri += w;
                }
            }
        }

        private int clampX(int x, int w) { return Math.max(0, Math.min(w - 1, x)); }
        private int clampY(int y, int h) { return Math.max(0, Math.min(h - 1, y)); }
        private int clampIndex(int i, int min, int max) { return Math.max(min, Math.min(max, i)); }
    }
}