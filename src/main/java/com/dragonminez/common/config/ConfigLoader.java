package com.dragonminez.common.config;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigLoader {

    private final Gson gson;

    public ConfigLoader(Gson gson) {
        this.gson = gson;
    }

    public <T> T loadConfig(Path path, Class<T> clazz) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        return gson.fromJson(content, clazz);
    }

    public void saveConfig(Path path, Object config) throws IOException {
        String jsonContent = gson.toJson(config);
        Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
    }

    public Map<String, FormConfig> loadRaceForms(String raceName, Path formsPath) throws IOException {
        Map<String, FormConfig> forms = new HashMap<>();

        if (!Files.exists(formsPath)) {
            return forms;
        }

        try (var stream = Files.list(formsPath)) {
            stream.filter(path -> path.toString().endsWith(".json"))
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

	public void saveDefaultFromTemplate(Path target, String templateName) {
		try (var inputStream = getClass().getResourceAsStream("/assets/dragonminez/config_defaults/" + templateName)) {
			if (inputStream != null) {
				Files.copy(inputStream, target);
				LogUtil.info(Env.COMMON, "Created default config from template: {}", templateName);
			} else {
				LogUtil.error(Env.COMMON, "Template not found: {}", templateName);
			}
		} catch (IOException e) {
			LogUtil.error(Env.COMMON, "Could not copy default config: {}", e.getMessage());
		}
	}

    public boolean hasExistingFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return false;
        }

        try (var stream = Files.list(directory)) {
            return stream.anyMatch(path -> path.toString().endsWith(".json"));
        }
    }
}

