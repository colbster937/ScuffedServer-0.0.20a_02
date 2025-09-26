package dev.colbster937.scuffed.utils;

import java.util.concurrent.TimeUnit;

import dev.colbster937.scuffed.ScuffedUtils;

public class DurationTracker {
    private long startTime;
    private TimeUnit unit;

    public DurationTracker(TimeUnit unit) {
        this.startTime = System.nanoTime();
        this.unit = unit;
    }

    public String time() {
        long endTime = System.nanoTime();
        long duration = endTime - this.startTime;

        switch (unit) {
          case MICROSECONDS: return this.d(duration, 1000) + " µs";
          case MILLISECONDS: return this.d(duration, 1000000) + " ms";
          case SECONDS: return this.d(duration, 1000000000) + " s";
          default: return duration + " ns";
        }
    }

    private String d(long var1, long var2) {
        return ScuffedUtils.formatDouble(var1 / var2);
    }
}
