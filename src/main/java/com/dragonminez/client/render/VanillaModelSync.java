package com.dragonminez.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class VanillaModelSync {
	private VanillaModelSync() {
	}

	public static void sync(BakedGeoModel geoModel, PlayerModel<AbstractClientPlayer> vanillaModel, AbstractClientPlayer player) {
		float waistRotX = 0, waistRotY = 0, waistRotZ = 0;
		var waistOpt = geoModel.getBone("waist");
		if (waistOpt.isPresent()) {
			GeoBone waist = waistOpt.get();
			waistRotX = waist.getRotX();
			waistRotY = waist.getRotY();
			waistRotZ = waist.getRotZ();
		}

		syncBoneWithParent(geoModel, "head", vanillaModel.head, waistRotX, waistRotY, waistRotZ);
		syncBoneWithParent(geoModel, "head", vanillaModel.hat, waistRotX, waistRotY, waistRotZ);

		syncBoneWithParent(geoModel, "body", vanillaModel.body, waistRotX, waistRotY, waistRotZ);
		syncBoneWithParent(geoModel, "right_arm", vanillaModel.rightArm, waistRotX, waistRotY, waistRotZ);
		syncBoneWithParent(geoModel, "left_arm", vanillaModel.leftArm, waistRotX, waistRotY, waistRotZ);
		syncBoneWithParent(geoModel, "right_leg", vanillaModel.rightLeg, waistRotX, waistRotY, waistRotZ);
		syncBoneWithParent(geoModel, "left_leg", vanillaModel.leftLeg, waistRotX, waistRotY, waistRotZ);

		vanillaModel.crouching = player.isCrouching();
		vanillaModel.young = false;
	}

	private static void syncBoneWithParent(BakedGeoModel geoModel, String boneName, ModelPart part, float parentRotX, float parentRotY, float parentRotZ) {
		geoModel.getBone(boneName).ifPresent(bone -> {
			part.xRot = parentRotX + bone.getRotX();
			part.yRot = parentRotY + bone.getRotY();
			part.zRot = parentRotZ + bone.getRotZ();
		});
	}
}
