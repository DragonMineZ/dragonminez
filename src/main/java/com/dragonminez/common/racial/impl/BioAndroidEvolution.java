package com.dragonminez.common.racial.impl;

import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainEntities;
import com.dragonminez.common.init.MainGameRules;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.PunchMachineEntity;
import com.dragonminez.common.init.entities.bioandroid.CellJrEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.racial.RacialAbility;
import com.dragonminez.common.racial.RacialContext;
import com.dragonminez.common.racial.RacialData;
import com.dragonminez.common.racial.RacialStatUtil;
import com.dragonminez.common.racial.capture.RacialCapture;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BioAndroidEvolution implements RacialAbility {
	private static final int STUN_DURATION_TICKS = 120;
	private static final int CELL_JR_CHARGE_SECONDS = 5;
	private static final float EXPLODE_SURVIVAL_HEALTH_RATIO = 0.01f;
	private static final float CHANNEL_SECONDS = 5.0f;
	private static final float DRAIN_RANGE = 6.0f;

	@Override
	public String id() {
		return "bioandroid";
	}

	@Override
	public boolean hasActiveAction() {
		return true;
	}

	@Override
	public boolean canActivate(RacialContext ctx) {
		StatsData data = ctx.data();
		String tier = resolveTier(data);
		if (tier.equals("perfect")) return canSummonCellJr(ctx, data);
		if (isExplodeSelected(data)) return canArmExplosion(ctx, data);
		return !data.getCooldowns().hasCooldown(Cooldowns.DRAIN);
	}

	@Override
	public int chargeSeconds(RacialContext ctx) {
		StatsData data = ctx.data();
		if (resolveTier(data).equals("perfect")) return CELL_JR_CHARGE_SECONDS;
		if (isExplodeSelected(data)) return ctx.config().getBioandroid().getExplodeChargeSeconds();
		return 0;
	}

	private static boolean isExplodeSelected(StatsData data) {
		return resolveTier(data).equals("semi")
				&& RacialData.BIO_SKILL_EXPLODE.equals(data.getRacialData().getBioSelectedSkill());
	}

	private static boolean canArmExplosion(RacialContext ctx, StatsData data) {
		if (!ctx.config().getBioandroid().getEnabled()) return false;
		if (data.getCooldowns().hasCooldown(Cooldowns.BIO_EXPLODE_CD)) return false;
		return !data.getRacialData().isBioSwellLocked();
	}

	public static boolean isExplosionRecovering(StatsData data) {
		return data.getCooldowns().hasCooldown(Cooldowns.BIO_EXPLODE_RECOVERY);
	}

	@Override
	public boolean onActivate(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		GeneralServerConfig.RacialSkillsConfig config = ctx.config();
		String tier = resolveTier(data);
		if (tier.equals("perfect")) return summonCellJr(ctx, player, data);
		if (isExplodeSelected(data)) {
			triggerExplosion(player, data, config.getBioandroid());
			return true;
		}

		LivingEntity target = RacialCapture.findTarget(player, 3.0);
		if (target == null) return true;
		if (target instanceof MastersEntity || target instanceof PunchMachineEntity) return true;
		if (TargetHelper.getRelation(player, target) == TargetHelper.Relation.FRIENDLY) return true;

		if (!config.getBioAndroidRacialSkill()) return true;

		if (data.getCooldowns().hasCooldown(Cooldowns.DRAIN)) {
			int secondsLeft = data.getCooldowns().getCooldown(Cooldowns.DRAIN) / 20;
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.cooldown", secondsLeft), true);
			return true;
		}

		teleportBehindTarget(player, target);
		target.addEffect(new MobEffectInstance(MainEffects.STUN.get(), STUN_DURATION_TICKS, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MainEffects.STUN.get(), STUN_DURATION_TICKS, 0, false, false, true));
		data.getStatus().setDrainingTargetId(target.getId());
		data.getCooldowns().addCooldown(Cooldowns.DRAIN_ACTIVE, STUN_DURATION_TICKS);
		data.getCooldowns().addCooldown(Cooldowns.DRAIN, config.getBioAndroidCooldownSeconds() * 20);
		player.addEffect(new MobEffectInstance(MainEffects.BIOANDROID_PASSIVE.get(), config.getBioAndroidCooldownSeconds() * 20, 0, false, false, true));
		player.playSound(MainSounds.TP_SHORT.get());
		target.playSound(MainSounds.TP_SHORT.get());
		return true;
	}

	@Override
	public boolean onSecondaryActivate(RacialContext ctx) {
		return resolveTier(ctx.data()).equals("perfect") && dismissAllCellJrs(ctx);
	}

	@Override
	public void onTick(RacialContext ctx) {
		StatsData data = ctx.data();

		tickExplosionSwell(ctx, data);

		ServerPlayer player = ctx.player();
		int targetId = data.getStatus().getDrainingTargetId();
		if (targetId == -1) return;

		if (data.getCooldowns().getCooldown(Cooldowns.DRAIN_ACTIVE) > 0) {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target && target.isAlive() && player.distanceTo(target) < DRAIN_RANGE) {
				float targetYRot = target.getYRot();

				double dist = 0.75;
				double rads = Math.toRadians(targetYRot);
				double xOffset = -Math.sin(rads) * dist;
				double zOffset = Math.cos(rads) * dist;

				double finalX = target.getX() - xOffset;
				double finalZ = target.getZ() - zOffset;
				double finalY = target.getY();

				player.connection.teleport(finalX, finalY, finalZ, targetYRot, 15.0F);
				player.setYRot(targetYRot);
				player.setYHeadRot(targetYRot);
			} else {
				data.getStatus().setDrainingTargetId(-1);
				data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
				player.removeEffect(MainEffects.STUN.get());
			}
		} else {
			Entity entity = player.level().getEntity(targetId);
			if (entity instanceof LivingEntity target) {
				Vec3 look = player.getLookAngle();
				target.setDeltaMovement(look.scale(1.5).add(0, 0.5, 0));
				target.hurtMarked = true;
				player.setDeltaMovement(look.scale(-1.0).add(0, 0.3, 0));
				player.hurtMarked = true;
				target.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
				player.playSound(MainSounds.KNOCKBACK_CHARACTER.get());
			}
			data.getStatus().setDrainingTargetId(-1);
			player.removeEffect(MainEffects.STUN.get());
		}
	}

	@Override
	public void onSecond(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		StatsData data = ctx.data();
		warnExplosionUnavailable(ctx, data);
		int targetId = data.getStatus().getDrainingTargetId();
		if (targetId == -1) return;
		if (data.getCooldowns().getCooldown(Cooldowns.DRAIN_ACTIVE) <= 0) return;

		Entity entity = player.level().getEntity(targetId);
		if (!(entity instanceof LivingEntity target) || !target.isAlive() || player.distanceTo(target) >= DRAIN_RANGE) return;

		GeneralServerConfig.BioAndroidRacialConfig config = ctx.config().getBioandroid();
		double totalDrainRatio = config.getDrainRatio();
		float totalHealthToDrain = (float) (target.getMaxHealth() * totalDrainRatio);
		float drainPerSecond = Math.round(totalHealthToDrain / CHANNEL_SECONDS);

		target.setHealth(Math.max(1, target.getHealth() - drainPerSecond));
		if (player.getHealth() < player.getMaxHealth()) player.heal(drainPerSecond);
		if (data.getResources().getCurrentEnergy() < data.getMaxEnergy()) {
			data.getResources().addEnergy(drainPerSecond * 5);
		}
		target.playSound(MainSounds.ABSORB1.get());
		player.playSound(MainSounds.ABSORB1.get());

		if (target.getHealth() <= 1.0f) {
			target.hurtMarked = true;
			data.getStatus().setDrainingTargetId(-1);
			data.getCooldowns().removeCooldown(Cooldowns.DRAIN_ACTIVE);
			player.removeEffect(MainEffects.STUN.get());

			if (resolveTier(data).equals("imperfect")) grantDrainTp(player, data, target, totalHealthToDrain, config);

			target.kill();
		}
	}

	private static void tickExplosionSwell(RacialContext ctx, StatsData data) {
		RacialData racialData = data.getRacialData();
		float swell = racialData.getBioSwell();

		boolean charging = isExplodeSelected(data)
				&& data.getStatus().isActionCharging()
				&& data.getStatus().getSelectedAction() == ActionMode.RACIAL
				&& canArmExplosion(ctx, data);
		if (!charging && swell <= 0f && !racialData.isBioSwellLocked()) return;

		GeneralServerConfig.BioAndroidRacialConfig config = ctx.config().getBioandroid();
		if (charging) {
			if (swell <= 0f) {
				ctx.player().displayClientMessage(Component.translatable("message.dragonminez.racial.bioandroid.explode_charging"), true);
				ctx.player().level().playSound(null, ctx.player().getX(), ctx.player().getY(), ctx.player().getZ(),
						MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
			}
			swell = Math.min(1f, swell + 1f / Math.max(1, config.getExplodeChargeSeconds() * 20));
		} else {
			if (swell > 0f) racialData.setBioSwellLocked(true);
			swell = Math.max(0f, swell - 1f / Math.max(1, config.getExplodeRevertSeconds() * 20));
			if (swell <= 0f) racialData.setBioSwellLocked(false);
		}
		racialData.setBioSwell(swell);
	}

	private static void warnExplosionUnavailable(RacialContext ctx, StatsData data) {
		if (!isExplodeSelected(data) || !data.getStatus().isActionCharging()) return;
		if (data.getCooldowns().hasCooldown(Cooldowns.BIO_EXPLODE_CD)) {
			int secondsLeft = data.getCooldowns().getCooldown(Cooldowns.BIO_EXPLODE_CD) / 20;
			ctx.player().displayClientMessage(Component.translatable("message.dragonminez.racial.cooldown", secondsLeft), true);
		} else if (data.getRacialData().isBioSwellLocked()) {
			ctx.player().displayClientMessage(Component.translatable("message.dragonminez.racial.bioandroid.explode_reverting"), true);
		}
	}

	private static void grantDrainTp(ServerPlayer player, StatsData data, LivingEntity target, float drainedHealth, GeneralServerConfig.BioAndroidRacialConfig config) {
		double eligibleAmount = DynamicGrowthService.practiceDamageXp(player, target, drainedHealth);

		var growthConfig = ConfigManager.getServerConfig().getDynamicGrowth();
		double repeatMult = data.getDynamicGrowth().recordTargetAndGetMultiplier(
				target.getUUID().toString(), System.currentTimeMillis(),
				growthConfig.getRepeatTargetWindowSeconds(), growthConfig.getRepeatTargetSoftCap(), growthConfig.getRepeatTargetHardCap(),
				growthConfig.getRepeatTargetSoftMultiplier(), growthConfig.getRepeatTargetHardMultiplier());

		int tpGain = (int) Math.round(eligibleAmount * repeatMult * config.getDrainTpRatio());
		if (tpGain > 0) data.getResources().addTrainingPoints(tpGain);
	}

	private static void triggerExplosion(ServerPlayer player, StatsData data, GeneralServerConfig.BioAndroidRacialConfig config) {
		data.getRacialData().setBioSwell(0f);
		data.getRacialData().setBioSwellLocked(false);
		data.getStatus().setActionCharging(false);
		data.getResources().setActionCharge(0);
		data.getCooldowns().setCooldown(Cooldowns.BIO_EXPLODE_CD, config.getExplodeCooldownSeconds() * 20);
		player.addEffect(new MobEffectInstance(MainEffects.BIOANDROID_EXPLODE_CD.get(),
				config.getExplodeCooldownSeconds() * 20, 0, false, false, true));

		Level level = player.level();
		double radius = config.getExplodeRadius();
		Vec3 center = player.position().add(0, player.getBbHeight() / 2.0, 0);

		if (level instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
					center.x, center.y, center.z, 1, 0, 0, 0, 0);
			for (int i = 0; i < 24; i++) {
				double angle = (Math.PI * 2.0 / 24.0) * i;
				double spread = radius * 0.6;
				serverLevel.sendParticles(ParticleTypes.EXPLOSION,
						center.x + Math.cos(angle) * spread, center.y + (i % 3) - 1.0, center.z + Math.sin(angle) * spread,
						1, 0, 0, 0, 0);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.KI_EXPLOSION_IMPACT.get(),
				SoundSource.PLAYERS, 4.0F, 0.8F);

		boolean partyPvpEnabled = PartyManager.isPartyPvpEnabled(player);
		List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius),
				e -> e.isAlive() && !e.isSpectator() && e.distanceTo(player) <= radius);

		for (LivingEntity victim : new ArrayList<>(victims)) {
			if (victim == player) continue;
			if (victim instanceof Player victimPlayer && PartyManager.areInSameParty(player, victimPlayer) && !partyPvpEnabled) continue;

			double distance = victim.distanceTo(player);
			double falloff = Math.max(0.0, 1.0 - (distance / radius));
			double damage = player.getMaxHealth() * config.getExplodeDamageRatio() * falloff;
			if (damage <= 0) continue;

			victim.hurt(MainDamageTypes.kiblast(level, player, player), (float) damage);
		}

		destroyTerrain(level, player, center, (float) radius);

		if (config.getExplodeKillsUser()) {
			player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
		} else {
			int knockdownTicks = config.getExplodeKnockdownSeconds() * 20;
			player.setHealth(Math.max(1.0F, player.getMaxHealth() * EXPLODE_SURVIVAL_HEALTH_RATIO));
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(playerData -> {
				playerData.getStatus().setKnockedDown(true);
				playerData.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, knockdownTicks);
				playerData.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_INVULN, knockdownTicks);
				playerData.getCooldowns().setCooldown(Cooldowns.BIO_EXPLODE_RECOVERY, knockdownTicks);
			});
		}

		NetworkHandler.sendToTrackingEntityAndSelf(
				new StatsSyncS2C(player), player);
	}

	private static void destroyTerrain(Level level, ServerPlayer source, Vec3 center, float radius) {
		int r = Math.round(radius);
		for (int x = -r; x <= r; x++) {
			for (int y = -r; y <= r; y++) {
				for (int z = -r; z <= r; z++) {
					if (x * x + y * y + z * z > radius * radius) continue;
					BlockPos pos = BlockPos.containing(center.x + x, center.y + y, center.z + z);
					if (!MainGameRules.canKiGrief(level, pos, source)) continue;
					if (level.getBlockState(pos).getExplosionResistance(level, pos, null) >= 1200f) continue;
					level.destroyBlock(pos, false);
				}
			}
		}
	}

	public static String resolveTier(StatsData data) {
		String form = data.getCharacter().getActiveForm();
		if (form == null || form.isEmpty() || "base".equalsIgnoreCase(form)) return "imperfect";
		if ("semiperfect".equalsIgnoreCase(form)) return "semi";
		return "perfect";
	}

	private static boolean canSummonCellJr(RacialContext ctx, StatsData data) {
		GeneralServerConfig.BioAndroidRacialConfig config = ctx.config().getBioandroid();
		if (!config.getEnabled()) return false;

		if (effectiveCellJrSlotsUsed(data) >= config.getCellJrMax()) return false;
		return CellJrEntity.globalLiveCount() < config.getCellJrGlobalCap();
	}

	private static int effectiveCellJrSlotsUsed(StatsData data) {
		int slotCooldowns = 0;
		for (var entry : data.getCooldowns().getAllCooldowns().entrySet()) {
			if (entry.getKey().startsWith("CellJrSlot_") && entry.getValue() > 0) slotCooldowns++;
		}
		return data.getRacialData().getCellJrs().size() + slotCooldowns;
	}

	private static boolean summonCellJr(RacialContext ctx, ServerPlayer player, StatsData data) {
		if (!canSummonCellJr(ctx, data)) {
			player.displayClientMessage(Component.translatable("message.dragonminez.racial.limit_reached"), true);
			return true;
		}

		GeneralServerConfig.BioAndroidRacialConfig config = ctx.config().getBioandroid();
		if (!(player.level() instanceof ServerLevel serverLevel)) return true;

		var jr = new CellJrEntity(
				MainEntities.BIO_CELL_JR.get(), serverLevel);
		jr.setOwnerUUID(player.getUUID());
		jr.applyOwnerScaling(player, data, config.getCellJrStatRatio());

		Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0));
		jr.moveTo(spawnPos.x, player.getY(), spawnPos.z, player.getYRot(), 0);
		serverLevel.addFreshEntity(jr);
		jr.markSpawned();

		data.getRacialData().getCellJrs().add(jr.getUUID());
		String bonusName = "CellJr_" + jr.getUUID();
		double penaltyPct = config.getCellJrOwnerPenalty();
		for (String stat : new String[]{"STR", "SKP", "DEF", "STM", "VIT", "PWR", "ENE"}) {
			int currentStat = RacialStatUtil.getStat(data, stat);
			if (currentStat <= 0) continue;
			data.getBonusStats().addBonus(stat, bonusName, "-", Math.max(1, currentStat * penaltyPct), true);
		}
		data.getRacialData().addOwnedBonusName(bonusName);

		player.playSound(MainSounds.TP_SHORT.get());
		NetworkHandler.sendToTrackingEntityAndSelf(
				new StatsSyncS2C(player), player);
		return true;
	}

	private static boolean dismissAllCellJrs(RacialContext ctx) {
		ServerPlayer player = ctx.player();
		if (!(player.level() instanceof ServerLevel serverLevel)) return false;

		for (UUID id : new ArrayList<>(ctx.data().getRacialData().getCellJrs())) {
			if (serverLevel.getEntity(id) instanceof CellJrEntity jr) {
				jr.discard();
			}
		}
		return true;
	}

	public static void onCellJrRemoved(CellJrEntity jr) {
		UUID ownerId = jr.getOwnerUUID();
		if (ownerId == null || !(jr.level() instanceof ServerLevel serverLevel)) return;

		ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null) return;

		StatsProvider.get(StatsCapability.INSTANCE, owner).ifPresent(data -> {
			boolean removed = data.getRacialData().getCellJrs().remove(jr.getUUID());
			if (!removed) return;

			String bonusName = "CellJr_" + jr.getUUID();
			data.getBonusStats().removeAllBonuses(bonusName);
			data.getRacialData().removeOwnedBonusName(bonusName);

			int cooldownSeconds = ConfigManager.getServerConfig().getRacialSkills().getBioandroid().getCellJrChargeCooldownSeconds();
			data.getCooldowns().setCooldown("CellJrSlot_" + jr.getUUID(), cooldownSeconds * 20);
			NetworkHandler.sendToTrackingEntityAndSelf(
					new StatsSyncS2C(owner), owner);
		});
	}

	private static void despawnAllCellJrs(RacialContext ctx) {
		if (!(ctx.player().level() instanceof ServerLevel serverLevel)) return;
		for (UUID id : new ArrayList<>(ctx.data().getRacialData().getCellJrs())) {
			if (serverLevel.getEntity(id) instanceof CellJrEntity jr) {
				jr.discard();
			}
		}
	}

	@Override
	public void onDeath(RacialContext ctx) {
		despawnAllCellJrs(ctx);
	}

	@Override
	public void onLogout(RacialContext ctx) {
		despawnAllCellJrs(ctx);
	}

	@Override
	public void onDimensionChange(RacialContext ctx) {
		despawnAllCellJrs(ctx);
	}

	@Override
	public void onLogin(RacialContext ctx) {
		StatsData data = ctx.data();
		List<UUID> tracked = new ArrayList<>(data.getRacialData().getCellJrs());
		if (tracked.isEmpty()) return;

		ServerPlayer player = ctx.player();
		if (!(player.level() instanceof ServerLevel serverLevel)) return;

		boolean changed = false;
		for (UUID id : tracked) {
			boolean stillAlive = false;
			for (var level : serverLevel.getServer().getAllLevels()) {
				if (level.getEntity(id) instanceof CellJrEntity) {
					stillAlive = true;
					break;
				}
			}
			if (stillAlive) continue;

			data.getRacialData().getCellJrs().remove(id);
			String bonusName = "CellJr_" + id;
			data.getBonusStats().removeAllBonuses(bonusName);
			data.getRacialData().removeOwnedBonusName(bonusName);
			changed = true;
		}

		if (changed) {
			NetworkHandler.sendToTrackingEntityAndSelf(
					new StatsSyncS2C(player), player);
		}
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
}
