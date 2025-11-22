package com.example.kombuchaapp;

import android.graphics.Color;

public final class PhFermentationStage {
    private PhFermentationStage() {}

    public enum Stage {
        INITIAL,
        ACTIVE,
        OPTIMAL,
        TASTE_TEST,
        UNKNOWN
    }

    public static final class Result {
        public final Stage stage;
        public final String title;
        public final String description;
        public final int color;

        public Result(Stage stage, String title, String description, int color) {
            this.stage = stage;
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    public static Result evaluate(float ph) {
        if (Float.isNaN(ph) || Float.isInfinite(ph)) {
            return new Result(
                    Stage.UNKNOWN,
                    "No Reading",
                    "Waiting for pH sensor data",
                    Color.parseColor("#9E9E9E")
            );
        }

        if (ph >= 4.5f) {
            return new Result(
                    Stage.INITIAL,
                    "Initial",
                    "Fermentation just started. Sweet tea taste.",
                    Color.parseColor("#2196F3") // Blue
            );
        }

        if (ph >= 3.5f) {
            return new Result(
                    Stage.ACTIVE,
                    "Active",
                    "Fermentation in progress. Becoming tangy.",
                    Color.parseColor("#FF9800") // Orange
            );
        }

        if (ph >= 2.5f) {
            return new Result(
                    Stage.OPTIMAL,
                    "Optimal",
                    "Perfect for harvesting! Balanced flavor.",
                    Color.parseColor("#4CAF50") // Green
            );
        }

        if (ph > 0) {
            return new Result(
                    Stage.TASTE_TEST,
                    "Taste Test",
                    "Very tart and acidic. Taste before bottling.",
                    Color.parseColor("#F44336") // Red
            );
        }

        return new Result(
                Stage.UNKNOWN,
                "Unknown",
                "pH reading out of range",
                Color.parseColor("#9E9E9E")
        );
    }
}
