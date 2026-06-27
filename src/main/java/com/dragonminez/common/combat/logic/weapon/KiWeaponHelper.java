package com.dragonminez.common.combat.logic.weapon;

import com.dragonminez.common.config.ConfigManager;

public class KiWeaponHelper {

    public static float[] resolveColorForType(String type, float[] kiColor) {
        var cfg = type != null ? ConfigManager.getCombatConfig().getKiWeaponConfig(type) : null;
        String forcedColor = cfg != null ? cfg.getForcedColor() : null;
        return resolveColor(forcedColor, kiColor);
    }

    public static float[] resolveColor(String forcedColor, float[] kiColor) {
        if (forcedColor == null) return kiColor;
        String hex = forcedColor.trim();
        if (hex.isEmpty() || hex.equalsIgnoreCase("#FFFFFF") || hex.equalsIgnoreCase("FFFFFF")) return kiColor;
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6) return kiColor;
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new float[]{
                    ((rgb >> 16) & 0xFF) / 255.0f,
                    ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f
            };
        } catch (NumberFormatException e) {
            return kiColor;
        }
    }
}
