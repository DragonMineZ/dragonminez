package com.dragonminez.client.events;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainItems;
import com.dragonminez.server.world.dimension.NamekDimension;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RadarRenderEvent {
	private static final ResourceLocation RADAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/gui/radar.png");

	private static List<BlockPos> clientEarthPositions = new ArrayList<>();
	private static List<BlockPos> clientNamekPositions = new ArrayList<>();

	public static void updateRadarData(List<BlockPos> earth, List<BlockPos> namek) {
		clientEarthPositions = earth;
		clientNamekPositions = namek;
	}

	@SubscribeEvent
	public static void onRenderGameOverlay(RenderGuiOverlayEvent.Pre event) {
		if (!event.getOverlay().id().getPath().equals("hotbar")) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.isPaused() || mc.player == null) return;

		Player player = mc.player;
		Level level = player.level();

		boolean isOverworld = level.dimension().equals(Level.OVERWORLD);
		boolean isNamek = level.dimension().equals(NamekDimension.NAMEK_KEY);

		if (!isOverworld && !isNamek) return;

		ItemStack mainHand = player.getMainHandItem();
		ItemStack offHand = player.getOffhandItem();

		var earthRadarItem = MainItems.DBALL_RADAR_ITEM.get();
		var namekRadarItem = MainItems.NAMEKDBALL_RADAR_ITEM.get();

		List<BlockPos> targets = null;
		int range = 75;
		boolean isMainHand = false;

		if (isOverworld && mainHand.getItem() == earthRadarItem) {
			targets = clientEarthPositions;
			range = getRadarRange(mainHand);
			isMainHand = true;
		} else if (isNamek && mainHand.getItem() == namekRadarItem) {
			targets = clientNamekPositions;
			range = getRadarRange(mainHand);
			isMainHand = true;
		}
		else if (isOverworld && offHand.getItem() == earthRadarItem) {
			targets = clientEarthPositions;
			range = getRadarRange(offHand);
			isMainHand = false;
		} else if (isNamek && offHand.getItem() == namekRadarItem) {
			targets = clientNamekPositions;
			range = getRadarRange(offHand);
			isMainHand = false;
		}

		if (targets != null) {
			int radarSize = 140;
			int centerX;
			int centerY = mc.getWindow().getGuiScaledHeight() - radarSize - 10;

			if (isMainHand) {
				centerX = mc.getWindow().getGuiScaledWidth() - radarSize - 10;
			} else {
				if (mc.getWindow().getWidth() < 1080) {
					centerX = (mc.getWindow().getGuiScaledWidth() / 2) - radarSize - 60;
				} else {
					centerX = (mc.getWindow().getGuiScaledWidth() / 2) - radarSize - 310;
				}
			}

			renderRadar(event.getGuiGraphics(), player, targets, range, centerX, centerY);
		}
	}

	private static int getRadarRange(ItemStack stack) {
		int r = stack.getOrCreateTag().getInt("RadarRange");
		return (r == 0) ? 150 : r;
	}

	private static void renderRadar(GuiGraphics gui, Player player, List<BlockPos> targets, int range, int centerX, int centerY) {
		int textureW = 121;
		int textureH = 146;

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, RADAR_TEXTURE);
		gui.blit(RADAR_TEXTURE, centerX, centerY, 0, 0, textureW, textureH);

		int radarCenterX = centerX + 61;
		int radarCenterY = centerY + 87;

		for (BlockPos pos : targets) {
			double dx = pos.getX() - player.getX();
			double dz = pos.getZ() - player.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);

			double angleToTarget = Math.atan2(dz, dx);
			double playerYaw = Math.toRadians(player.getYRot()) + (Math.PI / 2);
			double finalAngle = angleToTarget - playerYaw;

			double renderAngle = finalAngle - (Math.PI / 2);

			if (dist <= range) {
				double scaledDist = (dist / range) * 50.0;

				int dotX = (int) (radarCenterX + (scaledDist * Math.cos(renderAngle)));
				int dotY = (int) (radarCenterY + (scaledDist * Math.sin(renderAngle)));

				gui.blit(RADAR_TEXTURE, dotX - 3, dotY - 3, 130, 0, 6, 6);

			} else {
				int arrowRadius = 50;

				int arrowX = (int) (radarCenterX + (arrowRadius * Math.cos(renderAngle)));
				int arrowY = (int) (radarCenterY + (arrowRadius * Math.sin(renderAngle)));

				float rotation = (float) renderAngle + (float) (Math.PI / 2);

				gui.pose().pushPose();
				gui.pose().translate(arrowX, arrowY, 0);
				gui.pose().mulPose(new Quaternionf().rotationZ(rotation));
				gui.pose().translate(-3.5f, -3.0f, 0);

				gui.blit(RADAR_TEXTURE, 0, 0, 130, 8, 7, 6);

				gui.pose().popPose();
			}
		}
	}
}