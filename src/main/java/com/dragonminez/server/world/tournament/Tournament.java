package com.dragonminez.server.world.tournament;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TournamentDefinition;
import com.dragonminez.common.init.EntityAttributes;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.common.init.entities.MastersEntity;
import com.dragonminez.common.init.entities.sagas.DBSagasEntity;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.RaidMusicS2C;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.raid.RaidFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class Tournament {

	private Tournament() {}

	public static class Bracket {

		public static final String PLAYER_SLOT = "@player";

		private String tournamentId = "";
		private TournamentDefinition.Format format = TournamentDefinition.Format.BRACKET;
		private int round = 0;
		private boolean eliminated = false;
		private boolean completed = false;

		private final List<String> seeds = new ArrayList<>();
		private final List<List<String>> winners = new ArrayList<>();

		private String semifinalist = "";
		private String champion = "";

		public Bracket() {
		}

		public static Bracket draw(String tournamentId, TournamentDefinition def, Random random) {
			Bracket bracket = new Bracket();
			bracket.tournamentId = tournamentId;

			List<String> pool = new ArrayList<>();
			for (TournamentDefinition.Fighter fighter : def.getContenders()) {
				if (fighter.isUsable()) pool.add(fighter.getEntityId());
			}
			Collections.shuffle(pool, random);

			bracket.format = def.formatOr(TournamentDefinition.Format.BRACKET);
			if (bracket.format == TournamentDefinition.Format.GAUNTLET) {
				bracket.seeds.addAll(pool);
				bracket.semifinalist = def.getSemifinalist().getEntityId();
				bracket.champion = def.getChampion().getEntityId();
				return bracket;
			}

			int slots = def.qualifierSlotsOr(4);
			while (slots > 2 && pool.size() < slots - 1) slots /= 2;

			List<String> entrants = new ArrayList<>();
			entrants.add(PLAYER_SLOT);
			for (int i = 0; i < slots - 1 && i < pool.size(); i++) entrants.add(pool.get(i));
			Collections.shuffle(entrants, random);

			bracket.seeds.addAll(entrants);
			bracket.semifinalist = def.getSemifinalist().getEntityId();
			bracket.champion = def.getChampion().getEntityId();
			return bracket;
		}

		public String getTournamentId() { return tournamentId; }
		public int getRound() { return round; }
		public boolean isEliminated() { return eliminated; }
		public boolean isCompleted() { return completed; }
		public List<String> getSeeds() { return Collections.unmodifiableList(seeds); }
		public String getSemifinalist() { return semifinalist; }
		public String getChampion() { return champion; }

		public List<List<String>> getWinners() {
			List<List<String>> copy = new ArrayList<>(winners.size());
			for (List<String> roundWinners : winners) copy.add(Collections.unmodifiableList(roundWinners));
			return Collections.unmodifiableList(copy);
		}

		public TournamentDefinition.Format getFormat() { return format; }

		public boolean isGauntlet() {
			return format == TournamentDefinition.Format.GAUNTLET;
		}

		public int qualifierRounds() {
			if (isGauntlet()) return seeds.size();

			int rounds = 0;
			for (int remaining = seeds.size(); remaining > 1; remaining /= 2) rounds++;
			return rounds;
		}

		public int semiRound() { return qualifierRounds(); }
		public int finalRound() { return qualifierRounds() + 1; }
		public int totalRounds() { return qualifierRounds() + 2; }

		public boolean isQualifierRound(int r) { return r < qualifierRounds(); }

		public boolean isActive() {
			return !eliminated && !completed;
		}

		private int playerPosition(int r) {
			int position = seeds.indexOf(PLAYER_SLOT);
			if (position < 0) return 0;
			return position >> r;
		}

		private String entrantAt(int r, int position) {
			if (r == 0) {
				return position >= 0 && position < seeds.size() ? seeds.get(position) : "";
			}
			List<String> previous = r - 1 < winners.size() ? winners.get(r - 1) : List.of();
			return position >= 0 && position < previous.size() ? previous.get(position) : "";
		}

		public String currentOpponent() {
			if (round == semiRound()) return semifinalist;
			if (round == finalRound()) return champion;
			if (!isQualifierRound(round)) return "";

			if (isGauntlet()) return round < seeds.size() ? seeds.get(round) : "";

			return entrantAt(round, playerPosition(round) ^ 1);
		}

		public void advance(Random random) {
			if (!isActive()) return;

			if (!isGauntlet() && isQualifierRound(round)) {
				resolveQualifierRound(round, random);
			}

			round++;
			if (round >= totalRounds()) {
				round = totalRounds();
				completed = true;
			}
		}

		private void resolveQualifierRound(int r, Random random) {
			while (winners.size() <= r) winners.add(new ArrayList<>());
			List<String> roundWinners = winners.get(r);
			if (!roundWinners.isEmpty()) return;

			int playerPos = playerPosition(r);
			int entrants = seeds.size() >> r;

			for (int match = 0; match < entrants / 2; match++) {
				String left = entrantAt(r, match * 2);
				String right = entrantAt(r, match * 2 + 1);

				if (match == playerPos / 2) {
					// The player just won this one; no need to roll for it.
					roundWinners.add(PLAYER_SLOT);
				} else if (left.isEmpty()) {
					roundWinners.add(right);
				} else if (right.isEmpty()) {
					roundWinners.add(left);
				} else {
					roundWinners.add(random.nextBoolean() ? left : right);
				}
			}
		}

		public void eliminate() {
			eliminated = true;
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("tournament", tournamentId);
			tag.putString("format", format.name());
			tag.putInt("round", round);
			tag.putBoolean("eliminated", eliminated);
			tag.putBoolean("completed", completed);
			tag.putString("semifinalist", semifinalist);
			tag.putString("champion", champion);

			ListTag seedList = new ListTag();
			for (String id : seeds) {
				CompoundTag entry = new CompoundTag();
				entry.putString("id", id);
				seedList.add(entry);
			}
			tag.put("seeds", seedList);

			ListTag winnerRounds = new ListTag();
			for (List<String> roundWinners : winners) {
				CompoundTag entry = new CompoundTag();
				ListTag ids = new ListTag();
				for (String id : roundWinners) {
					CompoundTag winner = new CompoundTag();
					winner.putString("id", id);
					ids.add(winner);
				}
				entry.put("ids", ids);
				winnerRounds.add(entry);
			}
			tag.put("winners", winnerRounds);
			return tag;
		}

		public static Bracket load(CompoundTag tag) {
			Bracket bracket = new Bracket();
			bracket.tournamentId = tag.getString("tournament");
			try {
				String stored = tag.getString("format");
				if (!stored.isEmpty()) bracket.format = TournamentDefinition.Format.valueOf(stored);
			} catch (IllegalArgumentException ignored) {
				// Unknown format from a newer or edited save: fall back to the bracket.
			}
			bracket.round = tag.getInt("round");
			bracket.eliminated = tag.getBoolean("eliminated");
			bracket.completed = tag.getBoolean("completed");
			bracket.semifinalist = tag.getString("semifinalist");
			bracket.champion = tag.getString("champion");

			ListTag seedList = tag.getList("seeds", Tag.TAG_COMPOUND);
			for (int i = 0; i < seedList.size(); i++) {
				bracket.seeds.add(seedList.getCompound(i).getString("id"));
			}

			ListTag winnerRounds = tag.getList("winners", Tag.TAG_COMPOUND);
			for (int r = 0; r < winnerRounds.size(); r++) {
				List<String> roundWinners = new ArrayList<>();
				ListTag ids = winnerRounds.getCompound(r).getList("ids", Tag.TAG_COMPOUND);
				for (int i = 0; i < ids.size(); i++) roundWinners.add(ids.getCompound(i).getString("id"));
				bracket.winners.add(roundWinners);
			}
			return bracket;
		}
	}

	public static class Progress extends SavedData {
		private static final String NAME = "dragonminez_tournaments";

		private final Map<UUID, Bracket> brackets = new HashMap<>();
		private final Map<UUID, Map<String, Long>> reentryAt = new HashMap<>();

		public static Progress get(ServerLevel level) {
			ServerLevel target = level.getServer().overworld();
			return target.getDataStorage().computeIfAbsent(Progress::load, Progress::new, NAME);
		}

		@Nullable
		public Bracket getBracket(UUID player) {
			return brackets.get(player);
		}

		public void setBracket(UUID player, Bracket bracket) {
			brackets.put(player, bracket);
			setDirty();
		}

		public void clearBracket(UUID player) {
			if (brackets.remove(player) != null) setDirty();
		}

		public long remainingCooldownSeconds(UUID player, String tournamentId) {
			Map<String, Long> perTournament = reentryAt.get(player);
			if (perTournament == null) return 0L;

			Long until = perTournament.get(tournamentId);
			if (until == null) return 0L;

			long remaining = until - System.currentTimeMillis();
			return remaining <= 0L ? 0L : (remaining + 999L) / 1000L;
		}

		public void startCooldown(UUID player, String tournamentId, int seconds) {
			if (seconds <= 0) {
				clearCooldown(player, tournamentId);
				return;
			}
			reentryAt.computeIfAbsent(player, key -> new HashMap<>())
					.put(tournamentId, System.currentTimeMillis() + seconds * 1000L);
			setDirty();
		}

		public boolean clearCooldown(UUID player, String tournamentId) {
			Map<String, Long> perTournament = reentryAt.get(player);
			if (perTournament == null || perTournament.remove(tournamentId) == null) return false;

			if (perTournament.isEmpty()) reentryAt.remove(player);
			setDirty();
			return true;
		}

		public int clearAllCooldowns(UUID player) {
			Map<String, Long> perTournament = reentryAt.remove(player);
			if (perTournament == null || perTournament.isEmpty()) return 0;

			setDirty();
			return perTournament.size();
		}

		public static Progress load(CompoundTag tag) {
			Progress data = new Progress();

			ListTag list = tag.getList("brackets", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				if (!entry.hasUUID("player")) continue;
				data.brackets.put(entry.getUUID("player"), Bracket.load(entry.getCompound("bracket")));
			}

			ListTag cooldowns = tag.getList("cooldowns", Tag.TAG_COMPOUND);
			for (int i = 0; i < cooldowns.size(); i++) {
				CompoundTag entry = cooldowns.getCompound(i);
				if (!entry.hasUUID("player") || !entry.contains("entries", Tag.TAG_LIST)) continue;

				Map<String, Long> perTournament = new HashMap<>();
				ListTag entries = entry.getList("entries", Tag.TAG_COMPOUND);
				for (int j = 0; j < entries.size(); j++) {
					CompoundTag stored = entries.getCompound(j);
					perTournament.put(stored.getString("tournament"), stored.getLong("until"));
				}
				if (!perTournament.isEmpty()) data.reentryAt.put(entry.getUUID("player"), perTournament);
			}
			return data;
		}

		@Override
		public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
			ListTag list = new ListTag();
			for (Map.Entry<UUID, Bracket> entry : brackets.entrySet()) {
				CompoundTag stored = new CompoundTag();
				stored.putUUID("player", entry.getKey());
				stored.put("bracket", entry.getValue().save());
				list.add(stored);
			}
			tag.put("brackets", list);

			ListTag cooldowns = new ListTag();
			long now = System.currentTimeMillis();
			for (Map.Entry<UUID, Map<String, Long>> playerEntry : reentryAt.entrySet()) {
				ListTag entries = new ListTag();
				for (Map.Entry<String, Long> entry : playerEntry.getValue().entrySet()) {
					if (entry.getValue() <= now) continue;
					CompoundTag stored = new CompoundTag();
					stored.putString("tournament", entry.getKey());
					stored.putLong("until", entry.getValue());
					entries.add(stored);
				}
				if (entries.isEmpty()) continue;

				CompoundTag playerTag = new CompoundTag();
				playerTag.putUUID("player", playerEntry.getKey());
				playerTag.put("entries", entries);
				cooldowns.add(playerTag);
			}
			tag.put("cooldowns", cooldowns);
			return tag;
		}
	}

	public static final class RingAnchor {

		private RingAnchor() {}

		@Nullable
		public static StructureStart arenaAt(ServerLevel level, BlockPos pos) {
			var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
			for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
				Holder<Structure> holder = registry.wrapAsHolder(structure);
				if (!holder.is(MainTags.Structures.BUILD_PROTECTED)) continue;
				StructureStart start = level.structureManager().getStructureAt(pos, structure);
				if (start.isValid()) return start;
			}
			return null;
		}

		public static Rotation rotationOf(StructureStart start) {
			for (StructurePiece piece : start.getPieces()) {
				if (piece instanceof PoolElementStructurePiece pool) return pool.getRotation();
			}
			return Rotation.NONE;
		}

		public static Vec3i toTemplate(BoundingBox box, Rotation rotation, BlockPos world) {
			int wx = world.getX() - box.minX();
			int wy = world.getY() - box.minY();
			int wz = world.getZ() - box.minZ();

			int width = box.getXSpan();
			int depth = box.getZSpan();

			return switch (rotation) {
				case CLOCKWISE_90 -> new Vec3i(wz, wy, width - 1 - wx);
				case CLOCKWISE_180 -> new Vec3i(width - 1 - wx, wy, depth - 1 - wz);
				case COUNTERCLOCKWISE_90 -> new Vec3i(depth - 1 - wz, wy, wx);
				default -> new Vec3i(wx, wy, wz);
			};
		}

		public static BlockPos toWorld(BoundingBox box, Rotation rotation, int tx, int ty, int tz) {
			int width = box.getXSpan();
			int depth = box.getZSpan();

			int wx;
			int wz;
			switch (rotation) {
				case CLOCKWISE_90 -> {
					wx = width - 1 - tz;
					wz = tx;
				}
				case CLOCKWISE_180 -> {
					wx = width - 1 - tx;
					wz = depth - 1 - tz;
				}
				case COUNTERCLOCKWISE_90 -> {
					wx = tz;
					wz = depth - 1 - tx;
				}
				default -> {
					wx = tx;
					wz = tz;
				}
			}
			return new BlockPos(box.minX() + wx, box.minY() + ty, box.minZ() + wz);
		}
	}

	public static final class Manager {

		public static final String MATCH_TAG = "dmz_tournament_match";
		private static final String STATS_CONFIGURED_TAG = "dmz_stats_configured";

		private static final int COUNTDOWN_SECONDS = 3;
		private static final double DEFAULT_HEALTH = 100.0D;
		private static final double FORFEIT_DISTANCE_SQR = 60.0D * 60.0D;

		private static final Map<UUID, ActiveMatch> ACTIVE = new HashMap<>();
		private static final Random RANDOM = new Random();

		private Manager() {}

		public static void reset() {
			ACTIVE.clear();
		}

		@Nullable
		public static Bracket bracketOf(ServerPlayer player) {
			return Progress.get(player.serverLevel()).getBracket(player.getUUID());
		}

		@Nullable
		public static Bracket bracketOf(ServerPlayer player, String tournamentId) {
			Bracket bracket = bracketOf(player);
			return bracket != null && bracket.getTournamentId().equals(tournamentId) ? bracket : null;
		}

		public static boolean isFighting(ServerPlayer player) {
			return ACTIVE.containsKey(player.getUUID());
		}

		@Nullable
		public static Component signUp(ServerPlayer player, String tournamentId) {
			TournamentDefinition def = ConfigManager.getTournament(tournamentId);
			if (def == null) return Component.translatable("tournament.dragonminez.unavailable");

			Progress data = Progress.get(player.serverLevel());

			// One bracket at a time: a finished run elsewhere is fine to replace, a live one is not.
			Bracket existing = data.getBracket(player.getUUID());
			if (existing != null && existing.isActive()) {
				return existing.getTournamentId().equals(tournamentId) ? null
						: Component.translatable("tournament.dragonminez.busy_elsewhere");
			}

			long cooldown = data.remainingCooldownSeconds(player.getUUID(), tournamentId);
			if (cooldown > 0L) {
				return Component.translatable("tournament.dragonminez.cooldown", formatDuration(cooldown));
			}

			data.setBracket(player.getUUID(), Bracket.draw(tournamentId, def, RANDOM));
			return null;
		}

		@Nullable
		public static Component beginMatch(ServerPlayer player, BlockPos ringCentre, String tournamentId) {
			if (isFighting(player)) return Component.translatable("tournament.dragonminez.already_fighting");

			Bracket bracket = bracketOf(player, tournamentId);
			if (bracket == null || !bracket.isActive()) {
				return Component.translatable("tournament.dragonminez.not_entered");
			}

			TournamentDefinition def = ConfigManager.getTournament(bracket.getTournamentId());
			if (def == null) return Component.translatable("tournament.dragonminez.unavailable");

			TournamentDefinition.Fighter fighter = fighterFor(def, bracket);
			if (fighter == null) return Component.translatable("tournament.dragonminez.unavailable");

			ServerLevel level = player.serverLevel();
			int radius = def.getRing() != null ? def.getRing().radius() : 4;

			Mob opponent = spawnOpponent(level, fighter, ringCentre, radius, bracket.getRound(), player);
			if (opponent == null) return Component.translatable("tournament.dragonminez.unavailable");

			player.teleportTo(level, ringCentre.getX() + 0.5D - radius, ringCentre.getY(), ringCentre.getZ() + 0.5D,
					90.0F, 0.0F);

			opponent.setNoAi(true);
			if (opponent instanceof DBSagasEntity saga) saga.setCombatFrozen(true);

			long fightAt = level.getGameTime() + COUNTDOWN_SECONDS * 20L;
			ACTIVE.put(player.getUUID(), new ActiveMatch(opponent.getUUID(), bracket.getRound(),
					fightAt + def.matchTimeoutSecondsOr(300) * 20L, ringCentre, level.dimension(), fightAt));

			NetworkHandler.sendToPlayer(new TournamentPackets.CountdownS2C(COUNTDOWN_SECONDS), player);
			return null;
		}

		@Nullable
		public static UUID ownerOf(Entity entity) {
			if (entity == null) return null;
			String owner = entity.getPersistentData().getString(MATCH_TAG);
			if (owner.isEmpty()) return null;
			try {
				return UUID.fromString(owner);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		public static boolean isInGrace(UUID playerId, long gameTime) {
			ActiveMatch match = ACTIVE.get(playerId);
			return match != null && match.inGrace(gameTime);
		}

		public static void tick(ServerLevel level) {
			if (ACTIVE.isEmpty()) return;

			Iterator<Map.Entry<UUID, ActiveMatch>> it = ACTIVE.entrySet().iterator();
			List<Runnable> outcomes = new ArrayList<>();

			while (it.hasNext()) {
				Map.Entry<UUID, ActiveMatch> entry = it.next();
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
				ActiveMatch match = entry.getValue();

				if (player == null) continue;
				if (!level.dimension().equals(match.dimension())) continue;

				Entity opponent = level.getEntity(match.opponentId());

				if (!match.inGrace(level.getGameTime()) && opponent instanceof Mob mob && mob.isNoAi()) {
					mob.setNoAi(false);
					if (mob instanceof DBSagasEntity saga) saga.setCombatFrozen(false);
					mob.setTarget(player);
				} else if (opponent instanceof Mob mob && mob.getTarget() != player) {
					mob.setTarget(player);
				}

				boolean opponentGone = opponent == null || !opponent.isAlive();
				boolean timedOut = level.getGameTime() > match.deadline();
				boolean walkedOut = player.distanceToSqr(
						match.ringCentre().getX() + 0.5D,
						match.ringCentre().getY(),
						match.ringCentre().getZ() + 0.5D) > FORFEIT_DISTANCE_SQR;

				if (player.isDeadOrDying()) {
					it.remove();
					outcomes.add(() -> loseMatch(player, match));
				} else if (walkedOut) {
					it.remove();
					outcomes.add(() -> forfeitMatch(player, match));
				} else if (opponentGone) {
					it.remove();
					outcomes.add(() -> winMatch(player, match));
				} else if (timedOut) {
					it.remove();
					outcomes.add(() -> voidMatch(player, match, opponent));
				}
			}

			for (Runnable outcome : outcomes) outcome.run();
		}

		public static boolean isInNonLethalMatch(ServerPlayer player) {
			ActiveMatch match = ACTIVE.get(player.getUUID());
			if (match == null) return false;

			Bracket bracket = bracketOf(player);
			if (bracket == null) return false;

			TournamentDefinition def = ConfigManager.getTournament(bracket.getTournamentId());
			return def != null && !def.isLethal();
		}

		public static void knockOut(ServerPlayer player) {
			ActiveMatch match = ACTIVE.remove(player.getUUID());
			if (match == null) return;

			player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.1F));
			player.clearFire();
			player.sendSystemMessage(Component.translatable("tournament.dragonminez.knockout")
					.withStyle(ChatFormatting.RED));
			loseMatch(player, match);
		}

		public static void onPlayerDeath(ServerPlayer player) {
			ActiveMatch match = ACTIVE.remove(player.getUUID());
			if (match != null) loseMatch(player, match);
		}

		private static void winMatch(ServerPlayer player, ActiveMatch match) {
			Progress data = Progress.get(player.serverLevel());
			Bracket bracket = data.getBracket(player.getUUID());
			if (bracket == null) return;

			bracket.advance(RANDOM);
			data.setBracket(player.getUUID(), bracket);

			if (bracket.isCompleted()) {
				TournamentDefinition def = ConfigManager.getTournament(bracket.getTournamentId());
				grantVictory(player, def, match.ringCentre());
				data.startCooldown(player.getUUID(), bracket.getTournamentId(),
						def != null ? def.victoryCooldownSecondsOr(20 * 60) : 20 * 60);
			} else {
				player.sendSystemMessage(Component.translatable("tournament.dragonminez.round_won",
						match.round() + 1, roundName(bracket, match.round()))
						.withStyle(ChatFormatting.GREEN));
				player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
			}
		}

		private static Component roundName(Bracket bracket, int round) {
			if (round == bracket.finalRound()) return Component.translatable("tournament.dragonminez.round.final");
			if (round == bracket.semiRound()) return Component.translatable("tournament.dragonminez.round.semi");
			return Component.translatable("tournament.dragonminez.round.qualifier", round + 1);
		}

		private static void loseMatch(ServerPlayer player, ActiveMatch match) {
			Progress data = Progress.get(player.serverLevel());
			Bracket bracket = data.getBracket(player.getUUID());
			if (bracket == null) return;

			bracket.eliminate();
			data.setBracket(player.getUUID(), bracket);

			TournamentDefinition def = ConfigManager.getTournament(bracket.getTournamentId());
			int cooldown = def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60;
			data.startCooldown(player.getUUID(), bracket.getTournamentId(), cooldown);

			despawnOpponent(player.serverLevel(), match.opponentId());
			player.sendSystemMessage(Component.translatable("tournament.dragonminez.round_lost",
					match.round() + 1, roundName(bracket, match.round()), formatDuration(cooldown))
					.withStyle(ChatFormatting.RED));
			player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
		}

		private static void grantVictory(ServerPlayer player, @Nullable TournamentDefinition def, BlockPos ringCentre) {
			ServerLevel level = player.serverLevel();

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.victory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

			NetworkHandler.sendToPlayer(new RaidMusicS2C(RaidFeedback.VICTORY_MUSIC), player);
			RaidFeedback.launchFireworks(level, ringCentre);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
					80, 0.4, 0.8, 0.4, 0.35);

			if (def == null || def.getRewards() == null) return;
			TournamentDefinition.Rewards rewards = def.getRewards();

			int trainingPoints = rewards.trainingPointsOr(0);
			int alignment = rewards.alignmentOr(0);
			if (trainingPoints > 0 || alignment != 0) {
				StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
					if (trainingPoints > 0) data.getResources().addTrainingPoints(trainingPoints, false);
					if (alignment != 0) data.getResources().addAlignment(alignment);
				});
			}
			if (trainingPoints > 0) {
				player.sendSystemMessage(Component.translatable("tournament.dragonminez.reward.tp", trainingPoints).withStyle(ChatFormatting.YELLOW));
			}

			List<String> commands = rewards.getCommands();
			if (commands == null || commands.isEmpty()) return;
			for (String command : commands) {
				if (command == null || command.isBlank()) continue;
				String parsed = command.replace("%player%", player.getName().getString());
				level.getServer().getCommands().performPrefixedCommand(
						level.getServer().createCommandSourceStack().withPermission(4), parsed);
			}
		}

		private static void forfeitMatch(ServerPlayer player, ActiveMatch match) {
			Progress data = Progress.get(player.serverLevel());
			Bracket bracket = data.getBracket(player.getUUID());
			despawnOpponent(player.serverLevel(), match.opponentId());

			if (bracket == null) return;
			bracket.eliminate();
			data.setBracket(player.getUUID(), bracket);

			TournamentDefinition def = ConfigManager.getTournament(bracket.getTournamentId());
			int cooldown = def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60;
			data.startCooldown(player.getUUID(), bracket.getTournamentId(), cooldown);

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.forfeit",
					formatDuration(cooldown)));
		}

		private static void voidMatch(ServerPlayer player, ActiveMatch match, Entity opponent) {
			if (opponent != null) opponent.discard();
			player.displayClientMessage(Component.translatable("tournament.dragonminez.timeout"), false);
		}

		private static void despawnOpponent(ServerLevel level, UUID opponentId) {
			Entity opponent = level.getEntity(opponentId);
			if (opponent != null) opponent.discard();
		}

		@Nullable
		private static TournamentDefinition.Fighter fighterFor(TournamentDefinition def, Bracket bracket) {
			String wanted = bracket.currentOpponent();
			if (wanted == null || wanted.isBlank()) return null;

			if (wanted.equals(def.getChampion().getEntityId())) return def.getChampion();
			if (wanted.equals(def.getSemifinalist().getEntityId())) return def.getSemifinalist();
			for (TournamentDefinition.Fighter fighter : def.getContenders()) {
				if (wanted.equals(fighter.getEntityId())) return fighter;
			}
			return null;
		}

		@Nullable
		private static Mob spawnOpponent(ServerLevel level, TournamentDefinition.Fighter fighter,
										 BlockPos ringCentre, int radius, int round, ServerPlayer target) {
			ResourceLocation id = ResourceLocation.tryParse(fighter.getEntityId());
			if (id == null) return null;

			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
			if (type == null) {
				LogUtil.warn(Env.SERVER, "Tournament fighter '{}' is not a registered entity", fighter.getEntityId());
				return null;
			}

			Entity created = type.create(level);
			if (!(created instanceof Mob mob)) {
				if (created != null) created.discard();
				return null;
			}

			mob.moveTo(ringCentre.getX() + 0.5D + radius, ringCentre.getY(), ringCentre.getZ() + 0.5D, 270.0F, 0.0F);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(ringCentre), MobSpawnType.EVENT, null, null);

			mob.getPersistentData().putString(MATCH_TAG, target.getUUID().toString());
			mob.getPersistentData().putBoolean(STATS_CONFIGURED_TAG, true);
			mob.setPersistenceRequired();

			if (mob instanceof DBSagasEntity saga) {
				saga.setAiTierById(fighter.aiTierOr(1));
				saga.setTransformationDisabled(true);
			}
			mob.setTarget(target);

			level.addFreshEntity(mob);

			applyStats(mob, fighter, round);
			return mob;
		}

		private static void applyStats(Mob mob, TournamentDefinition.Fighter fighter, int round) {
			double scaling = Math.pow(fighter.perRoundScalingOr(1.0D), round);
			double health = fighter.healthOr(DEFAULT_HEALTH) * scaling;

			AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null) maxHealth.setBaseValue(health);

			AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
			if (attack != null) attack.setBaseValue(fighter.meleeDamageOr(5.0D) * scaling);

			AttributeInstance kiDamage = mob.getAttribute(EntityAttributes.KI_BLAST_DAMAGE.get());
			if (kiDamage != null) kiDamage.setBaseValue(fighter.kiDamageOr(5.0D) * scaling);

			mob.setHealth(mob.getMaxHealth());
		}

		private static String formatDuration(long seconds) {
			long minutes = seconds / 60L;
			long rest = seconds % 60L;
			return minutes > 0L ? minutes + "m " + rest + "s" : rest + "s";
		}

		private record ActiveMatch(UUID opponentId, int round, long deadline, BlockPos ringCentre,
								   ResourceKey<Level> dimension, long fightAt) {
			boolean inGrace(long gameTime) {
				return gameTime < fightAt;
			}
		}
	}

	public static final class Service {

		private static final double MAX_NPC_DISTANCE_SQR = 12.0D * 12.0D;

		private Service() {}

		public static void handleAction(ServerPlayer player, TournamentPackets.ActionC2S.Action action, int npcEntityId) {
			MastersEntity npc = resolveNpc(player, npcEntityId);
			if (npc == null) return;

			String tournamentId = TournamentDefinition.HOST_NPCS.get(npc.getMasterName());
			if (tournamentId == null) return;

			switch (action) {
				case OPEN_BRACKET -> sendBracket(player, tournamentId, npcEntityId);
				case SIGN_UP -> {
					Component refusal = Manager.signUp(player, tournamentId);
					if (refusal != null) {
						player.displayClientMessage(refusal, true);
						return;
					}
					sendBracket(player, tournamentId, npcEntityId);
				}
				case START_MATCH -> {
					BlockPos ring = resolveRing(player, npc, tournamentId);
					if (ring == null) {
						player.displayClientMessage(Component.translatable("tournament.dragonminez.no_ring"), true);
						return;
					}
					Component refusal = Manager.beginMatch(player, ring, tournamentId);
					if (refusal != null) player.displayClientMessage(refusal, true);
				}
			}
		}

		@Nullable
		private static MastersEntity resolveNpc(ServerPlayer player, int npcEntityId) {
			Entity entity = player.serverLevel().getEntity(npcEntityId);
			if (!(entity instanceof MastersEntity master)) return null;
			if (player.distanceToSqr(entity) > MAX_NPC_DISTANCE_SQR) return null;
			return master;
		}

		private static void sendBracket(ServerPlayer player, String tournamentId, int npcEntityId) {
			TournamentDefinition def = ConfigManager.getTournament(tournamentId);
			if (def == null) {
				player.displayClientMessage(Component.translatable("tournament.dragonminez.unavailable"), true);
				return;
			}

			Progress data = Progress.get(player.serverLevel());
			Bracket bracket = Manager.bracketOf(player, tournamentId);
			boolean signUp = bracket == null || !bracket.isActive();

			NetworkHandler.sendToPlayer(new TournamentPackets.OpenBracketS2C(
					tournamentId,
					def.displayNameOr("tournament.dragonminez." + tournamentId),
					def.difficultyStarsOr(1),
					bracket != null ? bracket.getSeeds() : List.of(),
					bracket != null ? bracket.getWinners() : List.of(),
					bracket != null ? bracket.getSemifinalist() : def.getSemifinalist().getEntityId(),
					bracket != null ? bracket.getChampion() : def.getChampion().getEntityId(),
					bracket != null ? bracket.getRound() : 0,
					bracket != null && bracket.isEliminated(),
					bracket != null && bracket.isCompleted(),
					bracket != null ? bracket.isGauntlet()
							: def.formatOr(TournamentDefinition.Format.BRACKET) == TournamentDefinition.Format.GAUNTLET,
					def.isLethal(),
					signUp,
					(int) data.remainingCooldownSeconds(player.getUUID(), tournamentId),
					npcEntityId,
					collectStats(def, bracket)
			), player);
		}

		private static Map<String, TournamentPackets.OpenBracketS2C.FighterStats> collectStats(
				TournamentDefinition def, @Nullable Bracket bracket) {
			Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats = new HashMap<>();

			int round = bracket != null ? bracket.getRound() : 0;
			for (TournamentDefinition.Fighter fighter : def.getContenders()) {
				putStats(stats, fighter, round);
			}
			int semiRound = bracket != null ? bracket.semiRound() : 1;
			putStats(stats, def.getSemifinalist(), semiRound);
			putStats(stats, def.getChampion(), semiRound + 1);
			return stats;
		}

		private static void putStats(Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats,
									 TournamentDefinition.Fighter fighter, int round) {
			if (fighter == null || !fighter.isUsable()) return;
			double scaling = Math.pow(fighter.perRoundScalingOr(1.0D), round);
			stats.put(fighter.getEntityId(), new TournamentPackets.OpenBracketS2C.FighterStats(
					(int) Math.round(fighter.healthOr(100.0D) * scaling),
					(int) Math.round(fighter.meleeDamageOr(5.0D) * scaling),
					(int) Math.round(fighter.kiDamageOr(5.0D) * scaling)));
		}

		@Nullable
		private static BlockPos resolveRing(ServerPlayer player, MastersEntity npc, String tournamentId) {
			TournamentDefinition def = ConfigManager.getTournament(tournamentId);
			if (def == null) return null;

			ServerLevel level = player.serverLevel();
			StructureStart start = RingAnchor.arenaAt(level, npc.blockPosition());
			if (start == null) start = RingAnchor.arenaAt(level, player.blockPosition());
			if (start == null) return null;

			BoundingBox box = start.getBoundingBox();

			TournamentDefinition.RingOffset ring = def.getRing();
			if (ring != null && ring.isExplicit()) {
				return RingAnchor.toWorld(box, RingAnchor.rotationOf(start), ring.x(), ring.y(), ring.z());
			}

			return groundInFrontOf(level, npc, ring != null ? ring.radius() : 4, box);
		}

		private static BlockPos groundInFrontOf(ServerLevel level, MastersEntity npc, int distance, BoundingBox box) {
			float yaw = npc.getYRot() * ((float) Math.PI / 180F);
			double dx = -Math.sin(yaw) * (distance + 1);
			double dz = Math.cos(yaw) * (distance + 1);

			int x = (int) Math.round(npc.getX() + dx);
			int z = (int) Math.round(npc.getZ() + dz);

			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, (int) Math.round(npc.getY()) + 2, z);
			int floor = Math.max(box.minY(), (int) Math.round(npc.getY()) - 8);
			while (cursor.getY() > floor) {
				if (!level.getBlockState(cursor.below()).isAir()) return cursor.immutable();
				cursor.move(0, -1, 0);
			}
			return new BlockPos(x, (int) Math.round(npc.getY()), z);
		}

	}
}
