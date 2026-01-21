package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.UUID;

public class FusionLogic {
	public static final int FUSION_DURATION = ConfigManager.getServerConfig().getGameplay().getFusionDurationSeconds() * 20;

	public static boolean executeMetamoru(ServerPlayer leader, ServerPlayer partner, StatsData lData, StatsData pData) {
		if (!lData.getCharacter().getRaceName().equals(pData.getCharacter().getRaceName())) {
			leader.sendSystemMessage(Component.translatable("message.dragonminez.fusion.different_race"));
			return false;
		}

		int lvl1 = getPlayerPowerLevel(lData);
		int lvl2 = getPlayerPowerLevel(pData);

		double threshold = ConfigManager.getServerConfig().getGameplay().getMetamoruFusionThreshold();
		if (threshold > 0) {
			double diff = (double) Math.abs(lvl1 - lvl2) / Math.max(lvl1, lvl2);
			if (diff > threshold) {
				leader.sendSystemMessage(Component.translatable("message.dragonminez.fusion.level_gap"));
				return false;
			}
		}

		DMZEvent.FusionEvent event = new DMZEvent.FusionEvent(leader, partner, DMZEvent.FusionEvent.FusionType.METAMORU);
		if (MinecraftForge.EVENT_BUS.post(event)) return false;
		applyFusion(leader, partner, lData, pData, "METAMORU", lvl1, lvl2);
		lData.getStatus().setFusionTimer(FUSION_DURATION);
		leader.sendSystemMessage(Component.translatable("message.dragonminez.fusion.metamoru.success"));
		return true;
	}

	public static void executePothala(ServerPlayer leader, ServerPlayer partner, StatsData lData, StatsData pData) {
		int lvl1 = getPlayerPowerLevel(lData);
		int lvl2 = getPlayerPowerLevel(pData);

		DMZEvent.FusionEvent event = new DMZEvent.FusionEvent(leader, partner, DMZEvent.FusionEvent.FusionType.POTHALA);
		if (MinecraftForge.EVENT_BUS.post(event)) return;
		applyFusion(leader, partner, lData, pData, "POTHALA", lvl1, lvl2);
		lData.getStatus().setFusionTimer(FUSION_DURATION);
		leader.sendSystemMessage(Component.translatable("message.dragonminez.fusion.pothala.success"));
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

		NetworkHandler.sendToPlayer(new StatsSyncS2C(leader), leader);
		NetworkHandler.sendToPlayer(new StatsSyncS2C(partner), partner);
	}

	public static void endFusion(ServerPlayer player, StatsData data, boolean forcedByDeath) {
		if (!data.getStatus().isFused()) return;

		boolean isLeader = data.getStatus().isFusionLeader();
		UUID partnerUUID = data.getStatus().getFusionPartnerUUID();
		ServerPlayer partner = player.getServer().getPlayerList().getPlayer(partnerUUID);

		ServerPlayer leaderRef = isLeader ? player : partner;
		StatsData leaderData = isLeader ? data : (partner != null ? StatsProvider.get(StatsCapability.INSTANCE, partner).orElse(null) : null);

		if (leaderData != null) {
			leaderData.getBonusStats().removeAllBonuses("FusionBonus");

			leaderData.getStatus().setFused(false);
			leaderData.getStatus().setFusionLeader(false);
			leaderData.getStatus().setFusionPartnerUUID(null);
			leaderData.getStatus().setFusionTimer(0);

			if (leaderData.getStatus().getOriginalAppearance() != null) {
				leaderData.getCharacter().loadAppearance(leaderData.getStatus().getOriginalAppearance());
			}

			if ("METAMORU".equals(leaderData.getStatus().getFusionType()) || !forcedByDeath) {
				leaderData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20);
			}

			if ("POTHALA".equals(leaderData.getStatus().getFusionType())) {
				removeEarring(leaderRef);
			}
			PartyManager.leaveParty(leaderRef);

			NetworkHandler.sendToPlayer(new StatsSyncS2C(leaderRef), leaderRef);
		}

		if (partner != null) {
			StatsProvider.get(StatsCapability.INSTANCE, partner).ifPresent(pData -> {
				pData.getStatus().setFused(false);
				pData.getStatus().setFusionLeader(false);
				pData.getStatus().setFusionPartnerUUID(null);

				if ("METAMORU".equals(pData.getStatus().getFusionType())) {
					pData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20);
				}

				partner.stopRiding();
				partner.setGameMode(GameType.SURVIVAL);
				partner.teleportTo(leaderRef.getX() + 1, leaderRef.getY(), leaderRef.getZ() + 1);

				if (forcedByDeath && "POTHALA".equals(pData.getStatus().getFusionType())) {
					removeEarring(partner);
				}
				PartyManager.leaveParty(partner);

				NetworkHandler.sendToPlayer(new StatsSyncS2C(partner), partner);
			});
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
		if (head.getItem().getDescriptionId().contains("pothala")) {
			Inventory inv = player.getInventory();
			inv.setItem(39, ItemStack.EMPTY);
		}
	}
}