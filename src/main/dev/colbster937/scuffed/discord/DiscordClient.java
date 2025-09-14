package dev.colbster937.scuffed.discord;

import java.awt.Color;
import java.util.List;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;

import dev.colbster937.scuffed.Messages;
import dev.colbster937.scuffed.ScuffedServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordClient extends ListenerAdapter {
  private HttpClient http = HttpClient.newHttpClient();
  private ScuffedServer server;
  private DiscordProperties properties;
  private JDA jda;

  public DiscordClient(ScuffedServer server) {
    this.server = server;
    this.properties = server.discordProperties;
  }

  public void start() throws Exception {
    jda = JDABuilder.createDefault(
        properties.token,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.MESSAGE_CONTENT
      )
      .disableCache(
        CacheFlag.VOICE_STATE,
        CacheFlag.EMOJI,
        CacheFlag.STICKER,
        CacheFlag.SCHEDULED_EVENTS
      )
      .addEventListeners(this)
      .build()
      .awaitReady();

    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  public void stop() {
    if (jda != null) jda.shutdownNow();
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent e) {
    if (e.getAuthor().isBot()) return;
    if (e.getChannel().getId().equals(properties.channel)) {
      String message = e.getMessage().getContentDisplay();
      if (message.equalsIgnoreCase("playerlist") || message.equalsIgnoreCase("players")) {
        String players = server.getPlayers();
        sendMessage("```" + String.format(Messages.ONLINE_PLAYERS, players) + "```");
      } else {
        String name = e.getAuthor().getName();
        server.sendChat("&b" + name + "&f", name, 0, message, "DISCORD");
      }
    }
  }

  public void sendMessage(String content) {
    TextChannel ch = jda.getTextChannelById(properties.channel);
    if (ch != null) {
      ch.sendMessage(content).queue();
    }
  }

  public void sendWebhook(String message, String username) {
    if (properties.webhook == null || properties.webhook.isEmpty()) {
      return;
    }

    try {
      String esc = message
          .replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "");

      StringBuilder sb = new StringBuilder();
      sb.append("{\"content\":\"").append(esc).append("\"");
      if (username != null && !username.isEmpty()) {
        sb.append(",\"username\":\"").append(username.replace("\"", "\\\"")).append("\"");
      }
      sb.append(",\"avatar_url\":\"").append(properties.avatar.replace("\"", "\\\"")).append("\"");
      sb.append(",\"allowed_mentions\":{\"parse\":[]}}");
      String json = sb.toString();

      URL url = new URL(properties.webhook);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");

      try (OutputStream os = conn.getOutputStream()) {
        os.write(json.getBytes("UTF-8"));
      }

      conn.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void send(String name, String message) {
    try {
      if (properties.webhook != null && !properties.webhook.isEmpty()) {
        sendWebhook(message, name);
        return;
      }

      TextChannel ch = jda.getTextChannelById(properties.channel);
      if (ch == null) {
        return;
      }

      List<Webhook> hooks = ch.retrieveWebhooks().complete();
      Webhook hook = null;
      for (Webhook h : hooks) {
        if (h.getName().equalsIgnoreCase("ScuffedBot")) {
          hook = h;
          break;
        }
      }

      if (hook == null) {
        hook = ch.createWebhook("ScuffedBot").complete();
      }

      properties.webhook = hook.getUrl();

      sendWebhook(message, name);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendJoinLeave(String username, boolean type) {
    TextChannel ch = jda.getTextChannelById(properties.channel);
    if (ch == null) return;

    EmbedBuilder eb = new EmbedBuilder();

    eb.setColor(type ? Color.decode("#00ff00") : Color.decode("#ff0000"));
    eb.setAuthor(
      String.format(type ? Messages.JOINED_GAME : Messages.LEFT_GAME, username),
      null,
      properties.avatar
    );

    MessageCreateBuilder mb = new MessageCreateBuilder().addEmbeds(eb.build());
    ch.sendMessage(mb.build()).queue();
  }
}
