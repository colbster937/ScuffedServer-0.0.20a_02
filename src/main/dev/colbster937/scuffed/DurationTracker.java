package dev.colbster937.scuffed;

import java.util.concurrent.TimeUnit;

public class DurationTracker {
    private long startTime;
    private long endTime;
    private long duration;
    private TimeUnit unit;

    public DurationTracker(TimeUnit unit) {
        this.startTime = System.nanoTime();
        this.unit = unit;
    }

    public String end() {
        this.endTime = System.nanoTime();
        this.duration = this.endTime - this.startTime;

        switch (unit) {
          case MICROSECONDS: return this.d(this.duration, 1000) + " µs";
          case MILLISECONDS: return this.d(this.duration, 1000000) + " ms";
          case SECONDS: return this.d(this.duration, 1000000000) + " s";
          default: return this.duration + " ns";
        }
    }

    private String d(long var1, long var2) {
        return ScuffedUtils.formatDouble(var1 / var2);
    }
}
