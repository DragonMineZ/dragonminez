package com.dragonminez.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de estadísticas para una raza específica
 */
public class RaceStatsConfig {

    @JsonProperty("race_name")
    private String raceName;

    @JsonProperty("description")
    private String description = "";

    @JsonProperty("has_gender")
    private boolean hasGender = true;

    @JsonProperty("classes")
    private Map<String, ClassStatsConfig> classes = new HashMap<>();

    public RaceStatsConfig() {
        classes.put("warrior", new ClassStatsConfig());
        classes.put("spiritualist", new ClassStatsConfig());
        classes.put("martialartist", new ClassStatsConfig());
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean hasGender() {
        return hasGender;
    }

    public void setHasGender(boolean hasGender) {
        this.hasGender = hasGender;
    }

    public Map<String, ClassStatsConfig> getClasses() {
        return classes;
    }

    public ClassStatsConfig getClassConfig(String className) {
        return classes.getOrDefault(className, classes.get("martialartist"));
    }
}

