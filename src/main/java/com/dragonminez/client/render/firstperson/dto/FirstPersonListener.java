package com.dragonminez.client.render.firstperson.dto;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.DMZRendererCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FirstPersonListener {

	@SubscribeEvent
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void onWorldRender(RenderLevelStageEvent event) {
		final LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

		final boolean isFirstPerson = FirstPersonManager.shouldRenderFirstPerson(player);
		if (!isFirstPerson) return;

		final DMZPlayerRenderer renderer = DMZRendererCache.getRenderer(player);
		if (renderer == null) return;
		if (!renderer.shouldRender(player, event.getFrustum(), player.getX(), player.getY(), player.getZ())) return;

		final MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		renderer.render(player, player.getYRot(), event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getPoseStack(), source,
				renderer.getPackedLightCoords(player, event.getPartialTick().getGameTimeDeltaPartialTick(false)));
		source.endBatch();
	}
}
