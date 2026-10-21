package com.dragonminez.common.init.entities.ai;

import com.dragonminez.common.quest.Difficulty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public final class AiTierResolver {

    private static final Set<String> FINAL_BOSSES = Set.of(
            "saga_kidbuu",
            "saga_super_janemba",
            "saga_super_hirudegarn",
            "saga_broly_lssj",
            "saga_bojack_fp",
            "saga_metal_cooler_core",
            "saga_super_a13",
            "saga_bio_broly_giant",
            "saga_cooler_5ta",
            "saga_slug_giant",
            "saga_omega_shenron",
            "saga_super_17",
            "saga_baby_golden_ozaru",
            "saga_super_baby_vegeta2",
            "saga_hyper_rilldo",
            "saga_luud"
    );

    private AiTierResolver() {}

    public static boolean isFinalBoss(EntityType<?> type) {
        if (type == null) return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key != null && FINAL_BOSSES.contains(key.getPath());
    }

    public static int storyTier(Difficulty difficulty, EntityType<?> type) {
        Difficulty resolved = difficulty != null ? difficulty : Difficulty.NORMAL;
        if (resolved == Difficulty.HARD && isFinalBoss(type)) return AiTier.EXPERT.getId();
        return resolved.aiTierId();
    }
}
