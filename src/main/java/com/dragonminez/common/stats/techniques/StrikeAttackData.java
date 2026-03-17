package com.dragonminez.common.stats.techniques;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class StrikeAttackData extends TechniqueData {
	private float damageMultiplier;

	public StrikeAttackData() { super(); }

	@Override
	public TechniqueType getType() { return TechniqueType.STRIKE_ATTACK; }

	@Override
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", this.id);
		tag.putString("Name", this.name);
		tag.putString("Author", this.author);
		tag.putInt("Experience", this.experience);
		tag.putDouble("BaseCost", this.baseCost);
		tag.putFloat("DamageMultiplier", this.damageMultiplier);
		tag.putInt("CastTime", this.castTime);
		tag.putInt("Cooldown", this.cooldown);
		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		this.id = tag.getString("Id");
		this.name = tag.getString("Name");
		this.author = tag.getString("Author");
		this.experience = tag.getInt("Experience");
		this.baseCost = tag.getDouble("BaseCost");
		this.damageMultiplier = tag.getFloat("DamageMultiplier");
		this.castTime = tag.getInt("CastTime");
		this.cooldown = tag.getInt("Cooldown");
	}
}