package com.dragonminez.common.events;

import com.dragonminez.common.quest.Quest;
import com.dragonminez.common.quest.Saga;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public abstract class DMZEvent extends Event {

	/**
	 * Event fired when a player's stat is about to change, doesn't matter from what source.
	 * This includes increases and decreases to stats, fired through commands, items, interfaces, etc.
	 * This event is cancelable; if canceled, the stat change will not occur.
	 */
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

		public Player getPlayer() {
			return player;
		}

		public StatType getStat() {
			return stat;
		}

		public int getOldValue() {
			return oldValue;
		}

		public int getNewValue() {
			return newValue;
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
	@Cancelable
	public static class KiChargeEvent extends Event {

		private final Player player;
		private final int currentEnergy;
		private final int maxEnergy;

		public KiChargeEvent(Player player, int currentEnergy, int maxEnergy) {
			this.player = player;
			this.currentEnergy = currentEnergy;
			this.maxEnergy = maxEnergy;
		}

		public Player getPlayer() {
			return player;
		}

		public int getCurrentEnergy() {
			return currentEnergy;
		}

		public int getMaxEnergy() {
			return maxEnergy;
		}

		public boolean isEnergyFull() {
			return currentEnergy >= maxEnergy;
		}
	}

	/**
	 * Event fired when a player is about to gain training points.
	 * This event is cancelable; if canceled, the TP gain will not occur.
	 */
	@Cancelable
	public static class TPGainEvent extends Event {

		private final Player player;
		private final int oldValue;
		private int tpGain;

		public TPGainEvent(Player player, int oldValue, int tpGain) {
			this.player = player;
			this.oldValue = oldValue;
			this.tpGain = tpGain;
		}

		public Player getPlayer() {
			return player;
		}

		public int getOldValue() {
			return oldValue;
		}

		public int getTpGain() {
			return tpGain;
		}

		public void setTpGain(int tpGain) {
			this.tpGain = tpGain;
		}

		public int getNewValue() {
			return oldValue + tpGain;
		}
	}

	/**
	 * Event fired when a player successfully blocks an attack.
	 * This event is cancelable; if canceled, the block will not occur, so the player will take full damage.
	 */
	@Cancelable
	public static class PlayerBlockEvent extends Event {
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

		public ServerPlayer getVictim() { return victim; }
		public LivingEntity getAttacker() { return attacker; }
		public float getOriginalDamage() { return originalDamage; }
		public float getFinalDamage() { return finalDamage; }
		public void setFinalDamage(float finalDamage) { this.finalDamage = finalDamage; }
		public boolean isParry() { return isParry; }
		public void setParry(boolean parry) { isParry = parry; }
		public float getPoiseDamage() { return poiseDamage; }
		public void setPoiseDamage(float poiseDamage) { this.poiseDamage = poiseDamage; }
	}

	/**
	 * Event fired when a player completes a quest.
	 */
	public static class QuestCompleteEvent extends Event {
		private final ServerPlayer player;
		private final Saga saga;
		private final Quest quest;
		private final List<ServerPlayer> partyMembers;

		public QuestCompleteEvent(ServerPlayer player, Saga saga, Quest quest, List<ServerPlayer> partyMembers) {
			this.player = player;
			this.saga = saga;
			this.quest = quest;
			this.partyMembers = partyMembers;
		}

		public ServerPlayer getPlayer() {
			return player;
		}

		public Saga getSaga() {
			return saga;
		}

		public Quest getQuest() {
			return quest;
		}

		public List<ServerPlayer> getPartyMembers() {
			return partyMembers;
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
			return player;
		}

		public CompoundTag getData() {
			return data;
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
			return player;
		}

		public CompoundTag getData() {
			return data;
		}
	}
}



