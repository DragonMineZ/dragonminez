package com.dragonminez.common.training;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.common.alignment.NpcDispositionService;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TpSource;
import com.dragonminez.common.config.TrainingConfig;
import com.dragonminez.common.network.C2S.MinigameInputC2S;
import com.dragonminez.common.network.C2S.NPCActionC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.PacketRateLimiter;
import com.dragonminez.common.network.S2C.MinigameResultS2C;
import com.dragonminez.common.network.S2C.MinigameStartS2C;
import com.dragonminez.common.network.S2C.OpenMinigameS2C;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TriggerAnimationS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class MinigameSessionManager {
	private static final long PACE_SLACK_MS = 3000L;
	private static final long IDLE_TIMEOUT_MS = 15000L;
	private static final long CHALLENGE_TIMEOUT_MS = 30L * 60L * 1000L;
	private static final int MAX_CHUNK_TICKS = 200;
	private static final int MAX_EVENTS_PER_TICK = 12;
	private static final int MAX_SESSION_TICKS = 20 * 60 * 60 * 4;
	private static final long START_MIN_INTERVAL_TICKS = 10L;
	private static final String[] TRAINING_ANIMATIONS = {"base.flex", "base.meditation"};

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, ChallengeProgress> CHALLENGES = new HashMap<>();
	private static final Map<UUID, CommandGrant> COMMAND_GRANTS = new HashMap<>();
	private static final long COMMAND_GRANT_MS = 60_000L;

	private record CommandGrant(String minigameId, boolean mentorBonus, long expiresAt) {}

	public static void grantCommandSession(ServerPlayer player, String minigameId, boolean mentorBonus) {
		COMMAND_GRANTS.put(player.getUUID(), new CommandGrant(minigameId, mentorBonus, System.currentTimeMillis() + COMMAND_GRANT_MS));
		NetworkHandler.sendToPlayer(new OpenMinigameS2C(minigameId), player);
	}
	private static int nextSessionId = 1;

	private MinigameSessionManager() {}

	private static final class Session {
		final int id;
		final String minigameId;
		final MinigameOrigin origin;
		final int challengeStage;
		final MinigameLogic logic;
		final long startMs;
		final boolean knownAtStart;
		final boolean mentor;
		long lastInputMs;
		boolean invalid;
		boolean desyncLogged;

		String track = "";

		Session(int id, String minigameId, MinigameOrigin origin, int challengeStage, MinigameLogic logic, long now, boolean knownAtStart, boolean mentor) {
			this.id = id;
			this.minigameId = minigameId;
			this.origin = origin;
			this.challengeStage = challengeStage;
			this.logic = logic;
			this.startMs = now;
			this.lastInputMs = now;
			this.knownAtStart = knownAtStart;
			this.mentor = mentor;
		}
	}

	private static final class ChallengeProgress {
		int nextStage;
		long lastActivityMs;

		ChallengeProgress(long now) {
			this.lastActivityMs = now;
		}
	}

	public static void start(ServerPlayer player, String minigameId, MinigameOrigin origin, int challengeStage, String option, byte[] payload) {
		UUID uuid = player.getUUID();
		String id = minigameId == null ? "" : minigameId.toLowerCase();
		if (!PacketRateLimiter.allow(uuid, "minigame_start", player.level().getGameTime(), START_MIN_INTERVAL_TICKS)) {
			reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.unavailable");
			return;
		}

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null || !data.getStatus().isHasCreatedCharacter() || !MinigameLogics.isValidId(id)) {
			reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.unavailable");
			return;
		}

		Session previous = SESSIONS.get(uuid);
		if (previous != null) finish(player, previous, true);

		TrainingConfig config = ConfigManager.getTrainingConfig();
		TrainingConfig.MinigameSettings settings = config.getSettings(id);
		boolean known = settings.isUnlockedByDefault() || data.getCharacter().isMinigameKnown(id);
		long now = System.currentTimeMillis();
		boolean mentor = false;
		int targetLevels = -1;

		switch (origin) {
			case MENU -> {
				if (!known) {
					reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.unknown");
					return;
				}
			}
			case MASTER -> {
				String master = settings.getMasterName();
				if (!NPCActionC2S.isNpcInRange(player, master) || NpcDispositionService.getServiceBlocker(player, master) != null) {
					reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.master");
					return;
				}
				mentor = known;
			}
			case COMMAND -> {
				CommandGrant grant = COMMAND_GRANTS.get(uuid);
				if (grant == null || !grant.minigameId().equals(id) || grant.expiresAt() < now) {
					reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.unavailable");
					return;
				}
				COMMAND_GRANTS.remove(uuid);
				mentor = grant.mentorBonus();
			}
			case CHALLENGE -> {
				if (!validateChallengeStage(player, data, id, challengeStage, now)) {
					reject(player, id, origin, challengeStage, "gui.dragonminez.minigame.reject.challenge");
					return;
				}
				targetLevels = Math.max(1, config.getChallengeTargetLevels(id));
			}
		}

		RhythmChart chart = null;
		String track = RhythmLogic.optionTrack(option);
		int difficulty = RhythmLogic.optionDifficulty(option);
		int variant = RhythmLogic.optionVariant(option);
		if (RhythmLogic.ID.equals(id) && !track.isEmpty() && origin != MinigameOrigin.CHALLENGE) {
			String problem = null;
			BuiltinSongs.Song song = BuiltinSongs.find(track);
			ResourceLocation itemId = song == null ? ResourceLocation.tryParse(track) : null;
			Item item = itemId == null ? null : ForgeRegistries.ITEMS.getValue(itemId);
			String soundId = song != null ? song.sound() : item instanceof RecordItem record ? record.getSound().getLocation().toString() : null;
			int lengthMs = song != null ? song.durationMs() : item instanceof RecordItem record ? record.getLengthInTicks() * 50 : 0;
			if (soundId == null) {
				problem = "gui.dragonminez.minigame.reject.no_chart";
			} else {
				chart = RhythmChartRegistry.find(track, soundId);
				String clientHash = RhythmLogic.optionHash(option);
				if (chart != null && !chart.matchesAudio(clientHash)) chart = null;
				if (chart == null && payload != null && payload.length > 0) {
					RhythmChart generated = RhythmChart.decode(payload);
					String invalid = RhythmAutoCharter.validate(generated, lengthMs);
					if (invalid == null) {
						chart = new RhythmChart("auto:" + track, track, soundId, generated.bpm, generated.durationMs, generated.notes, generated.vocalNotes, generated.audioHash);
					} else {
						LogUtil.warn(Env.SERVER, "Rejected generated rhythm chart for {} from {}: {}", track, player.getGameProfile().getName(), invalid);
					}
				}
				if (chart != null && chart.hasVocals() && RhythmAutoCharter.validateNotes(chart.vocalNotes, chart.durationMs) != null) chart = chart.withoutVocals();
				if (chart == null) problem = "gui.dragonminez.minigame.reject.no_chart";
				else if (song == null && config.getRhythm().isRequireDiscInInventory() && !data.getCharacter().isSongLearned(track)
						&& !player.getInventory().hasAnyMatching(stack -> stack.is(item))) {
					problem = "gui.dragonminez.minigame.reject.no_disc";
				}
			}
			if (problem != null) {
				reject(player, id, origin, challengeStage, problem);
				return;
			}
			variant = chart.effectiveVariant(variant);
		} else {
			track = "";
		}

		long seed = player.getRandom().nextLong() ^ System.nanoTime();
		MinigameLogic logic = MinigameLogics.create(id, config, seed, chart, difficulty, variant);
		if (targetLevels > 0) logic.setTargetLevelsCleared(targetLevels);
		int sessionId = nextSessionId++;
		if (nextSessionId <= 0) nextSessionId = 1;
		Session created = new Session(sessionId, id, origin, challengeStage, logic, now, known, mentor);
		created.track = track;
		SESSIONS.put(uuid, created);

		NetworkHandler.sendToPlayer(new MinigameStartS2C(true, sessionId, seed, id, origin, challengeStage, targetLevels, mentor, known,
				data.getCharacter().getLearnRunsFor(id), Math.max(1, config.getLearnRequiredRuns()), Math.max(1, config.getLearnRequiredLevel()), "",
				chart == null ? "" : RhythmLogic.buildOption(track, difficulty, variant), chart == null ? null : chart.encode()), player);
		startAnimation(player);
	}

	private static boolean validateChallengeStage(ServerPlayer player, StatsData data, String id, int stage, long now) {
		if (stage < 0 || stage >= MinigameLogics.CHALLENGE_STAGES.size()) return false;
		if (!MinigameLogics.CHALLENGE_STAGES.get(stage).equals(id)) return false;
		UUID uuid = player.getUUID();
		if (stage == 0) {
			if (!NPCActionC2S.isNpcInRange(player, "oldkai") || NpcDispositionService.getServiceBlocker(player, "oldkai") != null) return false;
			if (!NPCActionC2S.meetsOldKaiRequirements(data)) return false;
			CHALLENGES.put(uuid, new ChallengeProgress(now));
			return true;
		}
		ChallengeProgress progress = CHALLENGES.get(uuid);
		if (progress == null || progress.nextStage != stage) return false;
		progress.lastActivityMs = now;
		return true;
	}

	private static void reject(ServerPlayer player, String id, MinigameOrigin origin, int stage, String key) {
		NetworkHandler.sendToPlayer(MinigameStartS2C.rejected(id, origin, stage, key), player);
	}

	public static void input(ServerPlayer player, MinigameInputC2S msg) {
		Session session = SESSIONS.get(player.getUUID());
		if (session == null || session.id != msg.getSessionId()) return;

		long now = System.currentTimeMillis();
		MinigameLogic logic = session.logic;
		int ticks = msg.getToTick() - msg.getFromTick();
		boolean malformed = msg.isMalformed()
				|| msg.getFromTick() != logic.tickCount()
				|| ticks < 0 || ticks > MAX_CHUNK_TICKS
				|| msg.getEvents().size() > MAX_EVENTS_PER_TICK * (ticks + 1);
		long pacedTicks = (now - session.startMs + PACE_SLACK_MS) / 50L;
		if (!malformed && msg.getToTick() > pacedTicks) malformed = true;

		if (malformed) {
			session.invalid = true;
			LogUtil.warn(Env.SERVER, "Rejected minigame input from {} ({}): chunk {}-{} failed validation",
					player.getGameProfile().getName(), session.minigameId, msg.getFromTick(), msg.getToTick());
			finish(player, session, true);
			return;
		}

		List<MinigameEvent> events = msg.getEvents();
		int index = 0;
		int lastTick = msg.getFromTick();
		while (logic.tickCount() < msg.getToTick() && !logic.isFinished()) {
			int k = logic.tickCount();
			while (index < events.size() && events.get(index).tick <= k) {
				MinigameEvent event = events.get(index++);
				if (event.tick < lastTick) continue;
				lastTick = event.tick;
				logic.applyEvent(event);
			}
			logic.tick();
		}
		session.lastInputMs = now;

		if (!session.desyncLogged && !logic.isFinished() && msg.getChecksum() != 0L && msg.getChecksum() != logic.checksum()) {
			session.desyncLogged = true;
			LogUtil.debug(Env.SERVER, "Minigame {} desync for {} at tick {}", session.minigameId, player.getGameProfile().getName(), logic.tickCount());
		}

		if (msg.isFinished() || logic.isFinished() || logic.tickCount() >= MAX_SESSION_TICKS) {
			finish(player, session, true);
		} else if (session.origin != MinigameOrigin.CHALLENGE) {
			sendProgress(player, session);
		}
	}

	private static void sendProgress(ServerPlayer player, Session session) {
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
			MinigameLogic logic = session.logic;
			int tp = (int) Math.floor(computeReward(data, session));
			NetworkHandler.sendToPlayer(new MinigameResultS2C(session.id, false, false, tp, logic.levelsCleared(), logic.tickCount(),
					(float) logic.performance(), (float) logic.rewardRate(), session.mentor, data.getCharacter().getLearnRunsFor(session.minigameId),
					Math.max(1, ConfigManager.getTrainingConfig().getLearnRequiredRuns()), false, false, MinigameResultS2C.CHALLENGE_NONE, false), player);
		});
	}

	private static double computeReward(StatsData data, Session session) {
		if (session.invalid || session.origin == MinigameOrigin.CHALLENGE) return 0.0;
		TrainingConfig config = ConfigManager.getTrainingConfig();
		TrainingConfig.MinigameSettings settings = config.getSettings(session.minigameId);
		int tpc = data.getSingleStatCost(data.getTpCostTotalStats());
		if (tpc == Integer.MAX_VALUE) tpc = ConfigManager.getServerConfig().getGameplay().getMinTPCost();
		double reward = session.logic.rewardUnits() * config.computeTpsPerMinute(tpc, settings);
		reward *= data.getTpSourceMultiplier(TpSource.TRAINING);
		if (session.mentor) reward *= 1.0 + Math.max(0.0, config.getMentorBonus());
		float limit = settings.getTpsLimitPerGame();
		if (limit > 0 && reward > limit) reward = limit;
		return Math.max(0.0, reward);
	}

	private static void finish(ServerPlayer player, Session session, boolean notify) {
		UUID uuid = player.getUUID();
		SESSIONS.remove(uuid, session);
		stopAnimation(player);
		session.logic.forceFinish();

		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		TrainingConfig config = ConfigManager.getTrainingConfig();
		MinigameLogic logic = session.logic;

		int challengeOutcome = MinigameResultS2C.CHALLENGE_NONE;
		int tp = 0;
		boolean learnedNow = false;
		boolean learnRunCounted = false;
		boolean songLearned = false;

		if (session.origin == MinigameOrigin.CHALLENGE) {
			challengeOutcome = resolveChallenge(player, data, session);
		} else if (!session.invalid) {
			double reward = computeReward(data, session);
			tp = (int) Math.floor(reward);
			if (reward > 0) {
				data.getResources().addTrainingPoints((float) reward);
				player.playSound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.0F);
				NetworkHandler.sendToTrackingEntityAndSelf(new ProgressionSyncS2C(player), player);
			}
			if (session.origin == MinigameOrigin.MASTER && !session.knownAtStart && !data.getCharacter().isMinigameKnown(session.minigameId)
					&& logic.level() >= Math.max(1, config.getLearnRequiredLevel())) {
				learnRunCounted = true;
				int runs = data.getCharacter().addMinigameLearnRun(session.minigameId);
				if (runs >= Math.max(1, config.getLearnRequiredRuns())) {
					data.getCharacter().addKnownMinigame(session.minigameId);
					data.getCharacter().clearMinigameLearnRuns(session.minigameId);
					learnedNow = true;
				}
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
			if (logic instanceof RhythmLogic rhythm && rhythm.chart() != null && rhythm.isSongCompleted() && rhythm.health() > 0
					&& session.track != null && !session.track.isEmpty() && !BuiltinSongs.isBuiltin(session.track)
					&& rhythm.accuracy() >= config.getRhythm().getLearnSongAccuracy() && !data.getCharacter().isSongLearned(session.track)) {
				data.getCharacter().addLearnedSong(session.track);
				songLearned = true;
				NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			}
		}

		if (!notify) return;
		int runs = learnedNow ? Math.max(1, config.getLearnRequiredRuns()) : data.getCharacter().getLearnRunsFor(session.minigameId);
		NetworkHandler.sendToPlayer(new MinigameResultS2C(session.id, true, session.invalid, tp, logic.levelsCleared(), logic.tickCount(),
				(float) logic.performance(), (float) logic.rewardRate(), session.mentor, runs, Math.max(1, config.getLearnRequiredRuns()),
				learnedNow, learnRunCounted, challengeOutcome, songLearned), player);
	}

	private static int resolveChallenge(ServerPlayer player, StatsData data, Session session) {
		UUID uuid = player.getUUID();
		ChallengeProgress progress = CHALLENGES.get(uuid);
		if (session.invalid || progress == null || progress.nextStage != session.challengeStage || !session.logic.isTargetReached()) {
			CHALLENGES.remove(uuid);
			return MinigameResultS2C.CHALLENGE_FAILED;
		}
		progress.nextStage++;
		progress.lastActivityMs = System.currentTimeMillis();
		if (progress.nextStage >= MinigameLogics.CHALLENGE_STAGES.size()) {
			CHALLENGES.remove(uuid);
			NPCActionC2S.grantOldKaiChallengeReward(player, data);
			NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
			return MinigameResultS2C.CHALLENGE_COMPLETED;
		}
		return MinigameResultS2C.CHALLENGE_STAGE_CLEARED;
	}

	private static void startAnimation(ServerPlayer player) {
		String animation = TRAINING_ANIMATIONS[player.getRandom().nextInt(TRAINING_ANIMATIONS.length)];
		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION, 1, -1, animation), player);
	}

	private static void stopAnimation(ServerPlayer player) {
		NetworkHandler.sendToTrackingEntityAndSelf(
				new TriggerAnimationS2C(player.getUUID(), TriggerAnimationS2C.AnimationType.KI_ANIMATION_STOP, 0, -1, ""), player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		Session session = SESSIONS.get(player.getUUID());
		if (session != null) finish(player, session, false);
		CHALLENGES.remove(player.getUUID());
		COMMAND_GRANTS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty() && CHALLENGES.isEmpty()) return;
		MinecraftServer server = event.getServer();
		if (server == null || server.getTickCount() % 20 != 0) return;
		long now = System.currentTimeMillis();
		for (Map.Entry<UUID, Session> entry : new ArrayList<>(SESSIONS.entrySet())) {
			Session session = entry.getValue();
			if (now - session.lastInputMs < IDLE_TIMEOUT_MS) continue;
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) finish(player, session, true);
			else SESSIONS.remove(entry.getKey());
		}
		CHALLENGES.entrySet().removeIf(e -> now - e.getValue().lastActivityMs > CHALLENGE_TIMEOUT_MS && !SESSIONS.containsKey(e.getKey()));
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		SESSIONS.clear();
		CHALLENGES.clear();
		COMMAND_GRANTS.clear();
	}
}
