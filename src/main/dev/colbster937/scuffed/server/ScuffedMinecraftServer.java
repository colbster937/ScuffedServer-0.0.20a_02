package dev.colbster937.scuffed.server;

import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Logger;

import com.mojang.minecraft.net.Packet;
import com.mojang.minecraft.server.MinecraftServer;
import com.mojang.minecraft.server.PlayerInstance;

import dev.colbster937.scuffed.ScuffedServer;
import dev.colbster937.scuffed.ScuffedUtils;

public class ScuffedMinecraftServer extends MinecraftServer {
    public static Logger logger = Logger.getLogger("MinecraftServer");

    private static Scanner scanner;
    private static ScuffedServer staticScuffedServer;
    private static ScuffedMinecraftServer staticScuffedMinecraftServer;
    public ScuffedServer scuffedServer;

    public ScuffedMinecraftServer() throws IOException {
        super();
        staticScuffedMinecraftServer = this;
        staticScuffedServer = new ScuffedServer(this);
        this.scuffedServer = staticScuffedServer;

        /* new Thread(() -> {
            scanner = new Scanner(System.in);
            while (true) {
                try {
                    if (scanner.hasNextLine()) {
                        String command = scanner.nextLine().trim();
                        if (!command.isEmpty()) {
                            this.handleCommand(null, command);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start(); */

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.scuffedServer.shutdown();
                // scanner.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    public static ScuffedServer getInstance() {
        return staticScuffedServer;
    }

    public static ScuffedMinecraftServer getThis() {
        return staticScuffedMinecraftServer;
    }

    public void handleCommand(PlayerInstance player, String commandString) {
        ScuffedPlayer plr;
        if (player != null) {
            plr = player.scuffedPlayer;
        } else {
            plr = null;
        }
        if (!scuffedServer.handleCommand(plr, commandString)) super.parseCommand(player, commandString.substring(1));
    }

	public void sendLoggedInPacket(Packet packet, Object... data) {
		for(int i = 0; i < this.getPlayerList().size(); ++i) {
			try {
                PlayerInstance player = (PlayerInstance)this.getPlayerList().get(i);
				if (player.scuffedPlayer.loggedIn) player.sendPacket(packet, data);
			} catch (Exception e) {
				((PlayerInstance)this.getPlayerList().get(i)).handleException(e);
			}
		}
	}

    public void sendPlayerPacketLoggedIn(ScuffedPlayer player, Packet packet, Object... data) {
        if (player.loggedIn) this.sendPlayerPacket(player.player, packet, data);
    }

    public Object get(String name) {
        return ScuffedUtils.getField(this, name);
    }
}