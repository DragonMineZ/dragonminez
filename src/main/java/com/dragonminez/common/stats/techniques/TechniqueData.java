package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import java.util.Locale;
import java.util.UUID;

public abstract class TechniqueData {
	protected String id;
	protected String name;
	protected String author;
	protected int experience;
	protected double baseCost;
	protected int castTime;
	protected int cooldown;
	protected float tpCost;

	public TechniqueData() {
		this.id = UUID.randomUUID().toString();
		this.experience = 0;
		this.castTime = 0;
		this.cooldown = 0;
		this.tpCost = 0;
	}

	public abstract CompoundTag save();

	public abstract void load(CompoundTag tag);

	public abstract TechniqueType getType();

	public abstract double getCalculatedCost(StatsData statsData);

	public void addExperience(int amount) {
		this.experience += amount;
	}

	public static String generateId(String author, String name) {
		String a = sanitizeIdPart(author);
		String n = sanitizeIdPart(name);
		if (a.isEmpty()) a = "player";
		if (n.isEmpty()) n = "skill";
		return a + "_" + n;
	}

	private static String sanitizeIdPart(String value) {
		if (value == null) return "";
		return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getAuthor() {
		return this.author;
	}

	public int getExperience() {
		return this.experience;
	}

	public double getBaseCost() {
		return this.baseCost;
	}

	public int getCastTime() {
		return this.castTime;
	}

	public int getCooldown() {
		return this.cooldown;
	}

	public float getTpCost() {
		return this.tpCost;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setAuthor(final String author) {
		this.author = author;
	}

	public void setExperience(final int experience) {
		this.experience = experience;
	}

	public void setBaseCost(final double baseCost) {
		this.baseCost = baseCost;
	}

	public void setCastTime(final int castTime) {
		this.castTime = castTime;
	}

	public void setCooldown(final int cooldown) {
		this.cooldown = cooldown;
	}

	public void setTpCost(final float tpCost) {
		this.tpCost = tpCost;
	}
}
