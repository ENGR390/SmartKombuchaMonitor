package com.example.kombuchaapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Random;

public final class FizzTransitionUtil {

    private FizzTransitionUtil() {}

    public static void play(Activity activity, Runnable onEnd) {
        View root = activity.findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) {
            if (onEnd != null) onEnd.run();
            return;
        }

        ViewGroup vg = (ViewGroup) root;

        FrameLayout overlay = new FrameLayout(activity);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setBackgroundColor(Color.parseColor("#F4B266"));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        vg.addView(overlay, lp);

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;
        Random rand = new Random();

        int bubbleCount = 24;
        for (int i = 0; i < bubbleCount; i++) {
            View bubble = new View(activity);

            int sizeDp = 8 + rand.nextInt(16);
            int sizePx = dpToPx(activity, sizeDp);

            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(sizePx, sizePx);
            blp.gravity = Gravity.BOTTOM | Gravity.START;
            blp.leftMargin = rand.nextInt(Math.max(screenW - sizePx, 1));
            bubble.setLayoutParams(blp);

            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(Color.argb(220, 255, 255, 255));
            bubble.setBackground(gd);

            overlay.addView(bubble);

            float travel = screenH * (0.4f + rand.nextFloat() * 0.5f);
            long delay = rand.nextInt(250);
            long dur = 500 + rand.nextInt(500);

            bubble.setTranslationY(0f);
            bubble.setAlpha(0f);

            bubble.animate()
                    .alpha(1f)
                    .translationY(-travel)
                    .setStartDelay(delay)
                    .setDuration(dur)
                    .withEndAction(() -> bubble.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .start())
                    .start();
        }

        overlay.setAlpha(0f);
        overlay.animate()
                .alpha(1f)
                .setDuration(180)
                .withEndAction(() -> {
                    if (onEnd != null) {
                        onEnd.run();
                    }

                    overlay.postDelayed(() -> {
                        overlay.animate()
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction(() -> {
                                    ViewGroup parent = (ViewGroup) overlay.getParent();
                                    if (parent != null) {
                                        parent.removeView(overlay);
                                    }
                                })
                                .start();
                    }, 350);
                })
                .start();
    }

    private static int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}