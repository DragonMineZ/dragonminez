package com.dragonminez.mixin.client;

import com.dragonminez.client.collision.OrientedBoundingBox;
import com.dragonminez.client.collision.TargetFinder;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.util.Minecraft_DMZ;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugRenderer.class)
public class ColliderDebugRenderer {
	@Inject(method = "render", at = @At("TAIL"))
	public void dragonminez$renderColliderDebug(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (!client.getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

		LocalPlayer player = client.player;
		if (player == null) return;

		var mcDMZ = (Minecraft_DMZ) client;
		var comboCount = mcDMZ.getComboCount();
		var hand = PlayerAttackHelper.getCurrentAttack(player, comboCount);
		if (hand == null) return;

		var attack = hand.attack();
		var attributes = hand.attributes();
		double attackRange = attributes.attackRange();
		var cursorTarget = mcDMZ.getCursorTarget();

		TargetFinder.TargetResult targetResult = TargetFinder.findAttackTargetResult(player, cursorTarget, attack, attackRange);
		OrientedBoundingBox obb = targetResult.obb;
		List<AABB> targetBoxes = targetResult.entities.stream().map(Entity::getBoundingBox).toList();

		VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
		Matrix4f matrix = poseStack.last().pose();

		float r = 0.0F, g = 1.0F, b = 1.0F, a = 1.0F;

		drawObbLines(consumer, matrix, obb, r, g, b, a, cameraX, cameraY, cameraZ);

		for (AABB box : targetBoxes) {
			Vec3[] vertices = getVertices(box);
			drawBoxLines(consumer, matrix, vertices, 1.0F, 0.0F, 0.0F, 1.0F, cameraX, cameraY, cameraZ);
		}
	}

	private void drawObbLines(VertexConsumer consumer, Matrix4f matrix, OrientedBoundingBox obb, float r, float g, float b, float a, double cX, double cY, double cZ) {
		drawLine(consumer, matrix, obb.vertex1, obb.vertex2, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex2, obb.vertex3, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex3, obb.vertex4, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex4, obb.vertex1, r, g, b, a, cX, cY, cZ);

		drawLine(consumer, matrix, obb.vertex5, obb.vertex6, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex6, obb.vertex7, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex7, obb.vertex8, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex8, obb.vertex5, r, g, b, a, cX, cY, cZ);

		drawLine(consumer, matrix, obb.vertex1, obb.vertex5, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex2, obb.vertex6, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex3, obb.vertex7, r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, obb.vertex4, obb.vertex8, r, g, b, a, cX, cY, cZ);
	}

	private void drawBoxLines(VertexConsumer consumer, Matrix4f matrix, Vec3[] vertices, float r, float g, float b, float a, double cX, double cY, double cZ) {
		drawLine(consumer, matrix, vertices[0], vertices[1], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[1], vertices[2], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[2], vertices[3], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[3], vertices[0], r, g, b, a, cX, cY, cZ);

		drawLine(consumer, matrix, vertices[4], vertices[5], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[5], vertices[6], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[6], vertices[7], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[7], vertices[4], r, g, b, a, cX, cY, cZ);

		drawLine(consumer, matrix, vertices[0], vertices[4], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[1], vertices[5], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[2], vertices[6], r, g, b, a, cX, cY, cZ);
		drawLine(consumer, matrix, vertices[3], vertices[7], r, g, b, a, cX, cY, cZ);
	}

	private Vec3[] getVertices(AABB box) {
		return new Vec3[]{
				new Vec3(box.minX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.minZ),
				new Vec3(box.maxX, box.minY, box.maxZ),
				new Vec3(box.minX, box.minY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.minZ),
				new Vec3(box.maxX, box.maxY, box.maxZ),
				new Vec3(box.minX, box.maxY, box.maxZ)
		};
	}

	private void drawLine(VertexConsumer consumer, Matrix4f matrix, Vec3 start, Vec3 end, float r, float g, float b, float a, double cX, double cY, double cZ) {
		float nx = (float)(end.x - start.x);
		float ny = (float)(end.y - start.y);
		float nz = (float)(end.z - start.z);
		float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		nx /= len; ny /= len; nz /= len;

		consumer.vertex(matrix, (float) (start.x - cX), (float) (start.y - cY), (float) (start.z - cZ)).color(r, g, b, a).normal(nx, ny, nz).endVertex();
		consumer.vertex(matrix, (float) (end.x - cX), (float) (end.y - cY), (float) (end.z - cZ)).color(r, g, b, a).normal(nx, ny, nz).endVertex();
	}
}