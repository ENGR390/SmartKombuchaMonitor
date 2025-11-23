package com.example.kombuchaapp;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

import com.example.kombuchaapp.NotificationHelper;

public final class AlertAdapter {
    private AlertAdapter() {}

    private static TemperatureAlert.Level lastLevelShown = null;
    private static long lastSevereAtMs = 0;
    private static final long SEVERE_COOLDOWN_MS = 60_000;

    private static long lastCriticalPushAtMs = 0;
    private static final long CRITICAL_PUSH_COOLDOWN = 5 * 60_000;

    private static final long BANNER_DISPLAY_DURATION = 5000; // 5 seconds
    private static Handler bannerHandler = new Handler(Looper.getMainLooper());
    private static Runnable hideBannerRunnable = null;

    public static void handleNewReading(Activity activity, String recipeId, float tempF, View statusPill) {
        TemperatureAlert.Result r = TemperatureAlert.evaluateF(tempF);
        View root = activity.findViewById(android.R.id.content);

        if (r.level != TemperatureAlert.Level.OPTIMAL
                && r.level != TemperatureAlert.Level.UNKNOWN
                && r.level != lastLevelShown) {
            // Show alert banner at top instead of Snackbar at bottom
            showAlertBanner(activity, r.title + " • " + r.message, r.color);
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

        if (r.level == TemperatureAlert.Level.CRITICAL) {
            if (now - lastCriticalPushAtMs > CRITICAL_PUSH_COOLDOWN) {
                lastCriticalPushAtMs = now;
                Context ctx = activity.getApplicationContext();
                NotificationHelper.notifyCritical(
                        ctx,
                        recipeId != null ? recipeId : "",
                        r.title,
                        r.message,
                        tempF
                );
            }
        }
    }

    public static void resetDebounce() {
        lastLevelShown = null;
        lastSevereAtMs = 0;
        lastCriticalPushAtMs = 0;
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

    /**
     * Shows an alert banner at the top of the screen (under the toolbar)
     * instead of a Snackbar at the bottom.
     */
    private static void showAlertBanner(Activity activity, String message, int backgroundColor) {
        View alertBanner = activity.findViewById(R.id.alert_banner);
        TextView alertText = activity.findViewById(R.id.alert_banner_text);
        ImageButton dismissButton = activity.findViewById(R.id.alert_banner_dismiss);

        if (alertBanner == null || alertText == null) {
            return;
        }

        // Cancel any pending hide operation
        if (hideBannerRunnable != null) {
            bannerHandler.removeCallbacks(hideBannerRunnable);
        }

        // Set the message and background color
        alertText.setText(message);
        alertBanner.setBackgroundColor(backgroundColor);

        // Show the banner with animation
        alertBanner.setVisibility(View.VISIBLE);
        alertBanner.setAlpha(0f);
        alertBanner.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

        // Setup dismiss button
        if (dismissButton != null) {
            dismissButton.setOnClickListener(v -> hideAlertBanner(alertBanner));
        }

        // Auto-hide after duration
        hideBannerRunnable = () -> hideAlertBanner(alertBanner);
        bannerHandler.postDelayed(hideBannerRunnable, BANNER_DISPLAY_DURATION);
    }

    /**
     * Hides the alert banner with animation.
     */
    private static void hideAlertBanner(View alertBanner) {
        if (alertBanner == null || alertBanner.getVisibility() != View.VISIBLE) {
            return;
        }

        alertBanner.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> alertBanner.setVisibility(View.GONE))
                .start();

        if (hideBannerRunnable != null) {
            bannerHandler.removeCallbacks(hideBannerRunnable);
            hideBannerRunnable = null;
        }
    }
}
