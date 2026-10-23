package com.dragonminez.common.network;

import com.dragonminez.client.gui.tournament.TournamentBracketScreen;
import com.dragonminez.client.gui.tournament.TournamentOverlay;
import com.dragonminez.server.world.tournament.Tournament;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.dragonminez.compat.DistExecutor;
import com.dragonminez.compat.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class TournamentPackets {
	private TournamentPackets() {
	}


	public static class OpenBracketS2C {

		public enum Phase {
			NONE, RULES, INTERMISSION, PREVIEW, BOUT;
		}


		public enum MemberState {
			PENDING, ACCEPTED, DECLINED, OUT, ALIVE;
		}


		public record Member(String name, MemberState state, boolean leader, boolean active, boolean ready) {
		}


		public record FighterStats(int health, int melee, int ki) {
		}


		public record Rules(int seedCount, int matchTimeoutSeconds, int reentryCooldownSeconds, int returnSeconds, int rulesAcceptSeconds, int nextRoundSeconds, int rewardTrainingPoints, int rewardAlignment, int exclusionRadius, int forfeitDistance) {
		}

		private final String tournamentId;
		private final String displayName;
		private final int difficultyStars;
		private final boolean gauntlet;
		private final boolean lethal;
		private final Phase phase;
		private final int phaseSeconds;
		private final boolean revealed;
		private final List<String> seeds;
		private final List<List<String>> winners;
		private final String semifinalist;
		private final String finalist;
		private final String champion;
		private final int round;
		private final boolean eliminated;
		private final boolean completed;
		private final boolean signUp;
		private final boolean lockedByOther;
		private final boolean partyLeader;
		private final int cooldownSeconds;
		private final int npcEntityId;
		private final boolean push;
		private final Map<String, FighterStats> stats;
		private final List<Member> members;
		private final Map<String, String> slotNames;
		private final String activeSlot;
		private final String rivalSlot;
		private final MemberState myState;
		private final boolean myReady;
		private final Rules rules;

		public OpenBracketS2C(String tournamentId, String displayName, int difficultyStars, boolean gauntlet, boolean lethal, Phase phase, int phaseSeconds, boolean revealed, List<String> seeds, List<List<String>> winners, String semifinalist, String finalist, String champion, int round, boolean eliminated, boolean completed, boolean signUp, boolean lockedByOther, boolean partyLeader, int cooldownSeconds, int npcEntityId, boolean push, Map<String, FighterStats> stats, List<Member> members, Map<String, String> slotNames, String activeSlot, String rivalSlot, MemberState myState, boolean myReady, Rules rules) {
			this.tournamentId = tournamentId == null ? "" : tournamentId;
			this.displayName = displayName == null ? "" : displayName;
			this.difficultyStars = difficultyStars;
			this.gauntlet = gauntlet;
			this.lethal = lethal;
			this.phase = phase == null ? Phase.NONE : phase;
			this.phaseSeconds = phaseSeconds;
			this.revealed = revealed;
			this.seeds = seeds == null ? new ArrayList<>() : seeds;
			this.winners = winners == null ? new ArrayList<>() : winners;
			this.semifinalist = semifinalist == null ? "" : semifinalist;
			this.finalist = finalist == null ? "" : finalist;
			this.champion = champion == null ? "" : champion;
			this.round = round;
			this.eliminated = eliminated;
			this.completed = completed;
			this.signUp = signUp;
			this.lockedByOther = lockedByOther;
			this.partyLeader = partyLeader;
			this.cooldownSeconds = cooldownSeconds;
			this.npcEntityId = npcEntityId;
			this.push = push;
			this.stats = stats == null ? new HashMap<>() : stats;
			this.members = members == null ? new ArrayList<>() : members;
			this.slotNames = slotNames == null ? new HashMap<>() : slotNames;
			this.activeSlot = activeSlot == null ? "" : activeSlot;
			this.rivalSlot = rivalSlot == null ? "" : rivalSlot;
			this.myState = myState == null ? MemberState.PENDING : myState;
			this.myReady = myReady;
			this.rules = rules == null ? new Rules(0, 0, 0, 0, 0, 0, 0, 0, 0, 0) : rules;
		}

		public static void encode(OpenBracketS2C msg, FriendlyByteBuf buf) {
			buf.writeUtf(msg.tournamentId);
			buf.writeUtf(msg.displayName);
			buf.writeVarInt(msg.difficultyStars);
			buf.writeBoolean(msg.gauntlet);
			buf.writeBoolean(msg.lethal);
			buf.writeEnum(msg.phase);
			buf.writeVarInt(msg.phaseSeconds);
			buf.writeBoolean(msg.revealed);
			buf.writeVarInt(msg.seeds.size());
			for (String id : msg.seeds) buf.writeUtf(id);
			buf.writeVarInt(msg.winners.size());
			for (List<String> roundWinners : msg.winners) {
				buf.writeVarInt(roundWinners.size());
				for (String id : roundWinners) buf.writeUtf(id);
			}
			buf.writeUtf(msg.semifinalist);
			buf.writeUtf(msg.finalist);
			buf.writeUtf(msg.champion);
			buf.writeVarInt(msg.round);
			buf.writeBoolean(msg.eliminated);
			buf.writeBoolean(msg.completed);
			buf.writeBoolean(msg.signUp);
			buf.writeBoolean(msg.lockedByOther);
			buf.writeBoolean(msg.partyLeader);
			buf.writeVarInt(msg.cooldownSeconds);
			buf.writeInt(msg.npcEntityId);
			buf.writeBoolean(msg.push);
			buf.writeVarInt(msg.stats.size());
			for (Map.Entry<String, FighterStats> entry : msg.stats.entrySet()) {
				buf.writeUtf(entry.getKey());
				buf.writeVarInt(entry.getValue().health());
				buf.writeVarInt(entry.getValue().melee());
				buf.writeVarInt(entry.getValue().ki());
			}
			buf.writeVarInt(msg.members.size());
			for (Member member : msg.members) {
				buf.writeUtf(member.name());
				buf.writeEnum(member.state());
				buf.writeBoolean(member.leader());
				buf.writeBoolean(member.active());
				buf.writeBoolean(member.ready());
			}
			buf.writeVarInt(msg.slotNames.size());
			for (Map.Entry<String, String> entry : msg.slotNames.entrySet()) {
				buf.writeUtf(entry.getKey());
				buf.writeUtf(entry.getValue());
			}
			buf.writeUtf(msg.activeSlot);
			buf.writeUtf(msg.rivalSlot);
			buf.writeEnum(msg.myState);
			buf.writeBoolean(msg.myReady);
			buf.writeVarInt(msg.rules.seedCount());
			buf.writeVarInt(msg.rules.matchTimeoutSeconds());
			buf.writeVarInt(msg.rules.reentryCooldownSeconds());
			buf.writeVarInt(msg.rules.returnSeconds());
			buf.writeVarInt(msg.rules.rulesAcceptSeconds());
			buf.writeVarInt(msg.rules.nextRoundSeconds());
			buf.writeVarInt(msg.rules.rewardTrainingPoints());
			buf.writeInt(msg.rules.rewardAlignment());
			buf.writeVarInt(msg.rules.exclusionRadius());
			buf.writeVarInt(msg.rules.forfeitDistance());
		}

		public static OpenBracketS2C decode(FriendlyByteBuf buf) {
			String tournamentId = buf.readUtf();
			String displayName = buf.readUtf();
			int stars = buf.readVarInt();
			boolean gauntlet = buf.readBoolean();
			boolean lethal = buf.readBoolean();
			Phase phase = buf.readEnum(Phase.class);
			int phaseSeconds = buf.readVarInt();
			boolean revealed = buf.readBoolean();
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
			String finalist = buf.readUtf();
			String champion = buf.readUtf();
			int round = buf.readVarInt();
			boolean eliminated = buf.readBoolean();
			boolean completed = buf.readBoolean();
			boolean signUp = buf.readBoolean();
			boolean lockedByOther = buf.readBoolean();
			boolean partyLeader = buf.readBoolean();
			int cooldown = buf.readVarInt();
			int npcId = buf.readInt();
			boolean push = buf.readBoolean();
			int statCount = buf.readVarInt();
			Map<String, FighterStats> stats = new HashMap<>(statCount);
			for (int i = 0; i < statCount; i++) {
				stats.put(buf.readUtf(), new FighterStats(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
			}
			int memberCount = buf.readVarInt();
			List<Member> members = new ArrayList<>(memberCount);
			for (int i = 0; i < memberCount; i++) {
				members.add(new Member(buf.readUtf(), buf.readEnum(MemberState.class), buf.readBoolean(), buf.readBoolean(), buf.readBoolean()));
			}
			int nameCount = buf.readVarInt();
			Map<String, String> slotNames = new HashMap<>(nameCount);
			for (int i = 0; i < nameCount; i++) slotNames.put(buf.readUtf(), buf.readUtf());
			String activeSlot = buf.readUtf();
			String rivalSlot = buf.readUtf();
			MemberState myState = buf.readEnum(MemberState.class);
			boolean myReady = buf.readBoolean();
			Rules rules = new Rules(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readInt(), buf.readVarInt(), buf.readVarInt());
			return new OpenBracketS2C(tournamentId, displayName, stars, gauntlet, lethal, phase, phaseSeconds, revealed, seeds, winners, semifinalist, finalist, champion, round, eliminated, completed, signUp, lockedByOther, partyLeader, cooldown, npcId, push, stats, members, slotNames, activeSlot, rivalSlot, myState, myReady, rules);
		}

		public static void handle(OpenBracketS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TournamentBracketScreen.open(msg)));
			ctx.get().setPacketHandled(true);
		}

		public String getTournamentId() {
			return this.tournamentId;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public int getDifficultyStars() {
			return this.difficultyStars;
		}

		public boolean isGauntlet() {
			return this.gauntlet;
		}

		public boolean isLethal() {
			return this.lethal;
		}

		public Phase getPhase() {
			return this.phase;
		}

		public int getPhaseSeconds() {
			return this.phaseSeconds;
		}

		public boolean isRevealed() {
			return this.revealed;
		}

		public List<String> getSeeds() {
			return this.seeds;
		}

		public List<List<String>> getWinners() {
			return this.winners;
		}

		public String getSemifinalist() {
			return this.semifinalist;
		}

		public String getFinalist() {
			return this.finalist;
		}

		public String getChampion() {
			return this.champion;
		}

		public int getRound() {
			return this.round;
		}

		public boolean isEliminated() {
			return this.eliminated;
		}

		public boolean isCompleted() {
			return this.completed;
		}

		public boolean isSignUp() {
			return this.signUp;
		}

		public boolean isLockedByOther() {
			return this.lockedByOther;
		}

		public boolean isPartyLeader() {
			return this.partyLeader;
		}

		public int getCooldownSeconds() {
			return this.cooldownSeconds;
		}

		public int getNpcEntityId() {
			return this.npcEntityId;
		}

		public boolean isPush() {
			return this.push;
		}

		public Map<String, FighterStats> getStats() {
			return this.stats;
		}

		public List<Member> getMembers() {
			return this.members;
		}

		public Map<String, String> getSlotNames() {
			return this.slotNames;
		}

		public String getActiveSlot() {
			return this.activeSlot;
		}

		public String getRivalSlot() {
			return this.rivalSlot;
		}

		public MemberState getMyState() {
			return this.myState;
		}

		public boolean isMyReady() {
			return this.myReady;
		}

		public Rules getRules() {
			return this.rules;
		}
	}


	public static class PhaseS2C {

		public enum Phase {
			NEXT_ROUND, PREVIEW, COUNTDOWN, CLEAR;
		}

		private final Phase phase;
		private final int seconds;
		private final boolean fighter;
		private final Component left;
		private final Component right;

		public PhaseS2C(Phase phase, int seconds, boolean fighter, Component left, Component right) {
			this.phase = phase;
			this.seconds = seconds;
			this.fighter = fighter;
			this.left = left == null ? Component.empty() : left;
			this.right = right == null ? Component.empty() : right;
		}

		public static void encode(PhaseS2C msg, FriendlyByteBuf buf) {
			buf.writeEnum(msg.phase);
			buf.writeVarInt(msg.seconds);
			buf.writeBoolean(msg.fighter);
			buf.writeJsonWithCodec(ComponentSerialization.CODEC, msg.left);
			buf.writeJsonWithCodec(ComponentSerialization.CODEC, msg.right);
		}

		public static PhaseS2C decode(FriendlyByteBuf buf) {
			return new PhaseS2C(buf.readEnum(Phase.class), buf.readVarInt(), buf.readBoolean(), buf.readJsonWithCodec(ComponentSerialization.CODEC), buf.readJsonWithCodec(ComponentSerialization.CODEC));
		}

		public static void handle(PhaseS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TournamentOverlay.onPhase(msg)));
			ctx.get().setPacketHandled(true);
		}

		public Phase getPhase() {
			return this.phase;
		}

		public int getSeconds() {
			return this.seconds;
		}

		public boolean isFighter() {
			return this.fighter;
		}

		public Component getLeft() {
			return this.left;
		}

		public Component getRight() {
			return this.right;
		}
	}


	public static class RivalS2C {
		private static volatile UUID clientRival;
		@Nullable
		private final UUID rival;

		public RivalS2C(@Nullable UUID rival) {
			this.rival = rival;
		}

		public static void encode(RivalS2C msg, FriendlyByteBuf buf) {
			buf.writeBoolean(msg.rival != null);
			if (msg.rival != null) buf.writeUUID(msg.rival);
		}

		public static RivalS2C decode(FriendlyByteBuf buf) {
			return new RivalS2C(buf.readBoolean() ? buf.readUUID() : null);
		}

		public static void handle(RivalS2C msg, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> clientRival = msg.rival);
			ctx.get().setPacketHandled(true);
		}

		public static boolean isClientRival(UUID id) {
			return id != null && id.equals(clientRival);
		}

		public static void clearClient() {
			clientRival = null;
		}
	}


	public static class ActionC2S {

		public enum Action {
			OPEN_BRACKET, SIGN_UP, ACCEPT_RULES, DECLINE_RULES, READY;
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
