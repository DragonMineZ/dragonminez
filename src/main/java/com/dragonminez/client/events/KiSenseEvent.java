package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {

	private static final ResourceLocation HUD_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/gui/hud/xenoversehud.png");

	@SubscribeEvent
	public static void onRenderNameTag(RenderNameTagEvent event) {
		if (!(event.getEntity() instanceof LivingEntity entity)) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || entity == player) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().hasCreatedCharacter()) return;
			Skill kiSense = data.getSkills().getSkill("kisense");
			if (kiSense == null) return;
			if (!kiSense.isActive()) return;

			int skillLevel = kiSense.getLevel();

			if (skillLevel > 0) {
				double maxDistance = 3.0 * skillLevel;
				if (entity.distanceToSqr(player) <= (maxDistance * maxDistance)) {

					renderHealthBar(event.getPoseStack(), entity, event.getMultiBufferSource());
				}
			}
		});
	}

	private static void renderHealthBar(PoseStack poseStack, LivingEntity entity, MultiBufferSource bufferSource) {
		poseStack.pushPose();

		poseStack.translate(0.0D, entity.getBbHeight() + 0.5D, 0.0D);

		Minecraft mc = Minecraft.getInstance();
		poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

		float scale = 0.025F;
		poseStack.scale(-scale, -scale, scale);

		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		float healthPercent = Math.max(0, Math.min(1, health / maxHealth));

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, HUD_TEXTURE);

		float x = -141 / 2.0f;
		float y = 0;

		drawTexture(poseStack, x, y, 141, 9, 14, 2);

		int currentBarWidth = (int) (137 * healthPercent);
		int fillV;
		if (healthPercent < 0.33f) {
			fillV = 48;
		} else if (healthPercent < 0.66f) {
			fillV = 35;
		} else {
			fillV = 21;
		}

		if (currentBarWidth > 0) {
			drawTexture(poseStack, x + 1, y + 2, currentBarWidth, 5, 15, fillV);
		}

		poseStack.popPose();
	}

	private static void drawTexture(PoseStack poseStack, float x, float y, int width, int height, int u, int v) {
		Matrix4f matrix = poseStack.last().pose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		float minU = (float) u / 256;
		float maxU = (float) (u + width) / 256;
		float minV = (float) v / 256;
		float maxV = (float) (v + height) / 256;

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

		buffer.vertex(matrix, x, y + height, 0).uv(minU, maxV).endVertex();
		buffer.vertex(matrix, x + width, y + height, 0).uv(maxU, maxV).endVertex();
		buffer.vertex(matrix, x + width, y, 0).uv(maxU, minV).endVertex();
		buffer.vertex(matrix, x, y, 0).uv(minU, minV).endVertex();

		tesselator.end();
	}
}