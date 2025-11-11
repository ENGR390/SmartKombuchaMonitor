package com.example.kombuchaapp;

public final class PhAlert {
    private PhAlert() {}

    public enum Level {
        SWEET,
        TANGY,
        VINEGARY,
        UNKNOWN
    }

    public static final class Result {
        public final Level level;
        public final String title;
        public final String message;

        public Result(Level level, String title, String message) {
            this.level = level;
            this.title = title;
            this.message = message;
        }
    }

    public static Result evaluate(float ph) {
        if (Float.isNaN(ph) || Float.isInfinite(ph)) {
            return new Result(
                    Level.UNKNOWN,
                    "No pH reading",
                    "Waiting for a valid pH reading…"
            );
        }

        if (ph < 3.5f) {
            return new Result(
                    Level.VINEGARY,
                    "Vinegary (pH < 3.5)",
                    "Very tart and sour. Consider harvesting or diluting."
            );
        }

        if (ph < 4.0f) {
            return new Result(
                    Level.TANGY,
                    "Tangy (pH 3.5–4.0)",
                    "Nicely tart. This is a common harvest range."
            );
        }

        if (ph <= 4.5f) {
            return new Result(
                    Level.SWEET,
                    "Sweet (pH 4.0–4.5)",
                    "Still quite sweet. Let it ferment longer if you want more tang."
            );
        }

        return new Result(
                Level.UNKNOWN,
                "Outside range",
                "pH is outside the expected kombucha range."
        );
    }
}