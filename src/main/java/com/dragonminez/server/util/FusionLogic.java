package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.UUID;

public class FusionLogic {
	public static boolean executeMetamoru(ServerPlayer leader, ServerPlayer partner, StatsData lData, StatsData pData) {
		if (lData.getStatus().isAndroidUpgraded() || pData.getStatus().isAndroidUpgraded()) {
			leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.android_cannot_fuse"), true);
			return false;
		}

		if (!lData.getCharacter().getRaceName().equals(pData.getCharacter().getRaceName())) {
			leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.different_race"), true);
			return false;
		}

		int lvl1 = getPlayerPowerLevel(lData);
		int lvl2 = getPlayerPowerLevel(pData);

		double threshold = ConfigManager.getServerConfig().getGameplay().getMetamoruFusionThreshold();
		if (threshold > 0) {
			double diff = (double) Math.abs(lvl1 - lvl2) / Math.max(lvl1, lvl2);
			if (diff > threshold) {
				leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.level_gap"), true);
				return false;
			}
		}

		int fusionlvl1 = lData.getSkills().getSkillLevel("fusion");
		int fusionlvl2 = pData.getSkills().getSkillLevel("fusion");
		int fusionProm = (fusionlvl1 + fusionlvl2) / 2;
		int FUSION_DURATION = ConfigManager.getServerConfig().getGameplay().getFusionDurationSeconds() * 20;

		int durationPerLevel = FUSION_DURATION / lData.getSkills().getMaxSkillLevel("fusion");
		int finalDuration = durationPerLevel * fusionProm;

		DMZEvent.FusionEvent event = new DMZEvent.FusionEvent(leader, partner, DMZEvent.FusionEvent.FusionType.METAMORU);
		if (MinecraftForge.EVENT_BUS.post(event)) return false;
		applyFusion(leader, partner, lData, pData, "METAMORU", lvl1, lvl2);
		lData.getStatus().setFusionTimer(finalDuration);
		leader.addEffect(
				new MobEffectInstance(
						MainEffects.FUSED.get(),
						finalDuration,
						0,
						false,
						false
				)
		);
		partner.addEffect(
				new MobEffectInstance(
						MainEffects.FUSED.get(),
						finalDuration,
						0,
						false,
						false
				)
		);
		leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", partner.getDisplayName()),true);
		partner.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", leader.getDisplayName()), true);
		return true;
	}

	public static void executePothala(ServerPlayer leader, ServerPlayer partner, StatsData lData, StatsData pData) {
		int lvl1 = getPlayerPowerLevel(lData);
		int lvl2 = getPlayerPowerLevel(pData);

		DMZEvent.FusionEvent event = new DMZEvent.FusionEvent(leader, partner, DMZEvent.FusionEvent.FusionType.POTHALA);
		if (MinecraftForge.EVENT_BUS.post(event)) return;

		boolean isGreenPothala = leader.getItemBySlot(EquipmentSlot.HEAD).getItem().getDescriptionId().contains("green");
		lData.getStatus().setPothalaColor(isGreenPothala ? "green" : "yellow");
		pData.getStatus().setPothalaColor(isGreenPothala ? "green" : "yellow");

		removeEarring(leader);
		removeEarring(partner);
		int FUSION_DURATION = ConfigManager.getServerConfig().getGameplay().getFusionDurationSeconds() * 20;

		applyFusion(leader, partner, lData, pData, "POTHALA", lvl1, lvl2);
		lData.getStatus().setFusionTimer(FUSION_DURATION);
		leader.addEffect(new MobEffectInstance(MainEffects.FUSED.get(), FUSION_DURATION, 0, false, false));
		partner.addEffect(new MobEffectInstance(MainEffects.FUSED.get(), FUSION_DURATION, 0, false, false));
		leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", partner.getDisplayName()), true);
		partner.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", leader.getDisplayName()), true);
	}

