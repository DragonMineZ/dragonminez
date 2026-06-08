package com.dragonminez.common.stats.techniques;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredefinedTechniques {

	public static final Map<String, KiAttackData> REGISTRY = new HashMap<>();
	public static final Map<String, StrikeAttackData> STRIKE_REGISTRY = new HashMap<>();
	public static final List<String> STRIKE_IDS = java.util.List.of(
			"meteor",
			"dragon_fist",
			"deadly_dance_vegetto",
			"deadly_dance",
			"kaioken_attack",
			"wolf_fang",
			"oozaru_fist",
			"super_god_fist"
	);

	public void init() {
		registerKi("spiritbomb", "technique.dragonminez.spiritbomb", "Goku", KiAttackData.KiType.GIANT_BALL, 3.00F, 0x30FFF1, 0x00F8FF, 5.0F, 0.5F, 10, "ki.large_ball");
		registerKi("supernova", "technique.dragonminez.supernova", "Frieza", KiAttackData.KiType.GIANT_BALL, 3.00F, 0xFF9900, 0xFF4400, 5.0F, 0.5F, 10, "ki.large_ball");
		registerKi("supernova_cooler", "technique.dragonminez.supernova_cooler", "Cooler", KiAttackData.KiType.GIANT_BALL, 3.50F, 0xFFAA00, 0xFFAA00, 5.5F, 0.5F, 10, "ki.large_ball");
		registerKi("big_bang", "technique.dragonminez.big_bang", "Vegeta", KiAttackData.KiType.MEDIUM_BALL, 2.00F, 0x4FF7FF, 0x4FF7FF, 2.0F, 1.5F, 10, "ki.bigbang");
		registerKi("burning_attack", "technique.dragonminez.burning_attack", "Trunks", KiAttackData.KiType.MEDIUM_BALL, 1.50F, 0xFFAA00, 0xFFAA00, 1.5F, 1.5F, 10, "ki.masenko");
		registerKi("final_flash", "technique.dragonminez.final_flash", "Vegeta", KiAttackData.KiType.WAVE, 2.50F, 0xFF9900, 0xFF9900, 1.5F, 1.2F, 10, "ki.finalflash");
		registerKi("kamehameha", "technique.dragonminez.kamehameha", "Goku", KiAttackData.KiType.WAVE, 2.00F, 0x4FF7FF, 0x4FF7FF, 1.0F, 1.2F, 10, "ki.kameha");
		registerKi("galick_gun", "technique.dragonminez.galick_gun", "Vegeta", KiAttackData.KiType.WAVE, 2.00F, 0xCE10E3, 0xAE10E3, 1.0F, 1.2F, 10, "ki.galick");
		registerKi("masenko", "technique.dragonminez.masenko", "Gohan", KiAttackData.KiType.WAVE, 1.50F, 0xFFEA00, 0xFFEA00, 1.0F, 1.2F, 10, "ki.masenko");
		registerKi("kienzan", "technique.dragonminez.kienzan", "Krilin", KiAttackData.KiType.DISK, 1.50F, 0xFFEA00, 0xFFEA00, 1.0F, 1.5F, 10, "ki.kienzan");
		registerKi("kienzan_doble", "technique.dragonminez.double_kienzan", "Krilin", KiAttackData.KiType.DISK, 1.75F, 0xFF00AA, 0xFF00AA, 1.0F, 1.5F, 10, "ki.kienzan");
		registerKi("death_beam", "technique.dragonminez.death_beam", "Frieza", KiAttackData.KiType.LASER, 0.75F, 0xCE10E3, 0xCE10E3, 0.5F, 2.0F, 10, "ki.makkako");
		registerKi("emperor_death_beam", "technique.dragonminez.emperor_death_beam", "Frieza", KiAttackData.KiType.LASER, 1.25F, 0xCE10E3, 0xCE10E3, 0.6F, 2.0F, 10, "ki.makkako");
        registerKi("makkanko", "technique.dragonminez.makkankosanpo", "Piccolo", KiAttackData.KiType.BEAM, 0.75F, 0xFFE53B, 0xC43BFF, 0.8F, 2.0F, 10, "ki.makkako");
        registerKi("ki_barrage", "technique.dragonminez.barrage", "Vegeta", KiAttackData.KiType.BARRAGE, 1.00F, 0xFFFF00, 0xFFFF00, 0.4F, 1.5F, 10, "ki.barrage");
		registerKi("final_explosion", "technique.dragonminez.final_explosion", "Vegeta", KiAttackData.KiType.EXPLOSION, 3.00F, 0xFFFF00, 0xFFFF00, 15.0F, 0.0F, 10, "ki.explosion");
		registerStrike("skp.meteor", 1.0f, 40);
		registerStrike("skp.dragon_fist", 1.0f, 50);
		registerStrike("skp.deadly_dance_vegetto", 1.0f, 40);
		registerStrike("skp.deadly_dance", 1.0f, 40);
		registerStrike("skp.kaioken_attack", 1.0f, 45);
		registerStrike("skp.wolf_fang", 1.0f, 35);
		registerStrike("skp.oozaru_fist", 1.0f, 35);
		registerStrike("skp.super_god_fist", 1.0f, 25);
	}

	public static boolean isPredefinedTechniqueId(String techniqueId) {
		return techniqueId != null && REGISTRY.containsKey(techniqueId);
	}

	public static boolean isPredefinedTechnique(TechniqueData technique) {
		return technique != null && isPredefinedTechniqueId(technique.getId());
	}

	private void registerKi(String id, String name, String author, KiAttackData.KiType type, float dmgMult, int colorIn, int colorOut, float size, float speed, int cooldownSeconds, String animPrefix) {
		KiAttackData data = new KiAttackData();
		data.setId(id);
		data.setName(name);
		data.setAuthor(author);
		data.setKiType(type);
		data.setUtility(KiAttackData.Utility.DAMAGE);
		data.setDamageMultiplier(dmgMult);
		data.setColorInterior(colorIn);
		data.setColorExterior(colorOut);
		data.setSize(size);
		data.setSpeed(speed);
		data.setArmorPenetration(0);
		data.getAllowedRaces().add("ALL");
		data.setAnimation(animPrefix);
		data.setCastTime(5 * 20);
		// cooldown is stored in seconds (getActualCooldown() converts to ticks via KI_TIME_MULTIPLIER),
		// matching player-created techniques. Storing ticks here made every predefined attack ~20x too long.
		data.setCooldown(cooldownSeconds);
		data.calculateDerivedValues();
		REGISTRY.put(id, data);
	}

	private void registerStrike(String animationId, float damageMultiplier, int durationTicks) {
		String id = animationId != null && animationId.startsWith("skp.") ? animationId.substring(4) : animationId;
		StrikeAttackData data = new StrikeAttackData();
		data.setId(id);
		data.setName("technique.dragonminez." + id);
		data.setAuthor("System");
		data.setDamageMultiplier(damageMultiplier);
		data.setAnimationId(animationId);
		data.setDurationTicks(durationTicks);
		data.applyConfigDefaults();
		STRIKE_REGISTRY.put(id, data);
	}
}