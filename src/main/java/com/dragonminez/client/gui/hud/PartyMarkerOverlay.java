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
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyMarkerOverlay {
	private static final float HEAD_SPRITE = HudSprites.PARTY_HEAD_BACK.guiWidth();
	private static final float HEAD_BORDER = HudSprites.PARTY_HP.border();
	private static final float FACE = 8.0f;
	private static final float FRAME_INSET = FACE * HEAD_BORDER / (HEAD_SPRITE - HEAD_BORDER * 2.0f);
	private static final float FRAME = FACE + FRAME_INSET * 2.0f;
	private static final float ARROW = 8.0f;
	private static final float ARROW_TIP = 30.5f / 32.0f;
	private static final float ARROW_TOP = 3.0f / 32.0f;
	private static final float GAP = 1.5f;
	private static final float ARROW_Y = -ARROW * ARROW_TIP;
	private static final float FRAME_BOTTOM = ARROW_Y + ARROW * ARROW_TOP + 0.5f;
	private static final float FRAME_TOP = FRAME_BOTTOM - FRAME;
	private static final float FOCUS_SCALE = 1.6f;
	private static final float FOCUS_ENTER_RADIUS = 9.0f;
	private static final float FOCUS_KEEP_RADIUS = 15.0f;
	private static final float IDLE_ALPHA = 0.8f;
	private static final float TEXT_SCALE = 0.75f;
	private static final float SCREEN_PAD = 14.0f;
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
		float centerX = width / 2.0f;
		float centerY = height / 2.0f;
		float pad = SCREEN_PAD * unit;
		float[] projected = new float[4];
		List<MarkerView> visible = new ArrayList<>();
		MarkerView focusTarget = null;
		float focusDistance = Float.MAX_VALUE;

		for (Iterator<MarkerView> it = VIEWS.values().iterator(); it.hasNext(); ) {
			MarkerView view = it.next();
			view.appearValue = view.appear.update(view.present ? 1.0f : 0.0f);
			if (!view.present && view.appearValue <= 0.01f) {
				it.remove();
				continue;
			}
			view.onScreen = false;

			Vec3 head = resolveHead(mc, view, partialTicks);
			if (head == null) continue;
			view.distance = mc.player.getEyePosition(partialTicks).distanceTo(head);
			if (view.distance < HIDE_WITHIN_BLOCKS) continue;
			if (!CameraProjectionCapture.project(head, width, height, projected)) continue;
			float x = projected[0];
			float y = projected[1];
			if (x < -pad || x > width + pad || y < -pad || y > height + pad * 2.0f) continue;

			view.onScreen = true;
			view.screenX = x;
			view.screenY = y;
			visible.add(view);

			if (!view.present) continue;
			float iconY = y - (GAP - (FRAME_TOP + FRAME_BOTTOM) / 2.0f) * unit;
			float offset = Mth.sqrt((x - centerX) * (x - centerX) + (iconY - centerY) * (iconY - centerY));
			float radius = (view.focused ? FOCUS_KEEP_RADIUS : FOCUS_ENTER_RADIUS) * unit;
			if (offset <= radius && offset < focusDistance) {
				focusDistance = offset;
				focusTarget = view;
			}
		}

		for (MarkerView view : VIEWS.values()) {
			view.focused = view == focusTarget;
			view.focusValue = view.focus.update(view.focused && view.onScreen ? 1.0f : 0.0f);
		}
		if (visible.isEmpty()) return;

		visible.sort((a, b) -> {
			if (a.focused != b.focused) return a.focused ? 1 : -1;
			return Double.compare(b.distance, a.distance);
		});

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		for (MarkerView view : visible) drawMarker(guiGraphics, mc, view, unit);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static Vec3 resolveHead(Minecraft mc, MarkerView view, float partialTicks) {
		PartyPackets.HudMember member = view.member;
		Player entity = mc.level.getPlayerByUUID(member.id());
		if (entity != null && entity.isAlive()) {
			Vec3 position = entity.getPosition(partialTicks);
			double top = position.y + entity.getBbHeight();
			view.snapTo(position.x, top, position.z);
			return new Vec3(position.x, top, position.z);
		}
		return view.smoothed(member.x(), member.y(), member.z());
	}

	private static void drawMarker(GuiGraphics guiGraphics, Minecraft mc, MarkerView view, float unit) {
		PartyPackets.HudMember member = view.member;
		boolean down = member.health() <= 0.0f;
		Player entity = mc.level.getPlayerByUUID(member.id());
		if (entity != null) {
			StatsData data = StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null);
			if (data != null && data.getStatus().isKnockedDown()) down = true;
		}
		int tint = down ? DOWN_COLOR : member.auraRgb();
		float focus = view.focusValue;
		float scale = Mth.lerp(focus, 1.0f, FOCUS_SCALE);
		float alpha = view.appearValue * Mth.lerp(focus, IDLE_ALPHA, 1.0f);
		float anchorY = view.screenY - GAP * unit;

		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(view.screenX, anchorY, 0.0f);
		HudRender.setAlphaScale(alpha);
		try {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().scale(unit * scale, unit * scale, 1.0f);
			HudRender.sprite(guiGraphics, HudSprites.PARTY_MARKER, -ARROW / 2.0f, ARROW_Y, ARROW, ARROW, tint, 1.0f);
			float frameX = -FRAME / 2.0f;
			HudRender.sprite(guiGraphics, HudSprites.PARTY_HEAD_BACK, frameX, FRAME_TOP, FRAME, FRAME, 0xFFFFFF, 1.0f);
			drawFace(guiGraphics, skinOf(mc, member.id()), -FACE / 2.0f, FRAME_TOP + FRAME_INSET);
			if (down) HudRender.rect(guiGraphics, -FACE / 2.0f, FRAME_TOP + FRAME_INSET, FACE, FACE, HudRender.argb(0.55f, 0x7A0000));
			guiGraphics.pose().popPose();

			if (focus > 0.02f) {
				float textScale = TEXT_SCALE * unit;
				float lineHeight = mc.font.lineHeight * textScale;
				float rise = (1.0f - focus) * 3.0f * unit;
				float distanceY = FRAME_TOP * scale * unit - 2.0f * unit - lineHeight + rise;
				float nameY = distanceY - lineHeight - 1.0f * unit;
				HudRender.dmzText(guiGraphics, member.name(), 0.0f, nameY, textScale, 0.5f, down ? DOWN_COLOR : NAME_COLOR, focus);
				HudRender.dmzText(guiGraphics, Math.round(view.distance) + "m", 0.0f, distanceY, textScale, 0.5f, DISTANCE_COLOR, focus);
			}
		} finally {
			HudRender.setAlphaScale(1.0f);
			guiGraphics.pose().popPose();
		}
	}

	private static void drawFace(GuiGraphics guiGraphics, ResourceLocation skin, float x, float y) {
		float alpha = HudRender.currentAlphaScale();
		if (alpha <= 0.004f) return;
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
		HudRender.blit(guiGraphics, skin, x, y, 8.0f, 8.0f, FACE, FACE, 8.0f, 8.0f, 64, 64);
		HudRender.blit(guiGraphics, skin, x, y, 40.0f, 8.0f, FACE, FACE, 8.0f, 8.0f, 64, 64);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static ResourceLocation skinOf(Minecraft mc, UUID id) {
		if (mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(id);
			if (info != null) return info.getSkinLocation();
		}
		return DefaultPlayerSkin.getDefaultSkin(id);
	}

	private static final class MarkerView {
		private final HudSmoother appear = new HudSmoother(0.18f, 0.01f);
		private final HudSmoother focus = new HudSmoother(0.08f, 0.002f);
		private final HudSmoother x = new HudSmoother(0.22f, 0.001f);
		private final HudSmoother y = new HudSmoother(0.22f, 0.001f);
		private final HudSmoother z = new HudSmoother(0.22f, 0.001f);
		private PartyPackets.HudMember member;
		private boolean present;
		private boolean focused;
		private boolean onScreen;
		private float appearValue;
		private float focusValue;
		private float screenX;
		private float screenY;
		private double distance;

		private MarkerView() {
			appear.snap(0.0f);
			focus.snap(0.0f);
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
