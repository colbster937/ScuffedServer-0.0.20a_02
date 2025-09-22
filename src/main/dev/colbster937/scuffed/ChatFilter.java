package dev.colbster937.scuffed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ChatFilter {
    private static final List<Pattern> filter = new ArrayList<>();

    public static void reloadFilter() {
        filter.clear();
        File file = new File("chat-filter.txt");

        if (!file.exists()) {
            try {
                file.createNewFile();   
            } catch (IOException e) {}
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    filter.add(Pattern.compile(line));
                } catch (Exception ignored) {}
            }
            br.close();
        } catch (IOException e) {}
    }

    public static String filterMessage(String message) {
        String filtered = message;
        for (Pattern p : filter) {
            filtered = p.matcher(filtered).replaceAll("****");
        }
        return filtered;
    }
}
