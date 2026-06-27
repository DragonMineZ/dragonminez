package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import java.util.Locale;
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

	public void addExperience(int amount) { this.experience += amount; }

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
}