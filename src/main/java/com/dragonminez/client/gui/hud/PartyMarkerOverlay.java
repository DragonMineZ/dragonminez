package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.render.CameraProjectionCapture;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.PartyPackets;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyMarkerOverlay {
	private static final float ARROW_SIZE = 11.0f;
	private static final float ARROW_GAP = 2.0f;
	private static final float TEXT_SCALE = 0.75f;
	private static final float BOB_AMPLITUDE = 1.5f;
	private static final float BOB_SPEED = 2.4f;
	private static final float NDC_MARGIN = 1.05f;
	private static final double HIDE_WITHIN_BLOCKS = 2.5;
	private static final int NAME_COLOR = 0xFFFFFF;
	private static final int DISTANCE_COLOR = 0xB8C0CC;
	private static final int DOWN_COLOR = 0xFF7A7A;

	private static final Map<UUID, MarkerView> VIEWS = new HashMap<>();

	public static final IGuiOverlay HUD_PARTY_MARKER = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	private static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.options.hideGui || mc.player == null || mc.level == null) return;
		if (!ConfigManager.getUserConfig().getPartyMarkers() || !CameraProjectionCapture.isAvailable()) return;

		StatsData localData = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (localData == null || !localData.getStatus().isHasCreatedCharacter()) return;
		List<UUID> partyIds = localData.getPlayerQuestData().getPartyMemberIds();
		if (partyIds == null || partyIds.isEmpty()) {
			VIEWS.clear();
			return;
		}

		List<PartyPackets.HudMember> members = PartyHudCache.current();
		for (MarkerView view : VIEWS.values()) view.present = false;
		for (PartyPackets.HudMember member : members) {
			if (!member.marker() || member.id().equals(mc.player.getUUID()) || !partyIds.contains(member.id())) continue;
			MarkerView view = VIEWS.computeIfAbsent(member.id(), id -> new MarkerView());
			view.present = true;
			view.member = member;
		}

		float unit = HudLayout.unit(null, height);
		float seconds = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f;
		float[] projected = new float[4];

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		for (Iterator<MarkerView> it = VIEWS.values().iterator(); it.hasNext(); ) {
			MarkerView view = it.next();
			float appear = view.appear.update(view.present ? 1.0f : 0.0f);
			if (!view.present && appear <= 0.01f) {
				it.remove();
				continue;
			}

			Vec3 head = resolveHead(mc, view, partialTicks);
			if (head == null) continue;
			double distance = mc.player.getEyePosition(partialTicks).distanceTo(head);
			if (distance < HIDE_WITHIN_BLOCKS) continue;
			if (!CameraProjectionCapture.project(head, width, height, projected)) continue;
			if (Math.abs(projected[2]) > NDC_MARGIN || Math.abs(projected[3]) > NDC_MARGIN) continue;

			drawMarker(guiGraphics, mc, view, projected[0], projected[1], distance, unit, appear, seconds);
		}
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static Vec3 resolveHead(Minecraft mc, MarkerView view, float partialTicks) {
		PartyPackets.HudMember member = view.member;
		Player entity = mc.level.getPlayerByUUID(member.id());
		if (entity != null && entity.isAlive()) {
			Vec3 position = entity.getPosition(partialTicks);
			view.snapTo(position.x, position.y + entity.getBbHeight(), position.z);
			return new Vec3(position.x, position.y + entity.getBbHeight(), position.z);
		}
		return view.smoothed(member.x(), member.y(), member.z());
	}

	private static void drawMarker(GuiGraphics guiGraphics, Minecraft mc, MarkerView view, float screenX, float screenY,
								   double distance, float unit, float appear, float seconds) {
		PartyPackets.HudMember member = view.member;
		boolean down = member.health() <= 0.0f;
		Player entity = mc.level.getPlayerByUUID(member.id());
		if (entity != null) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
			if (data != null && data.getStatus().isKnockedDown()) down = true;
		}
		int tint = down ? DOWN_COLOR : member.auraRgb();
		float bob = Mth.sin(seconds * BOB_SPEED * (float) Math.PI + view.phase) * BOB_AMPLITUDE;
		float size = ARROW_SIZE * unit;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(screenX, screenY - size - ARROW_GAP * unit + bob * unit, 0.0f);
		HudRender.setAlphaScale(appear);
		try {
			HudRender.sprite(guiGraphics, HudSprites.PARTY_MARKER, -size / 2.0f, 0.0f, size, size, tint, 1.0f);
			float textScale = TEXT_SCALE * unit;
			float lineHeight = mc.font.lineHeight * textScale;
			String distanceText = Math.round(distance) + "m";
			HudRender.text(guiGraphics, distanceText, 0.0f, -lineHeight - 1.0f * unit, textScale, 0.5f, DISTANCE_COLOR, 1.0f);
			HudRender.text(guiGraphics, member.name(), 0.0f, -lineHeight * 2.0f - 2.0f * unit, textScale, 0.5f, down ? DOWN_COLOR : NAME_COLOR, 1.0f);
		} finally {
			HudRender.setAlphaScale(1.0f);
			guiGraphics.pose().popPose();
		}
	}

	private static final class MarkerView {
		private final HudSmoother appear = new HudSmoother(0.18f, 0.01f);
		private final HudSmoother x = new HudSmoother(0.22f, 0.001f);
		private final HudSmoother y = new HudSmoother(0.22f, 0.001f);
		private final HudSmoother z = new HudSmoother(0.22f, 0.001f);
		private final float phase = (float) (Math.random() * Math.PI * 2.0);
		private PartyPackets.HudMember member;
		private boolean present;

		private MarkerView() {
			appear.snap(0.0f);
		}

		private void snapTo(double px, double py, double pz) {
			x.snap((float) px);
			y.snap((float) py);
			z.snap((float) pz);
		}

		private Vec3 smoothed(double px, double py, double pz) {
			return new Vec3(x.update((float) px), y.update((float) py), z.update((float) pz));
		}
	}
}
