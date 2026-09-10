package com.dragonminez.client.render.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Shared render helpers for the conventions that changed in 1.21: immediate-mode buffers
 * (Tesselator no longer has getBuilder/end), the stage model-view pose, and the nameplate
 * billboard. Keeping them here means each convention is written down once.
 */
public final class RenderBufferUtil {
	/** Vanilla's nameplate scale, from {@code EntityRenderer#renderNameTag}. */
	public static final float NAMEPLATE_SCALE = 0.025F;

	private RenderBufferUtil() {}

	/** Pack RGBA floats (0-1) into ARGB int for GeckoLib 1.21 render APIs. */
	public static int packColor(float r, float g, float b, float a) {
		int ai = Math.max(0, Math.min(255, (int)(a * 255.0f)));
		int ri = Math.max(0, Math.min(255, (int)(r * 255.0f)));
		int gi = Math.max(0, Math.min(255, (int)(g * 255.0f)));
		int bi = Math.max(0, Math.min(255, (int)(b * 255.0f)));
		return (ai << 24) | (ri << 16) | (gi << 8) | bi;
	}

	public static void drawTexturedQuad(Matrix4f matrix, float x0, float y0, float x1, float y1, float z,
			float minU, float minV, float maxU, float maxV) {
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, x0, y1, z).setUv(minU, maxV);
		buffer.addVertex(matrix, x1, y1, z).setUv(maxU, maxV);
		buffer.addVertex(matrix, x1, y0, z).setUv(maxU, minV);
		buffer.addVertex(matrix, x0, y0, z).setUv(minU, minV);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	public static void drawTexturedQuadCentered(Matrix4f matrix, float halfSize, float minU, float minV, float maxU, float maxV) {
		drawTexturedQuad(matrix, -halfSize, -halfSize, halfSize, halfSize, 0.0F, minU, minV, maxU, maxV);
	}

	/**
	 * Model-view pose for anything drawn from a {@link RenderLevelStageEvent}.
	 *
	 * <p>1.21 hands the stage an identity {@link PoseStack} and keeps the camera rotation on
	 * {@code RenderSystem.getModelViewStack()} instead. Batched draws and {@code BufferUploader} pick
	 * that up on their own, but {@code VertexBuffer#drawWithShader} takes the model-view matrix
	 * explicitly, so anything using it has to supply the view itself or it renders unrotated and slides
	 * off the world as the camera turns. NeoForge exposes the real matrix on the event; rebuilding it
	 * from camera yaw and pitch instead keeps X/Y alignment but produces a different view-space Z,
	 * which lets terrain behind an effect wrongly win the depth test.</p>
	 */
	public static PoseStack stageModelViewPose(RenderLevelStageEvent event) {
		PoseStack poseStack = new PoseStack();
		poseStack.last().pose().set(event.getModelViewMatrix());
		poseStack.last().normal().set(new Matrix3f(event.getModelViewMatrix()));
		return poseStack;
	}

	/**
	 * Positions the pose above {@code entity} and turns it into vanilla's nameplate text space,
	 * for anything drawn above an entity's head. The caller owns the surrounding push/pop and
	 * picks how far above the hitbox to sit, since that offset is per-overlay.
	 *
	 * <p>The scale signs are the part worth centralising. 1.21 turned {@code Camera#rotation} an
	 * extra 180 degrees about Y (see {@code Camera#setRotation}), and vanilla absorbed that by
	 * flipping the X sign in {@code EntityRenderer#renderNameTag}: it now scales
	 * {@code (0.025F, -0.025F, 0.025F)} where 1.20.1 scaled {@code (-0.025F, -0.025F, 0.025F)}.
	 * Carrying the old signs over mirrors the overlay and inverts the winding of everything drawn
	 * in it, so its quads and text come out back-facing and are silently culled - which is exactly
	 * how the Ki Sense combat overlay went missing after the port.</p>
	 *
	 * @param heightAboveEntity how far above the entity's hitbox the overlay sits, in blocks
	 */
	public static void nameplateBillboard(PoseStack poseStack, Entity entity, double heightAboveEntity) {
		poseStack.translate(0.0D, entity.getBbHeight() + heightAboveEntity, 0.0D);
		poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
		poseStack.scale(NAMEPLATE_SCALE, -NAMEPLATE_SCALE, NAMEPLATE_SCALE);
	}
}
