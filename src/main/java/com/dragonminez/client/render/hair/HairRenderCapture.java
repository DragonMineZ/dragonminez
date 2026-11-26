package com.dragonminez.client.render.hair;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class HairRenderCapture {
	private static final Matrix4f BASE_POSE = new Matrix4f();
	private static int entityId = Integer.MIN_VALUE;
	private static int sequence;

	private HairRenderCapture() {}

	public static void beginEntity(Entity entity, PoseStack poseStack) {
		BASE_POSE.set(poseStack.last().pose());
		entityId = entity.getId();
		sequence++;
	}

	public static int sequence() {
		return sequence;
	}

	public static boolean isCurrent(Entity entity, int capturedSequence) {
		return entity.getId() == entityId && capturedSequence == sequence;
	}

	public static Matrix4f basePose() {
		return BASE_POSE;
	}
}
