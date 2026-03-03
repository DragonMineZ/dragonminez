package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public final class TransformationMaskRenderState {
	@Nullable
	private static RenderTarget currentMaskTarget;
	@Nullable
	private static RenderTarget currentParamsTarget;

	private TransformationMaskRenderState() {
	}

	public static void setCurrentTargets(@Nullable RenderTarget maskTarget, @Nullable RenderTarget paramsTarget) {
		currentMaskTarget = maskTarget;
		currentParamsTarget = paramsTarget;
	}

	public static void bindMaskTarget() {
		Minecraft mc = Minecraft.getInstance();
		RenderTarget target = currentMaskTarget != null ? currentMaskTarget : mc.getMainRenderTarget();
		target.bindWrite(false);
	}

	public static void bindParamsTarget() {
		Minecraft mc = Minecraft.getInstance();
		RenderTarget target = currentParamsTarget != null ? currentParamsTarget : mc.getMainRenderTarget();
		target.bindWrite(false);
	}

	public static void bindMainTarget() {
		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}
}
