package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.namek.NamekTraderEntity;
import com.dragonminez.common.init.entities.namek.NamekWarriorEntity;
import com.dragonminez.common.stats.Cooldowns;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class RacialSkillLogic {

	public static void attemptRacialAction(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			String race = data.getCharacter().getRaceName();
			LivingEntity target = getTargetEntity(player, 3.0);
			RaceCharacterConfig config = ConfigManager.getRaceCharacter(race);

			if (target == null) return;
			if (!canOverpowerTarget(player, data, target) && !race.equals("bioandroid") && !player.isCreative() && !(target instanceof MastersEntity) && !(target instanceof PunchMachineEntity)) {
				player.displayClientMessage(Component.translatable("message.dragonminez.racial.target_too_strong"), true);
				return;
			}

			if (config != null) switch (config.getRacialSkill()) {
				case "namekian" -> handleNamekianAssimilation(player, data, target);
				case "majin" -> handleMajinAbsorption(player, data, target);
				case "bioandroid" -> handleBioAndroidDrain(player, data, target);
				default -> {}
			}

		});
	}

	private static void handleNamekianAssimilation(ServerPlayer player, StatsData data, LivingEntity target) {
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();

		if (!config.getNamekianRacialSkill()) return;

		if (data.getResources().getRacialSkillCount() >= config.getNamekianAssimilationAmount()) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
			return;
		}

		boolean isValidTarget = false;

		if (target instanceof ServerPlayer targetPlayer) {
			AtomicBoolean isNamek = new AtomicBoolean(false);
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(tData ->
					isNamek.set(tData.getCharacter().getRaceName().equals("namekian")));
			isValidTarget = isNamek.get();
		} else if (config.getNamekianAssimilationOnNamekNpcs()) {
			isValidTarget = (target instanceof NamekWarriorEntity || target instanceof NamekTraderEntity);
		}

		if (!isValidTarget) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.invalid_target"), true);
			return;
		}

		double boostMult = config.getNamekianAssimilationStatBoost();
		String[] statsToBoost = config.getNamekianAssimilationBoosts();

		for (String statKey : statsToBoost) {
			int currentStat = getStatValue(data, statKey);
			int bonus = (int) Math.max(1, currentStat * boostMult);
			data.getBonusStats().addBonus(statKey, "Assimilation_" + (data.getResources().getRacialSkillCount() + 1), "+", bonus);
		}

		finalizeKill(player, data, target, config.getNamekianAssimilationHealthRegen());
		data.getResources().addRacialSkillCount(1);
		player.displayClientMessage(Component.translatable("message.dragonminez.racial.namek.success"), true);
	}

	private static void handleMajinAbsorption(ServerPlayer player, StatsData data, LivingEntity target) {
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();

		if (!config.getMajinAbsoprtionSkill()) return;

		if (data.getResources().getRacialSkillCount() >= config.getMajinAbsorptionAmount()) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
			return;
		}

		double ratio = config.getMajinAbsorptionStatCopy();
		boolean success = false;

		if (target instanceof ServerPlayer targetPlayer) {
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
				String[] stats = config.getMajinAbsorptionBoosts();
				for (String stat : stats) {
					int targetStatVal = getStatValue(targetData, stat);
					int bonus = (int) Math.max(1, targetStatVal * ratio);
					data.getBonusStats().addBonus(stat, "Absorption_" + (data.getResources().getRacialSkillCount() + 1), "+", bonus);
				}
			});
			success = true;
		} else if (target instanceof Mob && config.getMajinAbsorptionOnMobs()) {
			float maxHp = target.getMaxHealth();
			int bonus = (int) Math.max(1, maxHp * ratio);
			String[] mobBonuses = config.getMajinAbsorptionBoosts();

			for (String stat : mobBonuses) {
				data.getBonusStats().addBonus(stat, "Absorption_" + (data.getResources().getRacialSkillCount() + 1), "+", bonus);
			}
			success = true;
		}

		if (success) {
			finalizeKill(player, data, target, config.getMajinAbsorptionHealthRegen());
			data.getResources().addRacialSkillCount(1);
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.majin.success"), true);
		}
	}

	private static void handleBioAndroidDrain(ServerPlayer player, StatsData data, LivingEntity target) {
		GeneralServerConfig.RacialSkillsConfig config = ConfigManager.getServerConfig().getRacialSkills();

		if (!config.getBioAndroidRacialSkill()) return;
		if (target instanceof MastersEntity) return;

		if (data.getCooldowns().hasCooldown(Cooldowns.DRAIN)) {
			int secondsLeft = data.getCooldowns().getCooldown(Cooldowns.DRAIN);
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.cooldown", secondsLeft), true);
			return;
		}

		int duration = 120;

		teleportBehindTarget(player, target);
		target.addEffect(
				new MobEffectInstance(
						MainEffects.STUN.get(),
						duration,
						0,
						false,
						false,
						true
				)
		);
		player.addEffect(
				new MobEffectInstance(
						MainEffects.STUN.get(),
						duration,
						0,
						false,
						false,
						true
				)
		);
		data.getStatus().setDrainingTargetId(target.getId());
		data.getCooldowns().addCooldown(Cooldowns.DRAIN_ACTIVE, duration);
		data.getCooldowns().addCooldown(Cooldowns.DRAIN, config.getBioAndroidCooldownSeconds() * 20);
		player.addEffect(
				new MobEffectInstance(
						MainEffects.BIOANDROID_PASSIVE.get(),
						config.getBioAndroidCooldownSeconds() * 20,
						0,
						false,
						false,
						true
				)
		);
		player.playSound(MainSounds.TP_SHORT.get());
		target.playSound(MainSounds.TP_SHORT.get());
	}

	private static void teleportBehindTarget(ServerPlayer player, LivingEntity target) {
		Vec3 targetPos = target.position();
		Vec3 lookVec = target.getLookAngle().normalize();

		Vec3 behindPos = targetPos.add(lookVec.scale(-0.8));

		player.teleportTo(behindPos.x, target.getY(), behindPos.z);
		player.setYRot(target.getYRot());
		player.setXRot(target.getXRot());

		player.connection.teleport(behindPos.x, target.getY(), behindPos.z, target.getYRot(), target.getXRot());
	}

	private static boolean canOverpowerTarget(ServerPlayer player, StatsData playerData, LivingEntity target) {
		double maxDmg = Math.max(playerData.getMaxMeleeDamage(), Math.max(playerData.getMaxStrikeDamage(), playerData.getMaxKiDamage()));
		if (target.getHealth() > maxDmg) return false;

		if (target instanceof ServerPlayer targetPlayer) {
			AtomicBoolean levelCheck = new AtomicBoolean(false);
			StatsProvider.get(StatsCapability.INSTANCE, targetPlayer).ifPresent(targetData -> {
				if (targetData.getLevel() < playerData.getLevel()) {
					levelCheck.set(true);
				}
			});
			return levelCheck.get();
		}

		return true;
	}

	private static void finalizeKill(ServerPlayer user, StatsData userData, LivingEntity target, double healRatio) {
		if (healRatio > 0) {
			float heal = (float) (user.getMaxHealth() * healRatio);
			user.heal(heal);
		}
		target.kill();
	}

	private static LivingEntity getTargetEntity(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = start.add(look.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);

		List<Entity> entities = player.level().getEntities(player, searchBox,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable());

		for (Entity entity : entities) {
			AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (entityBox.contains(start) || entityBox.clip(start, end).isPresent()) {
				return (LivingEntity) entity;
			}
		}
		return null;
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
}