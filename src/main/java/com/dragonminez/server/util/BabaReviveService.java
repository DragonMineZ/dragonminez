package com.dragonminez.server.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.server.events.players.KiSurgeService;
import com.dragonminez.server.events.EntitiesEvents;
import com.dragonminez.server.events.QuestEvents;
import com.dragonminez.server.world.dimension.OtherworldDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class BabaReviveService {

	private static final int[] WARNING_SECONDS = {1800, 600, 300, 60, 30, 10, 5};
	public static final int KNOCKOUT_TICKS = 30 * 20;
	public static final int AGGRO_GRACE_TICKS = 15 * 20;
	private static final long RETRY_DAY_TICKS = 24000L;
	private static final double ESCALATION_STEP = 1.5;
	private static final double ESCALATION_CAP = 5.0;
	private static final double KO_TIME_CUT_RATIO = 0.20;
	private static final double KO_TIME_CUT_MIN_RATIO = 0.05;
	private static final float KO_HEALTH_TARGET = 0.33f;
	private static final float KO_RESOURCE_TARGET = 0.50f;
	private static final double AGGRO_SWEEP_RANGE = 64.0;
	private static final java.util.Set<java.util.UUID> PENDING_SPIRIT_ARRIVAL = java.util.concurrent.ConcurrentHashMap.newKeySet();

	private BabaReviveService() {
	}

	public static boolean isTemporaryReviveEnabled() {
		return ConfigManager.getServerConfig().getWorldGen().getOtherworldActive()
				&& ConfigManager.getServerConfig().getGameplay().getBabaTemporaryRevive();
	}

	public static boolean isTempReviveActive(StatsData data) {
		return !data.getStatus().isAlive() && data.getStatus().getTempReturnTimer() > 0;
	}

	public static boolean isKnockedOut(StatsData data) {
		return data.getCooldowns().hasCooldown(Cooldowns.BABA_KNOCKOUT);
	}

	public static boolean isHealingBlocked(StatsData data) {
		return isKnockedOut(data);
	}

	public static boolean isHiddenFromMobs(StatsData data) {
		return data.getCooldowns().hasCooldown(Cooldowns.BABA_AGGRO_GRACE);
	}

	public static boolean canEnmaReturn(StatsData data) {
		return data.getStatus().isAlive() || isTempReviveActive(data);
	}

	public static void handleBabaRevive(ServerPlayer player, StatsData data) {
		if (data.getStatus().isAlive()) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.already_alive"));
			return;
		}
		if (data.getCooldowns().hasCooldown(Cooldowns.REVIVE_BABA)) {
			int cooldownTicks = data.getCooldowns().getCooldown(Cooldowns.REVIVE_BABA);
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.cooldown",
					formatSeconds(Math.max(1, cooldownTicks / 20))));
			return;
		}

		if (!isTemporaryReviveEnabled()) {
			data.getStatus().setAlive(true);
			data.getStatus().setTempReturnTimer(0);
			data.getStatus().setTempReturnsUsed(0);
			data.getStatus().setTempReturnReadyAt(0L);
			player.sendSystemMessage(Component.translatable("gui.dragonminez.lines.baba.revived"));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			return;
		}

		if (isTempReviveActive(data)) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.already_active"));
			return;
		}
		long dayTime = player.getServer().overworld().getDayTime();
		long readyAt = data.getStatus().getTempReturnReadyAt();
		if (readyAt > dayTime) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.wait_day",
					formatSeconds((int) Math.max(1, (readyAt - dayTime) / 20))));
			return;
		}

		int seconds = ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds();
		data.getStatus().setTempReturnTimer(seconds * 20);
		data.getStatus().setTempReturnsUsed(data.getStatus().getTempReturnsUsed() + 1);
		double escalation = escalationMultiplier(data);
		if (escalation > 1.0) {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.granted_escalated",
					formatSeconds(seconds), formatMultiplier(escalation)));
		} else {
			player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.granted", formatSeconds(seconds)));
		}
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static void handleEnmaReturn(ServerPlayer player, StatsData data) {
		if (!canEnmaReturn(data)) {
			player.sendSystemMessage(Component.translatable("gui.dragonminez.lines.enma.revive"));
			return;
		}
		teleportToLivingWorld(player);
	}

	public static long getReadyInTicks(ServerPlayer player, StatsData data) {
		return Math.max(0L, data.getStatus().getTempReturnReadyAt() - player.getServer().overworld().getDayTime());
	}

	/** Called once per second from TickHandler while the timer is active. */
	public static void tickTempReturn(ServerPlayer player, StatsData data) {
		int timer = data.getStatus().getTempReturnTimer();
		if (timer <= 0 || data.getStatus().isAlive()) return;

		if (isKnockedOut(data)) {
			tickKnockoutRecovery(player, data);
			return;
		}
		if (player.serverLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return;

		int drained = (int) Math.round(20 * totalTimeMultiplier(player, data));
		int remaining = Math.max(0, timer - drained);
		data.getStatus().setTempReturnTimer(remaining);

		if (remaining <= 0) {
			endTempReturn(player, data);
			return;
		}

		int secondsBefore = timer / 20;
		int secondsAfter = remaining / 20;
		for (int warning : WARNING_SECONDS) {
			if (secondsAfter <= warning && secondsBefore > warning) {
				player.displayClientMessage(Component.translatable("message.dragonminez.baba_return.time_left",
						formatSeconds(secondsAfter)), true);
				break;
			}
		}
	}

	public static double totalTimeMultiplier(Player player, StatsData data) {
		if (player.level().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return 0.0;
		return formTimeMultiplier(data) * dimensionTimeMultiplier(player) * escalationMultiplier(data);
	}

	public static double formTimeMultiplier(StatsData data) {
		Character character = data.getCharacter();
		double multiplier = 1.0;
		FormConfig.FormData form = character.getActiveFormData();
		if (form != null) {
			double mastery = character.getFormMasteries().getMastery(character.getActiveFormGroup(), character.getActiveForm());
			multiplier += Math.max(0, form.getOtherworldTimeDrainAt(mastery) - 1.0);
		}
		FormConfig.FormData stackForm = character.getActiveStackFormData();
		if (stackForm != null) {
			double mastery = character.getStackFormMasteries().getMastery(character.getActiveStackFormGroup(), character.getActiveStackForm());
			multiplier += Math.max(0, stackForm.getOtherworldTimeDrainAt(mastery) - 1.0);
		}
		return multiplier;
	}

	public static double dimensionTimeMultiplier(Player player) {
		String dimensionId = player.level().dimension().location().toString();
		return ConfigManager.getServerConfig().getGameplay().getBabaTimeMultiplier(dimensionId);
	}

	public static double escalationMultiplier(StatsData data) {
		int repeats = Math.max(0, data.getStatus().getTempReturnsUsed() - 1);
		return Math.min(ESCALATION_CAP, Math.pow(ESCALATION_STEP, repeats));
	}

	public static void markRetryAfterDay(ServerPlayer player, StatsData data) {
		data.getStatus().setTempReturnReadyAt(player.getServer().overworld().getDayTime() + RETRY_DAY_TICKS);
	}

	public static void requestSpiritArrival(ServerPlayer player) {
		PENDING_SPIRIT_ARRIVAL.add(player.getUUID());
	}

	public static boolean isSpiritArrivalPending(java.util.UUID playerId) {
		return PENDING_SPIRIT_ARRIVAL.contains(playerId);
	}

	public static void clearSpiritArrival(java.util.UUID playerId) {
		PENDING_SPIRIT_ARRIVAL.remove(playerId);
	}

	private static void endTempReturn(ServerPlayer player, StatsData data) {
		data.getStatus().setTempReturnTimer(0);
		markRetryAfterDay(player, data);
		clearKnockout(player, data);
		player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.expired"));
		if (!player.isSpectator() && !player.isCreative()) OtherworldDimension.teleportToSpiritArrival(player);
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static boolean tryKnockOut(ServerPlayer player, StatsData data, DamageSource source) {
		if (!isTempReviveActive(data) || isKnockedOut(data)) return false;
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		if (player.serverLevel().dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) return false;

		int total = ConfigManager.getServerConfig().getGameplay().getBabaTempReturnSeconds() * 20;
		int timer = data.getStatus().getTempReturnTimer();
		int cut = (int) Math.round(Math.max(timer * KO_TIME_CUT_RATIO, total * KO_TIME_CUT_MIN_RATIO));
		data.getStatus().setTempReturnTimer(Math.max(1, timer - cut));

		data.getStatus().setKnockedDown(true);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, KNOCKOUT_TICKS);
		data.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_INVULN, KNOCKOUT_TICKS);
		data.getCooldowns().setCooldown(Cooldowns.BABA_KNOCKOUT, KNOCKOUT_TICKS);
		data.getCooldowns().setCooldown(Cooldowns.BABA_AGGRO_GRACE, KNOCKOUT_TICKS + AGGRO_GRACE_TICKS);
		data.getCharacter().clearActiveForm(player);
		data.getCharacter().clearActiveStackForm(player);
		data.getStatus().setChargingKi(false);
		data.getStatus().setActionCharging(false);
		data.getStatus().setBlocking(false);
		data.getResources().setActionCharge(0);
		data.getTechniques().clearTechniqueCharge();
		KiSurgeService.breakSurge(player, data);
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, KNOCKOUT_TICKS, 0, false, false, false));

		QuestEvents.handlePlayerQuestFailure(player);
		EntitiesEvents.cleanupQuestEntities(player.serverLevel(), player.getUUID());
		dropAggro(player);

		player.sendSystemMessage(Component.translatable("message.dragonminez.baba_return.knocked_out",
				formatSeconds(Math.max(1, cut / 20))));
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
		return true;
	}

	private static void tickKnockoutRecovery(ServerPlayer player, StatsData data) {
		float maxHealth = player.getMaxHealth();
		float healthTarget = maxHealth * KO_HEALTH_TARGET;
		if (player.getHealth() < healthTarget) {
			player.setHealth(Math.min(healthTarget, player.getHealth() + healthTarget / (KNOCKOUT_TICKS / 20f)));
		}
		float energyTarget = data.getMaxEnergy() * KO_RESOURCE_TARGET;
		if (data.getResources().getCurrentEnergy() < energyTarget) {
			data.getResources().setCurrentEnergy(Math.min(energyTarget, data.getResources().getCurrentEnergy() + energyTarget / (KNOCKOUT_TICKS / 20f)));
		}
		float staminaTarget = data.getMaxStamina() * KO_RESOURCE_TARGET;
		if (data.getResources().getCurrentStamina() < staminaTarget) {
			data.getResources().setCurrentStamina(Math.min(staminaTarget, data.getResources().getCurrentStamina() + staminaTarget / (KNOCKOUT_TICKS / 20f)));
		}
	}

	private static void clearKnockout(ServerPlayer player, StatsData data) {
		if (!isKnockedOut(data)) return;
		data.getCooldowns().removeCooldown(Cooldowns.BABA_KNOCKOUT);
		data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
		data.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_INVULN);
		data.getStatus().setKnockedDown(false);
		player.removeEffect(MobEffects.GLOWING);
	}

	private static void dropAggro(ServerPlayer player) {
		AABB area = player.getBoundingBox().inflate(AGGRO_SWEEP_RANGE);
		for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == player || mob.getLastHurtByMob() == player)) {
			mob.setTarget(null);
			if (mob.getLastHurtByMob() == player) mob.setLastHurtByMob(null);
			mob.getNavigation().stop();
		}
	}

	public static void reviveFully(ServerPlayer player, StatsData data) {
		data.getCooldowns().removeCooldown(Cooldowns.REVIVE_BABA);
		data.getStatus().setTempReturnTimer(0);
		data.getStatus().setTempReturnsUsed(0);
		data.getStatus().setTempReturnReadyAt(0L);
		clearKnockout(player, data);
		if (!data.getStatus().isAlive()) {
			data.getStatus().setAlive(true);
			player.setHealth(player.getMaxHealth());
			player.sendSystemMessage(Component.translatable("command.dragonminez.revive.target"));
		}
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
	}

	public static boolean canReceiveSharedTp(ServerPlayer earner, StatsData earnerData, ServerPlayer member, StatsData memberData) {
		if (!member.level().dimension().equals(earner.level().dimension())) return false;
		int skillLevel = earnerData.getSkills().getSkillLevel("instant_transmission");
		if (skillLevel <= 0) return false;
		double maxRange = (double) ConfigManager.getServerConfig().getGameplay().getInstantTransmissionPlayerRangePerLevel() * skillLevel;
		if (earner.position().distanceTo(member.position()) > maxRange) return false;
		if (member.isCreative() || member.isSpectator()) return false;
		if (TransformationsHelper.hasAntiKiCloak(member)) return false;
		if (TransformationsHelper.isInstantTransmissionBlocked(earnerData, memberData)) return false;
		return !memberData.getStatus().isFused() || memberData.getStatus().isFusionLeader();
	}

	public static void teleportToLivingWorld(ServerPlayer player) {
		ServerLevel respawnLevel = player.getServer().getLevel(player.getRespawnDimension());
		BlockPos respawnPos = player.getRespawnPosition();
		if (respawnLevel != null && respawnPos != null
				&& !respawnLevel.dimension().equals(OtherworldDimension.OTHERWORLD_KEY)) {
			Optional<Vec3> spot = ServerPlayer.findRespawnPositionAndUseSpawnBlock(
					respawnLevel, respawnPos, player.getRespawnAngle(), player.isRespawnForced(), false);
			if (spot.isPresent()) {
				Vec3 pos = spot.get();
				player.teleportTo(respawnLevel, pos.x, pos.y, pos.z, player.getRespawnAngle(), 0);
				return;
			}
		}
		ServerLevel overworld = player.getServer().overworld();
		BlockPos spawn = overworld.getSharedSpawnPos();
		int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ());
		player.teleportTo(overworld, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
	}

	public static String formatSeconds(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;
		if (hours > 0) return String.format("%d:%02d:%02d", hours, minutes, seconds);
		return String.format("%d:%02d", minutes, seconds);
	}

	public static String formatMultiplier(double multiplier) {
		if (Math.abs(multiplier - Math.rint(multiplier)) < 0.005) return String.format("x%d", Math.round(multiplier));
		return String.format("x%.2f", multiplier).replaceAll("0+$", "").replaceAll("\\.$", "");
	}
}
