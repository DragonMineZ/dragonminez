package com.dragonminez.client.animation;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.weapon.WeaponRegistry;
import com.dragonminez.common.combat.weapon.WeaponAttributes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class CombatAnimationResolver {
    private static final ResourceLocation BASE_ANIMATION_FILE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/combat.animation.json");

    private static final Set<String> AVAILABLE_RAW = new HashSet<>();

    private CombatAnimationResolver() {}

    public static void reload(ResourceManager resourceManager) {
        AVAILABLE_RAW.clear();
        try {
            var resourceOptional = resourceManager.getResource(BASE_ANIMATION_FILE);
            if (resourceOptional.isEmpty()) return;

            try (var stream = resourceOptional.get().open();
                 var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null) return;
                for (String key : animations.keySet()) AVAILABLE_RAW.add(key);
            }
        } catch (Exception ignored) {}
    }

    private static void ensureLoaded() {
        if (!AVAILABLE_RAW.isEmpty()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft != null) reload(minecraft.getResourceManager());
    }

    public static String sanitizeAnimationId(String animationId) {
        if (animationId == null) return "";

        String id = animationId.trim();
        if (id.startsWith(Reference.MOD_ID + ":")) id = id.substring((Reference.MOD_ID + ":").length());
        if (id.startsWith("animation.base.")) id = id.substring("animation.base.".length());
        if (id.startsWith("combat.")) id = id.substring("combat.".length());

        return id;
    }

    public static String resolveAttack(String animationId, boolean useLeftArm) {
        ensureLoaded();
        String resolved = sanitizeAnimationId(animationId);
        if (useLeftArm && resolved.contains("right")) {
            String mirrored = toPlayableKey(resolved.replace("right", "left"));
            if (!mirrored.isEmpty()) return mirrored;
        }
        return toPlayableKey(resolved);
    }

    public static String resolvePose(String poseId) {
        ensureLoaded();
        String resolved = sanitizeAnimationId(poseId);
        return toPlayableKey(resolved);
    }

    public static String resolvePlayerPose(Player player) {
        WeaponAttributes main = PlayerAttackHelper.isKiWeaponActive(player)
                ? PlayerAttackHelper.getKiWeaponAttributes(player)
                : WeaponRegistry.getAttributes(player.getMainHandItem());
        if (main == null) return "";
        return resolvePose(main.pose());
    }

    private static String toPlayableKey(String normalizedKey) {
        if (normalizedKey == null || normalizedKey.isBlank()) return "";
        if (AVAILABLE_RAW.contains(normalizedKey)) return normalizedKey;
        String prefixed = "combat." + normalizedKey;
        if (AVAILABLE_RAW.contains(prefixed)) return prefixed;
        return "";
    }
}

