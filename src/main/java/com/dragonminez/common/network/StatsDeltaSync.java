package com.dragonminez.common.network;

import com.dragonminez.Reference;
import com.dragonminez.common.network.S2C.StatsDeltaS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.network.S2C.TechniqueChargeSyncS2C;
import com.dragonminez.common.stats.FusedData;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.SkinPixels;
import com.dragonminez.common.stats.techniques.Techniques;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class StatsDeltaSync {
	public static final String TECHNIQUES = "Techniques";
	public static final String RACIAL_DATA = "RacialData";

	private static final String CHARACTER = "Character";
	private static final String SKILLS = "Skills";
	private static final int FULL_RESYNC_PERIODS = 20;

	private static final Map<UUID, Baseline> BASELINES = new HashMap<>();
	private static final Map<UUID, Set<String>> DIRTY = new HashMap<>();

	private StatsDeltaSync() {}

	public static void periodic(ServerPlayer player) {
		UUID id = player.getUUID();
		Set<String> forced = DIRTY.remove(id);
		Baseline baseline = BASELINES.get(id);
		if (baseline == null || ++baseline.periods >= FULL_RESYNC_PERIODS) sendFull(player);
		else send(player, forced);
	}

	public static void markDirty(ServerPlayer player, String... forcedGroups) {
		Set<String> forced = DIRTY.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
		Collections.addAll(forced, forcedGroups);
	}

	public static void reset(ServerPlayer player) {
		BASELINES.remove(player.getUUID());
	}

	public static void clear(UUID playerId) {
		BASELINES.remove(playerId);
		DIRTY.remove(playerId);
	}

	public static void invalidate(ServerPlayer player, String... groups) {
		Baseline baseline = BASELINES.get(player.getUUID());
		if (baseline == null) return;
		for (String group : groups) baseline.tag.remove(group);
	}

	public static void onSent(ServerPlayer player, Object message, boolean withTrackers) {
		if (message instanceof StatsGroupsPacket packet && packet.playerId() == player.getId()) syncGroups(player, packet.groups(), withTrackers);
		else if (message instanceof TechniqueChargeSyncS2C charge && charge.playerId() == player.getId()) syncCharge(player, charge, withTrackers);
	}

	private static void syncGroups(ServerPlayer player, CompoundTag groups, boolean withTrackers) {
		Baseline baseline = BASELINES.get(player.getUUID());
		if (baseline == null) return;
		for (String key : groups.getAllKeys()) {
			Tag sent = groups.get(key);
			if (sent.equals(baseline.tag.get(key))) continue;
			if (withTrackers) baseline.tag.put(key, sent.copy());
			else baseline.tag.remove(key);
		}
	}

	private static void syncCharge(ServerPlayer player, TechniqueChargeSyncS2C charge, boolean withTrackers) {
		Baseline baseline = BASELINES.get(player.getUUID());
		if (baseline == null || !baseline.tag.contains(TECHNIQUES, Tag.TAG_COMPOUND)) return;
		CompoundTag techniques = baseline.tag.getCompound(TECHNIQUES);
		if (techniques.getFloat(Techniques.CHARGE_PERCENT_KEY) == charge.percent()
				&& techniques.getBoolean(Techniques.CHARGING_KEY) == charge.charging()) return;
		if (!withTrackers) {
			baseline.tag.remove(TECHNIQUES);
			return;
		}
		techniques.putFloat(Techniques.CHARGE_PERCENT_KEY, charge.percent());
		techniques.putBoolean(Techniques.CHARGING_KEY, charge.charging());
	}

	public static StatsSyncS2C trackerSnapshot(ServerPlayer player) {
		return StatsProvider.get(StatsCapability.INSTANCE, player)
				.map(data -> new StatsSyncS2C(player.getId(), snapshot(data)).forTrackers())
				.orElse(null);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (DIRTY.isEmpty()) return;
		Map<UUID, Set<String>> pending = new HashMap<>(DIRTY);
		DIRTY.clear();
		for (Map.Entry<UUID, Set<String>> entry : pending.entrySet()) {
			ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
			if (player != null) send(player, entry.getValue());
		}
	}

	private static void sendFull(ServerPlayer player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;
		CompoundTag now = snapshot(data);
		BASELINES.remove(player.getUUID());
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player.getId(), now), player);
		BASELINES.put(player.getUUID(), new Baseline(now.copy()));
	}

	private static void send(ServerPlayer player, Set<String> forced) {
		Baseline baseline = BASELINES.get(player.getUUID());
		if (baseline == null) {
			sendFull(player);
			return;
		}
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return;

		CompoundTag now = snapshot(data);
		CompoundTag delta = diff(now, baseline.tag, forced);
		if (delta.isEmpty()) return;
		NetworkHandler.sendToTrackingEntityAndSelf(new StatsDeltaS2C(player.getId(), delta), player);
		advance(baseline.tag, delta, now);
	}

	public static CompoundTag diff(CompoundTag now, CompoundTag last, Set<String> forced) {
		CompoundTag delta = new CompoundTag();
		for (String key : now.getAllKeys()) {
			if (key.equals(CHARACTER)) continue;
			Tag tag = now.get(key);
			if (isForced(forced, key) || !tag.equals(last.get(key))) delta.put(key, tag);
		}
		boolean fusedChanged = !Objects.equals(now.get(FusedData.NBT_KEY), last.get(FusedData.NBT_KEY));
		if (fusedChanged || delta.contains(SKILLS)) {
			delta.put(SKILLS, now.get(SKILLS));
			if (now.contains(FusedData.NBT_KEY)) delta.put(FusedData.NBT_KEY, now.get(FusedData.NBT_KEY));
		}

		CompoundTag nowCharacter = now.getCompound(CHARACTER);
		CompoundTag lastCharacter = last.getCompound(CHARACTER);
		boolean forceCharacter = isForced(forced, CHARACTER);
		CompoundTag patch = new CompoundTag();
		for (String key : nowCharacter.getAllKeys()) {
			Tag tag = nowCharacter.get(key);
			if (forceCharacter || !tag.equals(lastCharacter.get(key))) patch.put(key, tag);
		}
		ListTag removed = new ListTag();
		for (String key : lastCharacter.getAllKeys()) {
			if (!nowCharacter.contains(key)) removed.add(StringTag.valueOf(key));
		}
		if (!patch.isEmpty()) delta.put(StatsDeltaS2C.CHARACTER_PATCH, patch);
		if (!removed.isEmpty()) delta.put(StatsDeltaS2C.CHARACTER_REMOVED, removed);
		return delta;
	}

	public static void advance(CompoundTag last, CompoundTag delta, CompoundTag now) {
		for (String key : delta.getAllKeys()) {
			if (key.equals(StatsDeltaS2C.CHARACTER_PATCH) || key.equals(StatsDeltaS2C.CHARACTER_REMOVED)) continue;
			last.put(key, delta.get(key).copy());
		}
		if (!now.contains(FusedData.NBT_KEY)) last.remove(FusedData.NBT_KEY);
		CompoundTag lastCharacter = last.getCompound(CHARACTER);
		StatsDeltaS2C.applyCharacterPatch(lastCharacter, delta, true);
		last.put(CHARACTER, lastCharacter);
	}

	private static boolean isForced(Set<String> forced, String key) {
		return forced != null && forced.contains(key);
	}

	private static CompoundTag snapshot(StatsData data) {
		CompoundTag tag = data.save();
		tag.getCompound(CHARACTER).remove(SkinPixels.NBT_KEY);
		return tag;
	}

	private static final class Baseline {
		private final CompoundTag tag;
		private int periods;

		private Baseline(CompoundTag tag) {
			this.tag = tag;
		}
	}
}
