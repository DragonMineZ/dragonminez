package com.dragonminez.client.render.effects;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import com.dragonminez.client.render.util.PlayerEffectQueue;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public class KiWeaponRenderer {
	private static ResourceLocation weaponModel(String type) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/kiweapon_" + type.toLowerCase() + ".geo.json");
		if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
		return null;
	}

	private static ResourceLocation weaponTexture(String type) {
		ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/weapons/kiweapon_" + type.toLowerCase() + ".png");
		if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) return loc;
		return null;
	}

	private static String weaponBone(String type) {
		return "kiweapon_" + type.toLowerCase();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void processWeapons(MultiBufferSource.BufferSource buffers, PoseStack poseStack) {
		var weapons = PlayerEffectQueue.getAndClearWeapons();
		if (weapons == null || weapons.isEmpty()) return;

		for (var entry : weapons) {
			if (entry == null || entry.player() == null) continue;

			String type = entry.weaponType();
			if (type == null || type.isEmpty()) continue;

			Player player = entry.player();
			DMZPlayerRenderer<?> renderer = DMZRendererCache.getTPRenderer(player);

			if (renderer != null) {
				ResourceLocation modelLoc = weaponModel(type);
				if (modelLoc == null) continue;
				BakedGeoModel weaponModel = renderer.getGeoModel().getBakedModel(modelLoc);
				if (weaponModel == null) continue;

				String boneName = weaponBone(type);
				ResourceLocation texture = weaponTexture(type);

				weaponModel.getBone(boneName).ifPresent(targetBone -> {

					syncTargetBoneAndParents(targetBone, entry.playerModel());

					poseStack.pushPose();
					poseStack.last().pose().set(entry.poseMatrix());

					RenderType renderType = ModRenderTypes.energy2(texture);
					VertexConsumer vertexConsumer = buffers.getBuffer(renderType);

					DMZPlayerRenderer rawRenderer = renderer;

					rawRenderer.renderRecursively(poseStack, player, targetBone, renderType, buffers, vertexConsumer, true,
							entry.partialTick(), 15728880, OverlayTexture.NO_OVERLAY,
							entry.color()[0], entry.color()[1], entry.color()[2], 0.65f);

					poseStack.popPose();
				});
			}
		}
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