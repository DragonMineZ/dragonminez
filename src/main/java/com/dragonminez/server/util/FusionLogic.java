package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import com.dragonminez.common.util.CuriosUtil;

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

		int lvl1 = lData.getStats().getTotalStats();
		int lvl2 = pData.getStats().getTotalStats();

		double threshold = ConfigManager.getServerConfig().getGameplay().getMetamoruFusionThreshold();
		if (threshold > 0) {
			double diff = (double) Math.abs(lvl1 - lvl2) / Math.max(lvl1, lvl2);
			if (diff > threshold) {
				leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.level_gap"), true);
				return false;
			}
		}

		if (lData.getStatus().isFused() || pData.getStatus().isFused() ||
				lData.getStatus().getFusionPartnerUUID() != null || pData.getStatus().getFusionPartnerUUID() != null) return false;

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
		if (lData.getStatus().isFused() || pData.getStatus().isFused() ||
				lData.getStatus().getFusionPartnerUUID() != null || pData.getStatus().getFusionPartnerUUID() != null) return;

		int lvl1 = lData.getStats().getTotalStats();
		int lvl2 = pData.getStats().getTotalStats();

		DMZEvent.FusionEvent event = new DMZEvent.FusionEvent(leader, partner, DMZEvent.FusionEvent.FusionType.POTHALA);
		if (MinecraftForge.EVENT_BUS.post(event)) return;

		ItemStack leaderHeadTech = CuriosUtil.getFirstStack(leader, "head_tech");
		boolean isGreenPothala = !leaderHeadTech.isEmpty() && leaderHeadTech.getItem().getDescriptionId().contains("green");

		lData.getStatus().setPothalaColor(isGreenPothala ? "green" : "yellow");
		pData.getStatus().setPothalaColor(isGreenPothala ? "green" : "yellow");

		int FUSION_DURATION = ConfigManager.getServerConfig().getGameplay().getFusionDurationSeconds() * 20;

		applyFusion(leader, partner, lData, pData, "POTHALA", lvl1, lvl2);
		lData.getStatus().setFusionTimer(FUSION_DURATION);
		leader.addEffect(new MobEffectInstance(MainEffects.FUSED.get(), FUSION_DURATION, 0, false, false));
		partner.addEffect(new MobEffectInstance(MainEffects.FUSED.get(), FUSION_DURATION, 0, false, false));
		leader.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", partner.getDisplayName()), true);
		partner.displayClientMessage(Component.translatable("message.dragonminez.fusion.success", leader.getDisplayName()), true);
		leader.level().playSound(null, leader.getX(), leader.getY(), leader.getZ(), MainSounds.FUSION.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
		damageEarring(leader);
		damageEarring(partner);
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

		String fusionName = buildFusionName(leader.getGameProfile().getName(), partner.getGameProfile().getName(), type);
		lData.getStatus().setFusionName(fusionName);
		pData.getStatus().setFusionName(fusionName);

		partner.setGameMode(GameType.SPECTATOR);
		partner.teleportTo(leader.getX(), leader.getY(), leader.getZ());
		partner.startRiding(leader, true);

		refreshNames(leader);
		refreshNames(partner);

		mixAppearance(lData, pData);
		calculateAndApplyStats(lData, pData, type, lvl1, lvl2);
		PartyManager.beginFusionParty(leader, partner);

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(leader), leader);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(partner), partner);
	}

	public static void endFusion(ServerPlayer player, StatsData data, boolean forcedByDeath) {
		if (!data.getStatus().isFused() && data.getStatus().getFusionPartnerUUID() == null) return;

		boolean isLeader = data.getStatus().isFusionLeader();
		UUID partnerUUID = data.getStatus().getFusionPartnerUUID();

		ServerPlayer otherPlayer = partnerUUID != null ? player.getServer().getPlayerList().getPlayer(partnerUUID) : null;
		ServerPlayer leaderRef = isLeader ? player : otherPlayer;
		ServerPlayer partnerRef = isLeader ? otherPlayer : player;

		StatsData leaderData = isLeader ? data : (leaderRef != null ? StatsProvider.get(StatsCapability.INSTANCE, leaderRef).orElse(null) : null);
		StatsData partnerData = !isLeader ? data : (partnerRef != null ? StatsProvider.get(StatsCapability.INSTANCE, partnerRef).orElse(null) : null);

		if (leaderData != null) {
			leaderData.getBonusStats().removeAllBonuses("FusionBonus");

			CompoundTag original = leaderData.getStatus().getOriginalAppearance();
			if (original != null && !original.isEmpty()) leaderData.getCharacter().loadAppearance(original);

			int fusionCdTicks = ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20;
			boolean appliesCd = "METAMORU".equals(leaderData.getStatus().getFusionType()) || !forcedByDeath;
			if (appliesCd) leaderData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, fusionCdTicks);

			clearFusionState(leaderData);

			if (leaderRef != null) {
				if (leaderRef.hasEffect(MainEffects.FUSED.get())) leaderRef.removeEffect(MainEffects.FUSED.get());
				if (appliesCd) leaderRef.addEffect(new MobEffectInstance(MainEffects.FUSION_CD.get(), fusionCdTicks, 0, false, false, true));
				PartyManager.endFusionParty(leaderRef);
				refreshNames(leaderRef);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(leaderRef), leaderRef);
			}
		}

		if (partnerData != null) {
			int fusionCdTicks = ConfigManager.getServerConfig().getGameplay().getFusionCooldownSeconds() * 20;
			boolean appliesCd = "METAMORU".equals(partnerData.getStatus().getFusionType());
			if (appliesCd) partnerData.getCooldowns().addCooldown(Cooldowns.FUSION_CD, fusionCdTicks);

			clearFusionState(partnerData);

			if (partnerRef != null) {
				partnerRef.stopRiding();
				partnerRef.setGameMode(GameType.SURVIVAL);
				if (partnerRef.hasEffect(MainEffects.FUSED.get())) partnerRef.removeEffect(MainEffects.FUSED.get());
				if (appliesCd) partnerRef.addEffect(new MobEffectInstance(MainEffects.FUSION_CD.get(), fusionCdTicks, 0, false, false, true));
				PartyManager.endFusionParty(partnerRef);
				refreshNames(partnerRef);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(partnerRef), partnerRef);
			}
		}
	}

	private static void clearFusionState(StatsData data) {
		data.getStatus().setFused(false);
		data.getStatus().setFusionLeader(false);
		data.getStatus().setFusionPartnerUUID(null);
		data.getStatus().setFusionTimer(0);
		data.getStatus().setFusionName("");
		data.getStatus().setFusionType("");
		data.getStatus().setOriginalAppearance(new CompoundTag());
	}

	private static void calculateAndApplyStats(StatsData l, StatsData p, String type, int lvl1, int lvl2) {
		double ratio = (double) Math.min(lvl1, lvl2) / Math.max(lvl1, lvl2);

		double minMult, maxMult;
		if ("POTHALA".equals(type)) {
			minMult = 1.75; maxMult = 2.5;
		} else {
			minMult = 1.25; maxMult = 2.0;
		}

		double finalMult = minMult + (ratio * (maxMult - minMult));
		String[] statsToBoost = ConfigManager.getServerConfig().getGameplay().getFusionBoosts();

		for (String stat : statsToBoost) {
			int partnerStatValue = getStatValue(p, stat);
			l.getBonusStats().addBonusSplit(stat, "FusionBonus", "+", partnerStatValue * finalMult, true);
		}
	}

	private static int getStatValue(StatsData data, String statName) {
		return switch (statName) {
			case "STR" -> data.getStats().getStrength();
			case "SKP" -> data.getStats().getStrikePower();
			case "RES" -> data.getStats().getResistance();
			case "VIT" -> data.getStats().getVitality();
			case "PWR" -> data.getStats().getKiPower();
			case "ENE" -> data.getStats().getEnergy();
			default -> 0;
		};
	}

	private static void refreshNames(ServerPlayer player) {
		if (player == null) return;
		player.refreshDisplayName();
		player.refreshTabListName();
	}

	private static String buildFusionName(String leaderName, String partnerName, String type) {
		if (leaderName == null || leaderName.isEmpty()) return partnerName;
		if (partnerName == null || partnerName.isEmpty()) return leaderName;

		int cmp = Character.toLowerCase(leaderName.charAt(0)) - Character.toLowerCase(partnerName.charAt(0));
		if (cmp == 0) cmp = leaderName.compareToIgnoreCase(partnerName);
		String alphaFirst = cmp <= 0 ? leaderName : partnerName;
		String alphaSecond = cmp <= 0 ? partnerName : leaderName;

		String first;
		String second;
		if ("POTHALA".equals(type)) {
			first = alphaSecond;
			second = alphaFirst;
		} else {
			first = alphaFirst;
			second = alphaSecond;
		}

		String head = first.substring(0, (first.length() + 1) / 2);
		String tail = second.substring((second.length() + 1) / 2);
		return head + tail;
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

	private static void damageEarring(ServerPlayer player) {
		ItemStack stack = CuriosUtil.getFirstStack(player, "head_tech");
		if (!stack.isEmpty() && stack.getItem().getDescriptionId().contains("pothala")) {
			stack.hurtAndBreak(1, player, (entity) -> {});
		}
	}

	// Fully breaks the worn pothala earring (if any), so the two can't immediately re-fuse by proximity.
	public static void breakPothala(ServerPlayer player) {
		ItemStack stack = CuriosUtil.getFirstStack(player, "head_tech");
		if (!stack.isEmpty() && stack.getItem().getDescriptionId().contains("pothala")) {
			stack.hurtAndBreak(stack.getMaxDamage() + 1, player, (entity) -> {});
		}
	}
}
