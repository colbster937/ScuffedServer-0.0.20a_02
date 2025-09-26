package dev.colbster937.scuffed.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.colbster937.scuffed.ScuffedUtils;

public class ScuffedHashMap<K, V> extends LinkedHashMap<K, V> {
    public ScuffedHashMap<K, V> toSnake() {
        ScuffedHashMap<K, V> map = new ScuffedHashMap<>();
        for (K key : this.keySet()) {
            if (key instanceof String) {
                map.put((K) ScuffedUtils.camelToSnake(((String) key)), this.get(key));
            } else {
                map.put(key, this.get(key));
            }
        }
        return map;
    }

    public String toJSON() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(this);
    }

    public String toQuery(boolean q) {
        String query = "";
        for (K key : this.keySet()) {
            if (!query.isEmpty() || q) query += "&";
            else query += "?";
            query += key + "=" + URLEncoder.encode(this.get(key).toString(), StandardCharsets.UTF_8);
        }
        return query;
    }
}
