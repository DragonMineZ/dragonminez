package com.dragonminez.common.stats.techniques;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

@Getter
@Setter
public abstract class TechniqueData {
	protected String id;
	protected String name;
	protected String author;
	protected int experience;
	protected double baseCost;
	protected int castTime;
	protected int cooldown;

	public TechniqueData() {
		this.id = UUID.randomUUID().toString();
		this.experience = 0;
		this.castTime = 0;
		this.cooldown = 0;
	}

	public abstract CompoundTag save();
	public abstract void load(CompoundTag tag);
	public abstract TechniqueType getType();

	public double getCalculatedCost() {
		// Mas adelante voy a pensar como hacer esta wbd de formula xd
		// La idea es que el dmg aumente el coste pero a su vez se reduzca el coste segun la experiencia gastada en "mejorar" el coste, en vez de la experiencia total.
		double reduction = Math.min(0.5, this.experience * 0.001);
		return baseCost * (1.0 - reduction);
	}

	public void addExperience(int amount) { this.experience += amount; }
}

