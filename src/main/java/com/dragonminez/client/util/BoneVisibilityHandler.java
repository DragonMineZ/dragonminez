package com.dragonminez.client.util;

import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
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
        int bodyType = character.getBodyType();

        // 1. Chequeo de Armaduras
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChestplate = !chestStack.isEmpty();
        boolean isCape = hasChestplate && (chestStack.getItem() instanceof DbzArmorCapeItem);

        ItemStack legsStack = player.getItemBySlot(EquipmentSlot.LEGS);
        boolean hasLeggings = !legsStack.isEmpty();

        // 2. Chequeo de Configuración de Skin (Overlay)
        // Solo aplica si es Saiyan/Humano y bodyType 0. Si no, asumimos que siempre se muestran (true)
        boolean isSaiyanOrHuman = race.equals("saiyan") || race.equals("human");
        boolean isStandardBody = (isSaiyanOrHuman && bodyType == 0);

        boolean showHat = isStandardBody && player.isModelPartShown(PlayerModelPart.HAT);
        boolean showJacket = isStandardBody && player.isModelPartShown(PlayerModelPart.JACKET);
        boolean showRightSleeve = isStandardBody && player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        boolean showLeftSleeve = isStandardBody && player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE); // Corregido a LEFT
        boolean showRightPants = isStandardBody && player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
        boolean showLeftPants = isStandardBody && player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);

        hideBone(model, "body_layer", hasChestplate || !showJacket);

        hideBone(model, "right_arm_layer", hasChestplate || !showRightSleeve);
        hideBone(model, "left_arm_layer", hasChestplate || !showLeftSleeve);

        hideBone(model, "right_leg_layer", hasLeggings || !showRightPants);
        hideBone(model, "left_leg_layer", hasLeggings || !showLeftPants);

        hideBone(model, "hat_layer", !showHat);

        hideBone(model, "boobas", isCape);

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

        if (isMajin && isFemale) {
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