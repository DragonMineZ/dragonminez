package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

import javax.annotation.Nullable;

public final class ArmorPieceInflation {
	public static final float ARMOR_GAP_PX = 0.1f;

	private ArmorPieceInflation() {
	}

	public static void inflateFittedPart(PoseStack poseStack, GeoBone bone, ModelPart part, ModelPart.Cube referenceCube) {
		if (bone.getCubes().isEmpty()) return;
		Vec3 boneSize = bone.getCubes().get(0).size();
		float kx = inflation(boneSize.x());
		float ky = inflation(boneSize.y());
		float kz = inflation(boneSize.z());
		if (kx == 1f && ky == 1f && kz == 1f) return;

		Vector3f centre = new Vector3f(
				(referenceCube.minX + referenceCube.maxX) * 0.5f,
				(referenceCube.minY + referenceCube.maxY) * 0.5f,
				(referenceCube.minZ + referenceCube.maxZ) * 0.5f);
		if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) {
			centre.rotate(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
		}
		centre.add(part.x, part.y, part.z).div(16f);

		poseStack.translate(centre.x, centre.y, centre.z);
		poseStack.scale(kx, ky, kz);
		poseStack.translate(-centre.x, -centre.y, -centre.z);
	}

	private static float inflation(double sizePx) {
		return sizePx <= 0.0 ? 1f : (float) ((sizePx + 2.0 * ARMOR_GAP_PX) / sizePx);
	}

	@Nullable
	public static Vector3f cubeCentreFromPivot(GeoBone bone) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		boolean any = false;
		for (GeoCube cube : bone.getCubes()) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) continue;
				for (GeoVertex vertex : quad.vertices()) {
					Vector3f p = vertex.position();
					minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
					minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
					minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
					any = true;
				}
			}
		}
		if (!any) return null;
		return new Vector3f(
				(minX + maxX) * 8f - bone.getPivotX(),
				(minY + maxY) * 8f - bone.getPivotY(),
				(minZ + maxZ) * 8f - bone.getPivotZ());
	}
}
