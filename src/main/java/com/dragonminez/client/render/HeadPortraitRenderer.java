package com.dragonminez.client.render;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.render.hair.HairRenderContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

public final class HeadPortraitRenderer {
	private static final float YAW_DEGREES = 24.0f;
	private static final float PITCH_DEGREES = 6.0f;
	private static final float HEAD_FILL = 0.62f;
	private static final float HEAD_SIZE_BLOCKS = 0.5f;
	private static final float DEFAULT_HEAD_PIVOT = 24.0f;

	private static boolean active;
	private static boolean failed;

	private HeadPortraitRenderer() {}

	public static boolean isActive() {
		return active;
	}

	public static void render(GuiGraphics graphics, AbstractClientPlayer player, float x, float y, float size, float partialTick) {
		render(graphics, player, x, y, size, partialTick, false);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void render(GuiGraphics graphics, AbstractClientPlayer player, float x, float y, float size, float partialTick, boolean mirrored) {
		if (player == null || size <= 0.0f || active) return;

		Matrix4f matrix = graphics.pose().last().pose();
		Vector3f min = matrix.transformPosition(new Vector3f(x, y, 0.0f));
		Vector3f max = matrix.transformPosition(new Vector3f(x + size, y + size, 0.0f));
		int scissorLeft = (int) Math.floor(Math.min(min.x, max.x));
		int scissorTop = (int) Math.floor(Math.min(min.y, max.y));
		int scissorRight = (int) Math.ceil(Math.max(min.x, max.x));
		int scissorBottom = (int) Math.ceil(Math.max(min.y, max.y));
		if (scissorRight <= scissorLeft || scissorBottom <= scissorTop) return;

		DMZPlayerRenderer renderer = failed ? null : DMZRendererCache.getTPRenderer(player);
		if (renderer == null) {
			PlayerFaceRenderer.draw(graphics, player.getSkinTextureLocation(), Math.round(x), Math.round(y), Math.round(size));
			return;
		}

		float headPivot = DEFAULT_HEAD_PIVOT;
		try {
			GeoBone head = (GeoBone) renderer.getGeoModel().getBone("head").orElse(null);
			if (head != null) headPivot = head.getPivotY();
		} catch (RuntimeException ignored) {
		}

		float scale = size * HEAD_FILL / HEAD_SIZE_BLOCKS;
		float headCenter = (headPivot + 4.5f) / 16.0f;

		graphics.flush();
		graphics.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x + size / 2.0f, y + size / 2.0f, 150.0f);
		pose.mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
		pose.mulPose(new Quaternionf().rotateZ((float) Math.PI));
		pose.mulPose(Axis.XP.rotationDegrees(PITCH_DEGREES));
		pose.mulPose(Axis.YP.rotationDegrees(mirrored ? -YAW_DEGREES : YAW_DEGREES));
		pose.translate(0.0f, -headCenter, 0.0f);

		Lighting.setupForEntityInInventory();
		active = true;
		try (HairRenderContext.Scope ignored = HairRenderContext.menuPreview()) {
			renderer.render(player, 0.0f, partialTick, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
			graphics.flush();
		} catch (RuntimeException e) {
			failed = true;
			LogUtil.error(Env.CLIENT, "Head portrait rendering failed, falling back to skin faces: {}", e.toString());
		} finally {
			active = false;
			pose.popPose();
			graphics.disableScissor();
			Lighting.setupFor3DItems();
		}
	}
}