	private static void applyFusion(ServerPlayer leader, ServerPlayer partner, StatsData lData, StatsData pData, String type, int lvl1, int lvl2) {
		CompoundTag original = new CompoundTag();
		lData.getCharacter().saveAppearance(original);
		lData.getStatus().setOriginalAppearance(original);

		lData.getStatus().setFused(true);
		lData.getStatus().setFusionLeader(true);
		lData.getStatus().setFusionPartnerUUID(partner.getUUID());
		lData.getStatus().setFusionType(type);

		pData.getStatus().setFused(true);
		pData.getStatus().setFusionLeader(false);
		pData.getStatus().setFusionPartnerUUID(leader.getUUID());
		pData.getStatus().setFusionType(type);

		partner.setGameMode(GameType.SPECTATOR);
		partner.teleportTo(leader.getX(), leader.getY(), leader.getZ());
		partner.startRiding(leader, true);

		mixAppearance(lData, pData);
		calculateAndApplyStats(lData, pData, type, lvl1, lvl2);
		PartyManager.forceJoinParty(leader, partner);

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(leader), leader);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(partner), partner);
	}

	public static void endFusion(ServerPlayer player, StatsData data, boolean forcedByDeath) {
		boolean isLeader = data.getStatus().isFusionLeader();
		UUID partnerUUID = data.getStatus().getFusionPartnerUUID();

		ServerPlayer otherPlayer = partnerUUID != null ? player.getServer().getPlayerList().getPlayer(partnerUUID) : null;
		ServerPlayer leaderRef = isLeader ? player : otherPlayer;
		ServerPlayer partnerRef = isLeader ? otherPlayer : player;

		StatsData leaderData = null;
		if (leaderRef != null) {
			if (isLeader) leaderData = data;
			else leaderData = StatsProvider.get(StatsCapability.INSTANCE, leaderRef).orElse(null);
		}

		StatsData partnerData = null;
		if (partnerRef != null) {
			if (!isLeader) partnerData = data;
			else partnerData = StatsProvider.get(StatsCapability.INSTANCE, partnerRef).orElse(null);
		}

		if (leaderRef != null && leaderData != null) {
			leaderData.getBonusStats().removeAllBonuses("FusionBonus");

			leaderData.getStatus().setFused(false);
			leaderData.getStatus().setFusionLeader(false);
			leaderData.getStatus().setFusionPartnerUUID(null);
			leaderData.getStatus().setFusionTimer(0);

			if (leaderData.getStatus().getOriginalAppearance() != null) leaderData.getCharacter().loadAppearance(leaderData.getStatus().getOriginalAppearance());
			if ("METAMORU".equals(leaderData.getStatus().getFusionType()) || !forcedByDeath) leaderData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20);
			if (leaderRef.hasEffect(MainEffects.FUSED.get())) leaderRef.removeEffect(MainEffects.FUSED.get());
			PartyManager.leaveParty(leaderRef);
			leaderData.getStatus().setFusionPartnerUUID(null);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(leaderRef), leaderRef);
		}

		if (partnerRef != null && partnerData != null) {
			partnerData.getStatus().setFused(false);
			partnerData.getStatus().setFusionLeader(false);
			partnerData.getStatus().setFusionPartnerUUID(null);
			partnerData.getStatus().setFusionTimer(0);

			if ("METAMORU".equals(partnerData.getStatus().getFusionType())) partnerData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20);

			partnerRef.stopRiding();
			partnerRef.setGameMode(GameType.SURVIVAL);
			if (partnerRef.hasEffect(MainEffects.FUSED.get())) partnerRef.removeEffect(MainEffects.FUSED.get());
			PartyManager.leaveParty(partnerRef);
			partnerData.getStatus().setFusionPartnerUUID(null);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(partnerRef), partnerRef);
		}
	}

	private static void calculateAndApplyStats(StatsData l, StatsData p, String type, int lvl1, int lvl2) {
		double ratio = (double) Math.min(lvl1, lvl2) / Math.max(lvl1, lvl2);

		double minMult, maxMult;
		if ("POTHALA".equals(type)) {
			minMult = 2.0; maxMult = 3.0;
		} else {
			minMult = 1.25; maxMult = 2.0;
		}

		double finalMult = minMult + (ratio * (maxMult - minMult));

		l.getBonusStats().addBonus("STR", "FusionBonus", "+", p.getStats().getStrength() * finalMult);
		l.getBonusStats().addBonus("SKP", "FusionBonus", "+", p.getStats().getStrikePower() * finalMult);
		l.getBonusStats().addBonus("PWR", "FusionBonus", "+", p.getStats().getKiPower() * finalMult);
	}

	private static void mixAppearance(StatsData l, StatsData p) {
		l.getCharacter().setBodyColor(mixHex(l.getCharacter().getBodyColor(), p.getCharacter().getBodyColor()));
		l.getCharacter().setHairColor(mixHex(l.getCharacter().getHairColor(), p.getCharacter().getHairColor()));
		l.getCharacter().setAuraColor(mixHex(l.getCharacter().getAuraColor(), p.getCharacter().getAuraColor()));
		l.getCharacter().setEye2Color(p.getCharacter().getEye1Color());
	}

	private static String mixHex(String c1, String c2) {
		try {
			if (c1.startsWith("#")) c1 = c1.substring(1);
			if (c2.startsWith("#")) c2 = c2.substring(1);
			int rgb1 = Integer.parseInt(c1, 16);
			int rgb2 = Integer.parseInt(c2, 16);

			Color color1 = new Color(rgb1);
			Color color2 = new Color(rgb2);

			int r = (color1.getRed() + color2.getRed()) / 2;
			int g = (color1.getGreen() + color2.getGreen()) / 2;
			int b = (color1.getBlue() + color2.getBlue()) / 2;

			return String.format("%02x%02x%02x", r, g, b);
		} catch (Exception e) { return c1; }
	}

	private static int getPlayerPowerLevel(StatsData data) {
		return data.getStats().getTotalStats();
	}

	private static void removeEarring(ServerPlayer player) {
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (head.getItem().getDescriptionId().contains("pothala")) player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
	}
}
