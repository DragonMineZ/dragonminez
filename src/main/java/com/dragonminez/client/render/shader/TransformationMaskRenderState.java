package com.dragonminez.client.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

public final class TransformationMaskRenderState {
	@Nullable
	private static RenderTarget currentTarget;

	private TransformationMaskRenderState() {
	}

	public static void setCurrentTarget(@Nullable RenderTarget target) {
		currentTarget = target;
	}

	public static void bindMaskTarget() {
		Minecraft mc = Minecraft.getInstance();
		RenderTarget target = currentTarget != null ? currentTarget : mc.getMainRenderTarget();
		target.bindWrite(false);
	}

	public static void bindMainTarget() {
		Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
	}
}
