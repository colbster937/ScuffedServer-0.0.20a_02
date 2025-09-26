package dev.colbster937.scuffed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.mojang.minecraft.server.PlayerInstance;
import com.mojang.minecraft.server.PlayerList;

import dev.colbster937.scuffed.discord.DiscordClient;
import dev.colbster937.scuffed.discord.DiscordProperties;
import dev.colbster937.scuffed.heartbeat.HeartbeatData;
import dev.colbster937.scuffed.heartbeat.HeartbeatProperties;
import dev.colbster937.scuffed.heartbeat.HeartbeatPumper;
import dev.colbster937.scuffed.password.PasswordHasher;
import dev.colbster937.scuffed.server.ScuffedMinecraftServer;
import dev.colbster937.scuffed.server.ScuffedPlayer;
import dev.colbster937.scuffed.utils.DurationTracker;

import com.mojang.minecraft.level.Level;
import com.mojang.minecraft.net.Packet;

public class ScuffedServer {
    private static Logger logger;
    private Properties properties = new Properties();
    public ScuffedMinecraftServer server;
    public DiscordClient discordClient;
    public DiscordProperties discordProperties;
    public HeartbeatProperties heartbeatProperties;
    public boolean authSystem = true;
    public boolean antiCheat = true;
    public boolean liquidFlow = true;
    public boolean lavaSponge = false;
    public String publicAddr = "0.0.0.0";
    public String levelSizeStr = "256x64x256";
    public int[] levelSize = ScuffedUtils.getLevelSize(this.levelSizeStr);
    public int maxPlayers = 16;
    public int loginTimeout = 30;

