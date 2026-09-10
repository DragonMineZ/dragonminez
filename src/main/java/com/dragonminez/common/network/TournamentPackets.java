package com.dragonminez.common.network;

import com.dragonminez.client.gui.tournament.TournamentBracketScreen;
import com.dragonminez.client.gui.tournament.TournamentOverlay;
import com.dragonminez.server.world.tournament.Tournament;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class TournamentPackets {

	private TournamentPackets() {}

	@Getter
	public static class OpenBracketS2C {

		private final String tournamentId;
		private final String displayName;
		private final int difficultyStars;
		private final List<String> seeds;
		private final List<List<String>> winners;
		private final String semifinalist;
		private final String champion;
		private final int round;
		private final boolean eliminated;
		private final boolean completed;
		private final boolean gauntlet;
		private final boolean lethal;
		private final boolean signUp;
		private final int cooldownSeconds;
		private final int npcEntityId;
		private final Map<String, FighterStats> stats;

		private final boolean lockedByOther;
		private final boolean yourTurn;
		private final int turnSeconds;
		private final List<String> queue;
		private final List<Boolean> queueOut;
		private final int activeIndex;
		private final boolean push;
		private final Map<String, String> slotNames;
		private final String activeSlot;
		private final int leaderIndex;
		private final boolean partyLeader;

		public record FighterStats(int health, int melee, int ki) {}

		public OpenBracketS2C(String tournamentId, String displayName, int difficultyStars,
							  List<String> seeds, List<List<String>> winners, String semifinalist, String champion,
							  int round, boolean eliminated, boolean completed,
							  boolean gauntlet, boolean lethal, boolean signUp, int cooldownSeconds, int npcEntityId,
							  Map<String, FighterStats> stats,
							  boolean lockedByOther, boolean yourTurn, int turnSeconds,
							  List<String> queue, List<Boolean> queueOut, int activeIndex, boolean push,
							  Map<String, String> slotNames, String activeSlot,
							  int leaderIndex, boolean partyLeader) {
			this.leaderIndex = leaderIndex;
			this.partyLeader = partyLeader;
			this.slotNames = slotNames == null ? new HashMap<>() : slotNames;
			this.activeSlot = activeSlot == null ? "" : activeSlot;
			this.lockedByOther = lockedByOther;
			this.yourTurn = yourTurn;
			this.turnSeconds = turnSeconds;
			this.queue = queue == null ? new ArrayList<>() : queue;
			this.queueOut = queueOut == null ? new ArrayList<>() : queueOut;
			this.activeIndex = activeIndex;
			this.push = push;
			this.stats = stats == null ? new HashMap<>() : stats;
			this.tournamentId = tournamentId == null ? "" : tournamentId;
			this.displayName = displayName == null ? "" : displayName;
			this.difficultyStars = difficultyStars;
			this.seeds = seeds == null ? new ArrayList<>() : seeds;
			this.winners = winners == null ? new ArrayList<>() : winners;
			this.semifinalist = semifinalist == null ? "" : semifinalist;
			this.champion = champion == null ? "" : champion;
			this.round = round;
			this.eliminated = eliminated;
			this.completed = completed;
			this.gauntlet = gauntlet;
			this.lethal = lethal;
			this.signUp = signUp;
			this.cooldownSeconds = cooldownSeconds;
			this.npcEntityId = npcEntityId;
		}

		public static void encode(OpenBracketS2C msg, FriendlyByteBuf buf) {
			buf.writeUtf(msg.tournamentId);
			buf.writeUtf(msg.displayName);
			buf.writeVarInt(msg.difficultyStars);
			buf.writeVarInt(msg.seeds.size());
			for (String id : msg.seeds) buf.writeUtf(id);
			buf.writeVarInt(msg.winners.size());
			for (List<String> roundWinners : msg.winners) {
				buf.writeVarInt(roundWinners.size());
				for (String id : roundWinners) buf.writeUtf(id);
			}
			buf.writeUtf(msg.semifinalist);
			buf.writeUtf(msg.champion);
			buf.writeVarInt(msg.round);
			buf.writeBoolean(msg.eliminated);
			buf.writeBoolean(msg.completed);
			buf.writeBoolean(msg.gauntlet);
			buf.writeBoolean(msg.lethal);
			buf.writeBoolean(msg.signUp);
			buf.writeVarInt(msg.cooldownSeconds);
			buf.writeInt(msg.npcEntityId);
			buf.writeVarInt(msg.stats.size());
			for (Map.Entry<String, FighterStats> entry : msg.stats.entrySet()) {
				buf.writeUtf(entry.getKey());
				buf.writeVarInt(entry.getValue().health());
				buf.writeVarInt(entry.getValue().melee());
				buf.writeVarInt(entry.getValue().ki());
			}
			buf.writeBoolean(msg.lockedByOther);
			buf.writeBoolean(msg.yourTurn);
			buf.writeVarInt(msg.turnSeconds);
			buf.writeVarInt(msg.queue.size());
			for (int i = 0; i < msg.queue.size(); i++) {
				buf.writeUtf(msg.queue.get(i));
				buf.writeBoolean(i < msg.queueOut.size() && msg.queueOut.get(i));
			}
			buf.writeInt(msg.activeIndex);
			buf.writeBoolean(msg.push);
			buf.writeVarInt(msg.slotNames.size());
			for (Map.Entry<String, String> entry : msg.slotNames.entrySet()) {
				buf.writeUtf(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
			buf.writeUtf(msg.activeSlot);
			buf.writeInt(msg.leaderIndex);
			buf.writeBoolean(msg.partyLeader);
		}

		public static OpenBracketS2C decode(FriendlyByteBuf buf) {
			String tournamentId = buf.readUtf();
			String displayName = buf.readUtf();
			int stars = buf.readVarInt();
			int seedCount = buf.readVarInt();
			List<String> seeds = new ArrayList<>(seedCount);
			for (int i = 0; i < seedCount; i++) seeds.add(buf.readUtf());
			int winnerRounds = buf.readVarInt();
			List<List<String>> winners = new ArrayList<>(winnerRounds);
			for (int r = 0; r < winnerRounds; r++) {
				int size = buf.readVarInt();
				List<String> roundWinners = new ArrayList<>(size);
				for (int i = 0; i < size; i++) roundWinners.add(buf.readUtf());
				winners.add(roundWinners);
			}
			String semifinalist = buf.readUtf();
			String champion = buf.readUtf();
			int round = buf.readVarInt();
			boolean eliminated = buf.readBoolean();
			boolean completed = buf.readBoolean();
			boolean gauntlet = buf.readBoolean();
			boolean lethal = buf.readBoolean();
			boolean signUp = buf.readBoolean();
			int cooldown = buf.readVarInt();
			int npcId = buf.readInt();

			int statCount = buf.readVarInt();
			Map<String, FighterStats> stats = new HashMap<>(statCount);
			for (int i = 0; i < statCount; i++) {
				stats.put(buf.readUtf(), new FighterStats(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
			}

			boolean lockedByOther = buf.readBoolean();
			boolean yourTurn = buf.readBoolean();
			int turnSeconds = buf.readVarInt();
			int queueSize = buf.readVarInt();
			List<String> queue = new ArrayList<>(queueSize);
			List<Boolean> queueOut = new ArrayList<>(queueSize);
			for (int i = 0; i < queueSize; i++) {
				queue.add(buf.readUtf());
				queueOut.add(buf.readBoolean());
			}
			int activeIndex = buf.readInt();
			boolean push = buf.readBoolean();

			int nameCount = buf.readVarInt();
			Map<String, String> slotNames = new HashMap<>(nameCount);
			for (int i = 0; i < nameCount; i++) slotNames.put(buf.readUtf(), buf.readUtf());
			String activeSlot = buf.readUtf();
			int leaderIndex = buf.readInt();
			boolean partyLeader = buf.readBoolean();

			return new OpenBracketS2C(tournamentId, displayName, stars, seeds, winners,
					semifinalist, champion, round, eliminated, completed, gauntlet, lethal, signUp, cooldown, npcId,
					stats, lockedByOther, yourTurn, turnSeconds, queue, queueOut, activeIndex, push, slotNames,
					activeSlot, leaderIndex, partyLeader);
		}

		public static void handle(OpenBracketS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> TournamentBracketScreen.open(msg)));
			ctx.get().setPacketHandled(true);
		}
	}

	@Getter
	public static class CountdownS2C {

		private final int seconds;

		public CountdownS2C(int seconds) {
			this.seconds = seconds;
		}

		public static void encode(CountdownS2C msg, FriendlyByteBuf buf) {
			buf.writeVarInt(msg.seconds);
		}

		public static CountdownS2C decode(FriendlyByteBuf buf) {
			return new CountdownS2C(buf.readVarInt());
		}

		public static void handle(CountdownS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
					() -> () -> TournamentOverlay.startCountdown(msg.getSeconds())));
			ctx.get().setPacketHandled(true);
		}
	}

	public static class ActionC2S {

		public enum Action {
			OPEN_BRACKET,
			SIGN_UP,
			START_MATCH
		}

		private final Action action;
		private final int npcEntityId;

		public ActionC2S(Action action, int npcEntityId) {
			this.action = action;
			this.npcEntityId = npcEntityId;
		}

		public ActionC2S(FriendlyByteBuf buf) {
			this.action = buf.readEnum(Action.class);
			this.npcEntityId = buf.readInt();
		}

		public void encode(FriendlyByteBuf buf) {
			buf.writeEnum(this.action);
			buf.writeInt(this.npcEntityId);
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				ServerPlayer player = ctx.get().getSender();
				if (player == null) return;
				Tournament.Service.handleAction(player, this.action, this.npcEntityId);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
