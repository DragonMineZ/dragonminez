package com.dragonminez.client.util;

import com.dragonminez.client.render.layer.DMZCustomArmorLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.armor.DbzArmorCapeItem;
import com.dragonminez.common.init.armor.DbzArmorItem;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.Objects;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class BoneVisibilityHandler {

	public static void updateVisibility(BakedGeoModel model, AbstractClientPlayer player, GeoRenderLayer<?> renderLayer) {
		var stats = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (stats == null) return;

		if (shouldShowBodyOnly(renderLayer, player)) {
			applyBodyOnlyVisibility(model);
			return;
		}

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

        var raceConfig = ConfigManager.getRaceCharacter(race);
        String raceCustomModel = (raceConfig != null && raceConfig.getCustomModel() != null) ? raceConfig.getCustomModel().toLowerCase() : "";
        String formCustomModel = (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel())
                ? character.getActiveFormData().getCustomModel().toLowerCase() : "";

        String tempLogicKey = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
        if (tempLogicKey.isEmpty()) {
            tempLogicKey = race;
        }

        final String logicKey = tempLogicKey;


        boolean isSpectator = player.isSpectator();
		setBonesHidden(model, isSpectator, "body", "right_arm", "left_arm", "right_leg", "left_leg");
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
            boolean isTargetMajinModel = logicKey.equals("majin_kid") || logicKey.equals("majin_ultra") || logicKey.equals("majin");
			boolean showAntenna = (isMajin && isFemale && isSuperOrUltra) || (isTargetMajinModel && isFemale);
			setHiddenRecursive(bone, !showAntenna);
		});

        model.getBone("tail1").ifPresent(bone -> {
            boolean showNormalTail;

            boolean isTaillessRace = isHuman || isNamekian || isMajin;
            boolean isTaillessModel = logicKey.equals("human") ||
                    logicKey.equals("namekian") ||
                    logicKey.equals("namekian_orange") ||
                    logicKey.equals("majin") ||
                    logicKey.equals("majin_kid") ||
                    logicKey.equals("majin_ultra");

            boolean configHasSaiyanTail = ConfigManager.getRaceCharacter(race) != null && ConfigManager.getRaceCharacter(race).getHasSaiyanTail();

            if (isSaiyan || configHasSaiyanTail) {
                showNormalTail = stats.getStatus().isTailVisible() && stats.getCharacter().isHasSaiyanTail();
            } else if (isTaillessRace || isTaillessModel) {
                showNormalTail = false;
            } else {
                showNormalTail = true;
            }

            setHiddenRecursive(bone, !showNormalTail);
        });

		setBonesHidden(model, true, "armorHead", "armorBody", "armorBody2", "armorLeggingsBody", "armorRightArm", "armorLeftArm");
	}

	private static boolean shouldShowBodyOnly(GeoRenderLayer<?> renderLayer, AbstractClientPlayer player) {
		return renderLayer instanceof DMZCustomArmorLayer<?> && hasModerfokinArmor(player);
	}

	private static boolean hasModerfokinArmor(AbstractClientPlayer player) {
		final ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
		if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) return false;
		return armorItem instanceof DbzArmorItem;
	}

	private static void applyBodyOnlyVisibility(BakedGeoModel model) {
		for (GeoBone bone : model.topLevelBones()) {
			setHiddenRecursive(bone, true);
		}

		model.getBone("body").ifPresent(body -> {
			body.setHidden(false);

			GeoBone parent = body.getParent();
			while (parent != null) {
				parent.setHidden(false);
				parent = parent.getParent();
			}

			for (GeoBone child : body.getChildBones()) {
				if(child.getName().equals("armorBody") || child.getName().equals("armorLeggingsBody") || child.getName().equals("body_layer")) {
					child.setHidden(true);
					continue;
				}
				System.out.println(child.getName());
				setHiddenRecursive(child, false);
			}
		});
	}

	private static void setBonesHidden(BakedGeoModel model, boolean shouldHide, String... boneNames) {
		for (String boneName : boneNames) {
			hideBone(model, boneName, shouldHide);
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
