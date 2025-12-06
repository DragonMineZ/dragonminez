package com.dragonminez.common.events;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class DMZEvent extends Event {

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
}



