package com.dragonminez.client.gui.character;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.network.C2S.CreateTechniqueC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.techniques.KiAttackData;
import com.dragonminez.common.stats.techniques.TechniqueData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class TechniqueDraft {

	public enum ColorTarget {
		INTERIOR, EXTERIOR, OUTLINE;
	}

	private String name;
	private KiAttackData.KiType type = KiAttackData.KiType.SMALL_BALL;
	private KiAttackData.Utility utility = KiAttackData.Utility.DAMAGE;
	private float damage = KiAttackData.getDefaultDamageForType(KiAttackData.KiType.SMALL_BALL);
	private float size = KiAttackData.getDefaultSizeForType(KiAttackData.KiType.SMALL_BALL);
	private float speed = 1.0F;
	private int armorPen = 0;
	private int cast = 20;
	private int cooldown = 20;
	private float kiCost = 25;
	private float tpCost = 120;
	private int colorInterior = 16777215;
	private int colorExterior = 44783;
	private int colorOutline = 16777215;
	private KiAttackData.SecondaryEffectType secondaryType = KiAttackData.SecondaryEffectType.NONE;
	private KiAttackData.AffectedStat affectedStat = KiAttackData.AffectedStat.STR;
	private int secondaryIntensity = KiAttackData.MIN_SECONDARY_INTENSITY;
	private int secondaryDuration = KiAttackData.MIN_SECONDARY_DURATION;

	public static TechniqueDraft create() {
		TechniqueDraft draft = new TechniqueDraft();
		draft.name = Component.translatable("gui.dragonminez.skills.new_skill").getString();
		draft.applyAuraDefaults();
		draft.recompute();
		return draft;
	}

	private void applyAuraDefaults() {
		StatsData stats = localStats();
		if (stats == null) return;
		String auraColor = stats.getCharacter().getAuraColor();
		if (auraColor == null || auraColor.isEmpty()) return;
		int aura = ColorUtils.hexToInt(auraColor);
		colorInterior = aura;
		colorExterior = ColorUtils.darkenColor(aura, 0.75F);
	}

	public void setType(KiAttackData.KiType newType) {
		type = newType;
		utility = KiAttackData.Utility.DAMAGE;
		damage = KiAttackData.getDefaultDamageForType(type);
		size = KiAttackData.getDefaultSizeForType(type);
		speed = KiAttackData.getDefaultSpeedForType(type);
		armorPen = KiAttackData.getDefaultArmorPenForType(type);
		recompute();
	}

	public void cycleType(int direction) {
		KiAttackData.KiType[] values = KiAttackData.KiType.values();
		setType(values[Math.floorMod(type.ordinal() + direction, values.length)]);
	}

	public boolean allowsUtility() {
		return KiAttackData.allowsHealUtility(type);
	}

	public KiAttackData.Utility getEffectiveUtility() {
		return allowsUtility() ? utility : KiAttackData.Utility.DAMAGE;
	}

	public void toggleUtility() {
		if (!allowsUtility()) return;
		utility = utility == KiAttackData.Utility.DAMAGE ? KiAttackData.Utility.HEAL : KiAttackData.Utility.DAMAGE;
		recompute();
	}

	public void adjustDamage(boolean increase, boolean coarse) {
		float step = coarse ? 0.25F : 0.05F;
		damage = Mth.clamp(damage + (increase ? step : -step), KiAttackData.getMinDamageForType(type), KiAttackData.getMaxDamageForType(type));
		recompute();
	}

	public void adjustSize(boolean increase, boolean coarse) {
		if (!KiAttackData.usesCustomSize(type)) return;
		float step = coarse ? 5.0F : 0.5F;
		size = Mth.clamp(size + (increase ? step : -step), KiAttackData.getMinSizeForType(type), KiAttackData.getMaxSizeForType(type));
		recompute();
	}

	public void adjustSpeed(boolean increase, boolean coarse) {
		if (!KiAttackData.usesCustomSpeed(type)) return;
		float step = coarse ? 0.5F : 0.1F;
		speed = Mth.clamp(speed + (increase ? step : -step), KiAttackData.getMinSpeedForType(type), KiAttackData.getMaxSpeedForType(type));
		recompute();
	}

	public void adjustArmorPen(boolean increase, boolean coarse) {
		if (!KiAttackData.usesCustomArmorPen(type)) return;
		int step = coarse ? 5 : 1;
		armorPen = Mth.clamp(armorPen + (increase ? step : -step), 0, KiAttackData.getMaxArmorPenForType(type));
		recompute();
	}

	public void adjustIntensity(boolean increase, boolean coarse) {
		int step = coarse ? 25 : 5;
		secondaryIntensity = Mth.clamp(secondaryIntensity + (increase ? step : -step), KiAttackData.MIN_SECONDARY_INTENSITY, KiAttackData.MAX_SECONDARY_INTENSITY);
		recompute();
	}

	public void adjustDuration(boolean increase, boolean coarse) {
		int step = coarse ? 4 : 1;
		secondaryDuration = Mth.clamp(secondaryDuration + (increase ? step : -step), KiAttackData.MIN_SECONDARY_DURATION, KiAttackData.MAX_SECONDARY_DURATION);
		recompute();
	}

	public void cycleSecondaryType() {
		KiAttackData.SecondaryEffectType valid = utility == KiAttackData.Utility.HEAL ? KiAttackData.SecondaryEffectType.BUFF : KiAttackData.SecondaryEffectType.DEBUFF;
		secondaryType = secondaryType == KiAttackData.SecondaryEffectType.NONE ? valid : KiAttackData.SecondaryEffectType.NONE;
		recompute();
	}

	public void cycleAffectedStat(int direction) {
		KiAttackData.AffectedStat[] values = KiAttackData.AffectedStat.values();
		affectedStat = values[Math.floorMod(affectedStat.ordinal() + direction, values.length)];
		recompute();
	}

	public boolean hasSecondaryEffect() {
		return secondaryType != KiAttackData.SecondaryEffectType.NONE;
	}

	public int getColor(ColorTarget target) {
		return switch (target) {
			case INTERIOR -> colorInterior;
			case EXTERIOR -> colorExterior;
			case OUTLINE -> colorOutline;
		};
	}

	public void setColor(ColorTarget target, int color) {
		int rgb = color & 16777215;
		switch (target) {
			case INTERIOR -> colorInterior = rgb;
			case EXTERIOR -> colorExterior = rgb;
			case OUTLINE -> colorOutline = rgb;
		}
	}

	public void recompute() {
		if (secondaryType != KiAttackData.SecondaryEffectType.NONE) {
			boolean valid = (secondaryType == KiAttackData.SecondaryEffectType.BUFF && utility == KiAttackData.Utility.HEAL) || (secondaryType == KiAttackData.SecondaryEffectType.DEBUFF && utility == KiAttackData.Utility.DAMAGE);
			if (!valid) secondaryType = KiAttackData.SecondaryEffectType.NONE;
		}
		float[] normalized = KiAttackData.normalizeStatsForType(type, damage, size, speed, armorPen);
		damage = normalized[0];
		size = normalized[1];
		speed = normalized[2];
		armorPen = Math.round(normalized[3]);
		KiAttackData preview = buildTechnique();
		preview.calculateDerivedValues();
		tpCost = Math.max(0, Math.round(preview.getTpCost()));
		cast = preview.getActualCastTime();
		cooldown = preview.getCooldown();
		StatsData stats = localStats();
		kiCost = stats == null ? 0.0F : (float) preview.getCalculatedCost(stats);
	}

	public KiAttackData buildTechnique() {
		KiAttackData ki = new KiAttackData();
		ki.setKiType(type);
		ki.setUtility(getEffectiveUtility());
		ki.setDamageMultiplier(damage);
		ki.setSize(size);
		ki.setSpeed(speed);
		ki.setArmorPenetration(armorPen);
		ki.setColorInterior(colorInterior);
		ki.setColorExterior(colorExterior);
		ki.setColorOutline(colorOutline);
		ki.setSecondaryEffectType(secondaryType);
		if (secondaryType != KiAttackData.SecondaryEffectType.NONE) {
			ki.setAffectedStat(affectedStat);
			ki.setSecondaryIntensity(secondaryIntensity);
			ki.setSecondaryDuration(secondaryDuration);
		}
		return ki;
	}

	public String resolveName() {
		String fallback = Component.translatable("gui.dragonminez.skills.new_skill").getString();
		String trimmed = name == null ? "" : name.trim();
		return trimmed.isEmpty() ? fallback : trimmed;
	}

	public boolean isDuplicateName() {
		Minecraft mc = Minecraft.getInstance();
		StatsData stats = localStats();
		if (mc.player == null || stats == null) return false;
		String id = TechniqueData.generateId(mc.player.getName().getString(), resolveName());
		return stats.getTechniques().getUnlockedTechniques().containsKey(id);
	}

	public double availableTrainingPoints() {
		StatsData stats = localStats();
		return stats == null ? 0.0 : stats.getResources().getTrainingPointsExact();
	}

	public boolean canAfford() {
		return availableTrainingPoints() >= tpCost;
	}

	public double damageOutput() {
		StatsData stats = localStats();
		double baseKiDamage = stats == null ? 0.0 : stats.getKiDamage();
		double configDamage = Math.max(0.0, ConfigManager.getTechniqueConfig().getKiTypeConfig(type).getDamageMultiplier());
		double output = getEffectiveUtility() == KiAttackData.Utility.HEAL ? KiAttackData.HEAL_OUTPUT_FACTOR : 1.0;
		return baseKiDamage * damage * configDamage * output;
	}

	public String typeKey() {
		return "technique.type." + type.name().toLowerCase(Locale.ROOT);
	}

	public CreateTechniqueC2S toPacket() {
		float[] normalized = KiAttackData.normalizeStatsForType(type, damage, size, speed, armorPen);
		return new CreateTechniqueC2S(resolveName(), type.name(), getEffectiveUtility().name(), normalized[0], normalized[2], normalized[1], Math.round(normalized[3]), cast, cooldown, colorInterior, colorExterior, colorOutline, secondaryType.name(), secondaryType == KiAttackData.SecondaryEffectType.NONE ? "" : affectedStat.name(), secondaryIntensity, secondaryDuration);
	}

	static StatsData localStats() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return null;
		return StatsProvider.get(StatsCapability.INSTANCE, mc.player).resolve().orElse(null);
	}

	public String getName() {
		return this.name;
	}

	public KiAttackData.KiType getType() {
		return this.type;
	}

	public KiAttackData.Utility getUtility() {
		return this.utility;
	}

	public float getDamage() {
		return this.damage;
	}

	public float getSize() {
		return this.size;
	}

	public float getSpeed() {
		return this.speed;
	}

	public int getArmorPen() {
		return this.armorPen;
	}

	public int getCast() {
		return this.cast;
	}

	public int getCooldown() {
		return this.cooldown;
	}

	public float getKiCost() {
		return this.kiCost;
	}

	public float getTpCost() {
		return this.tpCost;
	}

	public int getColorInterior() {
		return this.colorInterior;
	}

	public int getColorExterior() {
		return this.colorExterior;
	}

	public int getColorOutline() {
		return this.colorOutline;
	}

	public KiAttackData.SecondaryEffectType getSecondaryType() {
		return this.secondaryType;
	}

	public KiAttackData.AffectedStat getAffectedStat() {
		return this.affectedStat;
	}

	public int getSecondaryIntensity() {
		return this.secondaryIntensity;
	}

	public int getSecondaryDuration() {
		return this.secondaryDuration;
	}

	public void setName(final String name) {
		this.name = name;
	}
}
