package com.example.kombuchaapp;

import android.graphics.Color;

public final class TemperatureAlert {
    private TemperatureAlert() {}

    public enum Level { DORMANT, CRITICAL, WARNING, OPTIMAL, LETHAL, UNKNOWN }

    public static final class Result {
        public final Level level;
        public final String title;
        public final String message;
        public final int color;
        public Result(Level level, String title, String message, int color) {
            this.level = level; this.title = title; this.message = message; this.color = color;
        }
    }

    public static Result evaluateF(float tempF) {
        if (Float.isNaN(tempF) || Float.isInfinite(tempF)) {
            return new Result(Level.UNKNOWN, "No reading", "Waiting for a valid sensor reading…", Color.parseColor("#9E9E9E"));
        }
        if (tempF < 50f) {
            return new Result(Level.DORMANT, "Dormant (<50°F)", "Fermentation may stall. Warm to 75–80°F.", Color.parseColor("#546E7A"));
        }
        if (tempF > 90f) {
            return new Result(Level.LETHAL, "Lethal (>90°F)", "Risk of SCOBY death. Cool down immediately!", Color.parseColor("#B71C1C"));
        }
        if (tempF < 65f || tempF > 85f) {
            return new Result(Level.CRITICAL, "Critical (<65°F or >85°F)", "Outside safe range. Adjust ASAP.", Color.parseColor("#D32F2F"));
        }
        if ((tempF >= 65f && tempF < 75f) || (tempF >= 80f && tempF <= 85f)) {
            return new Result(Level.WARNING, "Warning (65–75°F or 80–85°F)", "Not ideal. Aim for 75–80°F.", Color.parseColor("#F57C00"));
        }
        if (tempF >= 75f && tempF < 80f) {
            return new Result(Level.OPTIMAL, "Optimal (75–80°F)", "Perfect brewing temperature.", Color.parseColor("#388E3C"));
        }
        return new Result(Level.UNKNOWN, "Unknown", "Could not classify temperature.", Color.parseColor("#9E9E9E"));
    }
}
