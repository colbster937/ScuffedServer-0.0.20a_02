package dev.colbster937.scuffed.discord;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Logger;

public class DiscordProperties {
  private Logger logger;
  private Properties properties = new Properties();

  public String token;
  public String channel;
  public String invite;
  public String webhook;
  public String avatar;
  public boolean webhookType;
  public boolean enabled;

  public DiscordProperties(Logger logger) {
    this.logger = logger;
    this.avatar = "https://minotar.net/helm/MHF_Steve";
  }

  public void reload() {
    try {
      this.properties.load(new FileReader("discord.properties"));
    } catch (Exception var3) {
      logger.warning("Failed to load discord.properties!");
    }

    try {
      this.token = this.properties.getProperty("bot-token", "BOTTOKEN").toString();
      this.channel = this.properties.getProperty("chat-channel", "000000000000000000").toString();
      this.invite = this.properties.getProperty("server-invite", "https://discord.gg/changethisintheproperties").toString();
      this.webhookType = Boolean.parseBoolean(this.properties.getProperty("webhook-type", "true"));
      this.enabled = Boolean.parseBoolean(this.properties.getProperty("enabled", "false"));
      this.properties.setProperty("bot-token", this.token);
      this.properties.setProperty("chat-channel", this.channel);
      this.properties.setProperty("server-invite", this.invite);
      this.properties.setProperty("webhook-type", "" + this.webhookType);
      this.properties.setProperty("enabled", "" + this.enabled);
    } catch (Exception e) {
      logger.warning("discord.properties is broken! Delete it or fix it!");
      System.exit(0);
    }

    try {
      this.properties.store(new FileWriter("discord.properties"), "Discord Properties");
    } catch (Exception var1) {
      logger.warning("Failed to save discord.properties!");
    }
  }
}
