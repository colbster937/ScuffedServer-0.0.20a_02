package dev.colbster937.scuffed.heartbeat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatPumper {
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HeartbeatProperties properties;
    private Heartbeat classicube;

    public HeartbeatPumper(HeartbeatProperties properties) {
        this.properties = properties;
        this.classicube = new Heartbeat("https://www.classicube.net/heartbeat.jsp", "classicube", properties.heartbeatProxy);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (properties.sendClassicubeHeartbeat) classicube.pump();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
