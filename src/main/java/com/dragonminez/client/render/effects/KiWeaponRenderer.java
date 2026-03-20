package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

public class KiWeaponRenderer {
	private static final ResourceLocation KI_WEAPONS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/kiweapons.geo.json");
	private static final ResourceLocation KI_WEAPONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

	public static void processWeapons(MultiBufferSource.BufferSource buffers, PoseStack poseStack) {
		var weapons = PlayerEffectQueue.getAndClearWeapons();
		if (weapons == null || weapons.isEmpty()) return;

		for (var entry : weapons) {
			if (entry == null || entry.player() == null) continue;

			Player player = entry.player();
			DMZPlayerRenderer renderer = DMZRendererCache.getTPRenderer(player);

			if (renderer != null) {
				BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(KI_WEAPONS_MODEL);
				if (weaponModel == null) continue;

				resetModelParts(weaponModel);
				boolean isRight = player.getMainArm() == HumanoidArm.RIGHT;
				String boneName = getWeaponBoneName(entry.weaponType(), isRight);

				if (!boneName.isEmpty()) {
					weaponModel.getBone(boneName).ifPresent(KiWeaponRenderer::showBoneChain);
					syncModelToPlayer(weaponModel, entry.playerModel());

					poseStack.pushPose();
					poseStack.last().pose().set(entry.poseMatrix());

					renderer.reRender(weaponModel, poseStack, buffers, (GeoAnimatable) player,
							ModRenderTypes.energy(KI_WEAPONS_TEXTURE),
							buffers.getBuffer(ModRenderTypes.energy(KI_WEAPONS_TEXTURE)),
							entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
							entry.color()[0], entry.color()[1], entry.color()[2], 0.85f);

					poseStack.popPose();
				}
			}
		}
	}

	private static String getWeaponBoneName(String type, boolean isRight) {
		return switch (type.toLowerCase()) {
			case "blade" -> isRight ? "blade_right" : "blade_left";
			case "scythe" -> isRight ? "scythe_right" : "scythe_left";
			case "clawlance" -> isRight ? "trident_right" : "trident_left";
			default -> "";
		};
	}

	private static void syncModelToPlayer(BakedGeoModel auraModel, BakedGeoModel playerModel) {
		for (GeoBone auraBone : auraModel.topLevelBones()) {
			syncBoneRecursively(auraBone, playerModel);
		}
	}

	private static void showBoneChain(GeoBone bone) {
		setHiddenRecursive(bone, false);
		GeoBone parent = bone.getParent();
		while (parent != null) {
			parent.setHidden(false);
			parent = parent.getParent();
		}
	}

	private static void resetModelParts(BakedGeoModel model) {
		for (GeoBone bone : model.topLevelBones()) {
			setHiddenRecursive(bone, true);
		}
	}

	private static void setHiddenRecursive(GeoBone bone, boolean hidden) {
		bone.setHidden(hidden);
		for (GeoBone child : bone.getChildBones()) {
			setHiddenRecursive(child, hidden);
		}
	}

	private static void syncBoneRecursively(GeoBone destBone, BakedGeoModel sourceModel) {
		sourceModel.getBone(destBone.getName()).ifPresent(sourceBone -> {
			destBone.setRotX(sourceBone.getRotX());
			destBone.setRotY(sourceBone.getRotY());
			destBone.setRotZ(sourceBone.getRotZ());
			destBone.setPosX(sourceBone.getPosX());
			destBone.setPosY(sourceBone.getPosY());
			destBone.setPosZ(sourceBone.getPosZ());
		});
		for (GeoBone child : destBone.getChildBones()) syncBoneRecursively(child, sourceModel);
	}
}