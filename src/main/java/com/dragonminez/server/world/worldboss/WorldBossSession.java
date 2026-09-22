package com.dragonminez.server.world.worldboss;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.combat.HealContext;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.GeneralServerConfig;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.init.entities.worldboss.WorldBossEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.WorldBossContributionS2C;
import com.dragonminez.common.network.S2C.WorldBossPlayerStateS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.techniques.ReviveTechniqueData;
import com.dragonminez.common.worldboss.WorldBossResults;
import com.dragonminez.server.events.players.KiSurgeService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class WorldBossSession {
	public static final int KNOCKOUT_TICKS = 999_999;
	private static final int CONTRIBUTION_SYNC_INTERVAL = 2;
	private static final int STATE_CHECK_INTERVAL = 20;
	private static final double CAST_MOVE_TOLERANCE_SQR = 1.0 * 1.0;
	private static final double CAST_TARGET_RANGE_SLACK = 1.5;
	private static final double LOOK_TARGET_HALO = 0.6;

	private static final class Participant {
		private final UUID id;
		private String name;
		private int livesLeft;
		private boolean knockedOut;

		private Participant(UUID id, String name, int livesLeft) {
			this.id = id;
			this.name = name;
			this.livesLeft = livesLeft;
		}
	}

	private static final class ReviveCast {
		private final UUID caster;
		private final UUID target;
		private final Vec3 origin;
		private int ticks;

		private ReviveCast(UUID caster, UUID target, Vec3 origin) {
			this.caster = caster;
			this.target = target;
			this.origin = origin;
		}
	}

	private final String bossKey;
	private final ResourceKey<Level> dimension;
	private final long startTick;
	private final Map<UUID, Participant> participants = new LinkedHashMap<>();
	private final Set<UUID> audience = new HashSet<>();
	private final Map<UUID, ReviveCast> casts = new HashMap<>();
	private String bossNameKey = "";
	private UUID bossId;
	private long lastAudienceRefresh = -1_000_000L;
	private long lastContributionSync = -1_000_000L;
	private int bossMissingTicks;
	private boolean ended;
	private boolean wiping;

	WorldBossSession(String bossKey, ServerLevel level) {
		this.bossKey = bossKey;
		this.dimension = level.dimension();
		this.startTick = level.getGameTime();
	}

	public String bossKey() {
		return bossKey;
	}

	public ResourceKey<Level> dimension() {
		return dimension;
	}

	public boolean isEnded() {
		return ended;
	}

	public boolean isParticipant(ServerPlayer player) {
		return participants.containsKey(player.getUUID());
	}

	public boolean isKnockedOut(UUID id) {
		Participant participant = participants.get(id);
		return participant != null && participant.knockedOut;
	}

	public boolean isCasting(UUID id) {
		return casts.containsKey(id);
	}

	void attachBoss(WorldBossEntity boss) {
		this.bossId = boss.getUUID();
		this.bossNameKey = boss.getType().getDescriptionId();
		this.bossMissingTicks = 0;
	}

	WorldBossEntity resolveBoss(ServerLevel level) {
		WorldBossSavedData.Entry entry = WorldBossSavedData.get(level.getServer()).peek(bossKey);
		UUID id = entry != null && entry.bossId != null ? entry.bossId : bossId;
		if (id == null) return null;
		Entity entity = level.getEntity(id);
		if (entity instanceof WorldBossEntity boss && boss.isAlive()) {
			attachBoss(boss);
			return boss;
		}
		return null;
	}

	int noteBossMissing() {
		return ++bossMissingTicks;
	}

	void tick(ServerLevel level, WorldBossEntity boss) {
		if (ended) return;
		long now = level.getGameTime();
		GeneralServerConfig.WorldBossConfig config = config();

		if (now - lastAudienceRefresh >= config.getAudienceRefreshSeconds() * 20L) {
			refreshAudience(level, boss, config);
			lastAudienceRefresh = now;
		}

		tickCasts(level, config);

		boolean dirty = WorldBossContribution.consumeDirty(bossKey);
		if ((dirty && now - lastContributionSync >= CONTRIBUTION_SYNC_INTERVAL) || now % 20 == 0) {
			broadcastContribution(level, false, false);
			lastContributionSync = now;
		}

		if (now % STATE_CHECK_INTERVAL == 0) {
			enforceKnockouts(level);
			checkWipe(level, boss);
		}
	}

	private void refreshAudience(ServerLevel level, WorldBossEntity boss, GeneralServerConfig.WorldBossConfig config) {
		double rangeSqr = (double) config.getContributionRange() * config.getContributionRange();
		Set<UUID> next = new HashSet<>();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			if (player.distanceToSqr(boss) > rangeSqr) continue;
			next.add(player.getUUID());
			if (!participants.containsKey(player.getUUID())) join(level, player, config);
		}
		boolean changed = !next.equals(audience);
		audience.clear();
		audience.addAll(next);
		if (changed) broadcastContribution(level, false, false);
	}

	private void join(ServerLevel level, ServerPlayer player, GeneralServerConfig.WorldBossConfig config) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter()) return;

		Participant participant = new Participant(player.getUUID(), player.getGameProfile().getName(), config.getLives());
		participants.put(player.getUUID(), participant);
		installRevive(player, data, config);
		sendPlayerState(player, participant, null);
		LogUtil.debug(Env.SERVER, "{} joined the {} world boss fight", participant.name, bossKey);
	}

	private void installRevive(ServerPlayer player, StatsData data, GeneralServerConfig.WorldBossConfig config) {
		if (data.getTechniques().installRevive(config.getReviveSlotIndex())) {
			NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
		}
	}

	private void uninstallRevive(ServerPlayer player) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			if (data.getTechniques().uninstallRevive()) {
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			}
		});
	}

	private void broadcastContribution(ServerLevel level, boolean finished, boolean cleared) {
		List<WorldBossContribution.Score> scores = WorldBossContribution.snapshot(bossKey);
		double total = WorldBossContribution.totalPoints(scores);
		List<WorldBossContributionS2C.Entry> entries = new ArrayList<>();
		for (WorldBossContribution.Score score : scores) {
			if (entries.size() >= WorldBossContributionS2C.MAX_ENTRIES) break;
			double points = score.points();
			if (points <= 0.0) continue;
			entries.add(new WorldBossContributionS2C.Entry(score.id(), score.name(), (float) points,
					total > 0.0 ? (float) (points / total) : 0.0f, score.auraRgb(), isKnockedOut(score.id())));
		}
		WorldBossContributionS2C packet = new WorldBossContributionS2C(bossKey, bossNameKey,
				level.getGameTime() - startTick, finished, cleared, entries);

		Set<UUID> recipients = new HashSet<>(audience);
		if (finished || cleared) recipients.addAll(participants.keySet());
		for (UUID id : recipients) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) NetworkHandler.sendToPlayer(packet, player);
		}
	}

	private void sendPlayerState(ServerPlayer player, Participant participant, ReviveCast cast) {
		boolean casting = cast != null && cast.caster.equals(player.getUUID());
		boolean beingRevived = cast != null && cast.target.equals(player.getUUID());
		String castTargetName = casting ? nameOf(cast.target) : "";
		String reviverName = beingRevived ? nameOf(cast.caster) : "";
		float progress = cast != null ? Math.min(1.0f, cast.ticks / (float) castTicks()) : 0.0f;
		NetworkHandler.sendToPlayer(new WorldBossPlayerStateS2C(true, participant.knockedOut, participant.livesLeft,
				casting, progress, castTargetName, beingRevived, reviverName), player);
	}

	private void sendClearedState(ServerPlayer player) {
		NetworkHandler.sendToPlayer(WorldBossPlayerStateS2C.cleared(), player);
	}

	private String nameOf(UUID id) {
		Participant participant = participants.get(id);
		return participant != null ? participant.name : "";
	}

	boolean knockOut(ServerLevel level, ServerPlayer victim, StatsData stats, DamageSource source) {
		if (ended || wiping) return false;
		Participant participant = participants.get(victim.getUUID());
		if (participant == null || participant.knockedOut) return false;
		if (source != null && source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

		participant.knockedOut = true;
		applyKnockoutState(victim, stats);
		interruptCast(victim, false);

		int titleTicks = config().getKnockoutTitleSeconds() * 20;
		victim.connection.send(new ClientboundSetTitlesAnimationPacket(5, Math.max(1, titleTicks - 10), 5));
		victim.connection.send(new ClientboundSetSubtitleTextPacket(
				Component.translatable("worldboss.dragonminez.knockout.subtitle", participant.livesLeft).withStyle(ChatFormatting.GRAY)));
		victim.connection.send(new ClientboundSetTitleTextPacket(
				Component.translatable("worldboss.dragonminez.knockout.title").withStyle(ChatFormatting.RED)));
		level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), MainSounds.KNOCKBACK_CHARACTER.get(),
				SoundSource.PLAYERS, 1.0F, 0.7F);

		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(victim), victim);
		sendPlayerState(victim, participant, null);
		WorldBossContribution.touch(bossKey, victim);
		LogUtil.info(Env.SERVER, "{} was knocked out in the {} fight ({} revives left)", participant.name, bossKey, participant.livesLeft);

		WorldBossEntity boss = resolveBoss(level);
		if (boss != null) checkWipe(level, boss);
		return true;
	}

	private void applyKnockoutState(ServerPlayer player, StatsData stats) {
		stats.getStatus().setKnockedDown(true);
		stats.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_DURATION, KNOCKOUT_TICKS);
		stats.getCooldowns().setCooldown(Cooldowns.KNOCKDOWN_INVULN, KNOCKOUT_TICKS);
		stats.getCharacter().clearActiveForm(player);
		stats.getCharacter().clearActiveStackForm(player);
		stats.getStatus().setChargingKi(false);
		stats.getStatus().setActionCharging(false);
		stats.getStatus().setBlocking(false);
		stats.getResources().setActionCharge(0);
		stats.getTechniques().clearTechniqueCharge();
		KiSurgeService.breakSurge(player, stats);
		player.addEffect(new MobEffectInstance(MobEffects.GLOWING, KNOCKOUT_TICKS, 0, false, false, false));
	}

	private void clearKnockoutState(ServerPlayer player, StatsData stats) {
		stats.getStatus().setKnockedDown(false);
		stats.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_DURATION);
		stats.getCooldowns().removeCooldown(Cooldowns.KNOCKDOWN_INVULN);
		player.removeEffect(MobEffects.GLOWING);
	}

	private void enforceKnockouts(ServerLevel level) {
		GeneralServerConfig.WorldBossConfig config = config();
		for (Participant participant : participants.values()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(participant.id);
			if (player == null || player.level() != level) continue;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				if (!stats.getTechniques().hasRevive()) installRevive(player, stats, config);
				if (!participant.knockedOut) return;
				if (stats.getStatus().isKnockedDown() && stats.getCooldowns().hasCooldown(Cooldowns.KNOCKDOWN_DURATION)) return;
				applyKnockoutState(player, stats);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			});
		}
	}

	private void revive(ServerLevel level, ServerPlayer target, Participant participant, boolean consumeLife) {
		StatsData stats = StatsProvider.get(StatsCapability.INSTANCE, target).orElse(null);
		participant.knockedOut = false;
		if (consumeLife) participant.livesLeft = Math.max(0, participant.livesLeft - 1);
		if (stats != null) {
			clearKnockoutState(target, stats);
			double ratio = config().getReviveRestoreRatio();
			float health = (float) (target.getMaxHealth() * ratio);
			HealContext.asSystemHeal(() -> target.heal(health));
			stats.getResources().addEnergy((float) (stats.getMaxEnergy() * ratio));
			stats.getResources().addStamina((float) (stats.getMaxStamina() * ratio));
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(target), target);
		}
		level.playSound(null, target.getX(), target.getY(), target.getZ(), MainSounds.TRANSFORM_ON.get(),
				SoundSource.PLAYERS, 0.8F, 1.3F);
		sendPlayerState(target, participant, null);
		WorldBossContribution.touch(bossKey, target);
	}

	void tryStartRevive(ServerLevel level, ServerPlayer caster, int targetEntityId) {
		if (ended) return;
		Participant casterEntry = participants.get(caster.getUUID());
		if (casterEntry == null || casterEntry.knockedOut || casts.containsKey(caster.getUUID())) return;
		if (caster.isSpectator() || !caster.isAlive()) return;

		StatsData casterStats = StatsProvider.get(StatsCapability.INSTANCE, caster).orElse(null);
		if (casterStats == null || casterStats.getStatus().isStunned()) return;
		if (!casterStats.getTechniques().hasRevive()) return;
		if (casterStats.getCooldowns().hasCooldown(cooldownKey())) return;
		if (casterStats.getTechniques().isTechniqueCharging() || casterStats.getStatus().isActionCharging()) return;

		GeneralServerConfig.WorldBossConfig config = config();
		ServerPlayer target = resolveReviveTarget(level, caster, targetEntityId, config.getReviveRange());
		if (target == null) {
			caster.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.no_target").withStyle(ChatFormatting.RED), true);
			return;
		}
		Participant targetEntry = participants.get(target.getUUID());
		if (targetEntry == null || !targetEntry.knockedOut) return;
		if (targetEntry.livesLeft <= 0) {
			caster.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.no_lives", targetEntry.name).withStyle(ChatFormatting.RED), true);
			return;
		}
		for (ReviveCast other : casts.values()) {
			if (other.target.equals(target.getUUID())) {
				caster.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.already", targetEntry.name).withStyle(ChatFormatting.RED), true);
				return;
			}
		}

		ReviveCast cast = new ReviveCast(caster.getUUID(), target.getUUID(), caster.position());
		casts.put(caster.getUUID(), cast);
		casterStats.getTechniques().selectSlot(casterStats.getTechniques().getReviveSlot());
		sendPlayerState(caster, casterEntry, cast);
		sendPlayerState(target, targetEntry, cast);
		level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), MainSounds.KI_CHARGE_LOOP.get(),
				SoundSource.PLAYERS, 0.6F, 1.2F);
	}

	private ServerPlayer resolveReviveTarget(ServerLevel level, ServerPlayer caster, int targetEntityId, double range) {
		double rangeSqr = range * range;
		if (targetEntityId >= 0 && level.getEntity(targetEntityId) instanceof ServerPlayer direct
				&& direct != caster && isKnockedOut(direct.getUUID()) && caster.distanceToSqr(direct) <= rangeSqr) {
			return direct;
		}

		Vec3 eye = caster.getEyePosition();
		Vec3 end = eye.add(caster.getViewVector(1.0F).scale(range));
		AABB sweep = caster.getBoundingBox().expandTowards(caster.getViewVector(1.0F).scale(range)).inflate(1.0);
		ServerPlayer best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ServerPlayer candidate : level.getEntitiesOfClass(ServerPlayer.class, sweep,
				p -> p != caster && p.isAlive() && isKnockedOut(p.getUUID()))) {
			Optional<Vec3> hit = candidate.getBoundingBox().inflate(LOOK_TARGET_HALO).clip(eye, end);
			if (hit.isEmpty()) continue;
			double distance = eye.distanceToSqr(hit.get());
			if (distance < bestDistance) {
				best = candidate;
				bestDistance = distance;
			}
		}
		if (best != null) return best;

		for (ServerPlayer candidate : level.getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(range),
				p -> p != caster && p.isAlive() && isKnockedOut(p.getUUID()))) {
			double distance = caster.distanceToSqr(candidate);
			if (distance <= rangeSqr && distance < bestDistance) {
				best = candidate;
				bestDistance = distance;
			}
		}
		return best;
	}

	private void tickCasts(ServerLevel level, GeneralServerConfig.WorldBossConfig config) {
		if (casts.isEmpty()) return;
		int castTicks = castTicks();
		double maxRangeSqr = config.getReviveRange() * CAST_TARGET_RANGE_SLACK * config.getReviveRange() * CAST_TARGET_RANGE_SLACK;

		for (ReviveCast cast : new ArrayList<>(casts.values())) {
			ServerPlayer caster = level.getServer().getPlayerList().getPlayer(cast.caster);
			ServerPlayer target = level.getServer().getPlayerList().getPlayer(cast.target);
			Participant casterEntry = participants.get(cast.caster);
			Participant targetEntry = participants.get(cast.target);

			if (caster == null || target == null || casterEntry == null || targetEntry == null
					|| caster.level() != level || target.level() != level || !targetEntry.knockedOut || casterEntry.knockedOut) {
				cancelCast(cast, caster, target, false);
				continue;
			}
			StatsData casterStats = StatsProvider.get(StatsCapability.INSTANCE, caster).orElse(null);
			if (casterStats == null || casterStats.getStatus().isStunned() || !caster.isAlive()) {
				cancelCast(cast, caster, target, true);
				continue;
			}
			if (caster.position().distanceToSqr(cast.origin) > CAST_MOVE_TOLERANCE_SQR || caster.distanceToSqr(target) > maxRangeSqr) {
				cancelCast(cast, caster, target, true);
				continue;
			}

			cast.ticks++;
			if (cast.ticks >= castTicks) {
				casts.remove(cast.caster);
				casterStats.getCooldowns().setCooldown(cooldownKey(), config.getReviveCooldownSeconds() * 20);
				revive(level, target, targetEntry, true);
				sendPlayerState(caster, casterEntry, null);
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(caster), caster);
				caster.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.done", targetEntry.name).withStyle(ChatFormatting.GREEN), true);
				target.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.revived", casterEntry.name).withStyle(ChatFormatting.GREEN), true);
				LogUtil.info(Env.SERVER, "{} revived {} in the {} fight ({} revives left)", casterEntry.name, targetEntry.name, bossKey, targetEntry.livesLeft);
				continue;
			}
			sendPlayerState(caster, casterEntry, cast);
			sendPlayerState(target, targetEntry, cast);
		}
	}

	private void cancelCast(ReviveCast cast, ServerPlayer caster, ServerPlayer target, boolean penalize) {
		casts.remove(cast.caster);
		if (caster != null) {
			Participant casterEntry = participants.get(cast.caster);
			if (penalize) {
				StatsProvider.get(StatsCapability.INSTANCE, caster).ifPresent(stats -> {
					stats.getCooldowns().setCooldown(cooldownKey(), config().getReviveInterruptCooldownSeconds() * 20);
					NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(caster), caster);
				});
				caster.displayClientMessage(Component.translatable("worldboss.dragonminez.revive.interrupted").withStyle(ChatFormatting.RED), true);
			}
			if (casterEntry != null) sendPlayerState(caster, casterEntry, null);
		}
		if (target != null) {
			Participant targetEntry = participants.get(cast.target);
			if (targetEntry != null) sendPlayerState(target, targetEntry, null);
		}
	}

	void interruptCast(ServerPlayer caster, boolean penalize) {
		ReviveCast cast = casts.get(caster.getUUID());
		if (cast == null) return;
		ServerPlayer target = caster.getServer() != null ? caster.getServer().getPlayerList().getPlayer(cast.target) : null;
		cancelCast(cast, caster, target, penalize);
	}

	private void checkWipe(ServerLevel level, WorldBossEntity boss) {
		if (ended || wiping || participants.isEmpty()) return;
		boolean anyKnockedOut = false;
		for (Participant participant : participants.values()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(participant.id);
			if (player == null || player.level() != level || player.isSpectator()) continue;
			if (!participant.knockedOut) return;
			anyKnockedOut = true;
		}
		if (!anyKnockedOut) return;
		wipe(level, boss);
	}

	private void wipe(ServerLevel level, WorldBossEntity boss) {
		wiping = true;
		LogUtil.info(Env.SERVER, "Every fighter is down: the {} world boss fight is lost", bossKey);
		killKnockedOut(level);
		broadcastContribution(level, false, true);
		endSession(level);
		if (boss.isAlive() && !boss.isBossAsleep()) boss.returnToSleep();
	}

	private void killKnockedOut(ServerLevel level) {
		for (Participant participant : participants.values()) {
			if (!participant.knockedOut) continue;
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(participant.id);
			if (player == null) continue;
			participant.knockedOut = false;
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> clearKnockoutState(player, stats));
			player.sendSystemMessage(Component.translatable("worldboss.dragonminez.wipe").withStyle(ChatFormatting.DARK_RED));
			player.kill();
		}
	}

	void finishVictory(ServerLevel level, WorldBossEntity boss) {
		if (ended) return;
		attachBoss(boss);
		casts.clear();
		for (Participant participant : participants.values()) {
			if (!participant.knockedOut) continue;
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(participant.id);
			if (player == null) {
				participant.knockedOut = false;
				continue;
			}
			revive(level, player, participant, false);
		}

		WorldBossResults results = WorldBossRewards.grantAndBuild(level, bossKey, bossNameKey, level.getGameTime() - startTick);
		WorldBossResultsCache.store(results);
		broadcastContribution(level, true, false);
		endSession(level);
		LogUtil.info(Env.SERVER, "World boss {} defeated after {} ticks by {} contributors", bossKey,
				level.getGameTime() - startTick, results.players().size());
	}

	void finishDefeat(ServerLevel level) {
		if (ended) return;
		wiping = true;
		casts.clear();
		killKnockedOut(level);
		broadcastContribution(level, false, true);
		endSession(level);
		LogUtil.info(Env.SERVER, "World boss {} fight ended without a kill", bossKey);
	}

	private void endSession(ServerLevel level) {
		ended = true;
		casts.clear();
		for (Participant participant : participants.values()) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(participant.id);
			if (player == null) continue;
			uninstallRevive(player);
			sendClearedState(player);
		}
		WorldBossContribution.clear(bossKey);
	}

	void onPlayerLogout(ServerPlayer player) {
		if (ended) return;
		interruptCast(player, false);
		for (ReviveCast cast : new ArrayList<>(casts.values())) {
			if (cast.target.equals(player.getUUID())) {
				ServerPlayer caster = player.getServer() != null ? player.getServer().getPlayerList().getPlayer(cast.caster) : null;
				cancelCast(cast, caster, null, false);
			}
		}
		audience.remove(player.getUUID());
	}

	boolean onPlayerLogin(ServerPlayer player, StatsData data) {
		if (ended) return false;
		Participant participant = participants.get(player.getUUID());
		if (participant == null) return false;
		if (player.level().dimension() != dimension) {
			onPlayerLeftFight(player, data);
			return false;
		}
		installRevive(player, data, config());
		if (participant.knockedOut) applyKnockoutState(player, data);
		sendPlayerState(player, participant, null);
		return true;
	}

	void onPlayerLeftFight(ServerPlayer player, StatsData data) {
		if (ended) return;
		Participant participant = participants.remove(player.getUUID());
		interruptCast(player, false);
		audience.remove(player.getUUID());
		if (participant == null) return;
		if (participant.knockedOut) clearKnockoutState(player, data);
		if (data.getTechniques().uninstallRevive()) NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
		sendClearedState(player);
	}

	private int castTicks() {
		return config().getReviveCastSeconds() * 20;
	}

	private static String cooldownKey() {
		return "TechniqueCooldown_" + ReviveTechniqueData.ID;
	}

	private static GeneralServerConfig.WorldBossConfig config() {
		return ConfigManager.getServerConfig().getWorldBoss();
	}
}
