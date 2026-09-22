package com.dragonminez.client.gui.hud;

import com.dragonminez.client.gui.hud.layout.HudElement;
import com.dragonminez.client.gui.hud.layout.HudLayout;
import com.dragonminez.client.render.HeadPortraitRenderer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.PartyPackets;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyHUD {
	private static final float BORDER = HudSprites.PARTY_HP.border();
	private static final float HEAD = HudSprites.PARTY_HEAD_BACK.guiWidth() - BORDER * 2.0f;
	private static final float ROW_CONTENT_HEIGHT = HEAD + BORDER * 2.0f;
	private static final float ROW_HEIGHT = ROW_CONTENT_HEIGHT + 4.0f;
	private static final float BAR_X = HEAD + BORDER;
	private static final float HP_WIDTH = HudSprites.PARTY_HP.width(), HP_HEIGHT = HudSprites.PARTY_HP.height();
	private static final float KI_WIDTH = HudSprites.PARTY_KI.width(), KI_HEIGHT = HudSprites.PARTY_KI.height();
	private static final int DEFAULT_KI_COLOR = 0x4FC3FF;

	private static final List<PartyPackets.HudMember> PREVIEW_MEMBERS = List.of(
			new PartyPackets.HudMember(new UUID(0L, 1L), "Goku", 1800.0f, 2000.0f, 900.0f, 1500.0f, true, 12.0f),
			new PartyPackets.HudMember(new UUID(0L, 2L), "Vegeta", 450.0f, 2000.0f, 1300.0f, 1500.0f, true, 148.0f),
			new PartyPackets.HudMember(new UUID(0L, 3L), "Piccolo", 1200.0f, 2000.0f, 400.0f, 1500.0f, false, Float.MAX_VALUE));

	private static final Map<UUID, MemberView> VIEWS = new HashMap<>();
	private static final HudSmoother ROWS = new HudSmoother(0.12f, 0.01f);

	public static final IGuiOverlay HUD_PARTY = (forgeGui, guiGraphics, partialTicks, width, height) -> {
		if (!HudLayout.isPreview()) render(guiGraphics, partialTicks, width, height);
	};

	public static void render(GuiGraphics guiGraphics, float partialTicks, int width, int height) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug || mc.player == null || mc.level == null) return;

		HudLayout.Box box = HudLayout.resolve(HudElement.PARTY, width, height);
		boolean preview = HudLayout.isPreview();
		if (!box.visible() && !preview) return;

		StatsData localData = StatsProvider.get(StatsCapability.INSTANCE, mc.player).orElse(null);
		if (localData == null || !localData.getStatus().isHasCreatedCharacter()) return;

		List<UUID> partyIds = localData.getPlayerQuestData().getPartyMemberIds();
		List<PartyPackets.HudMember> members = PartyHudCache.current();
		boolean sample = preview && members.isEmpty();
		if (sample) members = PREVIEW_MEMBERS;

		for (MemberView view : VIEWS.values()) view.present = false;
		int slot = 0;
		for (PartyPackets.HudMember member : members) {
			if (slot >= PartyPackets.HUD_MAX_MEMBERS) break;
			if (!sample && (member.id().equals(mc.player.getUUID()) || partyIds == null || !partyIds.contains(member.id()))) continue;
			MemberView view = VIEWS.computeIfAbsent(member.id(), id -> new MemberView());
			if (view.slot.value() < 0.0f || view.appear.value() <= 0.01f) view.slot.snap(slot);
			if (preview) view.appear.snap(1.0f);
			view.present = true;
			view.member = member;
			view.targetSlot = slot++;
		}
		if (VIEWS.isEmpty()) return;

		float rows = preview ? PartyPackets.HUD_MAX_MEMBERS : ROWS.update(Math.max(1, slot));
		float contentHeight = (rows - 1.0f) * ROW_HEIGHT + ROW_CONTENT_HEIGHT;
		float baseHeight = (PartyPackets.HUD_MAX_MEMBERS - 1.0f) * ROW_HEIGHT + ROW_CONTENT_HEIGHT;
		float startY = BORDER + 1.0f + (baseHeight - contentHeight) / 2.0f;
		float tickTime = mc.player.tickCount + partialTicks;
		float visibleAlpha = box.visible() ? 1.0f : 0.35f;

		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(box.x(), box.y(), 0.0f);
		guiGraphics.pose().scale(box.scale(), box.scale(), 1.0f);

		for (Iterator<MemberView> it = VIEWS.values().iterator(); it.hasNext(); ) {
			MemberView view = it.next();
			float appear = view.appear.update(view.present ? 1.0f : 0.0f);
			if (!view.present && (appear <= 0.01f || preview)) {
				it.remove();
				continue;
			}
			float row = view.slot.update(view.targetSlot);

			guiGraphics.pose().pushPose();
			float slide = (1.0f - appear) * 40.0f;
			guiGraphics.pose().translate(BORDER + (box.mirrored() ? slide : -slide), startY + row * ROW_HEIGHT, 0.0f);
			HudRender.setAlphaScale(appear * visibleAlpha);
			try {
				drawMember(guiGraphics, mc, view, appear, partialTicks, tickTime, box.mirrored());
			} finally {
				HudRender.setAlphaScale(1.0f);
				guiGraphics.pose().popPose();
			}
		}
		guiGraphics.pose().popPose();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static void drawMember(GuiGraphics guiGraphics, Minecraft mc, MemberView view, float appear, float partialTicks, float tickTime, boolean mirrored) {
		PartyPackets.HudMember member = view.member;
		Player entity = mc.level.getPlayerByUUID(member.id());
		StatsData data = entity != null ? StatsProvider.get(StatsCapability.INSTANCE, entity).orElse(null) : null;

		float health = member.health();
		float maxHealth = member.maxHealth();
		float energy = member.energy();
		float maxEnergy = member.maxEnergy();
		int kiColor = DEFAULT_KI_COLOR;
		if (entity != null) {
			health = entity.getHealth();
			maxHealth = (float) entity.getAttributeValue(Attributes.MAX_HEALTH);
			if (data != null && data.getStatus().isHasCreatedCharacter()) {
				HudPlayerState state = HudPlayerState.of(entity, data);
				energy = state.energy();
				maxEnergy = state.maxEnergy();
				kiColor = state.auraColor();
			}
		}
		view.health.update(health, maxHealth);
		view.energy.update(energy, maxEnergy);
		boolean down = health <= 0.0f || (data != null && data.getStatus().isKnockedDown());

		float rowWidth = HudLayout.baseSize(HudStyle.current(), HudElement.PARTY)[0] - BORDER * 2.0f;
		float headX = mirrored ? rowWidth - HEAD : 0.0f;
		float barX = mirrored ? headX - BORDER - HP_WIDTH : BAR_X;
		view.health.setMirrored(mirrored);
		view.energy.setMirrored(mirrored);
		float outer = HEAD + BORDER * 2.0f;
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(headX, 0.0f, 0.0f);
		HudRender.sprite(guiGraphics, HudSprites.PARTY_HEAD_BACK, -BORDER, -BORDER, outer, outer, 0xFFFFFF, 1.0f);
		HudRender.sprite(guiGraphics, HudSprites.PARTY_HEAD_TINT, -BORDER, -BORDER, outer, outer, kiColor, 1.0f);
		if (appear > 0.6f) {
			if (entity instanceof AbstractClientPlayer clientPlayer) {
				HeadPortraitRenderer.render(guiGraphics, clientPlayer, 0.0f, 0.0f, HEAD, partialTicks, mirrored);
			} else if (mc.getConnection() != null) {
				PlayerInfo info = mc.getConnection().getPlayerInfo(member.id());
				if (info != null) PlayerFaceRenderer.draw(guiGraphics, info.getSkinLocation(), 2, 2, (int) HEAD - 4);
			}
		}
		if (down) {
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(0.0f, 0.0f, 300.0f);
			HudRender.rect(guiGraphics, 0.0f, 0.0f, HEAD, HEAD, HudRender.argb(0.55f, 0x7A0000));
			guiGraphics.pose().popPose();
		}
		guiGraphics.pose().popPose();

		String name = member.name();
		float nameWidth = mc.font.width(name) * 0.75f;
		float nameX = mirrored ? barX + HP_WIDTH - 2.0f : barX + 2.0f;
		HudRender.text(guiGraphics, name, nameX, -1.0f, 0.75f, mirrored ? 1.0f : 0.0f, down ? 0xFF7A7A : 0xFFFFFF, 1.0f);
		if (entity == null) {
			String where = member.sameDimension() ? Math.round(member.distance()) + "m" : "?";
			float whereX = mirrored ? nameX - nameWidth - 4.0f : nameX + nameWidth + 4.0f;
			HudRender.text(guiGraphics, where, whereX, -1.0f, 0.75f, mirrored ? 1.0f : 0.0f, 0xA8B0C0, 1.0f);
		}

		float kiY = HEAD - KI_HEIGHT;
		float hpY = kiY - BORDER - HP_HEIGHT;
		float seconds = (System.nanoTime() / 1_000_000L % 3_600_000L) / 1000.0f;
		float lowHealth = !down && view.health.targetFraction() < 0.25f ? 0.5f + 0.5f * (float) Math.sin(seconds * 7.0f) : 0.0f;

		view.health.draw(guiGraphics, HudSprites.PARTY_HP, barX, hpY, HP_WIDTH, HP_HEIGHT, HudPlayerState.healthColor(view.health.fraction()), lowHealth);
		view.energy.draw(guiGraphics, HudSprites.PARTY_KI, barX, kiY, KI_WIDTH, KI_HEIGHT, kiColor, 0.0f);
		if (!ConfigManager.getUserConfig().getHideHudNumbers()) view.health.drawValue(guiGraphics, mirrored ? barX - BORDER - 2.0f : barX + HP_WIDTH + BORDER + 2.0f, hpY, 0.75f, mirrored ? 1.0f : 0.0f, tickTime);
	}

	private static final class MemberView {
		private final HudBar health = new HudBar(HudStatNumberAnimator.StatKind.KISENSE_HEALTH);
		private final HudBar energy = new HudBar(HudStatNumberAnimator.StatKind.KI);
		private final HudSmoother appear = new HudSmoother(0.14f, 0.01f);
		private final HudSmoother slot = new HudSmoother(0.12f, 0.01f);
		private PartyPackets.HudMember member;
		private boolean present;
		private int targetSlot;

		private MemberView() {
			slot.snap(-1.0f);
			appear.snap(0.0f);
		}
	}
}
