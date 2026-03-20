package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public class KiWeaponRenderer {
	private static final ResourceLocation KI_WEAPONS_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/kiweapons.geo.json");
	private static final ResourceLocation KI_WEAPONS_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/kiweapons.png");

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void processWeapons(MultiBufferSource.BufferSource buffers, PoseStack poseStack) {
		var weapons = PlayerEffectQueue.getAndClearWeapons();
		if (weapons == null || weapons.isEmpty()) return;

		for (var entry : weapons) {
			if (entry == null || entry.player() == null) continue;

			Player player = entry.player();
			DMZPlayerRenderer<?> renderer = DMZRendererCache.getTPRenderer(player);

			if (renderer != null) {
				BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(KI_WEAPONS_MODEL);
				if (weaponModel == null) continue;

				boolean isRight = player.getMainArm() == HumanoidArm.RIGHT;
				String boneName = getWeaponBoneName(entry.weaponType(), isRight);

				if (!boneName.isEmpty()) {
					weaponModel.getBone(boneName).ifPresent(targetBone -> {

						syncTargetBoneAndParents(targetBone, entry.playerModel());

						poseStack.pushPose();
						poseStack.last().pose().set(entry.poseMatrix());

						RenderType renderType = ModRenderTypes.energy(KI_WEAPONS_TEXTURE);
						VertexConsumer vertexConsumer = buffers.getBuffer(renderType);

						DMZPlayerRenderer rawRenderer = renderer;

						rawRenderer.renderRecursively(poseStack, player, targetBone, renderType, buffers, vertexConsumer, true,
								entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
								entry.color()[0], entry.color()[1], entry.color()[2], 0.85f);

						poseStack.popPose();
					});
				}
			}
		}
	}

	private static String getWeaponBoneName(String type, boolean isRight) {
		if (type == null) return "";
		return switch (type) {
			case "blade", "BLADE", "Blade" -> isRight ? "blade_right" : "blade_left";
			case "scythe", "SCYTHE", "Scythe" -> isRight ? "scythe_right" : "scythe_left";
			case "clawlance", "CLAWLANCE", "Clawlance" -> isRight ? "trident_right" : "trident_left";
			default -> {
				String lower = type.toLowerCase();
				if (lower.equals("blade")) yield isRight ? "blade_right" : "blade_left";
				if (lower.equals("scythe")) yield isRight ? "scythe_right" : "scythe_left";
				if (lower.equals("clawlance")) yield isRight ? "trident_right" : "trident_left";
				yield "";
			}
		};
	}

	private static void syncTargetBoneAndParents(GeoBone destBone, BakedGeoModel sourceModel) {
		GeoBone currentDest = destBone;
		while (currentDest != null) {
			final GeoBone finalDest = currentDest;
			sourceModel.getBone(finalDest.getName()).ifPresent(sourceBone -> {
				finalDest.setRotX(sourceBone.getRotX());
				finalDest.setRotY(sourceBone.getRotY());
				finalDest.setRotZ(sourceBone.getRotZ());
				finalDest.setPosX(sourceBone.getPosX());
				finalDest.setPosY(sourceBone.getPosY());
				finalDest.setPosZ(sourceBone.getPosZ());
				finalDest.setScaleX(sourceBone.getScaleX());
				finalDest.setScaleY(sourceBone.getScaleY());
				finalDest.setScaleZ(sourceBone.getScaleZ());
			});
			currentDest = currentDest.getParent();
		}
	}
}