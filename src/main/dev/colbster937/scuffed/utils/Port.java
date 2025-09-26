package dev.colbster937.scuffed.utils;

public class Port {
    public static int parsePort(String p, int d) {
        try {
            int port = Integer.parseInt(p);
            if (port < 0 || port > 65535) return d;
            return port;
        } catch (Exception e) {
            return d;
        }
    }

    public static int parsePort(String s) {
        return parsePort(s, -1);
    }
}
