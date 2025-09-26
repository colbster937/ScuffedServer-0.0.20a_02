package dev.colbster937.scuffed.heartbeat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Heartbeat {
    private int MAX_RETRIES = 3;
    private String name;
    private URL url;
    private Proxy proxy;
    private StringBuilder response;
    public String res;

    public Heartbeat(String url, String name, String proxyStr) {
        try {
            this.url = new URL(url);
            this.name = name;

            if (proxyStr != null && !proxyStr.isEmpty() && !proxyStr.isBlank()) {
                String[] parts = proxyStr.split(":");
                if (parts.length == 2) {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    public String pump(String content) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            response = new StringBuilder();
            try {
                HttpURLConnection conn = (HttpURLConnection) (
                    proxy != null ? url.openConnection(proxy) : url.openConnection()
                );
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", Integer.toString(content.getBytes(StandardCharsets.UTF_8).length));
			    conn.setRequestProperty("Content-Language", "en-US");
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(content);
                out.flush();
                out.close();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                        response.append(System.lineSeparator());
                    }
                    Path outFile = Path.of("heartbeats/" + this.name + ".txt");
                    Files.createDirectories(outFile.getParent());
                    Files.write(outFile, response.toString().getBytes(StandardCharsets.UTF_8));
                }
                conn.disconnect();
                return response.toString().trim();
            } catch (Exception ex) {
                continue;
            }
        }
        return null;
    }
}
