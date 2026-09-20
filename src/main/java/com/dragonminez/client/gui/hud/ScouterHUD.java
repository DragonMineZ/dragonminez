package com.dragonminez.client.gui.hud;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.quest.QuestUnlocks;
import com.dragonminez.common.network.C2S.DamageCurioC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.util.CuriosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ScouterHUD {
	private static final ResourceLocation SCOUTER_GREEN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_green.png");
	private static final ResourceLocation SCOUTER_RED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_red.png");
	private static final ResourceLocation SCOUTER_BLUE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_blue.png");
	private static final ResourceLocation SCOUTER_PURPLE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_purple.png");

	private static final float FRAME_WIDTH = 70.0f;
	private static final float FRAME_HEIGHT = 41.0f;
	private static final float GLASS_ALPHA = 0.65f;
	private static final float HIDDEN_PREVIEW_ALPHA = 0.35f;
	private static final float CENTER_X = 41.0f;
	private static final float CENTER_Y = 19.0f;
	private static final float RETICLE_SIZE = 18.0f;
	private static final float ARROW_SIZE = 5.0f;
	private static final float ARROW_GAP = 2.0f;
	private static final float NUMBERS_RIGHT = 30.0f;
	private static final float NUMBERS_Y = 17.0f;
	private static final int MAX_NUMBER_CHARS = 5;
	private static final float GLYPH_SPACING = 1.0f;
	private static final double PREVIEW_BP = 12300.0;

	private static boolean isRenderingInfo = false;
	private static boolean mirrored = false;
	private static float drawAlpha = 1.0f;

	private static int scanTimer = 0;
	private static int strongestEntityID = -1;
	private static double cachedBP = 0;
	private static final double SCAN_RANGE = 50.0;
	private static final int BP_LIMIT = 150000000;

	private static ItemStack getScouterStack(Player player) {
		return CuriosUtil.getFirstStackForItem(player, "head_tech", "scouter");
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		ItemStack scouterStack = getScouterStack(mc.player);
		if (scouterStack.isEmpty()) {
			isRenderingInfo = false;
			return;
		}

		if (isRenderingInfo && scanTimer++ >= 20) {
			scanTimer = 0;
			performSmartScan(mc.player);
		}
	}

	private static void performSmartScan(Player player) {
		Entity currentTarget = player.level().getEntity(strongestEntityID);
		boolean cachedIsAlive = (currentTarget instanceof LivingEntity living && living.isAlive());
		double thresholdBP = cachedIsAlive ? cachedBP : -1;

		AABB searchBox = player.getBoundingBox().inflate(SCAN_RANGE);
		List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive());

		LivingEntity newStrongest = null;
		double maxFoundBP = thresholdBP;

		for (LivingEntity entity : entities) {
			double bp = getEntityBP(entity);

			if (bp > BP_LIMIT) {
				damageScouter(player);
				return;
			}

			if (bp > maxFoundBP) {
				maxFoundBP = bp;
				newStrongest = entity;
			}
		}

		if (newStrongest != null) {
			strongestEntityID = newStrongest.getId();
			cachedBP = maxFoundBP;
		} else if (!cachedIsAlive) {
			strongestEntityID = -1;
			cachedBP = 0;
		}
	}

	/** Bulma's Anti-Ki Cloak: a worn cloak hides the wearer's BP from scouters. */
	private static boolean isCloaked(Player target) {
		return CuriosUtil.getFirstStackForItem(target, "head_tech", "anti_ki_cloak").getItem() == MainItems.ANTI_KI_CLOAK.get();
	}

	private static double getEntityBP(LivingEntity entity) {
		try {
			if (entity instanceof Player player) {
				if (isCloaked(player)) return 0;
				var cap = StatsProvider.get(StatsCapability.INSTANCE, player);
				if (cap.isPresent()) return cap.map(StatsData::getBattlePower).orElse(0.0f);
			}
			if (entity instanceof IBattlePower bpEntity) return bpEntity.getBattlePower();
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Error calculating BP for entity ID " + entity.getId() + ": " + e.getMessage());
		}
		return 0;
	}

	public static final IGuiOverlay HUD_SCOUTER = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (HudStyle.current() == HudStyle.LEGACY_2) return;

		boolean preview = HudLayout.isPreview();
		HudLayout.Box box = HudLayout.resolve(HudElement.SCOUTER, width, height);
		if (!box.visible() && !preview) return;

		ItemStack scouterStack = getScouterStack(mc.player);
		if (scouterStack.isEmpty() && !preview) return;

		Item currentItem = scouterStack.getItem();
		ResourceLocation currentTexture = currentItem == MainItems.BLUE_SCOUTER.get() ? SCOUTER_BLUE :
				currentItem == MainItems.RED_SCOUTER.get() ? SCOUTER_RED :
				currentItem == MainItems.PURPLE_SCOUTER.get() ? SCOUTER_PURPLE : SCOUTER_GREEN;

		mirrored = box.mirrored();
		drawAlpha = box.visible() ? 1.0f : HIDDEN_PREVIEW_ALPHA;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);
		try {
			renderScouterFrame(guiGraphics, currentTexture);
			if (preview) {
				renderCustomNumbers(guiGraphics, currentTexture, formatBP(PREVIEW_BP));
				renderEntityInfo(guiGraphics, currentTexture, true, true);
			} else if (isRenderingInfo) {
				renderInfo(guiGraphics, mc, currentTexture);
			}
		} finally {
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			guiGraphics.pose().popPose();
		}
	}

	private static void renderInfo(GuiGraphics guiGraphics, Minecraft mc, ResourceLocation currentTexture) {
		HitResult hit = mc.hitResult;
		LivingEntity focusedEntity = null;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
			if (((EntityHitResult) hit).getEntity() instanceof LivingEntity living) focusedEntity = living;
		}

		double distToFocus = (focusedEntity != null) ? mc.player.distanceTo(focusedEntity) : Double.MAX_VALUE;

		if (focusedEntity != null && distToFocus <= 20) {
			double bp = getEntityBP(focusedEntity);
			if (bp > BP_LIMIT) {
				damageScouter(mc.player);
				return;
			}
			if (QuestUnlocks.isCompleted(mc.player, QuestUnlocks.SCOUTER_CALIBRATION))
				renderCustomNumbers(guiGraphics, currentTexture, formatBP(bp));
			renderEntityInfo(guiGraphics, currentTexture, QuestUnlocks.isCompleted(mc.player, QuestUnlocks.SCOUTER_BIOSCAN), focusedEntity instanceof Player);
		} else if (focusedEntity != null && distToFocus <= 50) {
			renderDirectionIcon(guiGraphics, currentTexture, mc.player, focusedEntity);
			renderEntityInfo(guiGraphics, currentTexture, false, focusedEntity instanceof Player);
		} else {
			Entity strongest = mc.player.level().getEntity(strongestEntityID);
			if (strongest instanceof LivingEntity livingStrongest && livingStrongest.isAlive()) {
				if (mc.player.distanceTo(livingStrongest) <= SCAN_RANGE) {
					renderDirectionIcon(guiGraphics, currentTexture, mc.player, livingStrongest);
				}
			}
		}
	}

	private static void damageScouter(Player player) {
		setRenderingInfo(false);
		NetworkHandler.sendToServer(new DamageCurioC2S("head_tech", 0, 1));
		CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
			var handler = inv.getCurios().get("head_tech");
			if (handler != null) {
				ItemStack stack = handler.getStacks().getStackInSlot(0);
				if (!stack.isEmpty() && stack.getDescriptionId().contains("scouter")) {
					stack.setDamageValue(stack.getDamageValue() + 1);
					if (stack.getDamageValue() >= stack.getMaxDamage()) {
						player.playSound(SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
						handler.getStacks().setStackInSlot(0, ItemStack.EMPTY);
					} else {
						player.playSound(SoundEvents.GLASS_HIT, 0.5F, 1.0F);
					}
				}
			}
		});
	}

	private static void part(GuiGraphics gui, ResourceLocation texture, float x, float y, float u, float v, float width, float height, float scale, boolean flip) {
		float drawnWidth = width * scale;
		float drawX = mirrored ? FRAME_WIDTH - x - drawnWidth : x;
		if (mirrored && flip) HudRender.blit(gui, texture, drawX, y, u + width, v, drawnWidth, height * scale, -width, height, 128, 128);
		else HudRender.blit(gui, texture, drawX, y, u, v, drawnWidth, height * scale, width, height, 128, 128);
	}

	private static void renderScouterFrame(GuiGraphics gui, ResourceLocation texture) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, drawAlpha);
		part(gui, texture, 0, 0, 0, 15, 7, FRAME_HEIGHT, 1.0f, true);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, GLASS_ALPHA * drawAlpha);
		part(gui, texture, 7, 0, 7, 15, 63, FRAME_HEIGHT, 1.0f, true);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, drawAlpha);
	}

	private static void renderCustomNumbers(GuiGraphics gui, ResourceLocation texture, String text) {
		if (text.length() > MAX_NUMBER_CHARS) text = text.substring(0, MAX_NUMBER_CHARS);
		float textWidth = -GLYPH_SPACING;
		for (int i = 0; i < text.length(); i++) textWidth += glyphWidth(text.charAt(i)) + GLYPH_SPACING;
		float blockX = NUMBERS_RIGHT - textWidth;
		float cursor = mirrored ? FRAME_WIDTH - blockX - textWidth : blockX;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int u = 2, v = 64;

			switch (c) {
				case '0' -> { u = 2; v = 64; }
				case '1' -> { u = 6; v = 64; }
				case '2' -> { u = 10; v = 64; }
				case '3' -> { u = 14; v = 64; }
				case '4' -> { u = 18; v = 64; }
				case '5' -> { u = 2; v = 68; }
				case '6' -> { u = 6; v = 68; }
				case '7' -> { u = 10; v = 68; }
				case '8' -> { u = 14; v = 68; }
				case '9' -> { u = 18; v = 68; }
				case 'k' -> { u = 22; v = 64; }
				case 'm' -> { u = 22; v = 68; }
				case '.' -> { u = 27; v = 64; }
			}
			int width = glyphWidth(c);
			HudRender.blit(gui, texture, cursor, NUMBERS_Y, u, v, width, 3, 128, 128);
			cursor += width + GLYPH_SPACING;
		}
	}

	private static int glyphWidth(char c) {
		return c == '.' ? 1 : 3;
	}

	private static void renderDirectionIcon(GuiGraphics gui, ResourceLocation texture, Player player, LivingEntity target) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double angleToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90;
		double diff = -Mth.wrapDegrees(angleToTarget - player.getYRot());
		int dir = (int) Math.floor(((-diff % 360 + 360 + 22.5) % 360) / 45.0) % 8;

		boolean up = dir == 7 || dir == 0 || dir == 1;
		boolean right = dir >= 1 && dir <= 3;
		boolean down = dir >= 3 && dir <= 5;
		boolean left = dir >= 5 && dir <= 7;

		float centerX = mirrored ? FRAME_WIDTH - CENTER_X : CENTER_X;
		float reach = RETICLE_SIZE / 2.0f + ARROW_GAP;
		float half = ARROW_SIZE / 2.0f;
		if (up) HudRender.blit(gui, texture, centerX - half, CENTER_Y - reach - ARROW_SIZE, 26, 75, ARROW_SIZE, ARROW_SIZE, 128, 128);
		if (right) HudRender.blit(gui, texture, centerX + reach, CENTER_Y - half, 14, 75, ARROW_SIZE, ARROW_SIZE, 128, 128);
		if (down) HudRender.blit(gui, texture, centerX - half, CENTER_Y + reach, 34, 75, ARROW_SIZE, ARROW_SIZE, 128, 128);
		if (left) HudRender.blit(gui, texture, centerX - reach - ARROW_SIZE, CENTER_Y - half, 19, 75, ARROW_SIZE, ARROW_SIZE, 128, 128);
	}

	private static void renderEntityInfo(GuiGraphics gui, ResourceLocation texture, boolean extraInfo, boolean isPlayer) {
		part(gui, texture, CENTER_X - RETICLE_SIZE / 2.0f, CENTER_Y - RETICLE_SIZE / 2.0f, 2, 73, 9, 9, 2.0f, false);

		if (!extraInfo) return;

		part(gui, texture, 49, 6, 4, 88, 12, 5, 1.0f, true);
		int uX = isPlayer ? 3 : 20;
		int w = isPlayer ? 14 : 11;
		part(gui, texture, 52, 9, uX, 98, w, 5, 1.0f, false);
	}

	private static String formatBP(double bp) {
		if (bp < 10000) return String.valueOf((long) bp);
		if (bp < 99950) return String.format(Locale.ROOT, "%.1fk", bp / 1000.0);
		if (bp < 1000000) return (long) (bp / 1000.0) + "k";
		if (bp < 99950000) return String.format(Locale.ROOT, "%.1fm", bp / 1000000.0);
		return (long) (bp / 1000000.0) + "m";
	}

	public static void setRenderingInfo(boolean render) { isRenderingInfo = render; }
	public static boolean isRenderingInfo() { return isRenderingInfo; }
}