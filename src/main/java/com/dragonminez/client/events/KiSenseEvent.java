package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.HudStatNumberAnimator;
import com.dragonminez.client.systems.kisense.CombatIndicators;
import com.dragonminez.client.systems.kisense.KiSenseScan;
import com.dragonminez.client.systems.kisense.KiSenseState;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class KiSenseEvent {
	private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/hud/alternativehud.png");
	private static final ResourceLocation DMZ_FONT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "smooth");
	static NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	private static final double LOD_DISTANCE = 24.0;
	private static final float BAR_MAX_WIDTH = 76.0f;
	private static final float BAR_STACK = 10.0f;
	private static final int DAMAGE_COLOR = 0xFF5555;
	private static final int HEAL_COLOR = 0x55FF55;

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

		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (!data.getStatus().isHasCreatedCharacter()) {
				KiSenseScan.clear();
				CombatIndicators.clear();
				return;
			}
			if (KiSenseState.isActive() && (!data.getSkills().isSkillActive("kisense") || data.getSkills().getSkillLevel("kisense") <= 0)) {
				KiSenseState.reset();
			}
			KiSenseScan.tick(player, data, KiSenseState.getMode());
			if (KiSenseState.isCombat()) CombatIndicators.tick();
			else CombatIndicators.clear();
		});

		lerpedHealthWidths.keySet().retainAll(KiSenseScan.getCombatEntities());
		healthAnimators.keySet().retainAll(KiSenseScan.getCombatEntities());
	}

	@SubscribeEvent
	public static void onRenderNameTag(RenderNameTagEvent event) {
		if (!KiSenseState.isCombat()) return;
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		if (!KiSenseScan.getCombatEntities().contains(entity.getId())) return;
		renderCombatOverlay(event.getPoseStack(), entity, event.getPartialTick());
	}

	private static void renderCombatOverlay(PoseStack poseStack, LivingEntity entity, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		boolean lod = mc.player != null && mc.player.distanceTo(entity) > LOD_DISTANCE;

		poseStack.pushPose();
		poseStack.translate(0.0D, entity.getBbHeight() + 0.8D, 0.0D);
		poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

		float scale = 0.025F;
		poseStack.scale(-scale, -scale, scale);

		RenderSystem.disableDepthTest();

		float topY;
		if (entity instanceof Player target) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null);
			if (data != null) {
				drawStaminaBar(poseStack, data, 0f);
				drawKiBar(poseStack, data, -BAR_STACK);
			}
			renderHealthBar(poseStack, entity, partialTick, lod, -2f * BAR_STACK);
			topY = -2f * BAR_STACK;
		} else {
			renderHealthBar(poseStack, entity, partialTick, lod, 0f);
			topY = 0f;
		}

		renderBPLabel(poseStack, entity, topY);
		renderIndicators(poseStack, entity, partialTick, lod, topY);

		RenderSystem.enableDepthTest();

		poseStack.popPose();
	}

	private static void drawKiBar(PoseStack poseStack, StatsData data, float baseY) {
		float maxKi = Math.max(1, data.getMaxEnergy());
		float pct = Mth.clamp(data.getResources().getCurrentEnergy() / maxKi, 0f, 1f);
		int fillW = 7 + (int) (BAR_MAX_WIDTH * pct);
		float x = -41.5f;

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, HUD_TEXTURE);
		drawTexture(poseStack, x, 0, 83, 9, 0, 44);

		float[] auraRgb = auraColorRgb(data);
		RenderSystem.setShaderColor(auraRgb[0], auraRgb[1], auraRgb[2], 1.0F);
		drawTexture(poseStack, x + 3, 3, fillW, 4, 3, 61);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		poseStack.popPose();
	}

	private static void drawStaminaBar(PoseStack poseStack, StatsData data, float baseY) {
		float maxStm = Math.max(1, data.getMaxStamina());
		float pct = Mth.clamp(data.getResources().getCurrentStamina() / maxStm, 0f, 1f);
		int fillW = (int) (BAR_MAX_WIDTH * pct) - 5;
		float x = -41.5f;

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, HUD_TEXTURE);
		drawTexture(poseStack, x, 0, 83, 9, 0, 72);
		if (fillW > 0) drawTexture(poseStack, x + 2, 3, fillW, 4, 2, 90);
		drawTexture(poseStack, x + 77, 3, 4, 4, 77, 90);

		poseStack.popPose();
	}

	private static float[] auraColorRgb(StatsData data) {
		com.dragonminez.common.stats.character.Character character = data.getCharacter();
		String auraColor = character.getAuraColor();
		com.dragonminez.common.config.FormConfig.FormData formData =
				character.getActiveStackForm() != null && !character.getActiveStackForm().isEmpty() ? character.getActiveStackFormData() :
						character.getActiveForm() != null && !character.getActiveForm().isEmpty() ? character.getActiveFormData() : null;
		if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) auraColor = formData.getAuraColor();
		return com.dragonminez.client.util.ColorUtils.hexToRgb(auraColor != null ? auraColor : "#FFFFFF");
	}

	private static void renderHealthBar(PoseStack poseStack, LivingEntity entity, float partialTick, boolean lod, float baseY) {
		Minecraft mc = Minecraft.getInstance();

		float health = entity.getHealth();
		float maxHealth = entity.getMaxHealth();
		float healthPercent = Math.max(0, Math.min(1, health / maxHealth));

		poseStack.pushPose();
		poseStack.translate(0f, baseY, 0f);

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, HUD_TEXTURE);

		float width = 83;
		float x = -width / 2.0f;
		float y = 0;

		drawTexture(poseStack, x, y, (int) width, 9, 0, 0);

		float targetBarWidth = 76 * healthPercent;
		int entityId = entity.getId();
		float currentBarWidth;
		if (lod) {
			currentBarWidth = targetBarWidth;
		} else {
			currentBarWidth = lerpedHealthWidths.getOrDefault(entityId, targetBarWidth);
			currentBarWidth += (targetBarWidth - currentBarWidth) * 0.25f * partialTick;
			if (Math.abs(currentBarWidth - targetBarWidth) <= 0.5f) currentBarWidth = targetBarWidth;
		}
		lerpedHealthWidths.put(entityId, currentBarWidth);

		int fillV;
		if (healthPercent < 0.33f) fillV = 33;
		else if (healthPercent < 0.66f) fillV = 22;
		else fillV = 11;

		if (currentBarWidth > 0) {
			drawTexture(poseStack, x + 2, y + 3, 7 + (int) currentBarWidth, 5, 2, fillV);
		}

		boolean showPercent = ConfigManager.getUserConfig().getAdvancedDescriptionPercentage();
		String textStr = showPercent
				? String.format("%.0f", health / maxHealth * 100) + "%"
				: numberFormat.format(health) + " / " + numberFormat.format(maxHealth);
		MutableComponent text = txt(textStr);

		poseStack.pushPose();
		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textY = 3.0f;

		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		float textWidth = mc.font.width(text);
		float textX = -textWidth / 2.0f;

		if (lod) {
			drawText(poseStack, text, textX, textY, HudStatNumberAnimator.WHITE_RGB, 1.0f, bufferSource);
			bufferSource.endBatch();
		} else {
			HudStatNumberAnimator animator = healthAnimators.computeIfAbsent(entityId, id -> new HudStatNumberAnimator(HudStatNumberAnimator.StatKind.KISENSE_HEALTH));
			float value = showPercent ? Math.round((health / maxHealth) * 100.0f) : Math.round(health);
			HudStatNumberAnimator.RenderState state = animator.update(textStr, value, mc.player.tickCount + partialTick);
			if (!state.isHidden()) {
				poseStack.translate(state.offsetX(), state.offsetY(), 0);
				drawText(poseStack, text, textX, textY, state.rgbColor(), state.alpha(), bufferSource);
				bufferSource.endBatch();
			}
		}
		poseStack.popPose();
		poseStack.popPose();
	}

	private static void renderIndicators(PoseStack poseStack, LivingEntity entity, float partialTick, boolean lod, float topY) {
		Minecraft mc = Minecraft.getInstance();
		int id = entity.getId();
		float time = mc.level.getGameTime() + partialTick;
		float maxHealth = entity.getMaxHealth();

		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

		poseStack.pushPose();
		poseStack.translate(0f, topY, 0f);
		float textScale = 1.05f;
		poseStack.scale(textScale, textScale, textScale);

		if (ConfigManager.getUserConfig().getShowAccumulativeDamage()) {
			float damage = CombatIndicators.getAccumDamage(id);
			float heal = CombatIndicators.getAccumHeal(id);
			if (damage > 0) {
				float alpha = lod ? 1.0f : accumAlpha(time - CombatIndicators.getDamageTick(id));
				drawIndicator(poseStack, formatDelta(damage, false, maxHealth), CombatIndicators.getDamageOffX(id), CombatIndicators.getDamageOffY(id), DAMAGE_COLOR, alpha, bufferSource);
			}
			if (heal > 0) {
				float alpha = lod ? 1.0f : accumAlpha(time - CombatIndicators.getHealTick(id));
				drawIndicator(poseStack, formatDelta(heal, true, maxHealth), CombatIndicators.getHealOffX(id), CombatIndicators.getHealOffY(id), HEAL_COLOR, alpha, bufferSource);
			}
		} else {
			for (CombatIndicators.DamagePopup popup : CombatIndicators.getPopups(id)) {
				float age = time - popup.bornTick();
				float life = Mth.clamp(age / CombatIndicators.POPUP_LIFETIME, 0f, 1f);
				float alpha = lod ? 1.0f : (1.0f - life);
				float rise = lod ? 0f : life * 10f;
				int color = popup.heal() ? HEAL_COLOR : DAMAGE_COLOR;
				drawIndicator(poseStack, formatDelta(popup.value(), popup.heal(), maxHealth), popup.offX(), popup.offY() - rise, color, alpha, bufferSource);
			}
		}

		bufferSource.endBatch();
		poseStack.popPose();
	}

	private static String formatDelta(float amount, boolean heal, float maxHealth) {
		String sign = heal ? "+" : "-";
		if (ConfigManager.getUserConfig().getAdvancedDescriptionPercentage() && maxHealth > 0) {
			return sign + String.format("%.0f", amount / maxHealth * 100f) + "%";
		}
		return sign + Math.round(amount);
	}

	private static void renderBPLabel(PoseStack poseStack, LivingEntity entity, float topY) {
		Minecraft mc = Minecraft.getInstance();
		int bp = KiSenseScan.getCachedBP(entity.getId());
		String bpStr = bp >= Integer.MAX_VALUE ? "BP: ???" : "BP: " + String.format("%,d", bp).replace(",", ".");
		MutableComponent text = txt(bpStr);

		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		poseStack.pushPose();
		poseStack.translate(0f, topY, 0f);
		float textScale = 0.6f;
		poseStack.scale(textScale, textScale, textScale);
		float textWidth = mc.font.width(text);
		drawText(poseStack, text, -textWidth / 2.0f, -8.0f, HudStatNumberAnimator.WHITE_RGB, 1.0f, bufferSource);
		bufferSource.endBatch();
		poseStack.popPose();
	}

	private static float accumAlpha(float ticksSinceChange) {
		float fadeStart = 25f;
		float fadeEnd = 40f;
		if (ticksSinceChange <= fadeStart) return 1.0f;
		if (ticksSinceChange >= fadeEnd) return 0.0f;
		return 1.0f - (ticksSinceChange - fadeStart) / (fadeEnd - fadeStart);
	}

	private static void drawIndicator(PoseStack poseStack, String str, float centerX, float y, int rgb, float alpha, MultiBufferSource.BufferSource bufferSource) {
		if (alpha <= 0.01f) return;
		Minecraft mc = Minecraft.getInstance();
		MutableComponent text = txt(str);
		float textWidth = mc.font.width(text);
		drawText(poseStack, text, centerX - textWidth / 2.0f, y, rgb, alpha, bufferSource);
	}

	private static void drawText(PoseStack poseStack, MutableComponent text, float textX, float textY, int rgb, float alpha, MultiBufferSource.BufferSource bufferSource) {
		Minecraft mc = Minecraft.getInstance();
		int color = withAlpha(rgb, alpha);
		int borderColor = withAlpha(0x000000, alpha);
		int light = 15728880;

		Matrix4f matrix = poseStack.last().pose();
		mc.font.drawInBatch(text, textX + 1, textY, borderColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, light);
		mc.font.drawInBatch(text, textX - 1, textY, borderColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, light);
		mc.font.drawInBatch(text, textX, textY + 1, borderColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, light);
		mc.font.drawInBatch(text, textX, textY - 1, borderColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, light);
		bufferSource.endBatch();

		mc.font.drawInBatch(text, textX, textY, color, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, light);
		bufferSource.endBatch();
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
