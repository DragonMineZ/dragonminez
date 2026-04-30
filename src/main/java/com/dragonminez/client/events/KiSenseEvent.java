package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudStatNumberAnimator;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {
	private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	private static final Set<Integer> SENSED_ENTITIES = new HashSet<>();
	private static int scanTickCounter = 0;

	private static final Map<Integer, HudStatNumberAnimator> healthAnimators = new HashMap<>();
	private static final Map<Integer, Float> lerpedHealthWidths = new HashMap<>();

	static {
		numberFormat.setMaximumFractionDigits(1);
		numberFormat.setMinimumFractionDigits(0);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.level == null) return;

		scanTickCounter++;
		if (scanTickCounter >= 5) {
			scanTickCounter = 0;
			SENSED_ENTITIES.clear();

			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
				if (!data.getStatus().isHasCreatedCharacter()) return;
				Skill kiSense = data.getSkills().getSkill("kisense");
				if (kiSense == null || !kiSense.isActive()) return;

				int skillLevel = kiSense.getLevel();
				if (skillLevel > 0) {
					double maxDistance = 5 + 3.0 * skillLevel;
					if (data.getStatus().isAndroidUpgraded()) maxDistance += 10.0;

					AABB searchBox = player.getBoundingBox().inflate(maxDistance);
					for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
						if (player.hasLineOfSight(entity) || data.getStatus().isAndroidUpgraded()) {
							if (!entity.isInvisible() || !entity.isInvisibleTo(player)) {
								SENSED_ENTITIES.add(entity.getId());
							}
						}
					}
				}
			});

			lerpedHealthWidths.keySet().retainAll(SENSED_ENTITIES);
			healthAnimators.keySet().retainAll(SENSED_ENTITIES);
		}
	}

	@SubscribeEvent
	public static void onRenderNameTag(RenderNameTagEvent event) {
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		if (!SENSED_ENTITIES.contains(entity.getId())) return;
		renderHealthBar(event.getPoseStack(), entity, event.getPartialTick());
	}

	private static void renderHealthBar(PoseStack poseStack, LivingEntity entity, float partialTick) {
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

		drawTexture(poseStack, x, y, (int) width, 9, 0, 0);

		float targetBarWidth = 76 * healthPercent;
		int entityId = entity.getId();
		float currentBarWidth = lerpedHealthWidths.getOrDefault(entityId, targetBarWidth);

		currentBarWidth += (targetBarWidth - currentBarWidth) * 0.25f * partialTick;
		if (Math.abs(currentBarWidth - targetBarWidth) <= 0.5f) currentBarWidth = targetBarWidth;

		lerpedHealthWidths.put(entityId, currentBarWidth);

		int fillV;
		if (healthPercent < 0.33f) {
			fillV = 33;
		} else if (healthPercent < 0.66f) {
			fillV = 22;
		} else {
			fillV = 11;
		}

		if (currentBarWidth > 0) {
			drawTexture(poseStack, x + 2, y + 3, 7 + (int)currentBarWidth, 5, 2, fillV);
		}

		poseStack.pushPose();
		String textStr = "";
		boolean showPercent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();
		if (showPercent) {
			textStr = String.format("%.0f", health / maxHealth * 100) + "%";
		} else {
			textStr = numberFormat.format(health) + " / " + numberFormat.format(maxHealth);
		}
		MutableComponent text = txt(textStr);

		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textY = 3.0f;

		HudStatNumberAnimator animator = healthAnimators.computeIfAbsent(entity.getId(), id -> new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.KISENSE_HEALTH));
		float value = showPercent ? Math.round((health / maxHealth) * 100.0f) : Math.round(health);
		HudStatNumberAnimator.RenderState state = animator.update(textStr, value, mc.player.tickCount + partialTick);

		if (!state.isHidden()) {
			poseStack.translate(state.offsetX(), state.offsetY(), 0);

			Matrix4f matrix = poseStack.last().pose();
			MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

			float textWidth = mc.font.width(text);
			float textX = -textWidth / 2.0f;

			int color = withAlpha(state.rgbColor(), state.alpha());
			int borderColor = withAlpha(0x000000, state.alpha());
			int light = 15728880;

			poseStack.translate(0.0F, 0.0F, -0.02F);
			mc.font.drawInBatch(text, textX + 1, textY, borderColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, light);
			mc.font.drawInBatch(text, textX - 1, textY, borderColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, light);
			mc.font.drawInBatch(text, textX, textY + 1, borderColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, light);
			mc.font.drawInBatch(text, textX, textY - 1, borderColor, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, light);

			poseStack.translate(0.0F, 0.0F, -0.02F);
			Matrix4f matrixFront = poseStack.last().pose();
			mc.font.drawInBatch(text, textX, textY, color, false, matrixFront, bufferSource, Font.DisplayMode.NORMAL, 0, light);

			bufferSource.endBatch();
		}

		poseStack.popPose();
		poseStack.popPose();
	}

	private static int withAlpha(int rgb, float alpha) {
		int alphaChannel = Math.round(Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
		return (alphaChannel << 24) | (rgb & 0xFFFFFF);
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

	private static MutableComponent txt(String text) {
		return Component.literal(text).withStyle(Style.EMPTY.withFont(DMZ_FONT));
	}
}