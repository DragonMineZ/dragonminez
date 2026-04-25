package com.dragonminez.client.systems.impactframes;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.systems.impactframes.ImpactFrame;
import com.dragonminez.common.config.ConfigManager;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ImpactFramesHandler {

	private static final Queue<ImpactFrame> IMPACT_FRAMES = new ArrayDeque<>();
	private static ImpactFrame currentFrame = null;
	private static int currentTick = 0;
	private static PostChain impactFrameShader = null;
	private static int lastWidth = 0;
	private static int lastHeight = 0;

	public static void addImpactFrame(ImpactFrame frame) {
		if (ConfigManager.getUserConfig().isImpactFramesEnabled()) {
			IMPACT_FRAMES.offer(frame);
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.isPaused()) return;

		if (currentFrame == null) {
			if (!IMPACT_FRAMES.isEmpty()) {
				currentFrame = IMPACT_FRAMES.poll();
				currentTick = 0;
			}
		} else {
			currentTick++;
			if (currentTick >= currentFrame.getDuration()) {
				currentFrame = null;
				currentTick = 0;
				if (!IMPACT_FRAMES.isEmpty()) {
					currentFrame = IMPACT_FRAMES.poll();
				}
			}
		}
	}

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
		if (currentFrame == null) return;

		Minecraft mc = Minecraft.getInstance();
		if (impactFrameShader == null) {
			try {
				impactFrameShader = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "shaders/post/impact_frame.json"));
				impactFrameShader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
				lastWidth = mc.getWindow().getWidth();
				lastHeight = mc.getWindow().getHeight();
			} catch (IOException | JsonSyntaxException e) {
				LogUtil.error(Env.CLIENT, "Failed to load impact frame shader", e);
				currentFrame = null;
				return;
			}
		}

		if (mc.getWindow().getWidth() != lastWidth || mc.getWindow().getHeight() != lastHeight) {
			impactFrameShader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
			lastWidth = mc.getWindow().getWidth();
			lastHeight = mc.getWindow().getHeight();
		}

		setUniform(impactFrameShader, "treshhold", currentFrame.getThreshold());
		setUniform(impactFrameShader, "treshholdLerp", currentFrame.getThresholdLerp());
		setUniform(impactFrameShader, "invert", currentFrame.isInverted() ? 1.0f : 0.0f);

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();

		impactFrameShader.process(event.getPartialTick());
		mc.getMainRenderTarget().bindWrite(false);

		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
	}

	private static void setUniform(PostChain chain, String uniformName, float value) {
		try {
			Field passesField = PostChain.class.getDeclaredField("passes");
			passesField.setAccessible(true);
			List<PostPass> passes = (List<PostPass>) passesField.get(chain);

			for (PostPass pass : passes) {
				if (pass.getEffect().getUniform(uniformName) != null) {
					pass.getEffect().safeGetUniform(uniformName).set(value);
				}
			}
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Failed to set uniform {} in ImpactFrame shader", uniformName);
		}
	}
}