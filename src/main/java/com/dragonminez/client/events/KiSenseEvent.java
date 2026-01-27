package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.text.NumberFormat;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {
	private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png");
	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	static {
		numberFormat.setMaximumFractionDigits(1);
		numberFormat.setMinimumFractionDigits(0);
	}

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
				double maxDistance = 5 + 3.0 * skillLevel;
				if (entity.distanceToSqr(player) <= (maxDistance * maxDistance)) {
					if (player.hasLineOfSight(entity)) {
						if (!entity.isInvisible() || !entity.isInvisibleTo(player)) {
							renderHealthBar(event.getPoseStack(), entity, event.getMultiBufferSource());
						}
					}
				}
			}
		});
	}

	private static void renderHealthBar(PoseStack poseStack, LivingEntity entity, MultiBufferSource bufferSource) {
		poseStack.pushPose();

		poseStack.translate(0.0D, entity.getBbHeight() + 0.8D, 0.0D);

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

		float width = 83;
		float x = -width / 2.0f;
		float y = 0;

		drawTexture(poseStack, x, y, (int)width, 9, 0, 0);

		int currentBarWidth = (int) (76 * healthPercent);

		int fillV;
		if (healthPercent < 0.33f) {
			fillV = 33;
		} else if (healthPercent < 0.66f) {
			fillV = 22;
		} else {
			fillV = 11;
		}

		if (currentBarWidth > 0) {
			drawTexture(poseStack, x + 2, y + 3, 7+ currentBarWidth, 5, 2, fillV);
		}

		poseStack.pushPose();
		String text = "";
		if (ConfigManager.getUserConfig().getHud().isAdvancedDescriptionPercentage()) {
			text = String.format("%.0f", health/maxHealth * 100) + "%";
		} else {
			text = numberFormat.format(health) + " / " + numberFormat.format(maxHealth);
		}
		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textY = 3.0f;
		drawBorderedText(poseStack, bufferSource, mc.font, text, 0, textY);

		poseStack.popPose();
		poseStack.popPose();
	}

	private static void drawTexture(PoseStack poseStack, float x, float y, int width, int height, int u, int v) {
		Matrix4f matrix = poseStack.last().pose();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();

		float textureSize = 128.0f;

		float minU = (float) u / textureSize;
		float maxU = (float) (u + width) / textureSize;
		float minV = (float) v / textureSize;
		float maxV = (float) (v + height) / textureSize;

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

		buffer.vertex(matrix, x, y + height, 0).uv(minU, maxV).endVertex();
		buffer.vertex(matrix, x + width, y + height, 0).uv(maxU, maxV).endVertex();
		buffer.vertex(matrix, x + width, y, 0).uv(maxU, minV).endVertex();
		buffer.vertex(matrix, x, y, 0).uv(minU, minV).endVertex();

		tesselator.end();
	}

	private static void drawBorderedText(PoseStack poseStack, MultiBufferSource buffer, Font font, String text, float x, float y) {
		float textWidth = font.width(text);
		float centeredX = x - (textWidth / 2.0f);

		int black = 0x000000;
		int white = 0xFFFFFF;
		int packedLight = 0xF000F0;

		poseStack.pushPose();
		poseStack.translate(0.0f, 0.0f, 0.025f);
		drawTextRaw(poseStack, buffer, font, text, centeredX - 1, y, black, packedLight);
		drawTextRaw(poseStack, buffer, font, text, centeredX + 1, y, black, packedLight);
		drawTextRaw(poseStack, buffer, font, text, centeredX, y - 1, black, packedLight);
		drawTextRaw(poseStack, buffer, font, text, centeredX, y + 1, black, packedLight);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(0.0f, 0.0f, -0.15f);
		drawTextRaw(poseStack, buffer, font, text, centeredX, y, white, packedLight);
		poseStack.popPose();
	}

	private static void drawTextRaw(PoseStack poseStack, MultiBufferSource buffer, Font font, String text, float x, float y, int color, int packedLight) {
		font.drawInBatch(text, x, y, color, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
	}
}