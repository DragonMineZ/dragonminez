package com.dragonminez.common.worldboss;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WorldBossResults {
	public static final int MAX_PLAYERS = 32;
	public static final int MAX_REWARDS = 32;
	public static final int MAX_RECEIVED_SOURCES = 8;

	public record Reward(String json, float baseChance) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeUtf(json);
			buf.writeFloat(baseChance);
		}

		public static Reward decode(FriendlyByteBuf buf) {
			return new Reward(buf.readUtf(), buf.readFloat());
		}
	}

	public record NamedAmount(String nameKey, float amount) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeUtf(nameKey);
			buf.writeFloat(amount);
		}

		public static NamedAmount decode(FriendlyByteBuf buf) {
			return new NamedAmount(buf.readUtf(), buf.readFloat());
		}
	}

	public record PlayerEntry(UUID id, String name, int rank, float points, float share, int auraRgb,
							  float damageMelee, float damageStrike, float damageKi, float damageOther,
							  float mitigatedDefense, float mitigatedBlock, float mitigatedShield,
							  List<NamedAmount> received,
							  float healedSelf, float healedAllies,
							  float[] rewardChance, float[] rewardAmount) {

		public float damage() {
			return damageMelee + damageStrike + damageKi + damageOther;
		}

		public float mitigated() {
			return mitigatedDefense + mitigatedBlock + mitigatedShield;
		}

		public float receivedTotal() {
			float total = 0.0f;
			for (NamedAmount amount : received) total += amount.amount();
			return total;
		}

		public float healed() {
			return healedSelf + healedAllies;
		}

		public void encode(FriendlyByteBuf buf) {
			buf.writeUUID(id);
			buf.writeUtf(name);
			buf.writeVarInt(rank);
			buf.writeFloat(points);
			buf.writeFloat(share);
			buf.writeInt(auraRgb);
			buf.writeFloat(damageMelee);
			buf.writeFloat(damageStrike);
			buf.writeFloat(damageKi);
			buf.writeFloat(damageOther);
			buf.writeFloat(mitigatedDefense);
			buf.writeFloat(mitigatedBlock);
			buf.writeFloat(mitigatedShield);
			buf.writeVarInt(Math.min(received.size(), MAX_RECEIVED_SOURCES));
			for (int i = 0; i < Math.min(received.size(), MAX_RECEIVED_SOURCES); i++) received.get(i).encode(buf);
			buf.writeFloat(healedSelf);
			buf.writeFloat(healedAllies);
			buf.writeVarInt(rewardChance.length);
			for (float chance : rewardChance) buf.writeFloat(chance);
			buf.writeVarInt(rewardAmount.length);
			for (float amount : rewardAmount) buf.writeFloat(amount);
		}

		public static PlayerEntry decode(FriendlyByteBuf buf) {
			UUID id = buf.readUUID();
			String name = buf.readUtf();
			int rank = buf.readVarInt();
			float points = buf.readFloat();
			float share = buf.readFloat();
			int auraRgb = buf.readInt();
			float damageMelee = buf.readFloat();
			float damageStrike = buf.readFloat();
			float damageKi = buf.readFloat();
			float damageOther = buf.readFloat();
			float mitigatedDefense = buf.readFloat();
			float mitigatedBlock = buf.readFloat();
			float mitigatedShield = buf.readFloat();
			int receivedCount = Math.min(buf.readVarInt(), MAX_RECEIVED_SOURCES);
			List<NamedAmount> received = new ArrayList<>(receivedCount);
			for (int i = 0; i < receivedCount; i++) received.add(NamedAmount.decode(buf));
			float healedSelf = buf.readFloat();
			float healedAllies = buf.readFloat();
			int chanceCount = Math.min(buf.readVarInt(), MAX_REWARDS);
			float[] rewardChance = new float[chanceCount];
			for (int i = 0; i < chanceCount; i++) rewardChance[i] = buf.readFloat();
			int amountCount = Math.min(buf.readVarInt(), MAX_REWARDS);
			float[] rewardAmount = new float[amountCount];
			for (int i = 0; i < amountCount; i++) rewardAmount[i] = buf.readFloat();
			return new PlayerEntry(id, name, rank, points, share, auraRgb, damageMelee, damageStrike, damageKi, damageOther,
					mitigatedDefense, mitigatedBlock, mitigatedShield, received, healedSelf, healedAllies, rewardChance, rewardAmount);
		}
	}

	private final String bossKey;
	private final String bossNameKey;
	private final long durationTicks;
	private final boolean victory;
	private final List<Reward> rewards;
	private final List<PlayerEntry> players;

	public WorldBossResults(String bossKey, String bossNameKey, long durationTicks, boolean victory,
							List<Reward> rewards, List<PlayerEntry> players) {
		this.bossKey = bossKey;
		this.bossNameKey = bossNameKey;
		this.durationTicks = durationTicks;
		this.victory = victory;
		this.rewards = rewards.size() > MAX_REWARDS ? new ArrayList<>(rewards.subList(0, MAX_REWARDS)) : new ArrayList<>(rewards);
		this.players = players.size() > MAX_PLAYERS ? new ArrayList<>(players.subList(0, MAX_PLAYERS)) : new ArrayList<>(players);
	}

	public String bossKey() {
		return bossKey;
	}

	public String bossNameKey() {
		return bossNameKey;
	}

	public long durationTicks() {
		return durationTicks;
	}

	public boolean victory() {
		return victory;
	}

	public List<Reward> rewards() {
		return rewards;
	}

	public List<PlayerEntry> players() {
		return players;
	}

	public PlayerEntry find(UUID id) {
		for (PlayerEntry entry : players) {
			if (entry.id().equals(id)) return entry;
		}
		return null;
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(bossKey);
		buf.writeUtf(bossNameKey);
		buf.writeVarLong(durationTicks);
		buf.writeBoolean(victory);
		buf.writeVarInt(rewards.size());
		for (Reward reward : rewards) reward.encode(buf);
		buf.writeVarInt(players.size());
		for (PlayerEntry player : players) player.encode(buf);
	}

	public static WorldBossResults decode(FriendlyByteBuf buf) {
		String bossKey = buf.readUtf();
		String bossNameKey = buf.readUtf();
		long durationTicks = buf.readVarLong();
		boolean victory = buf.readBoolean();
		int rewardCount = Math.min(buf.readVarInt(), MAX_REWARDS);
		List<Reward> rewards = new ArrayList<>(rewardCount);
		for (int i = 0; i < rewardCount; i++) rewards.add(Reward.decode(buf));
		int playerCount = Math.min(buf.readVarInt(), MAX_PLAYERS);
		List<PlayerEntry> players = new ArrayList<>(playerCount);
		for (int i = 0; i < playerCount; i++) players.add(PlayerEntry.decode(buf));
		return new WorldBossResults(bossKey, bossNameKey, durationTicks, victory, rewards, players);
	}
}
