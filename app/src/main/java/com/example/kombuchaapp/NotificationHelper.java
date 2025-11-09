package com.example.kombuchaapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.kombuchaapp.R;
import com.example.kombuchaapp.ViewRecipeActivity;

import java.util.Locale;

public final class NotificationHelper {
    private NotificationHelper() {}

    public static final String CHANNEL_ID_CRITICAL = "temp_alerts_critical";
    private static final String CHANNEL_NAME_CRITICAL = "Temperature Alerts";
    private static final String CHANNEL_DESC_CRITICAL = "Critical kombucha temperature alerts";

    private static final int ID_BASE_CRITICAL = 40000;
    private static final int ID_BASE_HARVEST  = 41000;

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID_CRITICAL,
                    CHANNEL_NAME_CRITICAL,
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription(CHANNEL_DESC_CRITICAL);
            ch.enableLights(true);
            ch.setLightColor(Color.RED);
            ch.enableVibration(true);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    public static void notifyCritical(Context context, String recipeId, String title, String message, float currentF) {
        ensureChannels(context);

        NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
        if (!nmc.areNotificationsEnabled()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            int granted = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, ViewRecipeActivity.class);
        intent.putExtra("recipe_id", recipeId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int reqCode = (recipeId != null ? recipeId.hashCode() : 0);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String content = message + "  •  Current: " + String.format(Locale.getDefault(), "%.1f°F", currentF);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID_CRITICAL)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pi)
                .setAutoCancel(true);

        int notifId = ID_BASE_CRITICAL + Math.abs(reqCode);
        try {
            nmc.notify(notifId, nb.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void notifyReadyToHarvest(Context context,
                                            String recipeId,
                                            String recipeName,
                                            float phValue) {
        ensureChannels(context);

        NotificationManagerCompat nmc = NotificationManagerCompat.from(context);
        if (!nmc.areNotificationsEnabled()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            int granted = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS);
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, ViewRecipeActivity.class);
        intent.putExtra("recipe_id", recipeId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int reqCode = (recipeId != null ? recipeId.hashCode() : 0);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String batchLabel;
        if (recipeName != null && !recipeName.trim().isEmpty()) {
            batchLabel = recipeName;
        } else if (recipeId != null && !recipeId.trim().isEmpty()) {
            batchLabel = "Batch " + recipeId;
        } else {
            batchLabel = "Your kombucha";
        }

        String title = "Ready to Harvest";
        String content = String.format(
                Locale.getDefault(),
                "%s reached pH %.2f. Time to harvest!",
                batchLabel,
                phValue
        );

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID_CRITICAL)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pi)
                .setAutoCancel(true);

        int notifId = ID_BASE_HARVEST + Math.abs(reqCode);
        try {
            nmc.notify(notifId, nb.build());
        } catch (SecurityException ignored) {
        }
    }
}