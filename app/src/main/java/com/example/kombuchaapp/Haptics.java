package com.example.kombuchaapp;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public final class Haptics {
    private Haptics() {}

    public static void attachToTree(View root) {
        if (root == null) return;
        attachRecursively(root);
    }

    private static void attachRecursively(View v) {
        if (isButtonLike(v)) {
            v.setHapticFeedbackEnabled(true);

            v.setOnTouchListener((view, event) -> {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        return true;

                    case MotionEvent.ACTION_UP:
                        float x = event.getX();
                        float y = event.getY();
                        boolean inside =
                                (x >= 0 && y >= 0
                                        && x <= view.getWidth()
                                        && y <= view.getHeight());

                        if (inside) {
                            view.performHapticFeedback(
                                    HapticFeedbackConstants.CONTEXT_CLICK
                            );
                            view.performClick();
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return false;
            });
        }

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                attachRecursively(vg.getChildAt(i));
            }
        }
    }

    private static boolean isButtonLike(View v) {
        if (v instanceof Button) return true;
        if (v instanceof ImageButton) return true;
        if (v instanceof EditText) return false;
        if (v.isClickable()) return true;

        return false;
    }
}