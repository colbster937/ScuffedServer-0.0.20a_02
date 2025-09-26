package dev.colbster937.scuffed.heartbeat;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Logger;

import dev.colbster937.scuffed.utils.Port;

public class HeartbeatProperties {
  private Logger logger;
  private Properties properties = new Properties();

  public boolean sendClassicubeHeartbeat = false;
  public String heartbeatProxy = "";
  public int frontendPort;

  public HeartbeatProperties(Logger logger) {
    this.logger = logger;
  }

  public void reload() {
    try {
      this.properties.load(new FileReader("heartbeat.properties"));
    } catch (Exception var3) {
      logger.warning("Failed to load heartbeat.properties!");
    }

    try {
      this.heartbeatProxy = this.properties.getProperty("heartbeat-proxy", "").toString();
      this.sendClassicubeHeartbeat = Boolean.parseBoolean(this.properties.getProperty("classicube-heartbeat", "false"));
      this.frontendPort = Port.parsePort(this.properties.getProperty("frontend-port", "-1"));
      this.properties.setProperty("classicube-heartbeat", "" + this.sendClassicubeHeartbeat);
      this.properties.setProperty("heartbeat-proxy", "" + this.heartbeatProxy);
      this.properties.setProperty("frontend-port", "" + this.frontendPort);
    } catch (Exception e) {
      logger.warning("heartbeat.properties is broken! Delete it or fix it!");
      System.exit(0);
    }

    try {
      this.properties.store(new FileWriter("heartbeat.properties"), "Heartbeat Properties");
    } catch (Exception var1) {
      logger.warning("Failed to save heartbeat.properties!");
    }
  }
}
