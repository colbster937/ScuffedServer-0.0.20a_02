package dev.colbster937.scuffed.heartbeat;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;

import com.iwebpp.crypto.TweetNaclFast.Hash;

import dev.colbster937.scuffed.ScuffedServer;
import dev.colbster937.scuffed.ScuffedUtils;
import dev.colbster937.scuffed.utils.ScuffedHashMap;
import dev.colbster937.scuffed.ScuffedConstants;

public class HeartbeatData {
    private ScuffedServer server;
    private String salt;
    private HeartbeatInfo info;

    private static HeartbeatData instance;

    public class HeartbeatInfo {
        public String name;
        public String salt;
        public String software;
        public String version;
        public String host;
        public int port;
        public int maxPlayers;
        public int players;
        public boolean isPublic;
        public int protocol;
        public boolean web;
    }

    public HeartbeatData(ScuffedServer server) {
        this.server = server;
        this.info = new HeartbeatInfo();
        this.salt = this.generateSalt();
        this.updateInfo();
        instance = this;
    }
    
    public boolean updateInfo() {
        this.info.name = this.server.server.serverName;
        this.info.salt = this.salt;
        this.info.software = ScuffedConstants.SERVER_STRING;
        this.info.version = ScuffedConstants.SERVER_VERSION;
        this.info.host = this.server.publicAddr;
        this.info.port = server.getFrontendPort();
        this.info.maxPlayers = this.server.maxPlayers;
        this.info.players = this.server.getPlayerList().size();
        this.info.isPublic = (boolean) ScuffedUtils.getField(this.server.server, "isPublic");
        this.info.protocol = ScuffedConstants.SERVER_PROTOCOL;
        this.info.web = true;

        return true;
    }
    
    public ScuffedHashMap<String, Object> getInfo() {
        this.updateInfo();
        ScuffedHashMap<String, Object> info = new ScuffedHashMap<>();
        info.put("name", this.info.name);
        info.put("software", this.info.software);
        info.put("host", this.info.host);
        info.put("port", this.info.port);
        info.put("maxPlayers", this.info.maxPlayers);
        info.put("players", this.info.players);
        info.put("public", this.info.isPublic);
        info.put("version", this.info.version);
        info.put("web", this.info.web);
        return info;
    }

    public ScuffedHashMap<String, Object> toV2(ScuffedHashMap<String, Object> info) {
        info.put("users", info.get("players"));
        info.remove("players");
        info.put("salt", this.info.salt);
        info.remove("host");
        info.put("version", ScuffedConstants.SERVER_PROTOCOL);
        info.remove("protocol");
        return info;
    }

    public boolean saveFile(String name) {
        try {
            Files.write(new File(name).toPath(), this.getInfo().toSnake().toJSON().getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static HeartbeatData getInstance() {
        return instance;
    }

    private String generateSalt() {
        SecureRandom rng = new SecureRandom();
        char[] str = new char[32];
        byte[] one = new byte[1];

        for (int i = 0; i < str.length; i++) {
            rng.nextBytes(one);
            char c = (char) (one[0] & 0xFF);
            if (!this.acceptableSaltChar(c)) continue;
            str[i] = c;
        }
        return new String(str);
    }

    private boolean acceptableSaltChar(char c) {
        return (c >= 'a' && c <= 'z') 
            || (c >= 'A' && c <= 'Z') 
            || (c >= '0' && c <= '9');
    }
}
