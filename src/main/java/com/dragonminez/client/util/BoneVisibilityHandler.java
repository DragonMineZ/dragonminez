package com.dragonminez.client.util;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Objects;

public class BoneVisibilityHandler {

    public static void updateVisibility(BakedGeoModel model, AbstractClientPlayer player) {
        var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (stats == null) return;

        var character = stats.getCharacter();
        String race = character.getRaceName().toLowerCase();
        String gender = character.getGender().toLowerCase();
        String currentForm = character.getActiveForm();

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChestplate = !chestStack.isEmpty();

        hideBone(model, "right_arm_layer", hasChestplate);
        hideBone(model, "left_arm_layer", hasChestplate);
        hideBone(model, "body_layer", hasChestplate);

        ItemStack legsStack = player.getItemBySlot(EquipmentSlot.LEGS);
        boolean hasLeggings = !legsStack.isEmpty();

        hideBone(model, "left_leg_layer", hasLeggings);
        hideBone(model, "right_leg_layer", hasLeggings);

        hideBone(model, "armorHead", true);
        hideBone(model, "armorBody", true);
        hideBone(model, "armorBody2", true);
        hideBone(model, "armorLeggingsBody", true);
        hideBone(model, "armorRightArm", true);
        hideBone(model, "armorLeftArm", true);

        boolean isMajin = race.equals("majin");
        boolean isFemale = gender.equals("female") || gender.equals("mujer");
        boolean isSuperOrUltra = Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA);

        model.getBone("tail1m").ifPresent(bone ->
                setHiddenRecursive(bone, !(isMajin && isFemale && isSuperOrUltra))
        );

        if (race.equals("human")) {
            model.getBone("tail1").ifPresent(bone -> setHiddenRecursive(bone, true));
        }
    }

    private static void hideBone(BakedGeoModel model, String boneName, boolean shouldHide) {
        model.getBone(boneName).ifPresent(bone -> bone.setHidden(shouldHide));
    }

    private static void setHiddenRecursive(GeoBone bone, boolean shouldHide) {
        bone.setHidden(shouldHide);
        for (GeoBone child : bone.getChildBones()) {
            setHiddenRecursive(child, shouldHide);
        }
    }
}