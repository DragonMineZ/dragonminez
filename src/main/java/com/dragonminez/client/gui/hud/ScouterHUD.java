package com.dragonminez.client.gui.hud;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.entities.IBattlePower;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ScouterHUD {
	private static final ResourceLocation SCOUTER_GREEN = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_green.png");
	private static final ResourceLocation SCOUTER_RED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_red.png");
	private static final ResourceLocation SCOUTER_BLUE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_blue.png");
	private static final ResourceLocation SCOUTER_PURPLE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/scouter/scouter_purple.png");

	private static boolean isRenderingInfo = false;
	private static Item scouterColor = null;

	private static int scanTimer = 0;
	private static int strongestEntityID = -1;
	private static int cachedBP = 0;
	private static final double SCAN_RANGE = 50.0;
	private static final int BP_LIMIT = 150000000;
	private static final float TEX_SIZE = 128.0f;

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		if (!mc.player.getItemBySlot(EquipmentSlot.HEAD).getDescriptionId().contains("scouter")) return;

		if (scanTimer++ >= 20) {
			scanTimer = 0;
			performSmartScan(mc.player);
		}
	}

	private static void performSmartScan(Player player) {
		Entity currentTarget = player.level().getEntity(strongestEntityID);
		boolean cachedIsAlive = (currentTarget instanceof LivingEntity living && living.isAlive());
		int thresholdBP = cachedIsAlive ? cachedBP : -1;

		AABB searchBox = player.getBoundingBox().inflate(SCAN_RANGE);
		List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
				e -> e != player && e.isAlive());

		LivingEntity newStrongest = null;
		int maxFoundBP = thresholdBP;

		for (LivingEntity entity : entities) {
			int bp = getEntityBP(entity);
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

	private static int getEntityBP(LivingEntity entity) {
		try {
			if (entity instanceof Player player) {
				var cap = StatsProvider.get(StatsCapability.INSTANCE, player);
				if (cap.isPresent()) {
					return cap.map(StatsData::getBattlePower).orElse(0);
				}
			}
			if (entity instanceof IBattlePower bpEntity) {
				return bpEntity.getBattlePower();
			}
		} catch (Exception e) {
			LogUtil.error(Env.CLIENT, "Error calculating BP for entity ID " + entity.getId() + ": " + e.getMessage());
			return 0;
		}
		return 0;
	}

	public static final IGuiOverlay HUD_SCOUTER = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null) return;
		if (ConfigManager.getUserConfig().getHud().isAlternativeHud()) return;

		boolean hasScouter = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem().getDescriptionId().contains("scouter");
		if (!hasScouter) return;

		ResourceLocation currentTexture;
		if (scouterColor == MainItems.BLUE_SCOUTER.get()) currentTexture = SCOUTER_BLUE;
		else if (scouterColor == MainItems.RED_SCOUTER.get()) currentTexture = SCOUTER_RED;
		else if (scouterColor == MainItems.PURPLE_SCOUTER.get()) currentTexture = SCOUTER_PURPLE;
		else currentTexture = SCOUTER_GREEN;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().scale(3.0f, 3.0f, 1.0f);

		int scaledWidth = (int) (width / 3.0f);
		int scaledHeight = (int) (height / 3.0f);

		renderScouterFrame(guiGraphics, currentTexture, scaledWidth, scaledHeight);

		if (isRenderingInfo) {
			HitResult hit = mc.hitResult;
			LivingEntity focusedEntity = null;

			if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
				if (((EntityHitResult) hit).getEntity() instanceof LivingEntity living) focusedEntity = living;
			}

			double distToFocus = (focusedEntity != null) ? mc.player.distanceTo(focusedEntity) : Double.MAX_VALUE;

			if (focusedEntity != null && distToFocus <= 20) {
				int bp = getEntityBP(focusedEntity);
				if (bp <= BP_LIMIT) {
					String bpStr = formatBP(bp);
					renderCustomNumbers(guiGraphics, SCOUTER_PURPLE, bpStr, scaledWidth, scaledHeight);
					renderEntityInfo(guiGraphics, currentTexture, true, focusedEntity instanceof Player, scaledWidth, scaledHeight);
				}
			} else if (focusedEntity != null && distToFocus > 20 && distToFocus <= 50) {
				renderDirectionIcon(guiGraphics, currentTexture, mc.player, focusedEntity, scaledWidth, scaledHeight, true);
				renderEntityInfo(guiGraphics, currentTexture, false, focusedEntity instanceof Player, scaledWidth, scaledHeight);
			} else {
				Entity strongest = mc.player.level().getEntity(strongestEntityID);
				if (strongest instanceof LivingEntity livingStrongest && livingStrongest.isAlive()) {
					double dist = mc.player.distanceTo(livingStrongest);
					if (dist <= SCAN_RANGE) {
						renderDirectionIcon(guiGraphics, currentTexture, mc.player, livingStrongest, scaledWidth, scaledHeight, false);
					}
				}
			}
		}
		guiGraphics.pose().popPose();
	};


	private static void drawScouterSprite(GuiGraphics gui, ResourceLocation texture, float x, float y, float u, float v, float w, float h) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		Matrix4f matrix = gui.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();

		float minU = u / TEX_SIZE;
		float maxU = (u + w) / TEX_SIZE;
		float minV = v / TEX_SIZE;
		float maxV = (v + h) / TEX_SIZE;

		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, x, y + h, 0).uv(minU, maxV).endVertex();
		buffer.vertex(matrix, x + w, y + h, 0).uv(maxU, maxV).endVertex();
		buffer.vertex(matrix, x + w, y, 0).uv(maxU, minV).endVertex();
		buffer.vertex(matrix, x, y, 0).uv(minU, minV).endVertex();

		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	private static void renderScouterFrame(GuiGraphics gui, ResourceLocation texture, int screenWidth, int screenHeight) {
		int y = screenHeight / 2 - 40;
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		drawScouterSprite(gui, texture, 0, y, 0, 15, 7, 41);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.65F);
		drawScouterSprite(gui, texture, 7, y, 7, 15, 63, 41);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private static void renderCustomNumbers(GuiGraphics gui, ResourceLocation texture, String text, int w, int h) {
		int charWidth = 3;
		int spacing = 1;
		int currentTextWidth = (text.length() * charWidth) + ((text.length() - 1) * spacing);
		int maxPossibleWidth = (4 * charWidth) + (3 * spacing);
		int centerOffset = (maxPossibleWidth - currentTextWidth) / 2;
		int startX = ((w / 2) - 127) + centerOffset;
		int startY = (h / 2) - 6;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int u = 2, v = 64;
			int charW = 3;

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

			drawScouterSprite(gui, texture, startX + (i * (charW + 1)), startY, u, v, charW, 3);
		}
	}

	private static void renderDirectionIcon(GuiGraphics gui, ResourceLocation texture, Player player, LivingEntity target, int w, int h, boolean isCircleMode) {
		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();

		double angleToTarget = Math.toDegrees(Math.atan2(dz, dx)) - 90;
		double diff = Mth.wrapDegrees(angleToTarget - player.getYRot());
		int directionIndex = 0;
		diff = -diff;

		if (diff >= -22.5 && diff < 22.5) directionIndex = 0;       // N
		else if (diff >= -67.5 && diff < -22.5) directionIndex = 1; // NE
		else if (diff >= -112.5 && diff < -67.5) directionIndex = 2; // E
		else if (diff >= -157.5 && diff < -112.5) directionIndex = 3; // SE
		else if (diff >= 157.5 || diff < -157.5) directionIndex = 4; // S
		else if (diff >= 112.5 && diff < 157.5) directionIndex = 5; // SW
		else if (diff >= 67.5 && diff < 112.5) directionIndex = 6;  // W
		else if (diff >= 22.5 && diff < 67.5) directionIndex = 7;   // NW

		int centerX = w / 2;
		int centerY = h / 2 ;

		switch (directionIndex) {
			case 0 -> drawScouterSprite(gui, texture, centerX - 120, centerY - 36, 26, 75, 5, 5); // N
			case 1 -> {
				drawScouterSprite(gui, texture, centerX -  120, centerY -  36, 26, 75, 5, 5); // N
				drawScouterSprite(gui, texture, centerX - 105, centerY - 22, 14, 75, 5, 5); // E
			}
			case 2 -> drawScouterSprite(gui, texture, centerX - 105, centerY - 22, 14, 75, 5, 5); // E
			case 3 -> {
				drawScouterSprite(gui, texture, centerX - 105, centerY - 22, 14, 75, 5, 5); // E
				drawScouterSprite(gui, texture, centerX - 120, centerY - 8, 34, 75, 5, 5); // S
			}
			case 4 -> drawScouterSprite(gui, texture, centerX - 120, centerY - 8, 34, 75, 5, 5); // S
			case 5 -> {
				drawScouterSprite(gui, texture, centerX - 120, centerY - 8, 34, 75, 5, 5); // S
				drawScouterSprite(gui, texture, centerX - 135, centerY - 22, 19, 75, 5, 5); // W
			}
			case 6 -> drawScouterSprite(gui, texture, centerX - 135, centerY - 22, 19, 75, 5, 5); // W
			case 7 -> {
				drawScouterSprite(gui, texture, centerX - 120, centerY - 36, 26, 75, 5, 5); // N
				drawScouterSprite(gui, texture, centerX - 135, centerY - 22, 19, 75, 5, 5); // W
			}
		}
	}

	private static void renderEntityInfo(GuiGraphics gui, ResourceLocation texture, boolean extraInfo, boolean isPlayer, int w, int h) {
		int centerX = (w / 2) - 127;
		int centerY = (h / 2) - 10;

		gui.pose().pushPose();
		gui.pose().scale(2.0f, 2.0f, 1.0f);
		drawScouterSprite(gui, texture, centerX - 17, centerY - 46, 2, 73, 9, 9);
		gui.pose().popPose();

		if (!extraInfo) return;

		drawScouterSprite(gui, texture, centerX + 15, centerY - 20, 4, 88, 12, 5);

		int uX = isPlayer ? 3 : 20;
		int width = isPlayer ? 14 : 11;

		drawScouterSprite(gui, texture, centerX + 18, centerY - 25, uX, 98, width, 5);
	}

	private static String formatBP(int bp) {
		if (bp < 10000) return String.valueOf(bp);
		if (bp < 1000000) return String.format("%.1fk", bp / 1000.0f).replace(",", ".");
		return String.format("%.1fm", bp / 1000000.0f).replace(",", ".");
	}

	public static void setRenderingInfo(boolean render) {
		System.out.println("Setting Scouter Rendering Info to: " + render);
		isRenderingInfo = render;
	}
	public static boolean isRenderingInfo() { return isRenderingInfo; }
	public static void setScouterColor(Item item) { scouterColor = item; }
	public static Item getScouterColor() { return scouterColor; }
}