package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

@Getter
@Setter
public class EvasionAttackData extends TechniqueData {
	public static final float LEVEL_MODIFIER_STEP = 0.025f;
	public static final float MAX_MODIFIER_LIMIT = 0.5f;

	private float damageMultiplier;
	private int durationTicks;
	private int durationLevel;
	private int cooldownLevel;
	private String animationId;
	private boolean useStrikePower;

	public EvasionAttackData() { super(); }

	@Override
	public TechniqueType getType() { return TechniqueType.EVASION_ATTACK; }

	public float getActualDamageMultiplier() { return damageMultiplier; }

	public int getActualDurationTicks() {
		float bonus = Math.min(MAX_MODIFIER_LIMIT, Math.max(0, durationLevel) * LEVEL_MODIFIER_STEP);
		return Math.max(1, Math.round(durationTicks * (1.0f + bonus)));
	}

	public int getActualCooldown() {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		int base = cfg.getCooldownTicks() > 0 ? cfg.getCooldownTicks() : this.cooldown;
		float reduction = Math.max(1.0f - MAX_MODIFIER_LIMIT, 1.0f - Math.max(0, cooldownLevel) * LEVEL_MODIFIER_STEP);
		return Math.max(1, Math.round(base * reduction));
	}

	public boolean canUpgradeStat(String statName) {
		return "duration".equals(statName) || "cooldown".equals(statName);
	}

	public int getUpgradeXpCost(String statName) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		int baseMin = Math.max(0, cfg.getMinXPCost());
		double multiplier = Math.max(0.0, cfg.getXpCostMultiplier());
		int totalUpgrades = Math.max(0, durationLevel) + Math.max(0, cooldownLevel);
		int scaledBase = (int) Math.round(baseMin * multiplier);
		int upgradeExtra = (int) Math.round(totalUpgrades * Math.max(0.0, baseMin * (multiplier - 1.0)));
		int computed = Math.max(0, scaledBase + upgradeExtra);
		int max = cfg.getMaxXPCost();
		if (max >= 0) computed = Math.min(computed, max);
		return Math.max(0, computed);
	}

	public int getXpGainPerHit() {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double gain = Math.max(0.0, cfg.getXpGainPerHit() * cfg.getXpGainMultiplier());
		return Math.max(0, (int) Math.round(gain));
	}

	public double getActualHitDamage(StatsData statsData) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double statValue = useStrikePower ? statsData.getStats().getStrikePower() : statsData.getStats().getKiPower();
		return 0.10 * statValue * getActualDamageMultiplier() * Math.max(0.0, cfg.getDamageMultiplier());
	}

	@Override
	public double getCalculatedCost(StatsData statsData) {
		TechniqueConfig.EvasionAttackConfig cfg = ConfigManager.getTechniqueConfig().getEvasionConfig(this.id);
		double base = getActualDurationTicks() * 0.5 + getActualHitDamage(statsData) * 2.0;
		return Math.max(5.0, base * Math.max(0.0, cfg.getKiCostMultiplier()));
	}

	@Override
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("Id", this.id);
		tag.putString("Name", this.name);
		tag.putString("Author", this.author);
		tag.putInt("Experience", this.experience);
		tag.putDouble("BaseCost", this.baseCost);
		tag.putFloat("TpCost", this.tpCost);
		tag.putFloat("DamageMultiplier", this.damageMultiplier);
		tag.putInt("DurationTicks", this.durationTicks);
		tag.putInt("DurationLevel", this.durationLevel);
		tag.putInt("CooldownLevel", this.cooldownLevel);
		tag.putString("AnimationId", this.animationId != null ? this.animationId : "");
		tag.putBoolean("UseStrikePower", this.useStrikePower);
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
		this.tpCost = tag.contains("TpCost") ? tag.getFloat("TpCost") : 0;
		this.damageMultiplier = tag.getFloat("DamageMultiplier");
		this.durationTicks = tag.contains("DurationTicks") ? tag.getInt("DurationTicks") : 60;
		if (this.durationTicks <= 0) this.durationTicks = 60;
		this.durationLevel = tag.getInt("DurationLevel");
		this.cooldownLevel = tag.getInt("CooldownLevel");
		this.animationId = tag.getString("AnimationId");
		this.useStrikePower = tag.getBoolean("UseStrikePower");
		this.castTime = tag.getInt("CastTime");
		this.cooldown = tag.getInt("Cooldown");
	}
}