    public ScuffedServer(ScuffedMinecraftServer server) {
        DurationTracker loadTime = new DurationTracker(TimeUnit.MILLISECONDS);

        this.server = server;

        logger = ScuffedMinecraftServer.logger;

        logger.info("Loading " + ScuffedConstants.SERVER_STRING);

        this.reloadOverrides();

        this.discordProperties = new DiscordProperties(logger);
        this.heartbeatProperties = new HeartbeatProperties(logger);

        this.discordProperties.reload();
        this.heartbeatProperties.reload();

        ChatFilter.reloadFilter();

        this.discordClient = new DiscordClient(this);

        if (!"BOTTOKEN".equals(this.discordProperties.token)) {
            try {
                this.discordClient.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info(ScuffedConstants.SERVER_SOFTWARE + " loaded in " + loadTime.time());

        this.discordClient.sendMessage(Messages.SERVER_STARTED);

        HeartbeatPumper pumper = new HeartbeatPumper(this);
        pumper.start();
    }

    public void reloadOverrides() {
        try {
            this.properties.load(new FileReader("scuffed.properties"));
        } catch (Exception var3) {
            logger.warning("Failed to load scuffed.properties!");
        }

        try {
            this.authSystem = Boolean.parseBoolean(this.properties.getProperty("auth-system", "true"));
            this.antiCheat = Boolean.parseBoolean(this.properties.getProperty("anti-cheat", "true"));
            this.liquidFlow = Boolean.parseBoolean(this.properties.getProperty("liquid-flow", "true"));
            this.lavaSponge = Boolean.parseBoolean(this.properties.getProperty("lava-sponge", "false"));
            this.publicAddr = this.properties.getProperty("public-addr", "0.0.0.0").toString();
            this.levelSizeStr = this.properties.getProperty("level-size", "256x64x256").toString();
            this.maxPlayers = Integer.parseInt(this.properties.getProperty("max-players", "16"));
            this.loginTimeout = Integer.parseInt(this.properties.getProperty("login-timeout", "30"));

            if (this.maxPlayers < 1) {
                this.maxPlayers = 16;
            }

            if (this.loginTimeout < 1) {
                this.loginTimeout = 30;
            }

            this.levelSize = ScuffedUtils.getLevelSize(this.levelSizeStr);

            this.properties.setProperty("auth-system", "" + this.authSystem);
            this.properties.setProperty("anti-cheat", "" + this.antiCheat);
            this.properties.setProperty("liquid-flow", "" + this.liquidFlow);
            this.properties.setProperty("lava-sponge", "" + this.lavaSponge);
            this.properties.setProperty("public-addr", "" + this.publicAddr);
            this.properties.setProperty("level-size", "" + this.levelSizeStr);
            this.properties.setProperty("max-players", "" + this.maxPlayers);
            this.properties.setProperty("login-timeout", "" + this.loginTimeout);
        } catch (Exception e) {
            logger.warning("scuffed.properties is broken! Delete it or fix it!");
            System.exit(0);
        }

        try {
            this.properties.store(new FileWriter("scuffed.properties"), "Scuffed Overrides");
        } catch (Exception var1) {
            logger.warning("Failed to save scuffed.properties!");
        }

        logger.info("Scuffed Overrides:");
        logger.info(" * auth-system = " + ScuffedUtils.formatEnabledDisabled(this.authSystem));
        logger.info(" * anti-cheat = " + ScuffedUtils.formatEnabledDisabled(this.antiCheat));
        logger.info(" * liquid-flow = " + ScuffedUtils.formatEnabledDisabled(this.liquidFlow));
        logger.info(" * lava-sponge = " + ScuffedUtils.formatEnabledDisabled(this.lavaSponge));
        logger.info(" * public-addr = " + this.publicAddr.toString());
        logger.info(" * level-size = " + this.levelSizeStr + " (NOT IMPLEMENTED YET)");
        logger.info(" * max-players = " + this.maxPlayers);
        logger.info(" * login-timeout = " + this.loginTimeout);

        ScuffedUtils.setField(this.server, "maxPlayers", "" + this.maxPlayers);
        ScuffedUtils.setField(this.server, "maxConnectCount", "" + this.maxPlayers);
        ScuffedUtils.setField(this.server, "verifyNames", "false");
    }

    public void shutdown() {
        this.discordClient.sendMessage(Messages.SERVER_STOPPED);
    }

    public boolean handleCommand(ScuffedPlayer player, String commandString) {
		if (player != null && !player.loggedIn && ScuffedUtils.isLoginCommand(commandString) == 0) {
		    player.sendColoredChat(Messages.NOT_LOGGED_IN);
            return true;
        }
        String[] command = commandString.split(" ");
        if (ScuffedUtils.isLoginCommand(commandString) == 2 && player != null) {
            if (ScuffedUtils.isRegistered(player.player.name)) {
                player.sendColoredChat(Messages.ALREADY_REGISTERED);
                return true;
            }
            if (command.length != 3) {
                player.sendColoredChat("&eUsage: " + command[0] + " <password> <password>");
                return true;
            }
            if (!command[1].equals(command[2])) {
                player.sendColoredChat(Messages.NO_PASSWORD_MATCH);
                return true;
            }
            try {
                String hash = PasswordHasher.hash(command[1]);
                File file = new File("users", player.player.name + ".txt");
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), hash.getBytes());
                player.sendColoredChat(Messages.REGISTER_SUCCESS);
                player.registered = true;
                this.login(player, true);
            } catch (Exception e) {
                player.sendColoredChat(Messages.REGISTER_FAIL);
            }
            return true;
        } else if (ScuffedUtils.isLoginCommand(commandString) == 1 && player != null) {
            if (player.loggedIn) {
                player.sendColoredChat(Messages.ALREADY_LOGGED_IN);
                return true;
            }
            if (command.length != 2) {
                player.sendColoredChat("&eUsage: " + command[0] + " <password>");
                return true;
            }
            try {
                if (!ScuffedUtils.isRegistered(player.player.name)) {
                    player.sendColoredChat(Messages.NOT_REGISTERED);
                    return true;
                }
                String storedHash = new String(Files.readAllBytes(new File("users", player.player.name + ".txt").toPath())).trim();
                String inputHash = PasswordHasher.hash(command[1]);
                if (storedHash.equals(inputHash)) {
                    this.login(player, true);
                } else {
                    player.sendColoredChat(Messages.INCORRECT_PASSWORD);
                }
            } catch (Exception e) {
                player.sendColoredChat(Messages.LOGIN_FAIL);
            }
            return true;
        } else if ((ScuffedUtils.isCommand(commandString, "/sponge") || ScuffedUtils.isCommand(commandString, "/drain")) && player != null && ScuffedUtils.isAdmin(this.server, player.player.name)) {
            DurationTracker durationTracker = new DurationTracker(TimeUnit.MILLISECONDS);
            int count = 0;
            player.sendColoredChat(String.format(Messages.DRAIN_DONE, count, durationTracker.time()));
            return true;
        } else if ((ScuffedUtils.isCommand(commandString, "/reload") || ScuffedUtils.isCommand(commandString, "/rl")) && player != null && ScuffedUtils.isAdmin(this.server, player.player.name)) {
            DurationTracker durationTracker = new DurationTracker(TimeUnit.MILLISECONDS);
            this.reloadOverrides();
            this.discordProperties.reload();
            this.heartbeatProperties.reload();
            ChatFilter.reloadFilter();
            server.admins = new PlayerList("Admins", new File("admins.txt"));
            player.sendColoredChat(String.format(Messages.RELOADED, durationTracker.time()));
            return true;
        } else if ((ScuffedUtils.isCommand(commandString, "/say") || ScuffedUtils.isCommand(commandString, "/broadcast") || ScuffedUtils.isCommand(commandString, "/bc")) && ScuffedUtils.isAdmin(this.server, player.player.name)) {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(command));
            args.remove(0);
            String msg = String.join(" ", args);
            this.sendChat("&5Broadcast&f", "Broadcast", 0, msg, "CHAT");
            return true;
        } else if ((ScuffedUtils.isCommand(commandString, "/sayraw") || ScuffedUtils.isCommand(commandString, "/broadcastraw") || ScuffedUtils.isCommand(commandString, "/bcraw")) && ScuffedUtils.isAdmin(this.server, player.player.name)) {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(command));
            args.remove(0);
            String msg = String.join(" ", args);
            List players = this.server.getPlayerList();
            for (int i = 0; i < players.size(); i++) {
                ((PlayerInstance) players.get(i)).scuffedPlayer.sendColoredChat(msg);
            }
            return true;
        } else if (ScuffedUtils.isCommand(commandString, "/discord") || ScuffedUtils.isCommand(commandString, "/dsc") && player != null) {
            player.sendColoredChat(String.format(Messages.JOIN_DISCORD, this.discordProperties.invite));
            return true;
        } else if (ScuffedUtils.isCommand(commandString, "/rules") && player != null) {
            File file = new File("rules.txt");
            if (file.exists()) {
                player.sendColoredChat(Messages.RULES_TITLE);
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    int i = 1;
                    while ((line = reader.readLine()) != null) {
                        if (!line.trim().isEmpty()) {
                            player.sendColoredChat(String.format(Messages.RULES_LINE, i + ".", line));
                            i++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        } else if (ScuffedUtils.isCommand(commandString, "/playerlist") || ScuffedUtils.isCommand(commandString, "/players") || ScuffedUtils.isCommand(commandString, "/list") && player != null) {
            player.sendColoredChat(String.format(Messages.ONLINE_PLAYERS, this.getPlayers()));
            return true;
        } if (((ScuffedUtils.isCommand(commandString, "/sponge") || ScuffedUtils.isCommand(commandString, "/drain") || ScuffedUtils.isCommand(commandString, "/reload") || ScuffedUtils.isCommand(commandString, "/rl") || ScuffedUtils.isCommand(commandString, "/say") || ScuffedUtils.isCommand(commandString, "/broadcast") || ScuffedUtils.isCommand(commandString, "/bc") || ScuffedUtils.isCommand(commandString, "/sayraw") || ScuffedUtils.isCommand(commandString, "/broadcastraw") || ScuffedUtils.isCommand(commandString, "/bcraw")) && (!ScuffedUtils.isAdmin(this.server, player.player.name))) || ((ScuffedUtils.isCommand(commandString, "/op") || ScuffedUtils.isCommand(commandString, "/deop")) && player != null)) {
            player.sendColoredChat(Messages.NO_PERMISSION);
            return true;
        }
        return false;
    }

    public void login(ScuffedPlayer player, boolean alert) {
        player.loggedIn = true;
        if (alert) player.sendColoredChat(Messages.LOGIN_SUCCESS);
        this.server.sendPacket(Packet.CHAT_MESSAGE,
                new Object[] { Integer.valueOf(-1), String.format(Messages.JOINED_GAME, player.player.name) });
        
        this.discordClient.sendJoinLeave(player.player.name, true);

        Level lvl = this.server.level;

        this.server.sendPlayerPacket(player.player, Packet.PLAYER_JOIN,
                new Object[] { Integer.valueOf(player.player.playerID), player.player.name,
                        Integer.valueOf((lvl.xSpawn << 5) + 16), Integer.valueOf((lvl.ySpawn << 5) + 16),
                        Integer.valueOf((lvl.zSpawn << 5) + 16),
                        Integer.valueOf((int) (lvl.rotSpawn * 256.0F / 360.0F)), Integer.valueOf(0) });
        Iterator players = this.server.getPlayerList().iterator();

        while (players.hasNext()) {
            PlayerInstance playerInstance = (PlayerInstance) players.next();
            if (playerInstance != null && playerInstance != player.player && playerInstance.scuffedPlayer.loggedIn && ((boolean)ScuffedUtils.getField(playerInstance, "onlyIP"))) {
                player.player.connection.sendPacket(Packet.PLAYER_JOIN,
                        new Object[] { Integer.valueOf(playerInstance.playerID), playerInstance.name,
                                Integer.valueOf(playerInstance.x),
                                Integer.valueOf(playerInstance.y), Integer.valueOf(playerInstance.z),
                                Integer.valueOf(playerInstance.yaw),
                                Integer.valueOf(playerInstance.pitch) });
            }
        }

        HeartbeatData.getInstance().updateInfo();
    }

    public static boolean chatLoggedIn(ScuffedPlayer player, String message) {
		if (!player.loggedIn && ScuffedUtils.isLoginCommand(message) == 0) {
		    player.sendColoredChat(Messages.NOT_LOGGED_IN);
            return false;
        }

        return true;
    }

    public void sendLogout(ScuffedPlayer player) {
        this.server.sendPlayerPacketLoggedIn(player, Packet.CHAT_MESSAGE, new Object[]{Integer.valueOf(-1), String.format(Messages.LEFT_GAME, player.player.name)});
        if (!player.player.name.isEmpty() && !player.player.name.isBlank() && player.player.name != null && player.loggedIn) this.discordClient.sendJoinLeave(player.player.name, false);
        HeartbeatData.getInstance().updateInfo();
    }

    public void sendChat(PlayerInstance player, String message) {
        boolean admin = ScuffedUtils.isAdmin(this.server, player.name);
        if (player.scuffedPlayer.loggedIn) {
            this.sendChat(admin ? ("&c" + player.name + "&f") : player.name, player.name, player.playerID, message, "CHAT");
        } else {
            player.scuffedPlayer.sendColoredChat(Messages.NOT_LOGGED_IN);
        }
    }

    public void sendChat(String playerDisplay, String playerName, int playerID, String message, String messageType) {
        String msg = ChatFilter.filterMessage(message);
        logger.info("[" + messageType + "] " + String.format(Messages.CHAT_FORMAT, playerName, msg));
        this.server.sendLoggedInPacket(Packet.CHAT_MESSAGE, new Object[]{Integer.valueOf(playerID), String.format(Messages.CHAT_FORMAT, playerDisplay, msg)});
        if (messageType != "DISCORD") {
            if (discordProperties.webhookType) {
                this.discordClient.send(playerName, msg);
            } else {
                this.discordClient.sendMessage(String.format(Messages.DISCORD_CHAT_FORMAT, playerName, msg));
            }
        }
    }

    public List getPlayerList() {
        return (List) server.getPlayerList().stream()
            .filter(p -> ((PlayerInstance)p).scuffedPlayer != null && ((PlayerInstance)p).scuffedPlayer.loggedIn)
            .collect(Collectors.toList());
    }

    public String getPlayers() {
        String players = server.getPlayerList().stream()
            .map(o -> (PlayerInstance) o)
            .filter(p -> ((PlayerInstance)p).scuffedPlayer != null && ((PlayerInstance)p).scuffedPlayer.loggedIn)
            .map(p -> ((PlayerInstance)p).name)
            .collect(Collectors.joining(", ")).toString();

        return players.isEmpty() ? "None" : players;
    }

    public int getFrontendPort() {
        int port = this.heartbeatProperties.frontendPort;
        if (port >= 0 && port <= 65535) {
            return port;
        } else {
            return (int) ScuffedUtils.getField(this.server, "port");
        }
    }

    public Logger getLogger() {
        return logger;
    }
}