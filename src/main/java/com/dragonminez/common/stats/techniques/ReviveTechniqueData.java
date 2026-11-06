package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;

public class ReviveTechniqueData extends TechniqueData {
	public static final String ID = "worldboss_revive";
	public static final String TECH_CLASS_TYPE = "REVIVE";
	public static final String NAME_KEY = "technique.dragonminez.worldboss_revive";

	public ReviveTechniqueData() {
		super();
		this.id = ID;
		this.name = NAME_KEY;
		this.author = "dragonminez";
	}

	public static boolean isRevive(String techniqueId) {
		return ID.equals(techniqueId);
	}

	@Override
	public TechniqueType getType() {
		return TechniqueType.REVIVE;
	}

	@Override
	public double getCalculatedCost(StatsData statsData) {
		return 0.0;
	}

	@Override
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", ID);
		tag.putString("Name", NAME_KEY);
		tag.putString("Author", this.author != null ? this.author : "dragonminez");
		tag.putInt("Experience", this.experience);
		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		this.id = ID;
		this.name = NAME_KEY;
		this.author = tag.contains("Author") ? tag.getString("Author") : "dragonminez";
		this.experience = tag.getInt("Experience");
	}
}
