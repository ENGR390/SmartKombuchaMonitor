package com.example.kombuchaapp;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

public final class AlertAdapter {
    private AlertAdapter() {}

    private static TemperatureAlert.Level lastLevelShown = null;
    private static long lastSevereAtMs = 0;
    private static final long SEVERE_COOLDOWN_MS = 60_000;

    public static void handleNewReading(Activity activity, float tempF, View statusPill) {
        TemperatureAlert.Result r = TemperatureAlert.evaluateF(tempF);
        View root = activity.findViewById(android.R.id.content);

        if (r.level != TemperatureAlert.Level.OPTIMAL
                && r.level != TemperatureAlert.Level.UNKNOWN
                && r.level != lastLevelShown) {
            Snackbar sb = Snackbar.make(root, r.title + " • " + r.message, Snackbar.LENGTH_LONG);
            sb.setBackgroundTint(r.color);
            sb.setTextMaxLines(3);
            sb.show();
            lastLevelShown = r.level;
        }

        if (statusPill != null) {
            statusPill.setVisibility(View.VISIBLE);
            tintBadge(statusPill, r.color);

            if (statusPill instanceof android.widget.TextView) {
                ((android.widget.TextView) statusPill).setText(
                        r.level == TemperatureAlert.Level.OPTIMAL ? "OPTIMAL" :
                                r.level == TemperatureAlert.Level.WARNING ? "WARNING" :
                                        r.level == TemperatureAlert.Level.CRITICAL ? "CRITICAL" :
                                                r.level == TemperatureAlert.Level.LETHAL ? "LETHAL" :
                                                        r.level == TemperatureAlert.Level.DORMANT ? "DORMANT" : "—"
                );
                statusPill.setContentDescription("Temperature status: " + r.title);
            }
        }

        long now = System.currentTimeMillis();
        switch (r.level) {
            case LETHAL:
                if (now - lastSevereAtMs > SEVERE_COOLDOWN_MS) {
                    flashOverlay(root, 0x80B71C1C);
                    shake(root);
                    lastSevereAtMs = now;
                }
                break;
            case CRITICAL:
                wobble(statusPill != null ? statusPill : root);
                break;
            case WARNING:
                pulse(statusPill != null ? statusPill : root);
                break;
            default:
                break;
        }
    }

    public static void resetDebounce() {
        lastLevelShown = null;
        lastSevereAtMs = 0;
    }

    private static void tintBadge(View v, int toColor) {
        ColorStateList currentTint = ViewCompat.getBackgroundTintList(v);
        Integer from = (currentTint != null) ? currentTint.getDefaultColor() : null;

        if (from != null) {
            animateViewTint(v, from, toColor, 250);
            return;
        }

        Drawable bg = v.getBackground();
        if (bg != null) {
            Drawable d = bg.mutate();
            if (d instanceof GradientDrawable) {
                int fromColor = extractSolidColor((GradientDrawable) d, Color.parseColor("#4A148C"));
                animateGradientSolid((GradientDrawable) d, fromColor, toColor, 250);
                v.invalidate();
                return;
            }

            Drawable wrap = DrawableCompat.wrap(d);
            DrawableCompat.setTint(wrap, toColor);
            v.setBackground(wrap);
        }
    }

    private static void animateViewTint(View v, int from, int to, long dur) {
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        va.setDuration(dur);
        va.addUpdateListener(a ->
                ViewCompat.setBackgroundTintList(v, ColorStateList.valueOf((Integer) a.getAnimatedValue())));
        va.start();
    }

    private static void animateGradientSolid(GradientDrawable gd, int from, int to, long dur) {
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        va.setDuration(dur);
        va.addUpdateListener(a -> gd.setColor((Integer) a.getAnimatedValue()));
        va.start();
    }

    private static int extractSolidColor(GradientDrawable gd, int fallback) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24 && gd.getColor() != null) {
                return gd.getColor().getDefaultColor();
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static void flashOverlay(View root, int overlayColor) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) root;
        View overlay = new View(root.getContext());
        overlay.setBackgroundColor(overlayColor);
        overlay.setAlpha(0f);
        vg.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.animate().alpha(1f).setDuration(100).withEndAction(() ->
                overlay.animate().alpha(0f).setDuration(250).withEndAction(() -> vg.removeView(overlay)).start()
        ).start();
    }

    private static void pulse(View v) {
        if (v == null) return;
        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    private static void wobble(View v) {
        if (v == null) return;
        ObjectAnimator oa = ObjectAnimator.ofFloat(v, View.TRANSLATION_X,
                0f, -10f, 10f, -6f, 6f, -3f, 3f, 0f);
        oa.setDuration(380);
        oa.start();
    }

    private static void shake(View v) {
        if (v == null) return;
        ObjectAnimator oa = ObjectAnimator.ofFloat(v, View.TRANSLATION_X,
                0f, -18f, 18f, -12f, 12f, -6f, 6f, 0f);
        oa.setDuration(420);
        oa.start();
    }
}
