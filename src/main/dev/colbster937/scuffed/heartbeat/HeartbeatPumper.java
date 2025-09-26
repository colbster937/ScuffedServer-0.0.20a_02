package dev.colbster937.scuffed.heartbeat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.colbster937.scuffed.ScuffedServer;
import dev.colbster937.scuffed.utils.ScuffedHashMap;

public class HeartbeatPumper {
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private HeartbeatProperties properties;
    private Heartbeat classicube;
    private HeartbeatData data;
    private Logger logger;

    public HeartbeatPumper(ScuffedServer server) {
        this.properties = server.heartbeatProperties;
        this.data = new HeartbeatData(server);
        this.logger = server.getLogger();
        this.classicube = new Heartbeat("https://www.classicube.net/heartbeat.jsp", "classicube", this.properties.heartbeatProxy, this.logger);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (this.data.saveFile("heartbeat_data.json")) {
                    ScuffedHashMap<String, Object> content = this.data.getInfo();
                    if (properties.sendClassicubeHeartbeat) {
                        classicube.pump(this.data.toV2(content).toQuery(true));
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to pump heartbeat: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}
