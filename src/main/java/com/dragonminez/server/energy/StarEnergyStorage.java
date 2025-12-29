package com.dragonminez.server.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;

public class StarEnergyStorage extends EnergyStorage {
	public StarEnergyStorage(int capacity, int maxTransfer) {
		super(capacity, maxTransfer);
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		int extracted = super.extractEnergy(maxExtract, simulate);
		if (extracted != 0) {
			onEnergyChanged();
		}
		return extracted;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int received = super.receiveEnergy(maxReceive, simulate);
		if (received != 0) {
			onEnergyChanged();
		}
		return received;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
		onEnergyChanged();
	}

	public void onEnergyChanged() {
	}

	public void saveNBT(CompoundTag tag) {
		tag.putInt("energy", energy);
	}

	public void loadNBT(CompoundTag tag) {
		energy = tag.getInt("energy");
	}
}