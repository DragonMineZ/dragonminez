package com.dragonminez.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public final class VanillaModelSync {
	private VanillaModelSync() {
	}

	public static void sync(BakedGeoModel geoModel, PlayerModel<AbstractClientPlayer> vanillaModel, AbstractClientPlayer player) {
		GeoBone waist = geoModel.getBone("waist").orElse(null);

		syncBone(geoModel, "head", vanillaModel.head, waist);
		syncBone(geoModel, "head", vanillaModel.hat, waist);
		syncBone(geoModel, "body", vanillaModel.body, waist);
		syncBone(geoModel, "right_arm", vanillaModel.rightArm, waist);
		syncBone(geoModel, "left_arm", vanillaModel.leftArm, waist);

		syncBone(geoModel, "right_leg", vanillaModel.rightLeg, null);
		syncBone(geoModel, "left_leg", vanillaModel.leftLeg, null);

		vanillaModel.crouching = player.isCrouching();
		vanillaModel.young = false;
	}

	private static void syncBone(BakedGeoModel geoModel, String boneName, ModelPart part, GeoBone parent) {
		geoModel.getBone(boneName).ifPresent(bone -> {
			float parentRotX = 0, parentRotY = 0, parentRotZ = 0;
			float parentPosX = 0, parentPosY = 0, parentPosZ = 0;
			float parentPivX = 0, parentPivY = 0, parentPivZ = 0;
			if (parent != null) {
				parentRotX = parent.getRotX();
				parentRotY = parent.getRotY();
				parentRotZ = parent.getRotZ();
				parentPosX = parent.getPosX();
				parentPosY = parent.getPosY();
				parentPosZ = parent.getPosZ();
				parentPivX = parent.getPivotX();
				parentPivY = parent.getPivotY();
				parentPivZ = parent.getPivotZ();
			}

			part.xRot = -(parentRotX + bone.getRotX());
			part.yRot = -(parentRotY + bone.getRotY());
			part.zRot = (parentRotZ + bone.getRotZ());

			float relX = (bone.getPivotX() + bone.getPosX()) - parentPivX;
			float relY = (bone.getPivotY() + bone.getPosY()) - parentPivY;
			float relZ = (bone.getPivotZ() + bone.getPosZ()) - parentPivZ;

			float[] rotated = rotateZYX(relX, relY, relZ, parentRotX, parentRotY, parentRotZ);

			float deltaX = (parentPivX + parentPosX + rotated[0]) - bone.getPivotX();
			float deltaY = (parentPivY + parentPosY + rotated[1]) - bone.getPivotY();
			float deltaZ = (parentPivZ + parentPosZ + rotated[2]) - bone.getPivotZ();

			PartPose init = part.getInitialPose();
			part.x = init.x - deltaX;
			part.y = init.y - deltaY;
			part.z = init.z + deltaZ;
		});
	}

	private static float[] rotateZYX(float x, float y, float z, float rotX, float rotY, float rotZ) {
		float cx = Mth.cos(rotX), sx = Mth.sin(rotX);
		float y1 = y * cx - z * sx;
		float z1 = y * sx + z * cx;
		float x1 = x;

		float cy = Mth.cos(rotY), sy = Mth.sin(rotY);
		float x2 = x1 * cy + z1 * sy;
		float z2 = -x1 * sy + z1 * cy;
		float y2 = y1;

		float cz = Mth.cos(rotZ), sz = Mth.sin(rotZ);
		float x3 = x2 * cz - y2 * sz;
		float y3 = x2 * sz + y2 * cz;
		float z3 = z2;
		return new float[]{x3, y3, z3};
	}
}
