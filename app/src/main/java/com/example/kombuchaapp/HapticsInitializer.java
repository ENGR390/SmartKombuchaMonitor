package com.example.kombuchaapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;

public class HapticsInitializer extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) { attach(a); }
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}

            private void attach(Activity a) {
                View root = a.findViewById(android.R.id.content);
                if (root != null) {
                    root.post(() -> Haptics.attachToTree(root));
                }
            }
        });
    }
}