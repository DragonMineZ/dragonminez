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
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.TournamentPackets;
import com.dragonminez.common.quest.PartyManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.world.raid.RaidFeedback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class Tournament {

	private Tournament() {}

	public static class Bracket {

		public static final String PLAYER_SLOT = "@player";
		public static final String HIDDEN_SLOT = "?";
		public static final int SOURCE_SEMIFINALIST = -1;
		public static final int SOURCE_CHAMPION = -2;
		public static final int SOURCE_UNKNOWN = Integer.MIN_VALUE;
		private static final String PLAYER_PREFIX = "@player:";

		public static String slotFor(UUID player) {
			return PLAYER_PREFIX + player;
		}

		public static boolean isPlayerSlot(String slot) {
			return slot != null && (PLAYER_SLOT.equals(slot) || slot.startsWith(PLAYER_PREFIX));
		}

		@Nullable
		public static UUID playerOf(String slot) {
			if (slot == null || !slot.startsWith(PLAYER_PREFIX)) return null;
			try {
				return UUID.fromString(slot.substring(PLAYER_PREFIX.length()));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		private String tournamentId = "";
		private TournamentDefinition.Format format = TournamentDefinition.Format.BRACKET;
		private int round = 0;
		private boolean eliminated = false;
		private boolean completed = false;

		private final List<String> seeds = new ArrayList<>();
		private final List<List<String>> winners = new ArrayList<>();

		private String semifinalist = "";
		private String champion = "";
		private final Map<String, Integer> sources = new HashMap<>();

		public Bracket() {
		}

		public int sourceOf(String slot) {
			return slot == null ? SOURCE_UNKNOWN : sources.getOrDefault(slot, SOURCE_UNKNOWN);
		}

		public void setSource(String slot, int source) {
			if (slot != null && !slot.isEmpty()) sources.put(slot, source);
		}

		public static String resolveFighter(String entityId, Set<String> used, Random random) {
			if (entityId == null || entityId.isBlank()) return "";
			if (!entityId.startsWith("#")) {
				used.add(entityId);
				return entityId;
			}
			ResourceLocation tagId = ResourceLocation.tryParse(entityId.substring(1));
			if (tagId == null) return "";
			List<String> members = new ArrayList<>();
			var tags = ForgeRegistries.ENTITY_TYPES.tags();
			if (tags == null) return "";
			tags.getTag(TagKey.create(Registries.ENTITY_TYPE, tagId)).forEach(type -> {
				ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
				if (key != null) members.add(key.toString());
			});
			if (members.isEmpty()) {
				LogUtil.warn(Env.SERVER, "Tournament fighter tag '{}' is empty or unknown", entityId);
				return "";
			}
			List<String> fresh = new ArrayList<>(members);
			fresh.removeAll(used);
			List<String> pool = fresh.isEmpty() ? members : fresh;
			String picked = pool.get(random.nextInt(pool.size()));
			used.add(picked);
			return picked;
		}

		public static Bracket draw(String tournamentId, TournamentDefinition def, List<UUID> players,
								   Random random) {
			Bracket bracket = new Bracket();
			bracket.tournamentId = tournamentId;

			Set<String> used = new HashSet<>();
			bracket.semifinalist = resolveFighter(def.getSemifinalist().getEntityId(), used, random);
			bracket.setSource(bracket.semifinalist, SOURCE_SEMIFINALIST);
			bracket.champion = resolveFighter(def.getChampion().getEntityId(), used, random);
			bracket.setSource(bracket.champion, SOURCE_CHAMPION);

			List<String> pool = new ArrayList<>();
			for (int pass = 0; pass < 2; pass++) {
				for (int i = 0; i < def.getContenders().size(); i++) {
					TournamentDefinition.Fighter fighter = def.getContenders().get(i);
					if (!fighter.isUsable() || fighter.isTag() != (pass == 1)) continue;
					String resolved = resolveFighter(fighter.getEntityId(), used, random);
					if (resolved.isEmpty() || bracket.sources.containsKey(resolved)) continue;
					bracket.setSource(resolved, i);
					pool.add(resolved);
				}
			}
			Collections.shuffle(pool, random);
			if (pool.isEmpty()) {
				LogUtil.warn(Env.SERVER, "Tournament '{}' drew no contenders; check the entity ids and tags in its config", tournamentId);
			}

			bracket.format = def.formatOr(TournamentDefinition.Format.BRACKET);
			if (bracket.format == TournamentDefinition.Format.GAUNTLET) {
				bracket.seeds.addAll(pool);
				return bracket;
			}

			int entrants = Math.max(1, players.size());
			int slots = def.qualifierSlotsOr(4);
			while (slots < entrants && slots < 16) slots *= 2;
			while (slots > 2 && slots > entrants && pool.size() < slots - entrants) slots /= 2;

			String[] board = new String[slots];
			int block = Math.max(1, slots / entrants);
			for (int i = 0; i < entrants && i < slots; i++) {
				int offset = block > 1 ? random.nextInt(block) : 0;
				int position = Math.min(slots - 1, i * block + offset);
				board[position] = slotFor(players.get(i));
			}

			int next = 0;
			for (int i = 0; i < slots; i++) {
				if (board[i] != null) continue;
				board[i] = next < pool.size() ? pool.get(next++) : "";
			}

			bracket.seeds.addAll(List.of(board));
			return bracket;
		}

		public String getTournamentId() { return tournamentId; }
		public int getRound() { return round; }
		public boolean isEliminated() { return eliminated; }
		public boolean isCompleted() { return completed; }
		public List<String> getSeeds() { return Collections.unmodifiableList(seeds); }
		public String getSemifinalist() { return semifinalist; }
		public String getChampion() { return champion; }
		public TournamentDefinition.Format getFormat() { return format; }

		public List<List<String>> getWinners() {
			List<List<String>> copy = new ArrayList<>(winners.size());
			for (List<String> roundWinners : winners) copy.add(Collections.unmodifiableList(roundWinners));
			return Collections.unmodifiableList(copy);
		}

		public boolean isGauntlet() {
			return format == TournamentDefinition.Format.GAUNTLET;
		}

		public boolean hasContenders() {
			for (String seed : seeds) if (!seed.isEmpty() && !isPlayerSlot(seed)) return true;
			return false;
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

		public void eliminate() { eliminated = true; }
		public void complete() { completed = true; }


		public String currentOpponent() {
			if (round == semiRound()) return semifinalist;
			if (round == finalRound()) return champion;
			if (!isQualifierRound(round)) return "";
			return round < seeds.size() ? seeds.get(round) : "";
		}

		public void advance() {
			if (!isActive()) return;
			round++;
			if (round >= totalRounds()) {
				round = totalRounds();
				completed = true;
			}
		}


		public int pairCount(int r) {
			if (r < qualifierRounds()) return Math.max(1, (seeds.size() >> r) / 2);
			return 1;
		}

		public String slotAt(int r, int position) {
			if (r == 0) {
				return position >= 0 && position < seeds.size() ? seeds.get(position) : "";
			}
			List<String> previous = winnersOf(r - 1);
			return position >= 0 && position < previous.size() ? previous.get(position) : "";
		}

		public List<String> winnersOf(int r) {
			while (winners.size() <= r) winners.add(new ArrayList<>());
			return winners.get(r);
		}

		public int nextMatchIndex(int r) {
			return winnersOf(r).size();
		}

		public boolean isRoundComplete(int r) {
			return nextMatchIndex(r) >= pairCount(r);
		}

		public String[] matchSlots(int r, int m) {
			if (r < qualifierRounds()) {
				return new String[]{slotAt(r, m * 2), slotAt(r, m * 2 + 1)};
			}
			if (r == semiRound()) {
				List<String> qualified = winnersOf(Math.max(0, qualifierRounds() - 1));
				String survivor = qualified.isEmpty() ? slotAt(0, 0) : qualified.get(0);
				return new String[]{survivor, semifinalist};
			}
			List<String> semi = winnersOf(semiRound());
			return new String[]{semi.isEmpty() ? "" : semi.get(0), champion};
		}

		public void recordWinner(int r, String slot) {
			winnersOf(r).add(slot == null ? "" : slot);
		}

		public void advanceRound() {
			if (round <= finalRound()) round++;
		}

		public boolean replaceSeed(String slot, String replacement) {
			int index = seeds.indexOf(slot);
			if (index < 0) return false;
			seeds.set(index, replacement == null ? "" : replacement);
			return true;
		}

		public List<String> hiddenSeeds() {
			List<String> hidden = new ArrayList<>(seeds.size());
			for (int i = 0; i < seeds.size(); i++) hidden.add(HIDDEN_SLOT);
			return hidden;
		}

		@Nullable
		public String finalWinner() {
			List<String> last = winnersOf(finalRound());
			return last.isEmpty() ? null : last.get(0);
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

			ListTag sourceList = new ListTag();
			for (Map.Entry<String, Integer> entry : sources.entrySet()) {
				CompoundTag stored = new CompoundTag();
				stored.putString("id", entry.getKey());
				stored.putInt("source", entry.getValue());
				sourceList.add(stored);
			}
			tag.put("sources", sourceList);

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
			}
			bracket.round = tag.getInt("round");
			bracket.eliminated = tag.getBoolean("eliminated");
			bracket.completed = tag.getBoolean("completed");
			bracket.semifinalist = tag.getString("semifinalist");
			bracket.champion = tag.getString("champion");

			ListTag sourceList = tag.getList("sources", Tag.TAG_COMPOUND);
			for (int i = 0; i < sourceList.size(); i++) {
				CompoundTag stored = sourceList.getCompound(i);
				bracket.sources.put(stored.getString("id"), stored.getInt("source"));
			}

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

	public static final class Run {

		public enum Phase { RULES, INTERMISSION, PREVIEW, BOUT }

		private String tournamentId = "";
		private UUID partyId;

		private final List<UUID> participants = new ArrayList<>();
		private final Set<UUID> eliminated = new LinkedHashSet<>();
		private final Set<UUID> accepted = new LinkedHashSet<>();
		private final Set<UUID> declined = new LinkedHashSet<>();
		private final Set<UUID> ready = new LinkedHashSet<>();

		private Bracket bracket = new Bracket();

		private Phase phase = Phase.RULES;
		private long phaseDeadline;
		private boolean revealed;

		private UUID activeFighter;
		private UUID activeRival;
		private boolean finished;

		private BlockPos ringCentre = BlockPos.ZERO;
		private ResourceKey<Level> dimension = Level.OVERWORLD;
		private UUID standingOpponent;

		private final Map<UUID, Long> awayDeadline = new HashMap<>();
		private final Set<UUID> arrived = new LinkedHashSet<>();

		public Run() {
		}

		public String getTournamentId() { return tournamentId; }
		public UUID getPartyId() { return partyId; }
		public List<UUID> getParticipants() { return Collections.unmodifiableList(participants); }
		public boolean isFinished() { return finished; }
		public void finish() { finished = true; }
		public BlockPos getRingCentre() { return ringCentre; }
		public ResourceKey<Level> getDimension() { return dimension; }
		public UUID getActiveFighter() { return activeFighter; }
		public void setActiveFighter(UUID id) { activeFighter = id; }
		public UUID getActiveRival() { return activeRival; }
		public void setActiveRival(UUID id) { activeRival = id; }
		public Phase getPhase() { return phase; }
		public void setPhase(Phase phase) { this.phase = phase; }
		public long getPhaseDeadline() { return phaseDeadline; }
		public void setPhaseDeadline(long deadline) { phaseDeadline = deadline; }
		public boolean isRevealed() { return revealed; }
		public void setRevealed(boolean revealed) { this.revealed = revealed; }
		public UUID getStandingOpponent() { return standingOpponent; }
		public void setStandingOpponent(UUID id) { standingOpponent = id; }

		public Bracket getBracket() { return bracket; }
		public boolean isGauntlet() { return bracket.isGauntlet(); }

		public boolean isParticipant(UUID player) { return participants.contains(player); }
		public boolean isEliminated(UUID player) { return eliminated.contains(player); }
		public boolean isAlive(UUID player) { return participants.contains(player) && !eliminated.contains(player); }
		public Set<UUID> getEliminated() { return Collections.unmodifiableSet(eliminated); }

		public boolean isAccepted(UUID player) { return accepted.contains(player); }
		public boolean isDeclined(UUID player) { return declined.contains(player); }
		public boolean hasAnswered(UUID player) { return accepted.contains(player) || declined.contains(player); }

		public void accept(UUID player) {
			declined.remove(player);
			accepted.add(player);
		}

		public void decline(UUID player) {
			accepted.remove(player);
			declined.add(player);
		}

		public List<UUID> unanswered() {
			List<UUID> pending = new ArrayList<>();
			for (UUID id : aliveParticipants()) if (!hasAnswered(id)) pending.add(id);
			return pending;
		}

		public boolean isReady(UUID player) { return ready.contains(player); }
		public void markReady(UUID player) { ready.add(player); }
		public void clearReady() { ready.clear(); }

		public List<UUID> aliveParticipants() {
			List<UUID> alive = new ArrayList<>();
			for (UUID id : participants) if (!eliminated.contains(id)) alive.add(id);
			return alive;
		}

		public void eliminate(UUID player) {
			eliminated.add(player);
			awayDeadline.remove(player);
		}

		@Nullable
		public Long getAwayDeadline(UUID player) { return awayDeadline.get(player); }
		public void setAwayDeadline(UUID player, long deadline) { awayDeadline.put(player, deadline); }
		public void clearAwayDeadline(UUID player) { awayDeadline.remove(player); }

		public boolean hasArrived(UUID player) { return arrived.contains(player); }
		public void markArrived(UUID player) { arrived.add(player); }

		public static Run create(String tournamentId, TournamentDefinition def, List<UUID> members,
								 @Nullable UUID partyId, BlockPos ringCentre, ResourceKey<Level> dimension,
								 long rulesDeadline, Random random) {
			Run run = new Run();
			run.tournamentId = tournamentId;
			run.partyId = partyId;
			run.participants.addAll(members);
			run.ringCentre = ringCentre;
			run.dimension = dimension;
			run.phase = Phase.RULES;
			run.phaseDeadline = rulesDeadline;
			run.bracket = Bracket.draw(tournamentId, def, members, random);
			run.activeFighter = members.isEmpty() ? null : members.get(0);
			return run;
		}

		public CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("tournament", tournamentId);
			if (partyId != null) tag.putUUID("party", partyId);

			tag.put("participants", uuidList(participants));
			tag.put("eliminated", uuidList(eliminated));
			tag.put("accepted", uuidList(accepted));
			tag.put("declined", uuidList(declined));
			tag.put("ready", uuidList(ready));
			tag.put("bracket", bracket.save());

			if (activeFighter != null) tag.putUUID("active", activeFighter);
			if (activeRival != null) tag.putUUID("rival", activeRival);
			if (standingOpponent != null) tag.putUUID("standing", standingOpponent);
			tag.putString("phase", phase.name());
			tag.putLong("phaseDeadline", phaseDeadline);
			tag.putBoolean("revealed", revealed);
			tag.putBoolean("finished", finished);
			tag.putInt("ringX", ringCentre.getX());
			tag.putInt("ringY", ringCentre.getY());
			tag.putInt("ringZ", ringCentre.getZ());
			tag.putString("dimension", dimension.location().toString());
			return tag;
		}

		public static Run load(CompoundTag tag) {
			Run run = new Run();
			run.tournamentId = tag.getString("tournament");
			if (tag.hasUUID("party")) run.partyId = tag.getUUID("party");

			run.participants.addAll(uuids(tag, "participants"));
			run.eliminated.addAll(uuids(tag, "eliminated"));
			run.accepted.addAll(uuids(tag, "accepted"));
			run.declined.addAll(uuids(tag, "declined"));
			run.ready.addAll(uuids(tag, "ready"));

			if (tag.contains("bracket", Tag.TAG_COMPOUND)) run.bracket = Bracket.load(tag.getCompound("bracket"));

			if (tag.hasUUID("active")) run.activeFighter = tag.getUUID("active");
			if (tag.hasUUID("rival")) run.activeRival = tag.getUUID("rival");
			if (tag.hasUUID("standing")) run.standingOpponent = tag.getUUID("standing");
			try {
				String stored = tag.getString("phase");
				if (!stored.isEmpty()) run.phase = Phase.valueOf(stored);
			} catch (IllegalArgumentException ignored) {
			}
			run.phaseDeadline = tag.getLong("phaseDeadline");
			run.revealed = tag.getBoolean("revealed");
			run.finished = tag.getBoolean("finished");
			run.ringCentre = new BlockPos(tag.getInt("ringX"), tag.getInt("ringY"), tag.getInt("ringZ"));

			ResourceLocation dim = ResourceLocation.tryParse(tag.getString("dimension"));
			if (dim != null) run.dimension = ResourceKey.create(Registries.DIMENSION, dim);
			return run;
		}

		private static ListTag uuidList(Iterable<UUID> ids) {
			ListTag list = new ListTag();
			for (UUID id : ids) {
				CompoundTag entry = new CompoundTag();
				entry.putUUID("id", id);
				list.add(entry);
			}
			return list;
		}

		private static List<UUID> uuids(CompoundTag tag, String key) {
			List<UUID> ids = new ArrayList<>();
			ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				if (entry.hasUUID("id")) ids.add(entry.getUUID("id"));
			}
			return ids;
		}
	}

	public static class Progress extends SavedData {
		private static final String NAME = "dragonminez_tournaments";

		private final Map<String, Run> runs = new LinkedHashMap<>();
		private final Map<UUID, Map<String, Long>> reentryAt = new HashMap<>();

		public static Progress get(ServerLevel level) {
			ServerLevel target = level.getServer().overworld();
			return target.getDataStorage().computeIfAbsent(Progress::load, Progress::new, NAME);
		}

		@Nullable
		public Run getRun(String tournamentId) {
			Run run = runs.get(tournamentId);
			return run != null && !run.isFinished() ? run : null;
		}

		@Nullable
		public Run runOf(UUID player) {
			for (Run run : runs.values()) {
				if (!run.isFinished() && run.isParticipant(player)) return run;
			}
			return null;
		}

		public List<Run> activeRuns() {
			List<Run> active = new ArrayList<>();
			for (Run run : runs.values()) if (!run.isFinished()) active.add(run);
			return active;
		}

		public void putRun(Run run) {
			runs.put(run.getTournamentId(), run);
			setDirty();
		}

		public void removeRun(String tournamentId) {
			if (runs.remove(tournamentId) != null) setDirty();
		}

		@Nullable
		public Bracket getBracket(UUID player) {
			Run run = runOf(player);
			return run != null ? run.getBracket() : null;
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

			ListTag runList = tag.getList("runs", Tag.TAG_COMPOUND);
			for (int i = 0; i < runList.size(); i++) {
				Run run = Run.load(runList.getCompound(i));
				if (!run.getTournamentId().isEmpty()) data.runs.put(run.getTournamentId(), run);
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
			ListTag runList = new ListTag();
			for (Run run : runs.values()) {
				if (run.isFinished()) continue;
				runList.add(run.save());
			}
			tag.put("runs", runList);

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

		public static final int COUNTDOWN_SECONDS = 3;
		public static final int PREVIEW_SECONDS = 5;
		public static final double EXCLUSION_RADIUS = 15.0D;
		private static final double DEFAULT_HEALTH = 100.0D;
		private static final double FORFEIT_DISTANCE_SQR = 60.0D * 60.0D;
		private static final double PUSH_SPEED = 0.6D;

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
		public static Run runOf(ServerPlayer player) {
			return Progress.get(player.serverLevel()).runOf(player.getUUID());
		}

		public static boolean isFighting(ServerPlayer player) {
			return ACTIVE.containsKey(player.getUUID());
		}

		public static boolean isFighting(UUID player) {
			return ACTIVE.containsKey(player);
		}

		public static boolean isEntered(ServerPlayer player) {
			Run run = runOf(player);
			return run != null && !run.isEliminated(player.getUUID());
		}

		public static boolean arePvpRivals(UUID a, UUID b) {
			if (a == null || b == null) return false;
			ActiveMatch match = ACTIVE.get(a);
			return match != null && match.pvp() && b.equals(match.opponentId());
		}

		public static boolean canDamageFighter(ServerPlayer target, @Nullable Entity attacker, long gameTime) {
			ActiveMatch match = ACTIVE.get(target.getUUID());
			if (match == null) return true;
			if (attacker == null) return true;
			if (match.inGrace(gameTime)) return false;
			return attacker.getUUID().equals(match.opponentId());
		}

		public static boolean isInGrace(UUID playerId, long gameTime) {
			ActiveMatch match = ACTIVE.get(playerId);
			return match != null && match.inGrace(gameTime);
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

		@Nullable
		public static Component signUp(ServerPlayer player, String tournamentId, BlockPos ringCentre) {
			TournamentDefinition def = ConfigManager.getTournament(tournamentId);
			if (def == null) return Component.translatable("tournament.dragonminez.unavailable");

			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);

			Run running = data.getRun(tournamentId);
			if (running != null) {
				return running.isAlive(player.getUUID()) ? null
						: Component.translatable("tournament.dragonminez.in_progress");
			}

			if (data.runOf(player.getUUID()) != null) {
				return Component.translatable("tournament.dragonminez.busy_elsewhere");
			}

			long cooldown = data.remainingCooldownSeconds(player.getUUID(), tournamentId);
			if (cooldown > 0L) {
				return Component.translatable("tournament.dragonminez.cooldown", formatDuration(cooldown));
			}

			boolean inParty = PartyManager.isInParty(player);
			if (inParty && !PartyManager.isPartyLeader(player)) {
				return Component.translatable("tournament.dragonminez.party_leader_only");
			}

			List<UUID> members = new ArrayList<>();
			members.add(player.getUUID());

			if (inParty) {
				for (ServerPlayer member : PartyManager.getAllPartyMembers(player)) {
					if (member.getUUID().equals(player.getUUID())) continue;

					if (data.runOf(member.getUUID()) != null) {
						return Component.translatable("tournament.dragonminez.member_busy", member.getName());
					}

					long memberCooldown = data.remainingCooldownSeconds(member.getUUID(), tournamentId);
					if (memberCooldown > 0L) {
						return Component.translatable("tournament.dragonminez.member_cooldown",
								member.getName(), formatDuration(memberCooldown));
					}
					members.add(member.getUUID());
				}
			}

			long rulesDeadline = level.getGameTime() + def.rulesAcceptSecondsOr(60) * 20L;
			Run run = Run.create(tournamentId, def, members, inParty ? PartyManager.getPartyId(player) : null,
					ringCentre, level.dimension(), rulesDeadline, RANDOM);
			if (!run.getBracket().hasContenders()) {
				LogUtil.warn(Env.SERVER, "Tournament '{}' refused a sign-up: no contender could be resolved from its config", tournamentId);
				return Component.translatable("tournament.dragonminez.unavailable");
			}
			data.putRun(run);

			Component name = Component.translatable(def.displayNameOr("tournament.dragonminez." + tournamentId));
			broadcast(level, run, Component.translatable("tournament.dragonminez.entered", name)
					.withStyle(ChatFormatting.GOLD));

			Service.openForAll(level, run);
			return null;
		}

		public static void answerRules(ServerPlayer player, boolean accept) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null || !run.isAlive(player.getUUID())) {
				player.displayClientMessage(Component.translatable("tournament.dragonminez.not_entered"), true);
				return;
			}
			if (run.getPhase() != Run.Phase.RULES || run.hasAnswered(player.getUUID())) return;

			if (accept) {
				run.accept(player.getUUID());
				player.sendSystemMessage(Component.translatable("tournament.dragonminez.rules_accepted")
						.withStyle(ChatFormatting.GREEN));
				broadcast(level, run, Component.translatable("tournament.dragonminez.member_accepted",
						player.getName()).withStyle(ChatFormatting.GRAY));
			} else {
				run.decline(player.getUUID());
				dropOut(level, data, run, player);
			}
			data.setDirty();

			if (run.unanswered().isEmpty()) settleRules(level, data, run);
			else Service.pushBracket(level, run);
		}

		private static void dropOut(ServerLevel level, Progress data, Run run, ServerPlayer player) {
			UUID id = player.getUUID();
			run.eliminate(id);

			String replacement = "";
			if (!run.isGauntlet()) {
				TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
				replacement = replacementFighter(def, run.getBracket());
				run.getBracket().replaceSeed(Bracket.slotFor(id), replacement);
			}

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.rules_declined")
					.withStyle(ChatFormatting.RED));
			broadcast(level, run, Component.translatable("tournament.dragonminez.member_declined",
					player.getName()).withStyle(ChatFormatting.RED));
			data.setDirty();
		}

		private static String replacementFighter(@Nullable TournamentDefinition def, Bracket bracket) {
			if (def == null) return "";
			Set<String> taken = new HashSet<>(bracket.getSeeds());
			taken.add(bracket.getSemifinalist());
			taken.add(bracket.getChampion());
			Set<Integer> drawn = new HashSet<>();
			for (String seed : bracket.getSeeds()) {
				int source = bracket.sourceOf(seed);
				if (source >= 0) drawn.add(source);
			}
			List<Integer> unused = new ArrayList<>();
			List<Integer> usable = new ArrayList<>();
			for (int i = 0; i < def.getContenders().size(); i++) {
				if (!def.getContenders().get(i).isUsable()) continue;
				usable.add(i);
				if (!drawn.contains(i)) unused.add(i);
			}
			List<Integer> pool = unused.isEmpty() ? usable : unused;
			if (pool.isEmpty()) return "";
			for (int attempt = 0; attempt < pool.size() * 2; attempt++) {
				int index = pool.get(RANDOM.nextInt(pool.size()));
				Set<String> used = new HashSet<>(taken);
				String resolved = Bracket.resolveFighter(def.getContenders().get(index).getEntityId(), used, RANDOM);
				if (resolved.isEmpty()) continue;
				if (taken.contains(resolved) && attempt < pool.size()) continue;
				bracket.setSource(resolved, index);
				return resolved;
			}
			return "";
		}

		private static void settleRules(ServerLevel level, Progress data, Run run) {
			for (UUID id : run.unanswered()) {
				run.decline(id);
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
				if (player != null) {
					dropOut(level, data, run, player);
				} else {
					run.eliminate(id);
				}
			}

			if (run.aliveParticipants().isEmpty()) {
				cancelRun(level, data, run);
				return;
			}

			run.setRevealed(true);
			data.setDirty();
			broadcast(level, run, Component.translatable("tournament.dragonminez.rules_settled")
					.withStyle(ChatFormatting.GOLD));
			syncTurn(level, data, run);
		}

		private static void cancelRun(ServerLevel level, Progress data, Run run) {
			broadcast(level, run, Component.translatable("tournament.dragonminez.cancelled")
					.withStyle(ChatFormatting.RED));
			finishRun(level, data, run);
		}

		private static Vec3 centre(BlockPos pos) {
			return new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		}

		private record Bout(int round, int match, String left, String right,
							@Nullable UUID leftPlayer, @Nullable UUID rightPlayer) {

			boolean pvp() { return leftPlayer != null && rightPlayer != null; }

			UUID fighter() { return leftPlayer != null ? leftPlayer : rightPlayer; }

			String fighterSlot() { return leftPlayer != null ? left : right; }

			String opponentSlot() { return leftPlayer != null ? right : left; }
		}

		@Nullable
		private static Bout nextBout(Run run) {
			Bracket bracket = run.getBracket();

			for (int guard = 0; guard < 256; guard++) {
				int r = bracket.getRound();
				if (r > bracket.finalRound()) return null;

				int m = bracket.nextMatchIndex(r);
				if (m >= bracket.pairCount(r)) {
					bracket.advanceRound();
					continue;
				}

				String[] pair = bracket.matchSlots(r, m);
				UUID left = alivePlayerOf(run, pair[0]);
				UUID right = alivePlayerOf(run, pair[1]);

				if (left == null && right == null) {
					bracket.recordWinner(r, roll(pair[0], pair[1]));
					continue;
				}
				if (left == null && Bracket.isPlayerSlot(pair[0])) {
					bracket.recordWinner(r, pair[1]);
					continue;
				}
				if (right == null && Bracket.isPlayerSlot(pair[1])) {
					bracket.recordWinner(r, pair[0]);
					continue;
				}
				if (pair[0].isEmpty() || pair[1].isEmpty()) {
					bracket.recordWinner(r, pair[0].isEmpty() ? pair[1] : pair[0]);
					continue;
				}
				return new Bout(r, m, pair[0], pair[1], left, right);
			}
			return null;
		}

		@Nullable
		private static UUID alivePlayerOf(Run run, String slot) {
			UUID id = Bracket.playerOf(slot);
			return id != null && run.isAlive(id) ? id : null;
		}

		private static String roll(String left, String right) {
			if (Bracket.isPlayerSlot(left) || left.isEmpty()) return right;
			if (Bracket.isPlayerSlot(right) || right.isEmpty()) return left;
			return RANDOM.nextBoolean() ? left : right;
		}

		private static void syncTurn(ServerLevel level, Progress data, Run run) {
			if (run.isFinished()) return;

			if (run.aliveParticipants().isEmpty()) {
				run.getBracket().eliminate();
				finishRun(level, data, run);
				return;
			}

			if (run.isGauntlet()) {
				syncGauntletTurn(level, data, run);
				return;
			}

			Bout bout = nextBout(run);
			if (bout == null) {
				UUID champion = Bracket.playerOf(run.getBracket().finalWinner());
				if (champion != null && run.isAlive(champion)) {
					run.getBracket().complete();
					crown(level, data, run, champion);
					return;
				}
				finishRun(level, data, run);
				return;
			}

			run.setActiveFighter(bout.pvp() ? bout.leftPlayer() : bout.fighter());
			run.setActiveRival(bout.pvp() ? bout.rightPlayer() : null);
			startIntermission(level, data, run, bout.pvp());
		}

		private static void syncGauntletTurn(ServerLevel level, Progress data, Run run) {
			List<UUID> alive = run.aliveParticipants();
			if (alive.isEmpty()) {
				finishRun(level, data, run);
				return;
			}

			UUID current = run.getActiveFighter();
			UUID next;
			if (current != null && alive.contains(current)) {
				next = current;
			} else {
				List<UUID> pool = new ArrayList<>(alive);
				pool.remove(current);
				next = pool.isEmpty() ? alive.get(0) : pool.get(RANDOM.nextInt(pool.size()));
			}

			run.setActiveFighter(next);
			run.setActiveRival(null);
			startIntermission(level, data, run, false);
		}

		private static void startIntermission(ServerLevel level, Progress data, Run run, boolean pvp) {
			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			int seconds = def != null ? def.nextRoundSecondsOr(30) : 30;
			run.setPhase(Run.Phase.INTERMISSION);
			run.setPhaseDeadline(level.getGameTime() + seconds * 20L);
			run.clearReady();
			data.setDirty();

			UUID activeId = run.getActiveFighter();
			ServerPlayer active = activeId != null ? level.getServer().getPlayerList().getPlayer(activeId) : null;
			ServerPlayer rival = run.getActiveRival() != null
					? level.getServer().getPlayerList().getPlayer(run.getActiveRival()) : null;

			if (pvp && active != null && rival != null) {
				broadcast(level, run, Component.translatable("tournament.dragonminez.pvp_bout",
						active.getName(), rival.getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
			for (UUID id : run.getParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant == null || run.isEliminated(id)) continue;
				if (id.equals(activeId) || id.equals(run.getActiveRival())) {
					participant.sendSystemMessage(Component.translatable("tournament.dragonminez.your_turn", seconds)
							.withStyle(ChatFormatting.GOLD));
				} else if (active != null) {
					participant.sendSystemMessage(Component.translatable("tournament.dragonminez.turn_of",
							active.getName(), seconds).withStyle(ChatFormatting.YELLOW));
				}
			}

			Service.openForAll(level, run);
			Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.NEXT_ROUND, seconds);
		}

		public static void ready(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			UUID id = player.getUUID();
			if (run == null || run.getPhase() != Run.Phase.INTERMISSION || !run.isAlive(id)) return;
			if (!id.equals(run.getActiveFighter()) && !id.equals(run.getActiveRival())) return;
			if (run.isReady(id)) return;

			run.markReady(id);
			data.setDirty();
			broadcast(level, run, Component.translatable("tournament.dragonminez.member_ready", player.getName())
					.withStyle(ChatFormatting.GREEN));

			boolean rivalReady = run.getActiveRival() == null || run.isReady(run.getActiveRival());
			if (run.isReady(run.getActiveFighter()) && rivalReady) startPreview(level, data, run);
			else Service.pushBracket(level, run);
		}

		private static void startPreview(ServerLevel level, Progress data, Run run) {
			run.setPhase(Run.Phase.PREVIEW);
			run.setPhaseDeadline(level.getGameTime() + PREVIEW_SECONDS * 20L);
			data.setDirty();
			Service.openForAll(level, run);
			Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.PREVIEW, PREVIEW_SECONDS);
		}

		@Nullable
		private static Component beginMatch(ServerPlayer player, Run run, Progress data) {
			if (isFighting(player)) return Component.translatable("tournament.dragonminez.already_fighting");

			ServerLevel level = player.serverLevel();
			if (!run.isAlive(player.getUUID())) return Component.translatable("tournament.dragonminez.not_entered");
			if (!player.getUUID().equals(run.getActiveFighter())) {
				return Component.translatable("tournament.dragonminez.not_your_turn");
			}

			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			if (def == null) return Component.translatable("tournament.dragonminez.unavailable");

			Component refusal = run.isGauntlet()
					? beginGauntletMatch(level, data, run, def, player)
					: beginBracketMatch(level, data, run, def, player);
			if (refusal != null) return refusal;

			run.setPhase(Run.Phase.BOUT);
			run.setPhaseDeadline(0L);
			data.setDirty();
			Service.pushBracket(level, run);
			Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.COUNTDOWN, COUNTDOWN_SECONDS);
			return null;
		}

		@Nullable
		private static Component beginGauntletMatch(ServerLevel level, Progress data, Run run,
													TournamentDefinition def, ServerPlayer player) {
			Bracket bracket = run.getBracket();
			if (!bracket.isActive()) return Component.translatable("tournament.dragonminez.not_entered");

			String opponentId = bracket.currentOpponent();
			TournamentDefinition.Fighter fighter = fighterFor(def, bracket, opponentId);
			if (fighter == null) return Component.translatable("tournament.dragonminez.unavailable");

			int radius = ringRadius(def);
			Mob opponent = reuseStandingOpponent(level, run, opponentId);
			if (opponent == null) {
				opponent = spawnOpponent(level, fighter, opponentId, run.getRingCentre(), radius, bracket.getRound(), player);
			} else {
				opponent.getPersistentData().putString(MATCH_TAG, player.getUUID().toString());
				opponent.setTarget(player);
			}
			if (opponent == null) return Component.translatable("tournament.dragonminez.unavailable");

			run.setStandingOpponent(opponent.getUUID());
			startBout(level, data, run, def, player, opponent, null,
					bracket.getRound(), 0, Bracket.PLAYER_SLOT, opponentId, radius);
			return null;
		}

		@Nullable
		private static Component beginBracketMatch(ServerLevel level, Progress data, Run run,
												   TournamentDefinition def, ServerPlayer player) {
			Bout bout = nextBout(run);
			if (bout == null || !player.getUUID().equals(bout.pvp() ? bout.leftPlayer() : bout.fighter())) {
				return Component.translatable("tournament.dragonminez.not_your_turn");
			}

			int radius = ringRadius(def);

			if (bout.pvp()) {
				ServerPlayer rival = level.getServer().getPlayerList().getPlayer(bout.rightPlayer());
				if (rival == null || isFighting(rival)) {
					return Component.translatable("tournament.dragonminez.not_your_turn");
				}

				PartyManager.setTournamentFriendlyFire(level.getServer(), run.getPartyId(), true);
				startBout(level, data, run, def, player, null, rival,
						bout.round(), bout.match(), bout.left(), bout.right(), radius);
				startBout(level, data, run, def, rival, null, player,
						bout.round(), bout.match(), bout.right(), bout.left(), -radius);
				return null;
			}

			TournamentDefinition.Fighter fighter = fighterFor(def, run.getBracket(), bout.opponentSlot());
			if (fighter == null) return Component.translatable("tournament.dragonminez.unavailable");

			Mob opponent = spawnOpponent(level, fighter, bout.opponentSlot(), run.getRingCentre(), radius, bout.round(), player);
			if (opponent == null) return Component.translatable("tournament.dragonminez.unavailable");

			run.setStandingOpponent(opponent.getUUID());
			startBout(level, data, run, def, player, opponent, null,
					bout.round(), bout.match(), bout.fighterSlot(), bout.opponentSlot(), radius);
			return null;
		}

		private static int ringRadius(TournamentDefinition def) {
			return def.getRing() != null ? def.getRing().radius() : 4;
		}

		private static void startBout(ServerLevel level, Progress data, Run run, TournamentDefinition def,
									  ServerPlayer player, @Nullable Mob opponent, @Nullable ServerPlayer rival,
									  int round, int match, String selfSlot, String rivalSlot, int offset) {
			BlockPos ring = run.getRingCentre();
			player.teleportTo(level, ring.getX() + 0.5D - offset, ring.getY(), ring.getZ() + 0.5D,
					offset >= 0 ? 90.0F : 270.0F, 0.0F);
			player.setDeltaMovement(Vec3.ZERO);

			if (opponent != null) {
				opponent.setNoAi(true);
				if (opponent instanceof DBSagasEntity saga) saga.setCombatFrozen(true);
			}

			long fightAt = level.getGameTime() + COUNTDOWN_SECONDS * 20L;
			UUID opponentId = opponent != null ? opponent.getUUID() : rival.getUUID();

			ACTIVE.put(player.getUUID(), new ActiveMatch(opponentId, rival != null, round, match,
					selfSlot, rivalSlot, fightAt + def.matchTimeoutSecondsOr(300) * 20L, ring,
					level.dimension(), fightAt));

			setFrozen(player, true);
			if (rival != null) NetworkHandler.sendToPlayer(new TournamentPackets.RivalS2C(rival.getUUID()), player);
			clearBystanders(level, player, ACTIVE.get(player.getUUID()));
		}

		public static void setFrozen(ServerPlayer player, boolean frozen) {
			StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
				if (stats.getStatus().isMatchFrozen() == frozen) return;
				stats.getStatus().setMatchFrozen(frozen);
				NetworkHandler.sendToPlayer(new ResourceSyncS2C(player), player);
			});
		}

		@Nullable
		private static Mob reuseStandingOpponent(ServerLevel level, Run run, String entityId) {
			if (!run.isGauntlet() || run.getStandingOpponent() == null) return null;

			Entity entity = level.getEntity(run.getStandingOpponent());
			if (!(entity instanceof Mob mob) || !mob.isAlive()) {
				run.setStandingOpponent(null);
				return null;
			}

			ResourceLocation wanted = ResourceLocation.tryParse(entityId);
			ResourceLocation actual = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
			if (wanted == null || !wanted.equals(actual)) {
				mob.discard();
				run.setStandingOpponent(null);
				return null;
			}
			return mob;
		}

		public static void tick(ServerLevel level) {
			tickMatches(level);
			tickRuns(level);
		}

		private static void tickMatches(ServerLevel level) {
			if (ACTIVE.isEmpty()) return;

			Iterator<Map.Entry<UUID, ActiveMatch>> it = ACTIVE.entrySet().iterator();
			List<Runnable> outcomes = new ArrayList<>();

			while (it.hasNext()) {
				Map.Entry<UUID, ActiveMatch> entry = it.next();
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
				ActiveMatch match = entry.getValue();

				if (player == null) continue;
				if (!level.dimension().equals(match.dimension())) continue;

				clearBystanders(level, player, match);

				boolean timedOut = level.getGameTime() > match.deadline();

				if (match.pvp()) {
					ServerPlayer rival = level.getServer().getPlayerList().getPlayer(match.opponentId());
					if (player.isDeadOrDying()) {
						it.remove();
						outcomes.add(() -> resolvePvp(level, player, match, false));
					} else if (rival == null || rival.isDeadOrDying()) {
						it.remove();
						outcomes.add(() -> resolvePvp(level, player, match, true));
					} else if (timedOut) {
						it.remove();
						outcomes.add(() -> voidPvp(level, player, match));
					}
					continue;
				}

				Entity opponent = level.getEntity(match.opponentId());

				if (!match.inGrace(level.getGameTime()) && opponent instanceof Mob mob && mob.isNoAi()) {
					mob.setNoAi(false);
					if (mob instanceof DBSagasEntity saga) saga.setCombatFrozen(false);
					mob.setTarget(player);
				} else if (opponent instanceof Mob mob && mob.getTarget() != player) {
					mob.setTarget(player);
				}

				boolean opponentGone = opponent == null || !opponent.isAlive();

				if (player.isDeadOrDying()) {
					it.remove();
					outcomes.add(() -> loseMatch(player, match));
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

		private static void clearBystanders(ServerLevel level, ServerPlayer fighter, ActiveMatch match) {
			Vec3 centre = fighter.position();
			AABB box = fighter.getBoundingBox().inflate(EXCLUSION_RADIUS);

			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
				if (entity == fighter || entity.getUUID().equals(match.opponentId())) continue;
				if (entity instanceof ServerPlayer other && other.isSpectator()) continue;
				if (ACTIVE.containsKey(entity.getUUID()) || ownerOf(entity) != null) continue;

				Vec3 delta = entity.position().subtract(centre);
				double distance = delta.length();
				if (distance >= EXCLUSION_RADIUS) continue;

				Vec3 flat = new Vec3(delta.x, 0.0D, delta.z);
				Vec3 direction = flat.lengthSqr() < 1.0E-4D
						? new Vec3(Math.cos(entity.getId()), 0.0D, Math.sin(entity.getId()))
						: flat.normalize();
				Vec3 target = centre.add(direction.scale(EXCLUSION_RADIUS + 0.5D));

				if (entity instanceof ServerPlayer other) {
					other.teleportTo(level, target.x, entity.getY(), target.z, other.getYRot(), other.getXRot());
					other.setDeltaMovement(direction.scale(PUSH_SPEED));
					other.hurtMarked = true;
					if (level.getGameTime() % 20L == 0L) {
						other.displayClientMessage(Component.translatable("tournament.dragonminez.keep_away",
								(int) EXCLUSION_RADIUS).withStyle(ChatFormatting.RED), true);
					}
				} else {
					entity.teleportTo(target.x, entity.getY(), target.z);
					entity.setDeltaMovement(direction.scale(PUSH_SPEED));
					entity.hurtMarked = true;
				}
			}
		}

		private static void tickRuns(ServerLevel level) {
			Progress data = Progress.get(level);
			List<Run> runs = data.activeRuns();
			if (runs.isEmpty()) return;

			for (Run run : runs) {
				if (!level.dimension().equals(run.getDimension())) continue;
				tickRun(level, data, run);
			}
		}

		private static void tickRun(ServerLevel level, Progress data, Run run) {
			long gameTime = level.getGameTime();
			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			int returnSeconds = def != null ? def.returnSecondsOr(15) : 15;
			int arrivalSeconds = def != null ? def.arrivalSecondsOr(60) : 60;

			for (UUID id : run.aliveParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant == null) continue;

				boolean away = !participant.level().dimension().equals(run.getDimension())
						|| participant.distanceToSqr(centre(run.getRingCentre())) > FORFEIT_DISTANCE_SQR;
				Long deadline = run.getAwayDeadline(id);

				if (!away) {
					run.markArrived(id);
					if (deadline != null) {
						run.clearAwayDeadline(id);
						participant.displayClientMessage(
								Component.translatable("tournament.dragonminez.returned")
										.withStyle(ChatFormatting.GREEN), true);
					}
					continue;
				}

				if (deadline == null) {
					int grace = run.hasArrived(id) ? returnSeconds : arrivalSeconds;
					run.setAwayDeadline(id, gameTime + grace * 20L);
					broadcast(level, run, Component.translatable(run.hasArrived(id)
									? "tournament.dragonminez.withdrawing"
									: "tournament.dragonminez.summoned",
							participant.getName(), grace).withStyle(ChatFormatting.RED));
					continue;
				}

				if (gameTime >= deadline) {
					withdraw(level, data, run, participant);
					if (run.isFinished()) return;
					continue;
				}

				long left = deadline - gameTime;
				participant.displayClientMessage(Component.translatable("tournament.dragonminez.return_timer",
						(int) ((left + 19) / 20)).withStyle(ChatFormatting.RED), true);
				if (left % 20L == 0L) pling(participant, 0.8F);
			}

			if (run.isFinished()) return;

			switch (run.getPhase()) {
				case RULES -> {
					if (gameTime >= run.getPhaseDeadline()) settleRules(level, data, run);
				}
				case INTERMISSION -> tickIntermission(level, data, run, gameTime);
				case PREVIEW -> tickPreview(level, data, run, gameTime);
				case BOUT -> {
					UUID activeId = run.getActiveFighter();
					boolean fighting = activeId != null && isFighting(activeId)
							|| run.getActiveRival() != null && isFighting(run.getActiveRival());
					if (!fighting) syncTurn(level, data, run);
				}
			}
		}

		private static void tickIntermission(ServerLevel level, Progress data, Run run, long gameTime) {
			UUID activeId = run.getActiveFighter();
			if (activeId == null || run.isEliminated(activeId)) {
				syncTurn(level, data, run);
				return;
			}
			if (isFighting(activeId)) return;

			long left = run.getPhaseDeadline() - gameTime;
			if (left > 0L) {
				if (left % 20L == 0L) {
					Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.NEXT_ROUND, (int) (left / 20L));
				}
				return;
			}

			List<UUID> late = new ArrayList<>();
			if (!run.isReady(activeId)) late.add(activeId);
			if (run.getActiveRival() != null && !run.isReady(run.getActiveRival())) late.add(run.getActiveRival());
			if (late.isEmpty()) {
				startPreview(level, data, run);
				return;
			}
			for (UUID id : late) {
				if (run.isFinished() || !run.isAlive(id)) continue;
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
				if (player == null) continue;
				player.sendSystemMessage(Component.translatable("tournament.dragonminez.next_round_expired")
						.withStyle(ChatFormatting.RED));
				eliminate(level, data, run, player, null);
			}
		}

		private static void tickPreview(ServerLevel level, Progress data, Run run, long gameTime) {
			long left = run.getPhaseDeadline() - gameTime;
			if (left > 0L) {
				if (left % 20L == 0L) {
					Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.PREVIEW, (int) (left / 20L));
				}
				return;
			}

			UUID activeId = run.getActiveFighter();
			ServerPlayer active = activeId != null ? level.getServer().getPlayerList().getPlayer(activeId) : null;
			if (active == null || run.isEliminated(activeId)) {
				syncTurn(level, data, run);
				return;
			}

			Component refusal = beginMatch(active, run, data);
			if (refusal != null) {
				active.displayClientMessage(refusal, true);
				skipUnplayableBout(level, data, run, active);
			}
		}

		private static void skipUnplayableBout(ServerLevel level, Progress data, Run run, ServerPlayer active) {
			Bracket bracket = run.getBracket();
			if (run.isGauntlet()) {
				LogUtil.warn(Env.SERVER, "Tournament '{}': opponent '{}' could not be fielded, skipping it",
						run.getTournamentId(), bracket.currentOpponent());
				bracket.advance();
				data.setDirty();
				if (bracket.isCompleted()) {
					crown(level, data, run, active.getUUID());
					return;
				}
				syncGauntletTurn(level, data, run);
				return;
			}

			Bout bout = nextBout(run);
			if (bout != null && !bout.pvp() && bracket.nextMatchIndex(bout.round()) == bout.match()) {
				LogUtil.warn(Env.SERVER, "Tournament '{}': opponent '{}' could not be fielded, {} advances by walkover",
						run.getTournamentId(), bout.opponentSlot(), active.getName().getString());
				bracket.recordWinner(bout.round(), bout.fighterSlot());
				data.setDirty();
			}
			syncTurn(level, data, run);
		}

		public static boolean isNonLethalOpponent(Entity mob) {
			UUID owner = ownerOf(mob);
			if (owner == null) return false;

			ActiveMatch match = ACTIVE.get(owner);
			if (match == null || match.pvp() || !mob.getUUID().equals(match.opponentId())) return false;
			if (!(mob.level() instanceof ServerLevel level)) return false;

			Run run = Progress.get(level).runOf(owner);
			if (run == null) return false;

			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			return def != null && !def.isLethal();
		}

		public static void knockOutOpponent(Mob mob) {
			UUID owner = ownerOf(mob);
			if (owner == null || !(mob.level() instanceof ServerLevel level)) return;

			ActiveMatch match = ACTIVE.remove(owner);
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
			if (match == null || player == null) return;

			mob.setHealth(Math.max(1.0F, mob.getMaxHealth() * 0.05F));
			player.sendSystemMessage(Component.translatable("tournament.dragonminez.opponent_knockout")
					.withStyle(ChatFormatting.GREEN));
			mob.discard();

			winMatch(player, match);
		}

		public static boolean isInNonLethalMatch(ServerPlayer player) {
			ActiveMatch match = ACTIVE.get(player.getUUID());
			if (match == null || match.pvp()) return false;

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

			if (match.pvp()) resolvePvp(player.serverLevel(), player, match, false);
			else loseMatch(player, match);
		}

		public static void onKnockedDown(ServerPlayer player, @Nullable Entity attacker) {
			if (attacker == null || !arePvpRivals(player.getUUID(), attacker.getUUID())) return;

			ActiveMatch match = ACTIVE.remove(player.getUUID());
			if (match == null) return;

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.knockout")
					.withStyle(ChatFormatting.RED));
			resolvePvp(player.serverLevel(), player, match, false);
		}

		public static void onPlayerDeath(ServerPlayer player) {
			ActiveMatch match = ACTIVE.remove(player.getUUID());
			if (match == null) return;

			if (match.pvp()) resolvePvp(player.serverLevel(), player, match, false);
			else loseMatch(player, match);
		}

		public static void onPlayerLogout(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null || run.isEliminated(player.getUUID())) {
				ACTIVE.remove(player.getUUID());
				return;
			}

			if (run.getPhase() == Run.Phase.RULES) {
				run.decline(player.getUUID());
				dropOut(level, data, run, player);
				if (run.unanswered().isEmpty()) settleRules(level, data, run);
				else Service.pushBracket(level, run);
				return;
			}

			broadcast(level, run, Component.translatable("tournament.dragonminez.withdrew", player.getName())
					.withStyle(ChatFormatting.RED));
			eliminate(level, data, run, player, null);
		}

		public static void onPartyLeave(ServerPlayer player) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null || run.isEliminated(player.getUUID())) return;

			broadcast(level, run, Component.translatable("tournament.dragonminez.left_party", player.getName())
					.withStyle(ChatFormatting.RED));
			eliminate(level, data, run, player, null);
		}

		private static void winMatch(ServerPlayer player, ActiveMatch match) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			setFrozen(player, false);

			Run run = data.runOf(player.getUUID());
			if (run == null) return;

			Bracket bracket = run.getBracket();
			run.setStandingOpponent(null);

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.round_won",
					match.round() + 1, roundName(bracket, match.round()))
					.withStyle(ChatFormatting.GREEN));
			player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F);

			if (run.isGauntlet()) {
				bracket.advance();
				data.setDirty();

				if (bracket.isCompleted()) {
					crown(level, data, run, player.getUUID());
					return;
				}
				syncGauntletTurn(level, data, run);
				return;
			}

			if (bracket.nextMatchIndex(match.round()) == match.match()) {
				bracket.recordWinner(match.round(), match.selfSlot());
			}
			data.setDirty();
			syncTurn(level, data, run);
		}

		private static Component roundName(Bracket bracket, int round) {
			if (round == bracket.finalRound()) return Component.translatable("tournament.dragonminez.round.final");
			if (round == bracket.semiRound()) return Component.translatable("tournament.dragonminez.round.semi");
			return Component.translatable("tournament.dragonminez.round.qualifier", round + 1);
		}

		private static void loseMatch(ServerPlayer player, ActiveMatch match) {
			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			setFrozen(player, false);

			Run run = data.runOf(player.getUUID());
			if (run == null) return;

			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			int cooldown = def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60;

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.round_lost",
					match.round() + 1, roundName(run.getBracket(), match.round()), formatDuration(cooldown))
					.withStyle(ChatFormatting.RED));
			player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);

			eliminate(level, data, run, player, match);
		}

		private static void resolvePvp(ServerLevel level, ServerPlayer player, ActiveMatch match, boolean won) {
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null) return;

			Bracket bracket = run.getBracket();
			if (bracket.nextMatchIndex(match.round()) != match.match()) return;

			ACTIVE.remove(player.getUUID());
			ACTIVE.remove(match.opponentId());
			PartyManager.setTournamentFriendlyFire(level.getServer(), run.getPartyId(), false);
			clearRival(level, player.getUUID());
			clearRival(level, match.opponentId());

			ServerPlayer rival = level.getServer().getPlayerList().getPlayer(match.opponentId());
			ServerPlayer winner = won ? player : rival;
			ServerPlayer loser = won ? rival : player;

			bracket.recordWinner(match.round(), won ? match.selfSlot() : match.rivalSlot());
			data.setDirty();

			if (winner != null) {
				winner.sendSystemMessage(Component.translatable("tournament.dragonminez.round_won",
						match.round() + 1, roundName(bracket, match.round())).withStyle(ChatFormatting.GREEN));
				winner.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
			}

			if (loser != null) {
				TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
				int cooldown = def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60;
				loser.sendSystemMessage(Component.translatable("tournament.dragonminez.round_lost",
						match.round() + 1, roundName(bracket, match.round()), formatDuration(cooldown))
						.withStyle(ChatFormatting.RED));
				eliminate(level, data, run, loser, null);
				return;
			}

			syncTurn(level, data, run);
		}

		private static void voidPvp(ServerLevel level, ServerPlayer player, ActiveMatch match) {
			ACTIVE.remove(player.getUUID());
			ACTIVE.remove(match.opponentId());

			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null) return;

			PartyManager.setTournamentFriendlyFire(level.getServer(), run.getPartyId(), false);
			clearRival(level, player.getUUID());
			clearRival(level, match.opponentId());
			player.displayClientMessage(Component.translatable("tournament.dragonminez.timeout"), false);

			ServerPlayer rival = level.getServer().getPlayerList().getPlayer(match.opponentId());
			if (rival != null) {
				rival.displayClientMessage(Component.translatable("tournament.dragonminez.timeout"), false);
			}
			syncTurn(level, data, run);
		}

		private static void eliminate(ServerLevel level, Progress data, Run run, ServerPlayer player,
									  @Nullable ActiveMatch match) {
			UUID id = player.getUUID();
			if (run.isEliminated(id)) return;

			ActiveMatch active = match != null ? match : ACTIVE.get(id);
			ACTIVE.remove(id);
			setFrozen(player, false);

			Bracket bracket = run.getBracket();
			boolean gauntlet = run.isGauntlet();

			if (!gauntlet && active != null && bracket.nextMatchIndex(active.round()) == active.match()) {
				bracket.recordWinner(active.round(), active.rivalSlot());
				if (active.pvp()) {
					ACTIVE.remove(active.opponentId());
					PartyManager.setTournamentFriendlyFire(level.getServer(), run.getPartyId(), false);
					clearRival(level, id);
					clearRival(level, active.opponentId());
				}
			}

			run.eliminate(id);

			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			data.startCooldown(id, run.getTournamentId(),
					def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60);

			UUID opponentId = active != null && !active.pvp() ? active.opponentId() : run.getStandingOpponent();
			if (gauntlet && !run.aliveParticipants().isEmpty()) {
				freezeOpponent(level, opponentId);
				run.setStandingOpponent(opponentId);
			} else {
				despawnOpponent(level, opponentId);
				run.setStandingOpponent(null);
			}

			broadcast(level, run, Component.translatable("tournament.dragonminez.eliminated_broadcast",
					player.getName()).withStyle(ChatFormatting.RED));
			data.setDirty();

			if (run.aliveParticipants().isEmpty()) {
				bracket.eliminate();
				finishRun(level, data, run);
				return;
			}

			if (run.getPhase() == Run.Phase.RULES) {
				if (run.unanswered().isEmpty()) settleRules(level, data, run);
				else Service.pushBracket(level, run);
				return;
			}

			if (gauntlet) syncGauntletTurn(level, data, run);
			else syncTurn(level, data, run);
		}

		private static void withdraw(ServerLevel level, Progress data, Run run, ServerPlayer player) {
			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			int cooldown = def != null ? def.reentryCooldownSecondsOr(20 * 60) : 20 * 60;

			broadcast(level, run, Component.translatable("tournament.dragonminez.withdrew", player.getName())
					.withStyle(ChatFormatting.RED));
			player.sendSystemMessage(Component.translatable("tournament.dragonminez.forfeit",
					formatDuration(cooldown)).withStyle(ChatFormatting.RED));
			eliminate(level, data, run, player, null);
		}

		private static void finishRun(ServerLevel level, Progress data, Run run) {
			for (UUID id : run.getParticipants()) {
				if (ACTIVE.remove(id) != null) clearRival(level, id);
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant != null) setFrozen(participant, false);
			}
			PartyManager.setTournamentFriendlyFire(level.getServer(), run.getPartyId(), false);
			despawnOpponent(level, run.getStandingOpponent());
			run.setStandingOpponent(null);
			run.finish();

			broadcast(level, run, Component.translatable("tournament.dragonminez.run_over")
					.withStyle(ChatFormatting.GRAY));
			Service.sendPhase(level, run, TournamentPackets.PhaseS2C.Phase.CLEAR, 0);
			Service.pushBracket(level, run);
			data.removeRun(run.getTournamentId());
		}

		private static void clearRival(ServerLevel level, UUID id) {
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			if (player != null) NetworkHandler.sendToPlayer(new TournamentPackets.RivalS2C(null), player);
		}

		private static void broadcast(ServerLevel level, Run run, Component message) {
			for (UUID id : run.getParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant != null) participant.sendSystemMessage(message);
			}
		}

		private static void pling(ServerPlayer player, float pitch) {
			player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, pitch);
		}

		private static void crown(ServerLevel level, Progress data, Run run, UUID championId) {
			TournamentDefinition def = ConfigManager.getTournament(run.getTournamentId());
			ServerPlayer champion = level.getServer().getPlayerList().getPlayer(championId);

			if (champion != null) {
				celebrate(champion, run.getRingCentre());
				broadcast(level, run, Component.translatable("tournament.dragonminez.champion_broadcast",
						champion.getName()).withStyle(ChatFormatting.GOLD));
			}

			int victoryCooldown = def != null ? def.victoryCooldownSecondsOr(20 * 60) : 20 * 60;

			if (run.isGauntlet()) {
				for (UUID id : run.getParticipants()) {
					if (run.isDeclined(id)) continue;
					ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
					if (participant != null) grantRewards(participant, def);
					data.startCooldown(id, run.getTournamentId(), victoryCooldown);
				}
			} else if (champion != null) {
				grantRewards(champion, def);
				data.startCooldown(championId, run.getTournamentId(), victoryCooldown);
			}

			finishRun(level, data, run);
		}

		private static void celebrate(ServerPlayer player, BlockPos ringCentre) {
			ServerLevel level = player.serverLevel();

			player.sendSystemMessage(Component.translatable("tournament.dragonminez.victory")
					.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

			NetworkHandler.sendToPlayer(new RaidMusicS2C(RaidFeedback.VICTORY_MUSIC), player);
			RaidFeedback.launchFireworks(level, ringCentre);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
					player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
					80, 0.4, 0.8, 0.4, 0.35);
		}

		private static void grantRewards(ServerPlayer player, @Nullable TournamentDefinition def) {
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
				player.sendSystemMessage(Component.translatable("tournament.dragonminez.reward.tp", trainingPoints)
						.withStyle(ChatFormatting.YELLOW));
			}

			List<String> commands = rewards.getCommands();
			if (commands == null || commands.isEmpty()) return;

			ServerLevel level = player.serverLevel();
			for (String command : commands) {
				if (command == null || command.isBlank()) continue;
				String parsed = command.replace("%player%", player.getName().getString());
				level.getServer().getCommands().performPrefixedCommand(
						level.getServer().createCommandSourceStack().withPermission(4), parsed);
			}
		}

		private static void voidMatch(ServerPlayer player, ActiveMatch match, Entity opponent) {
			if (opponent != null) opponent.discard();
			player.displayClientMessage(Component.translatable("tournament.dragonminez.timeout"), false);
			setFrozen(player, false);

			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.runOf(player.getUUID());
			if (run == null) return;

			run.setStandingOpponent(null);
			if (run.isGauntlet()) syncGauntletTurn(level, data, run);
			else syncTurn(level, data, run);
		}

		private static void freezeOpponent(ServerLevel level, @Nullable UUID opponentId) {
			if (opponentId == null) return;
			Entity entity = level.getEntity(opponentId);
			if (!(entity instanceof Mob mob) || !mob.isAlive()) return;

			mob.setNoAi(true);
			mob.setTarget(null);
			if (mob instanceof DBSagasEntity saga) saga.setCombatFrozen(true);
		}

		private static void despawnOpponent(ServerLevel level, @Nullable UUID opponentId) {
			if (opponentId == null) return;
			Entity opponent = level.getEntity(opponentId);
			if (opponent != null) opponent.discard();
		}

		@Nullable
		private static TournamentDefinition.Fighter fighterFor(TournamentDefinition def, Bracket bracket, String slot) {
			if (slot == null || slot.isBlank() || Bracket.isPlayerSlot(slot)) return null;

			int source = bracket != null ? bracket.sourceOf(slot) : Bracket.SOURCE_UNKNOWN;
			if (source == Bracket.SOURCE_CHAMPION) return def.getChampion();
			if (source == Bracket.SOURCE_SEMIFINALIST) return def.getSemifinalist();
			if (source >= 0 && source < def.getContenders().size()) return def.getContenders().get(source);

			if (slot.equals(def.getChampion().getEntityId())) return def.getChampion();
			if (slot.equals(def.getSemifinalist().getEntityId())) return def.getSemifinalist();
			for (TournamentDefinition.Fighter fighter : def.getContenders()) {
				if (slot.equals(fighter.getEntityId())) return fighter;
			}
			return null;
		}

		@Nullable
		private static Mob spawnOpponent(ServerLevel level, TournamentDefinition.Fighter fighter, String entityId,
										 BlockPos ringCentre, int radius, int round, ServerPlayer target) {
			ResourceLocation id = ResourceLocation.tryParse(entityId);
			if (id == null) return null;

			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
			if (type == null) {
				LogUtil.warn(Env.SERVER, "Tournament fighter '{}' is not a registered entity", entityId);
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
				saga.setAiTierById(fighter.aiTierOr(2));
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

		private record ActiveMatch(UUID opponentId, boolean pvp, int round, int match,
								   String selfSlot, String rivalSlot, long deadline, BlockPos ringCentre,
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
			switch (action) {
				case ACCEPT_RULES -> Manager.answerRules(player, true);
				case DECLINE_RULES -> Manager.answerRules(player, false);
				case READY -> Manager.ready(player);
				case OPEN_BRACKET, SIGN_UP -> {
					MastersEntity npc = resolveNpc(player, npcEntityId);
					if (npc == null) return;

					String tournamentId = TournamentDefinition.HOST_NPCS.get(npc.getMasterName());
					if (tournamentId == null) return;

					if (action == TournamentPackets.ActionC2S.Action.OPEN_BRACKET) {
						sendBracket(player, tournamentId, npcEntityId, false);
						return;
					}

					BlockPos ring = resolveRing(player, npc, tournamentId);
					if (ring == null) {
						player.displayClientMessage(Component.translatable("tournament.dragonminez.no_ring"), true);
						return;
					}
					Component refusal = Manager.signUp(player, tournamentId, ring);
					if (refusal != null) {
						player.displayClientMessage(refusal, true);
						return;
					}
					sendBracket(player, tournamentId, npcEntityId, false);
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

		public static void pushBracket(ServerLevel level, Run run) {
			for (UUID id : run.getParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant == null) continue;
				sendBracket(participant, run.getTournamentId(), -1, true);
			}
		}

		public static void openForAll(ServerLevel level, Run run) {
			for (UUID id : run.aliveParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant == null) continue;
				sendBracket(participant, run.getTournamentId(), -1, false);
			}
		}

		public static void sendPhase(ServerLevel level, Run run, TournamentPackets.PhaseS2C.Phase phase, int seconds) {
			Component left = nameOf(level, run, run.getActiveFighter());
			Component right = run.getActiveRival() != null
					? nameOf(level, run, run.getActiveRival())
					: opponentName(run);

			for (UUID id : run.getParticipants()) {
				ServerPlayer participant = level.getServer().getPlayerList().getPlayer(id);
				if (participant == null) continue;
				boolean fighter = phase != TournamentPackets.PhaseS2C.Phase.CLEAR && !run.isEliminated(id)
						&& (id.equals(run.getActiveFighter()) || id.equals(run.getActiveRival()));
				NetworkHandler.sendToPlayer(new TournamentPackets.PhaseS2C(phase, seconds, fighter, left, right), participant);
			}
		}

		private static Component nameOf(ServerLevel level, Run run, @Nullable UUID id) {
			if (id == null) return Component.empty();
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
			return player != null ? player.getName().copy() : Component.literal("???");
		}

		private static Component opponentName(Run run) {
			Bracket bracket = run.getBracket();
			String slot;
			if (run.isGauntlet()) {
				slot = bracket.currentOpponent();
			} else {
				Manager.Bout bout = Manager.nextBout(run);
				slot = bout != null ? bout.opponentSlot() : "";
			}
			if (slot == null || slot.isEmpty() || Bracket.isPlayerSlot(slot)) return Component.empty();

			ResourceLocation location = ResourceLocation.tryParse(slot);
			EntityType<?> type = location != null ? ForgeRegistries.ENTITY_TYPES.getValue(location) : null;
			return type != null ? type.getDescription().copy() : Component.literal(slot);
		}

		private static void sendBracket(ServerPlayer player, String tournamentId, int npcEntityId, boolean push) {
			TournamentDefinition def = ConfigManager.getTournament(tournamentId);
			if (def == null) {
				if (!push) {
					player.displayClientMessage(Component.translatable("tournament.dragonminez.unavailable"), true);
				}
				return;
			}

			ServerLevel level = player.serverLevel();
			Progress data = Progress.get(level);
			Run run = data.getRun(tournamentId);

			boolean participant = run != null && run.isParticipant(player.getUUID());
			Bracket bracket = participant ? run.getBracket() : null;
			boolean eliminated = participant && run.isEliminated(player.getUUID());
			boolean revealed = participant && run.isRevealed();
			boolean gauntlet = bracket != null ? bracket.isGauntlet()
					: def.formatOr(TournamentDefinition.Format.BRACKET) == TournamentDefinition.Format.GAUNTLET;

			boolean signUp = run == null || !participant;
			boolean lockedByOther = run != null && !participant;

			TournamentPackets.OpenBracketS2C.Phase phase = TournamentPackets.OpenBracketS2C.Phase.NONE;
			int phaseSeconds = 0;
			if (participant) {
				phase = switch (run.getPhase()) {
					case RULES -> TournamentPackets.OpenBracketS2C.Phase.RULES;
					case INTERMISSION -> TournamentPackets.OpenBracketS2C.Phase.INTERMISSION;
					case PREVIEW -> TournamentPackets.OpenBracketS2C.Phase.PREVIEW;
					case BOUT -> TournamentPackets.OpenBracketS2C.Phase.BOUT;
				};
				if (run.getPhaseDeadline() > 0L) {
					long left = run.getPhaseDeadline() - level.getGameTime();
					phaseSeconds = left <= 0L ? 0 : (int) ((left + 19) / 20);
				}
			}

			List<String> seeds;
			List<List<String>> winners;
			if (bracket == null) {
				seeds = new ArrayList<>();
				int hidden = gauntlet ? usableContenders(def) : def.qualifierSlotsOr(4);
				for (int i = 0; i < hidden; i++) seeds.add(Bracket.HIDDEN_SLOT);
				winners = List.of();
			} else if (!revealed) {
				seeds = bracket.hiddenSeeds();
				winners = List.of();
			} else {
				seeds = bracket.getSeeds();
				winners = bracket.getWinners();
			}

			List<TournamentPackets.OpenBracketS2C.Member> members = new ArrayList<>();
			Map<String, String> slotNames = new HashMap<>();
			TournamentPackets.OpenBracketS2C.MemberState myState = TournamentPackets.OpenBracketS2C.MemberState.PENDING;

			if (run != null) {
				UUID leaderId = run.getParticipants().isEmpty() ? null : run.getParticipants().get(0);
				for (UUID id : run.getParticipants()) {
					ServerPlayer member = level.getServer().getPlayerList().getPlayer(id);
					String name = member != null ? member.getGameProfile().getName() : "???";
					TournamentPackets.OpenBracketS2C.MemberState state = memberState(run, id);
					members.add(new TournamentPackets.OpenBracketS2C.Member(name, state, id.equals(leaderId),
							id.equals(run.getActiveFighter()) || id.equals(run.getActiveRival()), run.isReady(id)));
					slotNames.put(Bracket.slotFor(id), name);
					if (id.equals(player.getUUID())) myState = state;
				}
			} else if (PartyManager.isInParty(player)) {
				ServerPlayer leader = PartyManager.getPartyLeader(player);
				for (ServerPlayer member : PartyManager.getAllPartyMembers(player)) {
					boolean cooling = data.remainingCooldownSeconds(member.getUUID(), tournamentId) > 0L;
					members.add(new TournamentPackets.OpenBracketS2C.Member(member.getGameProfile().getName(),
							cooling ? TournamentPackets.OpenBracketS2C.MemberState.OUT
									: TournamentPackets.OpenBracketS2C.MemberState.PENDING,
							leader != null && member.getUUID().equals(leader.getUUID()), false, false));
				}
			} else {
				members.add(new TournamentPackets.OpenBracketS2C.Member(player.getGameProfile().getName(),
						TournamentPackets.OpenBracketS2C.MemberState.PENDING, true, false, false));
			}

			boolean partyLeader = !PartyManager.isInParty(player) || PartyManager.isPartyLeader(player);
			boolean myReady = run != null && run.isReady(player.getUUID());

			String activeSlot = "";
			String rivalSlot = "";
			if (run != null && revealed && !run.isGauntlet() && run.getActiveFighter() != null) {
				activeSlot = Bracket.slotFor(run.getActiveFighter());
				if (run.getActiveRival() != null) {
					rivalSlot = Bracket.slotFor(run.getActiveRival());
				} else {
					Manager.Bout bout = Manager.nextBout(run);
					if (bout != null) rivalSlot = bout.opponentSlot();
				}
			}

			TournamentDefinition.Rewards rewards = def.getRewards();
			TournamentPackets.OpenBracketS2C.Rules rules = new TournamentPackets.OpenBracketS2C.Rules(
					seeds.size(),
					def.matchTimeoutSecondsOr(300),
					def.reentryCooldownSecondsOr(20 * 60),
					def.returnSecondsOr(15),
					def.rulesAcceptSecondsOr(60),
					def.nextRoundSecondsOr(10),
					rewards != null ? rewards.trainingPointsOr(0) : 0,
					rewards != null ? rewards.alignmentOr(0) : 0,
					(int) Manager.EXCLUSION_RADIUS,
					(int) Math.sqrt(Manager.FORFEIT_DISTANCE_SQR));

			NetworkHandler.sendToPlayer(new TournamentPackets.OpenBracketS2C(
					tournamentId,
					def.displayNameOr("tournament.dragonminez." + tournamentId),
					def.difficultyStarsOr(1),
					gauntlet,
					def.isLethal(),
					phase,
					phaseSeconds,
					revealed,
					seeds,
					winners,
					bracket != null ? bracket.getSemifinalist() : previewId(def.getSemifinalist()),
					bracket != null ? bracket.getChampion() : previewId(def.getChampion()),
					bracket != null ? bracket.getRound() : 0,
					eliminated || (bracket != null && bracket.isEliminated()),
					bracket != null && bracket.isCompleted(),
					signUp,
					lockedByOther,
					partyLeader,
					(int) data.remainingCooldownSeconds(player.getUUID(), tournamentId),
					npcEntityId,
					push,
					collectStats(def, bracket),
					members,
					slotNames,
					activeSlot,
					rivalSlot,
					myState,
					myReady,
					rules
			), player);
		}

		private static TournamentPackets.OpenBracketS2C.MemberState memberState(Run run, UUID id) {
			if (run.isDeclined(id)) return TournamentPackets.OpenBracketS2C.MemberState.DECLINED;
			if (run.isEliminated(id)) return TournamentPackets.OpenBracketS2C.MemberState.OUT;
			if (run.getPhase() == Run.Phase.RULES) {
				return run.isAccepted(id) ? TournamentPackets.OpenBracketS2C.MemberState.ACCEPTED
						: TournamentPackets.OpenBracketS2C.MemberState.PENDING;
			}
			return TournamentPackets.OpenBracketS2C.MemberState.ALIVE;
		}

		private static int usableContenders(TournamentDefinition def) {
			int count = 0;
			for (TournamentDefinition.Fighter fighter : def.getContenders()) if (fighter.isUsable()) count++;
			return Math.max(1, count);
		}

		private static String previewId(TournamentDefinition.Fighter fighter) {
			if (fighter == null || !fighter.isUsable()) return "";
			return fighter.isTag() ? Bracket.HIDDEN_SLOT : fighter.getEntityId();
		}

		private static Map<String, TournamentPackets.OpenBracketS2C.FighterStats> collectStats(
				TournamentDefinition def, @Nullable Bracket bracket) {
			Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats = new HashMap<>();

			if (bracket == null) {
				for (TournamentDefinition.Fighter fighter : def.getContenders()) putStats(stats, previewId(fighter), fighter, 0);
				putStats(stats, previewId(def.getSemifinalist()), def.getSemifinalist(), 1);
				putStats(stats, previewId(def.getChampion()), def.getChampion(), 2);
				return stats;
			}

			for (String seed : bracket.getSeeds()) {
				if (seed.isEmpty() || Bracket.isPlayerSlot(seed)) continue;
				putStats(stats, seed, Manager.fighterFor(def, bracket, seed), bracket.getRound());
			}
			putStats(stats, bracket.getSemifinalist(), Manager.fighterFor(def, bracket, bracket.getSemifinalist()), bracket.semiRound());
			putStats(stats, bracket.getChampion(), Manager.fighterFor(def, bracket, bracket.getChampion()), bracket.finalRound());
			return stats;
		}

		private static void putStats(Map<String, TournamentPackets.OpenBracketS2C.FighterStats> stats, String id,
									 @Nullable TournamentDefinition.Fighter fighter, int round) {
			if (fighter == null || !fighter.isUsable() || id == null || id.isEmpty() || Bracket.HIDDEN_SLOT.equals(id)) return;
			double scaling = Math.pow(fighter.perRoundScalingOr(1.0D), round);
			stats.put(id, new TournamentPackets.OpenBracketS2C.FighterStats(
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
