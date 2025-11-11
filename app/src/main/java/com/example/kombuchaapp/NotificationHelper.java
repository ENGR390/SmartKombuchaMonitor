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

public final class NotificationHelper {
    private NotificationHelper() {}

    public static final String CHANNEL_ID_CRITICAL = "temp_alerts_critical";
    private static final String CHANNEL_NAME_CRITICAL = "Temperature Alerts";
    private static final String CHANNEL_DESC_CRITICAL = "Critical kombucha temperature alerts";

    public static final String CHANNEL_ID_PH = "ph_alerts";
    private static final String CHANNEL_NAME_PH = "pH Alerts";
    private static final String CHANNEL_DESC_PH = "Kombucha pH phase and harvest alerts";

    private static final int ID_BASE_CRITICAL = 40000;
    private static final int ID_BASE_PH = 41000;

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            NotificationChannel chTemp = new NotificationChannel(
                    CHANNEL_ID_CRITICAL,
                    CHANNEL_NAME_CRITICAL,
                    NotificationManager.IMPORTANCE_HIGH
            );
            chTemp.setDescription(CHANNEL_DESC_CRITICAL);
            chTemp.enableLights(true);
            chTemp.setLightColor(Color.RED);
            chTemp.enableVibration(true);
            nm.createNotificationChannel(chTemp);

            NotificationChannel chPh = new NotificationChannel(
                    CHANNEL_ID_PH,
                    CHANNEL_NAME_PH,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            chPh.setDescription(CHANNEL_DESC_PH);
            chPh.enableLights(true);
            chPh.setLightColor(Color.GREEN);
            chPh.enableVibration(true);
            nm.createNotificationChannel(chPh);
        }
    }

    public static void notifyCritical(Context context,
                                      String recipeId,
                                      String title,
                                      String message,
                                      float currentF) {

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

        String content = message + "  •  Current: " + String.format("%.1f°F", currentF);

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
                                            String title,
                                            String message,
                                            float currentPh) {

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

        int reqCode = (recipeId != null ? recipeId.hashCode() : 0) ^ 0x0F0F0F0F;
        PendingIntent pi = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String content = message + "  •  Current pH: " + String.format("%.2f", currentPh);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID_PH)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pi)
                .setAutoCancel(true);

        int notifId = ID_BASE_PH + Math.abs(reqCode);
        try {
            nmc.notify(notifId, nb.build());
        } catch (SecurityException ignored) {
        }
    }
}