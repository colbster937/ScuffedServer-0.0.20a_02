package dev.colbster937.scuffed.server;

import com.mojang.minecraft.net.Packet;
import com.mojang.minecraft.server.PlayerInstance;

import dev.colbster937.scuffed.Messages;
import dev.colbster937.scuffed.ScuffedServer;
import dev.colbster937.scuffed.ScuffedUtils;

public class ScuffedPlayer {
    private long initTime;
    private long loginRemindTime;
    public ScuffedServer server;
    public PlayerInstance player;
    public long loginTimeout;
	public boolean loggedIn;
	public boolean registered;
    public boolean vanished;

    public ScuffedPlayer(PlayerInstance player) {
        this.server = ScuffedMinecraftServer.getInstance();
        this.player = player;
        this.initTime = System.currentTimeMillis();
        this.loginRemindTime = System.currentTimeMillis();
        this.loginTimeout = server.loginTimeout;
        this.loggedIn = !server.authSystem;
        this.registered = false;
        this.vanished = false;
    }

    public void remindLogin(boolean force) {
	    if (!this.loggedIn) {
            if ((System.currentTimeMillis() - this.loginRemindTime >= 5000L) || force) {
                if (ScuffedUtils.isRegistered(this)) {
                    this.sendColoredChat(Messages.LOGIN_REMINDER);
                } else {
                    this.sendColoredChat(Messages.REGISTER_REMINDER);
                }
                this.loginRemindTime = System.currentTimeMillis();
            }
        }
	}

    public void tick() {
        this.remindLogin(false);

        if (!this.loggedIn && server.authSystem && System.currentTimeMillis() - this.initTime > ((long) this.loginTimeout * 1000L)) {
            this.player.kick("You must log in within " + this.loginTimeout + " seconds!");
            return;
        }
    }

    public void sendColoredChat(String message) {
        this.player.sendPacket(Packet.CHAT_MESSAGE, new Object[]{Integer.valueOf(0), message});
    }

    public void clearChat() {
        for (int i = 0; i < 50; i++) {
            this.player.sendChatMessage(" ");
        }
    }
}
