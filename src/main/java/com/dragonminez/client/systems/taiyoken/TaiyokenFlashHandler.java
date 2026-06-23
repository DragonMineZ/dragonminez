package com.dragonminez.client.systems.taiyoken;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.client.render.util.IrisCompat;
import com.dragonminez.mixin.client.PostChainAccessor;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TaiyokenFlashHandler {

	private static final ResourceLocation EFFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/taiyoken_flash.json");

	private static PostChain shader = null;
	private static int lastWidth = 0;
	private static int lastHeight = 0;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			TaiyokenBlindState.clear();
			return;
		}
		if (mc.isPaused()) return;
		TaiyokenBlindState.tick();
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		if (!TaiyokenBlindState.isActive()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;

		if (shader == null) {
			try {
				shader = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), EFFECT);
				shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
				lastWidth = mc.getWindow().getWidth();
				lastHeight = mc.getWindow().getHeight();
			} catch (IOException | JsonSyntaxException e) {
				LogUtil.error(Env.CLIENT, "Failed to load taiyoken flash shader", e);
				TaiyokenBlindState.clear();
				return;
			}
		}

		if (mc.getWindow().getWidth() != lastWidth || mc.getWindow().getHeight() != lastHeight) {
			shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
			lastWidth = mc.getWindow().getWidth();
			lastHeight = mc.getWindow().getHeight();
		}

		float progress = TaiyokenBlindState.getProgress();
		float intensity = progress < 0.25f ? progress * 4.0f : 1.0f;
		boolean invert = Boolean.TRUE.equals(ConfigManager.getUserConfig().getTaiyokenInvertPalette());

		setUniform(shader, "intensity", intensity);
		setUniform(shader, "invert", invert ? 1.0f : 0.0f);

		boolean iris = IrisCompat.isShaderPackInUse(mc.level.getGameTime());

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();

		if (iris) mc.getMainRenderTarget().bindWrite(false);
		shader.process(event.getPartialTick());
		mc.getMainRenderTarget().bindWrite(false);

		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
	}

	private static void setUniform(PostChain chain, String uniformName, float value) {
		List<PostPass> passes = ((PostChainAccessor) chain).dragonminez$getPasses();
		for (PostPass pass : passes) {
			EffectInstance effect = pass.getEffect();
			Uniform uniform = effect.getUniform(uniformName);
			if (uniform != null) uniform.set(value);
		}
	}
}
