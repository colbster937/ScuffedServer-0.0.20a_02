package dev.colbster937.scuffed.server;

import com.mojang.minecraft.net.Packet;
import com.mojang.minecraft.server.PlayerInstance;

import dev.colbster937.scuffed.Messages;
import dev.colbster937.scuffed.ScuffedUtils;

public class ScuffedPlayer {
    public PlayerInstance player;
    private long initTime;
    private long loginRemindTime;
    public long loginTimeout;
	public boolean loggedIn;
	public boolean registered;
    public boolean vanished;

    public ScuffedPlayer(PlayerInstance player) {
        this.player = player;
        this.initTime = System.currentTimeMillis();
        this.loginRemindTime = System.currentTimeMillis();
        this.loginTimeout = ScuffedMinecraftServer.getInstance().loginTimeout;
        this.loggedIn = !ScuffedMinecraftServer.getInstance().authSystem;
        this.registered = false;
        this.vanished = false;
        if (!ScuffedMinecraftServer.getInstance().authSystem) ScuffedMinecraftServer.getInstance().login(this, false);
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

        if (!this.loggedIn && System.currentTimeMillis() - this.initTime > ((long) this.loginTimeout * 1000L)) {
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
