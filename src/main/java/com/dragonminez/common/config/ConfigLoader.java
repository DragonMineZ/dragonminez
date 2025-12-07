package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigLoader {

    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*");
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])");

    private final Gson gson;

    public ConfigLoader(Gson gson) {
        this.gson = gson;
    }

    public String cleanJson5(String json5) {
        String json = json5;
        json = MULTI_LINE_COMMENT.matcher(json).replaceAll("");
        json = SINGLE_LINE_COMMENT.matcher(json).replaceAll("");
        json = TRAILING_COMMA.matcher(json).replaceAll("$1");
        return json;
    }

    public <T> T loadConfig(Path path, Class<T> clazz) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        String cleanedJson = cleanJson5(content);
        JsonReader reader = new JsonReader(new StringReader(cleanedJson));
        reader.setLenient(true);
        return gson.fromJson(reader, clazz);
    }

    public void saveConfig(Path path, Object config) throws IOException {
        String json5Content = gson.toJson(config);
        Files.writeString(path, json5Content, StandardCharsets.UTF_8);
    }

    public void saveConfigWithComments(Path path, Object config, String... comments) throws IOException {
        StringBuilder content = new StringBuilder();

        if (comments != null && comments.length > 0) {
            for (String comment : comments) {
                content.append("// ").append(comment).append("\n");
            }
            content.append("\n");
        }

        content.append(gson.toJson(config));

        Files.writeString(path, content.toString(), StandardCharsets.UTF_8);
    }

    public Map<String, FormConfig> loadRaceForms(String raceName, Path formsPath) throws IOException {
        Map<String, FormConfig> forms = new HashMap<>();

        if (!Files.exists(formsPath)) {
            return forms;
        }

        try (var stream = Files.list(formsPath)) {
            stream.filter(path -> path.toString().endsWith(".json5"))
                    .forEach(formFile -> {
                        try {
                            FormConfig formConfig = loadConfig(formFile, FormConfig.class);

                            String groupName = formConfig.getGroupName();
                            if (groupName != null && !groupName.isEmpty()) {
                                forms.put(groupName.toLowerCase(), formConfig);
                                LogUtil.info(Env.COMMON, "Form group '{}' loaded for race '{}'", groupName, raceName);
                            }
                        } catch (IOException e) {
                            LogUtil.error(Env.COMMON, "Error loading form file '{}': {}",
                                    formFile.getFileName(), e.getMessage());
                        }
                    });
        }

        return forms;
    }

    public boolean hasExistingFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return false;
        }

        try (var stream = Files.list(directory)) {
            return stream.anyMatch(path -> path.toString().endsWith(".json5"));
        }
    }
}

