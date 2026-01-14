package com.dragonminez.client.util;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class BoneVisibilityHandler {

    public static void updateVisibility(BakedGeoModel model, AbstractClientPlayer player) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;

        boolean isFemale = stats.getCharacter().getGender().equals("female");

        hideBone(model, "armorHead", true);
        hideBone(model, "armorBody", true);
        hideBone(model, "armorBody2", true);
        hideBone(model, "armorLeggingsBody", true);
        hideBone(model, "armorRightArm", true);
        hideBone(model, "armorLeftArm", true);

        hideBone(model, "boobas", !isFemale);
    }

    private static void hideBone(BakedGeoModel model, String boneName, boolean shouldHide) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(shouldHide));
    }
}