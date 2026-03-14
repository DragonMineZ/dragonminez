package com.dragonminez.common.stats.techniques;

import java.util.HashMap;
import java.util.Map;

public class PredefinedTechniques {

	public static final Map<String, KiAttackData> REGISTRY = new HashMap<>();

	public static void init() {
		// Giant Ball
		registerKi("spiritbomb", "technique.dragonminez.spiritbomb", KiAttackData.KiType.GIANT_BALL, 3.00F, 0x30FFF1, 0x00F8FF, 5.0F, 0.5F);
		registerKi("supernova", "technique.dragonminez.supernova", KiAttackData.KiType.GIANT_BALL, 3.00F, 0xFF9900, 0xFF4400, 5.0F, 0.5F);
		registerKi("supernova_cooler", "technique.dragonminez.supernova_cooler", KiAttackData.KiType.GIANT_BALL, 3.50F, 0xFFAA00, 0xFFAA00, 5.5F, 0.5F);

		// Medium Ball
		registerKi("big_bang", "technique.dragonminez.big_bang", KiAttackData.KiType.MEDIUM_BALL, 2.00F, 0x4FF7FF, 0x4FF7FF, 2.0F, 1.5F);
		registerKi("burning_attack", "technique.dragonminez.burning_attack", KiAttackData.KiType.MEDIUM_BALL, 1.50F, 0xFFAA00, 0xFFAA00, 1.5F, 1.5F);

		// Waves
		registerKi("final_flash", "technique.dragonminez.final_flash", KiAttackData.KiType.WAVE, 2.50F, 0xFF9900, 0xFF9900, 1.5F, 1.2F);
		registerKi("kamehameha", "technique.dragonminez.kamehameha", KiAttackData.KiType.WAVE, 2.00F, 0x4FF7FF, 0x4FF7FF, 1.0F, 1.2F);
		registerKi("galick_gun", "technique.dragonminez.galick_gun", KiAttackData.KiType.WAVE, 2.00F, 0xCE10E3, 0xAE10E3, 1.0F, 1.2F);
		registerKi("masenko", "technique.dragonminez.masenko", KiAttackData.KiType.WAVE, 1.50F, 0xFFEA00, 0xFFEA00, 1.0F, 1.2F);

		// Disks
		registerKi("kienzan", "technique.dragonminez.kienzan", KiAttackData.KiType.DISK, 1.50F, 0xFFEA00, 0xFFEA00, 1.0F, 1.5F);
		registerKi("kienzan_doble", "technique.dragonminez.double_kienzan", KiAttackData.KiType.DISK, 1.75F, 0xFF00AA, 0xFF00AA, 1.0F, 1.5F);

		// Lasers
		registerKi("death_beam", "technique.dragonminez.death_beam", KiAttackData.KiType.LASER, 0.75F, 0xCE10E3, 0xCE10E3, 0.5F, 2.0F);
		registerKi("emperor_death_beam", "technique.dragonminez.emperor_death_beam", KiAttackData.KiType.LASER, 1.25F, 0xCE10E3, 0xCE10E3, 0.6F, 2.0F);

		// Barrage
		registerKi("ki_barrage", "technique.dragonminez.barrage", KiAttackData.KiType.BARRAGE, 1.00F, 0xFFFF00, 0xFFFF00, 0.4F, 1.5F);

		// Explosion
		registerKi("final_explosion", "technique.dragonminez.final_explosion", KiAttackData.KiType.EXPLOSION, 3.00F, 0xFFFF00, 0xFFFF00, 15.0F, 0.0F);
	}

	private static void registerKi(String id, String name, KiAttackData.KiType type, float dmgMult, int colorIn, int colorOut, float size, float speed) {
		KiAttackData data = new KiAttackData();
		data.setId(id);
		data.setName(name);
		data.setAuthor("System");
		data.setKiType(type);
		data.setUtility(KiAttackData.Utility.DAMAGE);
		data.setDamageMultiplier(dmgMult);
		data.setColorInterior(colorIn);
		data.setColorExterior(colorOut);
		data.setSize(size);
		data.setSpeed(speed);
		data.setArmorPenetration(0);
		data.getAllowedRaces().add("ALL");
		data.calculateAndSetBaseCost();

		REGISTRY.put(id, data);
	}
}