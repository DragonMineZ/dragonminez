package com.dragonminez.common.events;

import com.dragonminez.common.dragonball.DragonBallSetDefinition;
import com.dragonminez.common.dragonball.DragonDefinition;
import com.dragonminez.common.quest.Difficulty;
import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.Saga;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.StrikeAttackData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.Event;
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
	public static class StatChangeEvent extends Event implements ICancellableEvent {
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
			STRENGTH, STRIKE_POWER, RESISTANCE, VITALITY, KI_POWER, ENERGY;
		}

		public Player getPlayer() {
			return this.player;
		}

		public StatType getStat() {
			return this.stat;
		}

		public int getOldValue() {
			return this.oldValue;
		}

		public int getNewValue() {
			return this.newValue;
		}
	}


	/**
	 * Event fired when a player is about to charge ki energy.
	 * This event is cancelable; if canceled, the ki charge will not occur.
	 */
	public static class KiChargeEvent extends Event implements ICancellableEvent {
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

		public Player getPlayer() {
			return this.player;
		}

		public float getCurrentEnergy() {
			return this.currentEnergy;
		}

		public float getMaxEnergy() {
			return this.maxEnergy;
		}
	}


	/**
	 * Event fired when a player is about to gain training points.
	 * This event is cancelable; if canceled, the TP gain will not occur.
	 */
	public static class TPGainEvent extends Event implements ICancellableEvent {
		private final Player player;
		private final int oldValue;
		private boolean shareWithParty;
		private int tpGain;
		private final boolean preBoosted;

		public TPGainEvent(Player player, int oldValue, int tpGain, boolean shareWithParty) {
			this(player, oldValue, tpGain, shareWithParty, false);
		}

		public TPGainEvent(Player player, int oldValue, int tpGain, boolean shareWithParty, boolean preBoosted) {
			this.player = player;
			this.oldValue = oldValue;
			this.tpGain = tpGain;
			this.shareWithParty = shareWithParty;
			this.preBoosted = preBoosted;
		}

		public int getNewTpsValue() {
			return oldValue + tpGain;
		}

		public boolean getShareWithParty() {
			return shareWithParty;
		}

		public Player getPlayer() {
			return this.player;
		}

		public int getOldValue() {
			return this.oldValue;
		}

		public int getTpGain() {
			return this.tpGain;
		}

		public boolean isPreBoosted() {
			return this.preBoosted;
		}

		public void setShareWithParty(final boolean shareWithParty) {
			this.shareWithParty = shareWithParty;
		}

		public void setTpGain(final int tpGain) {
			this.tpGain = tpGain;
		}
	}


	/**
	 * Event fired when a player successfully blocks an attack.
	 * This event is cancelable; if canceled, the block will not occur, so the player will take full damage.
	 */
	public static class PlayerBlockEvent extends Event implements ICancellableEvent {
		private final ServerPlayer victim;
		private final LivingEntity attacker;
		private final float originalDamage;
		private float finalDamage;
		private boolean isParry;
		private float poiseDamage;

		public PlayerBlockEvent(ServerPlayer victim, LivingEntity attacker, float originalDamage, float finalDamage, boolean isParry, float poiseDamage) {
			this.victim = victim;
			this.attacker = attacker;
			this.originalDamage = originalDamage;
			this.finalDamage = finalDamage;
			this.isParry = isParry;
			this.poiseDamage = poiseDamage;
		}

		public ServerPlayer getVictim() {
			return this.victim;
		}

		public LivingEntity getAttacker() {
			return this.attacker;
		}

		public float getOriginalDamage() {
			return this.originalDamage;
		}

		public float getFinalDamage() {
			return this.finalDamage;
		}

		public boolean isParry() {
			return this.isParry;
		}

		public float getPoiseDamage() {
			return this.poiseDamage;
		}

		public void setFinalDamage(final float finalDamage) {
			this.finalDamage = finalDamage;
		}

		public void setParry(final boolean isParry) {
			this.isParry = isParry;
		}

		public void setPoiseDamage(final float poiseDamage) {
			this.poiseDamage = poiseDamage;
		}
	}


	/**
	 * Notification fired on the server once DMZ finished mitigating a hit on a player
	 * (end of {@code CombatEvent.overrideVanillaArmorReduction}, before the vanilla armor pass is replaced).
	 * <p>
	 * {@code rawDamage} is the incoming damage after the attacker's modifiers; {@code defenseMitigated}
	 * is what the defense stat, ki protection and racial reductions removed, {@code blockMitigated} what the
	 * manual block or parry removed, and {@code finalDamage} what the player actually takes.
	 * Not cancelable; listeners must not alter the damage.
	 */
	public static class PlayerDamageMitigatedEvent extends Event {
		private final ServerPlayer victim;
		private final DamageSource source;
		private final double rawDamage;
		private final double defenseMitigated;
		private final double blockMitigated;
		private final double finalDamage;

		public PlayerDamageMitigatedEvent(ServerPlayer victim, DamageSource source, double rawDamage, double defenseMitigated, double blockMitigated, double finalDamage) {
			this.victim = victim;
			this.source = source;
			this.rawDamage = rawDamage;
			this.defenseMitigated = defenseMitigated;
			this.blockMitigated = blockMitigated;
			this.finalDamage = finalDamage;
		}

		public ServerPlayer getVictim() {
			return this.victim;
		}

		public DamageSource getSource() {
			return this.source;
		}

		public double getRawDamage() {
			return this.rawDamage;
		}

		public double getDefenseMitigated() {
			return this.defenseMitigated;
		}

		public double getBlockMitigated() {
			return this.blockMitigated;
		}

		public double getFinalDamage() {
			return this.finalDamage;
		}
	}


	/**
	 * Notification fired on the server when a barrier (ki barrier technique, Android barrier) absorbs a hit
	 * before it reaches the protected entity. {@code owner} is who created the barrier and gets the credit;
	 * {@code protectedEntity} is who would have been hit. Not cancelable.
	 */
	public static class BarrierAbsorbEvent extends Event {
		private final Entity owner;
		private final LivingEntity protectedEntity;
		private final Entity attacker;
		private final float amount;

		public BarrierAbsorbEvent(Entity owner, LivingEntity protectedEntity, Entity attacker, float amount) {
			this.owner = owner;
			this.protectedEntity = protectedEntity;
			this.attacker = attacker;
			this.amount = amount;
		}

		public Entity getOwner() {
			return this.owner;
		}

		public LivingEntity getProtectedEntity() {
			return this.protectedEntity;
		}

		public Entity getAttacker() {
			return this.attacker;
		}

		public float getAmount() {
			return this.amount;
		}
	}


	/**
	 * Event fired when a player performs a dash.
	 * This event is cancelable; if canceled, the dash will not occur.
	 */
	public static class PlayerDashEvent extends Event implements ICancellableEvent {
		private final ServerPlayer player;
		private final DashType dashType;
		private double distance;
		private int kiCost;

		public PlayerDashEvent(ServerPlayer player, DashType dashType, double distance, int kiCost) {
			this.player = player;
			this.dashType = dashType;
			this.distance = distance;
			this.kiCost = kiCost;
		}


		public enum DashType {
			NORMAL, DOUBLE;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public DashType getDashType() {
			return this.dashType;
		}

		public double getDistance() {
			return this.distance;
		}

		public int getKiCost() {
			return this.kiCost;
		}

		public void setDistance(final double distance) {
			this.distance = distance;
		}

		public void setKiCost(final int kiCost) {
			this.kiCost = kiCost;
		}
	}


	/**
	 * Event fired when a player successfully evades an attack.
	 * This event is cancelable; if canceled, the evasion will not occur.
	 */
	public static class PlayerEvasionEvent extends Event implements ICancellableEvent {
		private final ServerPlayer player;
		private final LivingEntity attacker;
		private final float originalDamage;
		private int kiCost;

		public PlayerEvasionEvent(ServerPlayer player, LivingEntity attacker, float originalDamage, int kiCost) {
			this.player = player;
			this.attacker = attacker;
			this.originalDamage = originalDamage;
			this.kiCost = kiCost;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public LivingEntity getAttacker() {
			return this.attacker;
		}

		public float getOriginalDamage() {
			return this.originalDamage;
		}

		public int getKiCost() {
			return this.kiCost;
		}

		public void setKiCost(final int kiCost) {
			this.kiCost = kiCost;
		}
	}


	/**
	 * Event fired when two entities are about to fuse.
	 * This event is cancelable; if canceled, the fusion will not occur.
	 */
	public static class FusionEvent extends Event implements ICancellableEvent {
		private final ServerPlayer initiator;
		private final LivingEntity target;
		private final FusionType type;

		public FusionEvent(ServerPlayer initiator, LivingEntity target, FusionType type) {
			this.initiator = initiator;
			this.target = target;
			this.type = type;
		}


		public enum FusionType {
			METAMORU, POTHALA, ABSORPTION, ASSIMILATION, BEETLE;
		}

		public ServerPlayer getInitiator() {
			return this.initiator;
		}

		public LivingEntity getTarget() {
			return this.target;
		}

		public FusionType getType() {
			return this.type;
		}
	}


	/**
	 * Event fired after a dragon has been successfully summoned from a complete Dragon Ball set.
	 */
	public static class DragonSummonedEvent extends Event {
		private final Player player;
		private final ServerLevel level;
		private final BlockPos position;
		private final DragonDefinition dragonDefinition;
		private final DragonBallSetDefinition ballSetDefinition;
		private final List<BlockPos> consumedPositions;

		public DragonSummonedEvent(Player player, ServerLevel level, BlockPos position, DragonDefinition dragonDefinition, DragonBallSetDefinition ballSetDefinition, List<BlockPos> consumedPositions) {
			this.player = player;
			this.level = level;
			this.position = position != null ? position.immutable() : BlockPos.ZERO;
			this.dragonDefinition = dragonDefinition;
			this.ballSetDefinition = ballSetDefinition;
			this.consumedPositions = consumedPositions == null ? List.of() : consumedPositions.stream().map(BlockPos::immutable).toList();
		}

		public String getDragonId() {
			return dragonDefinition != null ? dragonDefinition.getId() : "";
		}

		public String getBallSetId() {
			return ballSetDefinition != null ? ballSetDefinition.getId() : "";
		}

		public Player getPlayer() {
			return this.player;
		}

		public ServerLevel getLevel() {
			return this.level;
		}

		public BlockPos getPosition() {
			return this.position;
		}

		public DragonDefinition getDragonDefinition() {
			return this.dragonDefinition;
		}

		public DragonBallSetDefinition getBallSetDefinition() {
			return this.ballSetDefinition;
		}

		public List<BlockPos> getConsumedPositions() {
			return this.consumedPositions;
		}
	}


	/**
	 * Base event for quest lifecycle hooks (start/progress/fail/turn-in/reward/complete).
	 */
	public static abstract class QuestLifecycleEvent extends Event implements ICancellableEvent {
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

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public String getQuestKey() {
			return this.questKey;
		}

		public Saga getSaga() {
			return this.saga;
		}

		public Quest getQuest() {
			return this.quest;
		}

		public List<ServerPlayer> getPartyMembers() {
			return this.partyMembers;
		}
	}


	/**
	 * Event fired before a quest is accepted.
	 */
	public static class QuestStartEvent extends QuestLifecycleEvent implements ICancellableEvent {
		private Difficulty difficulty;

		public QuestStartEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, Difficulty difficulty) {
			super(player, questKey, saga, quest, partyMembers);
			this.difficulty = difficulty;
		}

		public void setDifficulty(final Difficulty difficulty) {
			this.difficulty = difficulty;
		}

		public Difficulty getDifficulty() {
			return this.difficulty;
		}
	}


	/**
	 * Event fired before objective progress is stored.
	 */
	public static class QuestObjectiveProgressEvent extends QuestLifecycleEvent implements ICancellableEvent {
		private final int objectiveIndex;
		private final int oldProgress;
		private int newProgress;
		private final int objectiveRequired;

		public QuestObjectiveProgressEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, int objectiveIndex, int oldProgress, int newProgress, int objectiveRequired) {
			super(player, questKey, saga, quest, partyMembers);
			this.objectiveIndex = objectiveIndex;
			this.oldProgress = oldProgress;
			this.newProgress = newProgress;
			this.objectiveRequired = objectiveRequired;
		}

		public int getObjectiveIndex() {
			return this.objectiveIndex;
		}

		public int getOldProgress() {
			return this.oldProgress;
		}

		public int getNewProgress() {
			return this.newProgress;
		}

		public int getObjectiveRequired() {
			return this.objectiveRequired;
		}

		public void setNewProgress(final int newProgress) {
			this.newProgress = newProgress;
		}
	}


	/**
	 * Event fired before a quest is marked as failed.
	 */
	public static class QuestFailEvent extends QuestLifecycleEvent implements ICancellableEvent {
		private final FailureReason reason;

		public QuestFailEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, FailureReason reason) {
			super(player, questKey, saga, quest, partyMembers);
			this.reason = reason;
		}


		public enum FailureReason {
			PLAYER_DEATH, FORCED_RESET, SCRIPT, TIME_EXPIRED, ESCORT_FAILED;
		}

		public FailureReason getReason() {
			return this.reason;
		}
	}


	/**
	 * Event fired before a quest turn-in action is applied.
	 */
	public static class QuestTurnInEvent extends QuestLifecycleEvent implements ICancellableEvent {
		private final String npcId;

		public QuestTurnInEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, String npcId) {
			super(player, questKey, saga, quest, partyMembers);
			this.npcId = npcId == null ? "" : npcId;
		}

		public String getNpcId() {
			return this.npcId;
		}
	}


	/**
	 * Event fired before an individual reward is claimed.
	 */
	public static class QuestRewardClaimEvent extends QuestLifecycleEvent implements ICancellableEvent {
		private final int rewardIndex;

		public QuestRewardClaimEvent(ServerPlayer player, String questKey, Saga saga, Quest quest, List<ServerPlayer> partyMembers, int rewardIndex) {
			super(player, questKey, saga, quest, partyMembers);
			this.rewardIndex = rewardIndex;
		}

		public int getRewardIndex() {
			return this.rewardIndex;
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
	public static class PlayerDataSaveEvent extends Event {
		private final ServerPlayer player;
		private final CompoundTag data;

		public PlayerDataSaveEvent(ServerPlayer player, CompoundTag data) {
			this.player = player;
			this.data = data;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public CompoundTag getData() {
			return this.data;
		}
	}


	/**
	 * Event fired when a player's data is being loaded.
	 */
	public static class PlayerDataLoadEvent extends Event {
		private final ServerPlayer player;
		private final CompoundTag data;

		public PlayerDataLoadEvent(ServerPlayer player, CompoundTag data) {
			this.player = player;
			this.data = data;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public CompoundTag getData() {
			return this.data;
		}
	}


	/**
	 * Base for the per-second resource regeneration events. Modify {@link #amount} or cancel to suppress.
	 */
	public static abstract class ResourceRegenEvent extends Event implements ICancellableEvent {
		private final Player player;
		private final StatsData statsData;
		private double amount;

		protected ResourceRegenEvent(Player player, StatsData statsData, double amount) {
			this.player = player;
			this.statsData = statsData;
			this.amount = amount;
		}

		public Player getPlayer() {
			return this.player;
		}

		public StatsData getStatsData() {
			return this.statsData;
		}

		public double getAmount() {
			return this.amount;
		}

		public void setAmount(final double amount) {
			this.amount = amount;
		}
	}


	/**
	 * Fired before the per-second health (HP5) regen is applied.
	 */
	public static class HealthRegenEvent extends ResourceRegenEvent {
		public HealthRegenEvent(Player player, StatsData statsData, double amount) {
			super(player, statsData, amount);
		}
	}


	/**
	 * Fired before the per-second energy (Ki) regen is applied.
	 */
	public static class EnergyRegenEvent extends ResourceRegenEvent {
		public EnergyRegenEvent(Player player, StatsData statsData, double amount) {
			super(player, statsData, amount);
		}
	}


	/**
	 * Fired before the per-second stamina (STM) regen is applied.
	 */
	public static class StaminaRegenEvent extends ResourceRegenEvent {
		public StaminaRegenEvent(Player player, StatsData statsData, double amount) {
			super(player, statsData, amount);
		}
	}


	public enum DamageSourceType {
		MELEE, KI, STRIKE;
	}


	/**
	 * Phase 1: fired pre-mitigation for an outgoing hit. Modify {@link #amount}/{@link #defensePenetration} or cancel.
	 */
	public static class DamageModifyEvent extends Event implements ICancellableEvent {
		private final Player attacker;
		private final LivingEntity victim;
		private double amount;
		private double defensePenetration;
		private final DamageSourceType sourceType;

		public DamageModifyEvent(Player attacker, LivingEntity victim, double amount, double defensePenetration, DamageSourceType sourceType) {
			this.attacker = attacker;
			this.victim = victim;
			this.amount = amount;
			this.defensePenetration = defensePenetration;
			this.sourceType = sourceType;
		}

		public Player getAttacker() {
			return this.attacker;
		}

		public LivingEntity getVictim() {
			return this.victim;
		}

		public double getAmount() {
			return this.amount;
		}

		public double getDefensePenetration() {
			return this.defensePenetration;
		}

		public DamageSourceType getSourceType() {
			return this.sourceType;
		}

		public void setAmount(final double amount) {
			this.amount = amount;
		}

		public void setDefensePenetration(final double defensePenetration) {
			this.defensePenetration = defensePenetration;
		}
	}


	/**
	 * Phase 2: notification fired after blocking is resolved for a DMZ hit (pre-mitigation amount).
	 */
	public static class DamageDealtEvent extends Event {
		private final Player attacker;
		private final LivingEntity victim;
		private final double amount;
		private final boolean blocked;
		private final boolean parried;
		private final DamageSourceType sourceType;

		public DamageDealtEvent(Player attacker, LivingEntity victim, double amount, boolean blocked, boolean parried, DamageSourceType sourceType) {
			this.attacker = attacker;
			this.victim = victim;
			this.amount = amount;
			this.blocked = blocked;
			this.parried = parried;
			this.sourceType = sourceType;
		}

		public Player getAttacker() {
			return this.attacker;
		}

		public LivingEntity getVictim() {
			return this.victim;
		}

		public double getAmount() {
			return this.amount;
		}

		public boolean isBlocked() {
			return this.blocked;
		}

		public boolean isParried() {
			return this.parried;
		}

		public DamageSourceType getSourceType() {
			return this.sourceType;
		}
	}


	/**
	 * Fired when resolving a player's crit chance. Listeners may modify {@link #chance} (0..1).
	 */
	public static class CritChanceEvent extends Event {
		private final Player player;
		private double chance;

		public CritChanceEvent(Player player, double chance) {
			this.player = player;
			this.chance = chance;
		}

		public Player getPlayer() {
			return this.player;
		}

		public double getChance() {
			return this.chance;
		}

		public void setChance(final double chance) {
			this.chance = chance;
		}
	}


	/**
	 * Notification fired when a player begins charging (casts) a Ki attack.
	 */
	public static class KiAttackCastEvent extends Event {
		private final Player player;
		private final StatsData statsData;
		private final KiAttackData kiAttack;

		public KiAttackCastEvent(Player player, StatsData statsData, KiAttackData kiAttack) {
			this.player = player;
			this.statsData = statsData;
			this.kiAttack = kiAttack;
		}

		public Player getPlayer() {
			return this.player;
		}

		public StatsData getStatsData() {
			return this.statsData;
		}

		public KiAttackData getKiAttack() {
			return this.kiAttack;
		}
	}


	/**
	 * Fired when a Ki attack is released (fired). Listeners may modify {@link #cooldownTicks}.
	 */
	public static class KiAttackFireEvent extends Event {
		private final Player player;
		private final StatsData statsData;
		private final KiAttackData kiAttack;
		private final float chargeMultiplier;
		private int cooldownTicks;

		public KiAttackFireEvent(Player player, StatsData statsData, KiAttackData kiAttack, float chargeMultiplier, int cooldownTicks) {
			this.player = player;
			this.statsData = statsData;
			this.kiAttack = kiAttack;
			this.chargeMultiplier = chargeMultiplier;
			this.cooldownTicks = cooldownTicks;
		}

		public Player getPlayer() {
			return this.player;
		}

		public StatsData getStatsData() {
			return this.statsData;
		}

		public KiAttackData getKiAttack() {
			return this.kiAttack;
		}

		public float getChargeMultiplier() {
			return this.chargeMultiplier;
		}

		public int getCooldownTicks() {
			return this.cooldownTicks;
		}

		public void setCooldownTicks(final int cooldownTicks) {
			this.cooldownTicks = cooldownTicks;
		}
	}


	/**
	 * Notification fired when a player initiates (casts) a strike attack.
	 */
	public static class StrikeAttackCastEvent extends Event {
		private final ServerPlayer player;
		private final StatsData statsData;
		private final StrikeAttackData strike;

		public StrikeAttackCastEvent(ServerPlayer player, StatsData statsData, StrikeAttackData strike) {
			this.player = player;
			this.statsData = statsData;
			this.strike = strike;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public StatsData getStatsData() {
			return this.statsData;
		}

		public StrikeAttackData getStrike() {
			return this.strike;
		}
	}


	/**
	 * Notification fired when a strike attack connects and begins (fires) on a target.
	 */
	public static class StrikeAttackFireEvent extends Event {
		private final ServerPlayer player;
		private final StatsData statsData;
		private final StrikeAttackData strike;
		private final LivingEntity target;

		public StrikeAttackFireEvent(ServerPlayer player, StatsData statsData, StrikeAttackData strike, LivingEntity target) {
			this.player = player;
			this.statsData = statsData;
			this.strike = strike;
			this.target = target;
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public StatsData getStatsData() {
			return this.statsData;
		}

		public StrikeAttackData getStrike() {
			return this.strike;
		}

		public LivingEntity getTarget() {
			return this.target;
		}
	}


	/**
	 * Notification fired when a player's base form changes (transform or untransform).
	 */
	public static class FormChangeEvent extends Event {
		private final ServerPlayer player;
		private final String oldGroup;
		private final String oldForm;
		private final String newGroup;
		private final String newForm;

		public FormChangeEvent(ServerPlayer player, String oldGroup, String oldForm, String newGroup, String newForm) {
			this.player = player;
			this.oldGroup = oldGroup == null ? "" : oldGroup;
			this.oldForm = oldForm == null ? "" : oldForm;
			this.newGroup = newGroup == null ? "" : newGroup;
			this.newForm = newForm == null ? "" : newForm;
		}

		public boolean isTransform() {
			return oldForm.isEmpty() && !newForm.isEmpty();
		}

		public boolean isUntransform() {
			return !oldForm.isEmpty() && newForm.isEmpty();
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public String getOldGroup() {
			return this.oldGroup;
		}

		public String getOldForm() {
			return this.oldForm;
		}

		public String getNewGroup() {
			return this.newGroup;
		}

		public String getNewForm() {
			return this.newForm;
		}
	}


	/**
	 * Notification fired when a player's stack form changes.
	 */
	public static class StackFormChangeEvent extends Event {
		private final ServerPlayer player;
		private final String oldGroup;
		private final String oldForm;
		private final String newGroup;
		private final String newForm;

		public StackFormChangeEvent(ServerPlayer player, String oldGroup, String oldForm, String newGroup, String newForm) {
			this.player = player;
			this.oldGroup = oldGroup == null ? "" : oldGroup;
			this.oldForm = oldForm == null ? "" : oldForm;
			this.newGroup = newGroup == null ? "" : newGroup;
			this.newForm = newForm == null ? "" : newForm;
		}

		public boolean isTransform() {
			return oldForm.isEmpty() && !newForm.isEmpty();
		}

		public boolean isUntransform() {
			return !oldForm.isEmpty() && newForm.isEmpty();
		}

		public ServerPlayer getPlayer() {
			return this.player;
		}

		public String getOldGroup() {
			return this.oldGroup;
		}

		public String getOldForm() {
			return this.oldForm;
		}

		public String getNewGroup() {
			return this.newGroup;
		}

		public String getNewForm() {
			return this.newForm;
		}
	}
}
