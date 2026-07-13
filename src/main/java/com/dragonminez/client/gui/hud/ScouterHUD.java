package com.dragonminez.client.gui.hud;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
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

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ScouterHUD {
	private static final ResourceLocation SCOUTER_GREEN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_green.png");
	private static final ResourceLocation SCOUTER_RED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_red.png");
	private static final ResourceLocation SCOUTER_BLUE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_blue.png");
	private static final ResourceLocation SCOUTER_PURPLE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_purple.png");

	private static boolean isRenderingInfo = false;

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
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (ConfigManager.getUserConfig().getAlternativeHud()) return;

		ItemStack scouterStack = getScouterStack(mc.player);
		if (scouterStack.isEmpty()) return;

		Item currentItem = scouterStack.getItem();
		ResourceLocation currentTexture = currentItem == MainItems.BLUE_SCOUTER.get() ? SCOUTER_BLUE :
				currentItem == MainItems.RED_SCOUTER.get() ? SCOUTER_RED :
				currentItem == MainItems.PURPLE_SCOUTER.get() ? SCOUTER_PURPLE : SCOUTER_GREEN;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(0, height / 2.0f, 0);
		guiGraphics.pose().scale(2.5f, 2.5f, 1.0f);

		renderScouterFrame(guiGraphics, currentTexture);

		if (isRenderingInfo) {
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
					guiGraphics.pose().popPose();
					return;
				}

					if (QuestUnlocks.isCompleted(mc.player, QuestUnlocks.SCOUTER_CALIBRATION))
						renderCustomNumbers(guiGraphics, currentTexture, formatBP(bp));
				renderEntityInfo(guiGraphics, currentTexture, QuestUnlocks.isCompleted(mc.player, QuestUnlocks.SCOUTER_BIOSCAN), focusedEntity instanceof Player);

			} else if (focusedEntity != null && distToFocus > 20 && distToFocus <= 50) {
				renderDirectionIcon(guiGraphics, currentTexture, mc.player, focusedEntity, true);
				renderEntityInfo(guiGraphics, currentTexture, false, focusedEntity instanceof Player);
			} else {
				Entity strongest = mc.player.level().getEntity(strongestEntityID);
				if (strongest instanceof LivingEntity livingStrongest && livingStrongest.isAlive()) {
					if (mc.player.distanceTo(livingStrongest) <= SCAN_RANGE) {
						renderDirectionIcon(guiGraphics, currentTexture, mc.player, livingStrongest, false);
					}
				}
			}
		}
		guiGraphics.pose().popPose();
	};

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

	private static void renderScouterFrame(GuiGraphics gui, ResourceLocation texture) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		gui.blit(texture, 0, -20, 0, 15, 7, 41, 128, 128);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.65F);
		gui.blit(texture, 7, -20, 7, 15, 63, 41, 128, 128);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private static void renderCustomNumbers(GuiGraphics gui, ResourceLocation texture, String text) {
		int charWidth = 3;
		int spacing = 1;
		int currentTextWidth = (text.length() * charWidth) + ((text.length() - 1) * spacing);
		int maxPossibleWidth = (4 * charWidth) + (3 * spacing);
		int centerOffset = (maxPossibleWidth - currentTextWidth) / 2;

		int startX = 10 + centerOffset;
		int startY = -4;

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
				case '.' -> { u = 26; v = 64; }
			}
			gui.blit(texture, startX + (i * (charWidth + 1)), startY, u, v, charWidth, 3, 128, 128);
		}
	}

	private static void renderDirectionIcon(GuiGraphics gui, ResourceLocation texture, Player player, LivingEntity target, boolean isCircleMode) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double angleToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90;
		double diff = -Mth.wrapDegrees(angleToTarget - player.getYRot());
		int dir = 0;

		if (diff >= -22.5 && diff < 22.5) dir = 0;
		else if (diff >= -67.5 && diff < -22.5) dir = 1;
		else if (diff >= -112.5 && diff < -67.5) dir = 2;
		else if (diff >= -157.5 && diff < -112.5) dir = 3;
		else if (diff >= 157.5 || diff < -157.5) dir = 4;
		else if (diff >= 112.5 && diff < 157.5) dir = 5;
		else if (diff >= 67.5 && diff < 112.5) dir = 6;
		else if (diff >= 22.5 && diff < 67.5) dir = 7;

		switch (dir) {
			case 0 -> gui.blit(texture, 40, -16, 26, 75, 5, 5, 128, 128);
			case 1 -> { gui.blit(texture, 40, -16, 26, 75, 5, 5, 128, 128); gui.blit(texture, 50, -8, 14, 75, 5, 5, 128, 128); }
			case 2 -> gui.blit(texture, 50, -8, 14, 75, 5, 5, 128, 128);
			case 3 -> { gui.blit(texture, 50, -8, 14, 75, 5, 5, 128, 128); gui.blit(texture, 40, 0, 34, 75, 5, 5, 128, 128); }
			case 4 -> gui.blit(texture, 40, 0, 34, 75, 5, 5, 128, 128);
			case 5 -> { gui.blit(texture, 40, 0, 34, 75, 5, 5, 128, 128); gui.blit(texture, 30, -8, 19, 75, 5, 5, 128, 128); }
			case 6 -> gui.blit(texture, 30, -8, 19, 75, 5, 5, 128, 128);
			case 7 -> { gui.blit(texture, 40, -16, 26, 75, 5, 5, 128, 128); gui.blit(texture, 30, -8, 19, 75, 5, 5, 128, 128); }
		}
	}

	private static void renderEntityInfo(GuiGraphics gui, ResourceLocation texture, boolean extraInfo, boolean isPlayer) {
		gui.pose().pushPose();
		gui.pose().translate(40, -30, 0);
		gui.pose().scale(2.0f, 2.0f, 1.0f);
		gui.blit(texture, 0, 0, 2, 73, 9, 9, 128, 128);
		gui.pose().popPose();

		if (!extraInfo) return;

		gui.blit(texture, 50, -10, 4, 88, 12, 5, 128, 128);
		int uX = isPlayer ? 3 : 20;
		int w = isPlayer ? 14 : 11;
		gui.blit(texture, 53, -15, uX, 98, w, 5, 128, 128);
	}

	private static String formatBP(double bp) {
		if (bp < 10000) return String.valueOf(bp);
		if (bp < 1000000) return String.format("%.1fk", bp / 1000.0f).replace(",", ".");
		return String.format("%.1fm", bp / 1000000.0f).replace(",", ".");
	}

	public static void setRenderingInfo(boolean render) { isRenderingInfo = render; }
	public static boolean isRenderingInfo() { return isRenderingInfo; }
}