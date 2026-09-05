package com.dragonminez.common.stats.techniques;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredefinedTechniques {

	public static final Map<String, KiAttackData> REGISTRY = new HashMap<>();
	public static final Map<String, StrikeAttackData> STRIKE_REGISTRY = new HashMap<>();
	public static final Map<String, EvasionAttackData> EVASION_REGISTRY = new HashMap<>();
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
	public static final List<String> EVASION_IDS = java.util.List.of(
			"taiyoken",
			"rage_scream"
	);

	public void init() {
		registerKi("spiritbomb", "technique.dragonminez.spiritbomb", "Goku", KiAttackData.KiType.GIANT_BALL, 3.00F, 0xC4FFFD, 0x00F8FF, 0xFFFFFF, 10.0F, 0.5F, 10, "ki.large_ball");
		registerKi("supernova", "technique.dragonminez.supernova", "Frieza", KiAttackData.KiType.GIANT_BALL, 3.00F, 0xFF7438, 0xC92620, 0x800E0E, 10.0F, 0.5F, 10, "ki.large_ball");
		registerKi("supernova_cooler", "technique.dragonminez.supernova_cooler", "Cooler", KiAttackData.KiType.GIANT_BALL, 3.50F, 0xFF3866, 0xA3143A, 0x4A0316, 10.5F, 0.5F, 10, "ki.large_ball");
		registerKi("big_bang", "technique.dragonminez.big_bang", "Vegeta", KiAttackData.KiType.MEDIUM_BALL, 2.00F, 0x4FF7FF, 0x4FF7FF, 0x0077FF, 2.0F, 1.5F, 10, "ki.bigbang");
		registerKi("burning_attack", "technique.dragonminez.burning_attack", "Trunks", KiAttackData.KiType.MEDIUM_BALL, 1.50F, 0xF0FFFF, 0x00A6FF, 0x0055C8, 1.5F, 1.5F, 10, "ki.masenko");
		registerKi("sokidan", "technique.dragonminez.sokidan", "Yamcha", KiAttackData.KiType.MEDIUM_BALL, 1.25F, 0xFCFC5D, 0xF7F723, 0xF7B736, 1.5F, 1.0F, 12, "ki.bigbang");
		registerKi("final_flash", "technique.dragonminez.final_flash", "Vegeta", KiAttackData.KiType.WAVE, 2.50F, 0xFFFCD6, 0xFFEB52, 0xF5C020, 0.7F, 1.2F, 10, "ki.finalflash");
		registerKi("kamehameha", "technique.dragonminez.kamehameha", "Goku", KiAttackData.KiType.WAVE, 2.00F, 0xEDF4FF, 0x29D8FF, 0x0077FF, 0.6F, 1.2F, 10, "ki.kameha");
		registerKi("galick_gun", "technique.dragonminez.galick_gun", "Vegeta", KiAttackData.KiType.WAVE, 2.00F, 0xFAE5FF, 0xA63EF0, 0x7106BD, 0.6F, 1.2F, 10, "ki.galick");
		registerKi("masenko", "technique.dragonminez.masenko", "Gohan", KiAttackData.KiType.WAVE, 1.50F, 0xFFFC85, 0xFCE062, 0xFFFFFF, 0.4F, 1.2F, 10, "ki.masenko");
		registerKi("kienzan", "technique.dragonminez.kienzan", "Krilin", KiAttackData.KiType.DISK, 1.50F, 0xFFFB7D, 0xFFEA00, 0xFFFFFF, 1.0F, 1.5F, 10, "ki.kienzan");
		registerKi("kienzan_doble", "technique.dragonminez.double_kienzan", "Krilin", KiAttackData.KiType.DISK, 1.75F, 0xFF00AA, 0xFF00AA, 0xA30070, 1.0F, 1.5F, 10, "ki.kienzandoble");
		registerKi("death_beam", "technique.dragonminez.death_beam", "Frieza", KiAttackData.KiType.LASER, 0.75F, 0xFF59FF, 0xD859FF, 0x9238F2, 0.5F, 2.0F, 10, "ki.makkako");
		registerKi("emperor_death_beam", "technique.dragonminez.emperor_death_beam", "Frieza", KiAttackData.KiType.LASER, 1.25F, 0xCE10E3, 0xCE10E3, 0x9238F2, 0.6F, 2.0F, 10, "ki.makkako");
		registerKi("makkanko", "technique.dragonminez.makkankosanpo", "Piccolo", KiAttackData.KiType.BEAM, 0.75F, 0xFFE657, 0xF5A627, 0x8B17CF, 1.0F, 2.0F, 20, "ki.makkako");
		registerKi("ki_barrage", "technique.dragonminez.barrage", "Vegeta", KiAttackData.KiType.BARRAGE, 1.00F, 0xFFFF00, 0xFFFF00, 0xC8A000, 0.4F, 1.5F, 10, "ki.barrage");
		registerKi("final_explosion", "technique.dragonminez.final_explosion", "Vegeta", KiAttackData.KiType.EXPLOSION, 2.25F, 0xFFFA99, 0xFCF56A, 0xFFFFFC, 15.0F, 0.0F, 10, "ki.explosion");
		registerKi("soul_punisher", "technique.dragonminez.soul_punisher", "Gogeta", KiAttackData.KiType.MEDIUM_BALL, 3.50F, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 5.0F, 0.5F, 45, "ki.kienzan");
		registerKi("fake_moon", "technique.dragonminez.fake_moon", "Vegeta", KiAttackData.KiType.MEDIUM_BALL, 0.00F, 0xF5F3D0, 0xFFFFFF, 0xFFFFFF, 2.0F, 0.8F, 45, "ki.bigbang");
		registerEvasion("taiyoken", 0.00F, 30, 900, false);
		registerEvasion("rage_scream", 1.2F, 60, 80, true);
		registerStrike("skp.meteor", 1.25f, 40);
		registerStrike("skp.dragon_fist", 2.5f, 50);
		registerStrike("skp.deadly_dance_vegetto", 1.5f, 40);
		registerStrike("skp.deadly_dance", 1.25f, 40);
		registerStrike("skp.kaioken_attack", 1.75f, 45);
		registerStrike("skp.wolf_fang", 1.25f, 35);
		registerStrike("skp.oozaru_fist", 2.25f, 35);
		registerStrike("skp.super_god_fist", 2.0f, 25);
	}

	public static boolean isPredefinedTechniqueId(String techniqueId) {
		return techniqueId != null && REGISTRY.containsKey(techniqueId);
	}

	public static boolean isPredefinedTechnique(TechniqueData technique) {
		return technique != null && isPredefinedTechniqueId(technique.getId());
	}

	/**
	 * @param colorIn     core colour, the hot middle of the ball
	 * @param colorBorder the body colour banded around the core
	 * @param colorOut    the outline colour at the rim
	 */
	private void registerKi(String id, String name, String author, KiAttackData.KiType type, float dmgMult, int colorIn, int colorBorder, int colorOut, float size, float speed, int cooldownSeconds, String animPrefix) {
		KiAttackData data = new KiAttackData();
		data.setId(id);
		data.setName(name);
		data.setAuthor(author);
		data.setKiType(type);
		data.setUtility(KiAttackData.Utility.DAMAGE);
		data.setDamageMultiplier(dmgMult);
		data.setColorInterior(colorIn);
		data.setColorExterior(colorBorder);
		data.setColorOutline(colorOut);
		data.setSize(size);
		data.setSpeed(speed);
		data.setArmorPenetration(0);
		data.getAllowedRaces().add("ALL");
		data.setAnimation(animPrefix);
		data.setCastTime(5 * 20);
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

	private void registerEvasion(String id, float damageMultiplier, int durationTicks, int cooldownTicks, boolean useSKP) {
		EvasionAttackData data = new EvasionAttackData();
		data.setId(id);
		data.setName("technique.dragonminez." + id);
		data.setAuthor("System");
		data.setDamageMultiplier(damageMultiplier);
		data.setDurationTicks(durationTicks);
		data.setCooldown(cooldownTicks);
		data.setAnimationId("evs." + id);
		data.setUseStrikePower(useSKP);
		data.setCastTime(0);
		EVASION_REGISTRY.put(id, data);
	}
}