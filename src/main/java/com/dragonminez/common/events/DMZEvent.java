package com.dragonminez.common.events;

import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.Saga;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public abstract class DMZEvent extends Event {

	private static String buildQuestKey(Saga saga, Quest quest) {
		if (quest == null) {
			return "";
		}
		if (saga == null) {
			return quest.getStringId() != null ? quest.getStringId() : String.valueOf(quest.getId());
		}
		return saga.getId() + ":" + quest.getId();
	}

	/**
	 * Event fired when a player's stat is about to change, doesn't matter from what source.
	 * This includes increases and decreases to stats, fired through commands, items, interfaces, etc.
	 * This event is cancelable; if canceled, the stat change will not occur.
	 */
	@Getter
	@Cancelable
	public static class StatChangeEvent extends Event {

		private final Player player;
		private final StatType stat;
		private final int oldValue;
		private final int newValue;

		public StatChangeEvent(Player player, StatType stat, int oldValue, int newValue) {
			this.player = player;
			this.stat = stat;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public enum StatType {
			STRENGTH,
			STRIKE_POWER,
			RESISTANCE,
			VITALITY,
			KI_POWER,
			ENERGY
		}
	}

	/**
	 * Event fired when a player is about to charge ki energy.
	 * This event is cancelable; if canceled, the ki charge will not occur.
	 */
	@Getter
	@Cancelable
	public static class KiChargeEvent extends Event {

		private final Player player;
		private final float currentEnergy;
		private final float maxEnergy;

		public KiChargeEvent(Player player, float currentEnergy, float maxEnergy) {
			this.player = player;
			this.currentEnergy = currentEnergy;
			this.maxEnergy = maxEnergy;
		}

		public boolean isEnergyFull() {
			return currentEnergy >= maxEnergy;
		}
	}

	/**
	 * Event fired when a player is about to gain training points.
	 * This event is cancelable; if canceled, the TP gain will not occur.
	 */
	@Getter
	@Cancelable
	public static class TPGainEvent extends Event {

		private final Player player;
		private final int oldValue;
		@Setter
		private int tpGain;

		public TPGainEvent(Player player, int oldValue, int tpGain) {
			this.player = player;
			this.oldValue = oldValue;
			this.tpGain = tpGain;
		}

		public int getNewTpsValue() {
			return oldValue + tpGain;
		}
	}

	/**
	 * Event fired when a player successfully blocks an attack.
	 * This event is cancelable; if canceled, the block will not occur, so the player will take full damage.
	 */
	@Getter
	@Cancelable
	public static class PlayerBlockEvent extends Event {
		private final ServerPlayer victim;
		private final LivingEntity attacker;
		private final float originalDamage;
		@Setter
		private float finalDamage;
		@Setter
		private boolean isParry;
		@Setter
		private float poiseDamage;

		public PlayerBlockEvent(ServerPlayer victim, LivingEntity attacker, float originalDamage, float finalDamage, boolean isParry, float poiseDamage) {
			this.victim = victim;
			this.attacker = attacker;
			this.originalDamage = originalDamage;
			this.finalDamage = finalDamage;
			this.isParry = isParry;
			this.poiseDamage = poiseDamage;
		}

	}

	/**
	 * Event fired when a player performs a dash.
	 * This event is cancelable; if canceled, the dash will not occur.
	 */
	@Getter
	@Cancelable
	public static class PlayerDashEvent extends Event {
		private final ServerPlayer player;
		private final DashType dashType;
		@Setter
		private double distance;
		@Setter
		private int kiCost;

		public PlayerDashEvent(ServerPlayer player, DashType dashType, double distance, int kiCost) {
			this.player = player;
			this.dashType = dashType;
			this.distance = distance;
			this.kiCost = kiCost;
		}

		public enum DashType {
			NORMAL,
			DOUBLE
		}
	}

	/**
	 * Event fired when a player successfully evades an attack.
	 * This event is cancelable; if canceled, the evasion will not occur.
	 */
	@Getter
	@Cancelable
	public static class PlayerEvasionEvent extends Event {
		private final ServerPlayer player;
		private final LivingEntity attacker;
		private final float originalDamage;
		@Setter
		private int kiCost;

		public PlayerEvasionEvent(ServerPlayer player, LivingEntity attacker, float originalDamage, int kiCost) {
			this.player = player;
			this.attacker = attacker;
			this.originalDamage = originalDamage;
			this.kiCost = kiCost;
		}

	}

	/**
	 * Event fired when two entities are about to fuse.
	 * This event is cancelable; if canceled, the fusion will not occur.
	 */
	@Getter
	@Cancelable
	public static class FusionEvent extends Event {
		private final ServerPlayer initiator;
		private final LivingEntity target;
		private final FusionType type;

		public FusionEvent(ServerPlayer initiator, LivingEntity target, FusionType type) {
			this.initiator = initiator;
			this.target = target;
			this.type = type;
		}

		public enum FusionType {
			METAMORU, POTHALA, ABSORPTION, ASSIMILATION
		}
	}

	/**
	 * Event fired after a dragon has been successfully summoned from a complete Dragon Ball set.
	 */
	@Getter
	public static class DragonSummonedEvent extends Event {
		private final Player player;
		private final ServerLevel level;
		private final BlockPos position;
		private final DragonDefinition dragonDefinition;
		private final DragonBallSetDefinition ballSetDefinition;
		private final List<BlockPos> consumedPositions;

		public DragonSummonedEvent(Player player, ServerLevel level, BlockPos position, DragonDefinition dragonDefinition,
								  DragonBallSetDefinition ballSetDefinition, List<BlockPos> consumedPositions) {
			this.player = player;
			this.level = level;
			this.position = position != null ? position.immutable() : BlockPos.ZERO;
			this.dragonDefinition = dragonDefinition;
			this.ballSetDefinition = ballSetDefinition;
			this.consumedPositions = consumedPositions == null
					? List.of()
					: consumedPositions.stream().map(BlockPos::immutable).toList();
		}

		public String getDragonId() {
			return dragonDefinition != null ? dragonDefinition.getId() : "";
		}

		public String getBallSetId() {
			return ballSetDefinition != null ? ballSetDefinition.getId() : "";
		}
	}

	/**
	 * Base event for quest lifecycle hooks (start/progress/fail/turn-in/reward/complete).
	 */
	@Getter
	public abstract static class QuestLifecycleEvent extends Event {
		private final ServerPlayer player;
		private final String questKey;
		private final Saga saga;
		private final Quest quest;
		private final List<ServerPlayer> partyMembers;

		public QuestLifecycleEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers) {
			this.player = player;
			this.questKey = questKey == null ? "" : questKey;
			this.saga = saga;
			this.quest = quest;
			this.partyMembers = partyMembers == null ? List.of() : List.copyOf(partyMembers);
		}

	}

	/**
	 * Event fired before a quest is accepted.
	 */
	@Setter
	@Getter
	@Cancelable
	public static class QuestStartEvent extends QuestLifecycleEvent {
		private boolean hardMode;

		public QuestStartEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, boolean hardMode) {
			super(player, questKey, saga, quest, partyMembers);
			this.hardMode = hardMode;
		}

	}

	/**
	 * Event fired before objective progress is stored.
	 */
	@Getter
	@Cancelable
	public static class QuestObjectiveProgressEvent extends QuestLifecycleEvent {
		private final int objectiveIndex;
		private final int oldProgress;
		@Setter
		private int newProgress;
		private final int objectiveRequired;

		public QuestObjectiveProgressEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers,
										  int objectiveIndex, int oldProgress, int newProgress, int objectiveRequired) {
			super(player, questKey, saga, quest, partyMembers);
			this.objectiveIndex = objectiveIndex;
			this.oldProgress = oldProgress;
			this.newProgress = newProgress;
			this.objectiveRequired = objectiveRequired;
		}

	}

	/**
	 * Event fired before a quest is marked as failed.
	 */
	@Getter
	@Cancelable
	public static class QuestFailEvent extends QuestLifecycleEvent {
		private final FailureReason reason;

		public QuestFailEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, FailureReason reason) {
			super(player, questKey, saga, quest, partyMembers);
			this.reason = reason;
		}

		public enum FailureReason {
			PLAYER_DEATH,
			FORCED_RESET,
			SCRIPT
		}
	}

	/**
	 * Event fired before a quest turn-in action is applied.
	 */
	@Getter
	@Cancelable
	public static class QuestTurnInEvent extends QuestLifecycleEvent {
		private final String npcId;

		public QuestTurnInEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, String npcId) {
			super(player, questKey, saga, quest, partyMembers);
			this.npcId = npcId == null ? "" : npcId;
		}

	}

	/**
	 * Event fired before an individual reward is claimed.
	 */
	@Getter
	@Cancelable
	public static class QuestRewardClaimEvent extends QuestLifecycleEvent {
		private final int rewardIndex;

		public QuestRewardClaimEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, int rewardIndex) {
			super(player, questKey, saga, quest, partyMembers);
			this.rewardIndex = rewardIndex;
		}

	}

	/**
	 * Fires when quest is completed
	 */
	public static class QuestCompletedEvent extends QuestLifecycleEvent {
		public QuestCompletedEvent(ServerPlayer player, Saga saga, Quest quest, List<ServerPlayer> partyMembers) {
			super(player, buildQuestKey(saga, quest), saga, quest, partyMembers);
		}

		public QuestCompletedEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers) {
			super(player, questKey, saga, quest, partyMembers);
		}
	}

	/**
	 * Event fired when a player's data is being saved.
	 */
	@Getter
	public static class PlayerDataSaveEvent extends Event {
		private final ServerPlayer player;
		private final CompoundTag data;

		public PlayerDataSaveEvent(ServerPlayer player, CompoundTag data) {
			this.player = player;
			this.data = data;
		}

	}

	/**
	 * Event fired when a player's data is being loaded.
	 */
	@Getter
	public static class PlayerDataLoadEvent extends Event {
		private final ServerPlayer player;
		private final CompoundTag data;

		public PlayerDataLoadEvent(ServerPlayer player, CompoundTag data) {
			this.player = player;
			this.data = data;
		}

	}
}



