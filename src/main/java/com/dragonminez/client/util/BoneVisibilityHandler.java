package com.dragonminez.client.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
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

		boolean isFemale = gender.equals("female") || gender.equals("mujer") || bodyType == 1;
		boolean isMajin = race.equals("majin");
		boolean isSaiyan = race.equals("saiyan");
		boolean isHuman = race.equals("human");
		boolean isNamekian = race.equals("namekian");
		boolean isSuperOrUltra = Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA);

		boolean isSpectator = player.isSpectator();
		hideBone(model, "body", isSpectator);
		hideBone(model, "right_arm", isSpectator);
		hideBone(model, "left_arm", isSpectator);
		hideBone(model, "right_leg", isSpectator);
		hideBone(model, "left_leg", isSpectator);
		model.getBone("head").ifPresent(head -> head.setHidden(false));

		ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
		boolean hasChestplate = !chestStack.isEmpty();
		boolean isCape = hasChestplate && (chestStack.getItem() instanceof DbzArmorCapeItem);
		ItemStack legsStack = player.getItemBySlot(EquipmentSlot.LEGS);
		boolean hasLeggings = !legsStack.isEmpty();

		boolean isStandardBody = (isSaiyan || isHuman) && (bodyType == 0 || bodyType == 1);
		hideBone(model, "body_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.JACKET)));
		hideBone(model, "right_arm_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE)));
		hideBone(model, "left_arm_layer", hasChestplate || (isStandardBody && !player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)));
		hideBone(model, "right_leg_layer", hasLeggings || (isStandardBody && !player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG)));
		hideBone(model, "left_leg_layer", hasLeggings || (isStandardBody && !player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG)));
		hideBone(model, "hat_layer", (isStandardBody && !player.isModelPartShown(PlayerModelPart.HAT)));

		hideBone(model, "boobas", isCape || !isFemale);

		model.getBone("tail1m").ifPresent(bone -> {
			boolean showAntenna = isMajin && isFemale && isSuperOrUltra;
			setHiddenRecursive(bone, !showAntenna);
		});

		model.getBone("tail1").ifPresent(bone -> {
			boolean showNormalTail;

			if (isSaiyan) {
				showNormalTail = stats.getStatus().isTailVisible() && stats.getCharacter().isHasSaiyanTail();
			} else {
				boolean hasSaiyanTail = stats.getCharacter().isHasSaiyanTail() && ConfigManager.getRaceCharacter(character.getRace()).getHasSaiyanTail();
				showNormalTail = !isHuman && !isNamekian && !isMajin && hasSaiyanTail;
			}

			setHiddenRecursive(bone, !showNormalTail);
		});

		hideBone(model, "armorHead", true);
		hideBone(model, "armorBody", true);
		hideBone(model, "armorBody2", true);
		hideBone(model, "armorLeggingsBody", true);
		hideBone(model, "armorRightArm", true);
		hideBone(model, "armorLeftArm", true);
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