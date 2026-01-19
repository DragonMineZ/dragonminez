package com.dragonminez.client.util;

public enum ResourceType {
    GEO(".geo.json"),
    TEXTURES(".png"),
    ANIMATIONS(".animation.json");

    private final String extension;

    ResourceType(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return this.extension;
    }
}
