package com.dragonminez.common.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RaidDefaults {

	public static final String FRIEZA_INVASION = "frieza_invasion";
	public static final String SAIYAN_ASSAULT = "saiyan_assault";

	private RaidDefaults() {}

	public static Map<String, RaidDefinition> create() {
		Map<String, RaidDefinition> defaults = new LinkedHashMap<>();
		defaults.put(FRIEZA_INVASION, friezaInvasion());
		defaults.put(SAIYAN_ASSAULT, saiyanAssault());
		return defaults;
	}
	private static RaidDefinition friezaInvasion() {
		RaidDefinition def = new RaidDefinition();
		def.setConfigVersion(RaidDefinition.CURRENT_VERSION);
		def.setDisplayName("raid.dragonminez.frieza_invasion");
		def.setEnabled(true);
		def.setActivationRadius(48.0D);
		def.setLeashDistance(80.0D);
		def.setInterWaveDelaySeconds(5);

		RaidDefinition.Music music = new RaidDefinition.Music();
		music.setPreparation("dragonminez:menu_music_11");
		music.setBattle("dragonminez:raid_ost_1");
		def.setMusic(music);

		RaidDefinition.Trigger trigger = new RaidDefinition.Trigger();
		trigger.setEntityId("#dragonminez:frieza_soldiers");
		trigger.setDimension("dragonminez:namek");
		trigger.setSpawnChance(0.05D);
		trigger.setSupervillain(true);
		trigger.setPreparationSeconds(60);
		trigger.setOmenMessage("raid.dragonminez.frieza_invasion.omen");
		trigger.setStartMessage("raid.dragonminez.frieza_invasion.start");
		def.setTrigger(trigger);

		String soldiers = "#dragonminez:frieza_soldiers";
		def.setWaves(List.of(
				wave(mob(soldiers, 5, 100, 15, 10, 1)),
				wave(mob(soldiers, 10, 130, 18, 12, 1)),
				wave(mob(soldiers, 15, 160, 22, 15, 1),
						boss("dragonminez:saga_cui", 900, 45, 40, 2)),
				wave(mob(soldiers, 15, 190, 26, 18, 1),
						boss("dragonminez:saga_dodoria", 1400, 60, 50, 2),
						transformingBoss("dragonminez:saga_zarbon", 1600, 65, 55, 2)),
				wave(mob(soldiers, 15, 220, 30, 20, 1),
						boss("dragonminez:saga_guldo", 1800, 55, 60, 2),
						boss("dragonminez:saga_recoome", 2400, 90, 50, 2),
						boss("dragonminez:saga_burter", 2000, 70, 55, 3),
						boss("dragonminez:saga_jeice", 2000, 70, 65, 3),
						boss("dragonminez:saga_ginyu", 3200, 110, 85, 3))));

		RaidDefinition.Rewards rewards = new RaidDefinition.Rewards();
		rewards.setTrainingPoints(15_000F);
		rewards.setEffects(List.of(
				effect("minecraft:hero_of_the_village", 600, 0),
				effect("dragonminez:world_hero", 600, 1)));
		rewards.setItems(List.of(
				item("minecraft:golden_carrot", 16),
				item("dragonminez:senzu_bean", 3),
				item("dragonminez:blaster_cannon", 1)));
		def.setRewards(rewards);

		return def;
	}

	private static RaidDefinition saiyanAssault() {
		RaidDefinition def = new RaidDefinition();
		def.setConfigVersion(RaidDefinition.CURRENT_VERSION);
		def.setDisplayName("raid.dragonminez.saiyan_assault");
		def.setEnabled(true);

		String saibaman = "dragonminez:saga_saibaman";
		def.setWaves(List.of(
				wave(mob(saibaman, 3, 60, 8, 5, 1)),
				wave(mob(saibaman, 5, 75, 10, 6, 1)),
				wave(mob(saibaman, 4, 90, 12, 8, 1),
						boss("dragonminez:saga_raditz", 1200, 50, 45, 2)),
				wave(mob(saibaman, 3, 90, 12, 8, 1),
						boss("dragonminez:saga_nappa", 2200, 80, 70, 3))));

		RaidDefinition.Rewards rewards = new RaidDefinition.Rewards();
		rewards.setItems(List.of(item("minecraft:diamond", 4)));
		def.setRewards(rewards);

		return def;
	}

	// ------------------------------------------------------------------------------------------------
	// Builders
	// ------------------------------------------------------------------------------------------------

	private static RaidDefinition.Wave wave(RaidDefinition.Spawn... spawns) {
		RaidDefinition.Wave wave = new RaidDefinition.Wave();
		wave.setSpawns(List.of(spawns));
		return wave;
	}

	private static RaidDefinition.Spawn mob(String entityId, int count, double health, double melee, double ki, int aiTier) {
		RaidDefinition.Spawn spawn = new RaidDefinition.Spawn();
		spawn.setEntityId(entityId);
		spawn.setCount(count);
		spawn.setHealth(health);
		spawn.setMeleeDamage(melee);
		spawn.setKiDamage(ki);
		spawn.setAiTier(aiTier);
		return spawn;
	}

	private static RaidDefinition.Spawn boss(String entityId, double health, double melee, double ki, int aiTier) {
		RaidDefinition.Spawn spawn = mob(entityId, 1, health, melee, ki, aiTier);
		spawn.setDormantUntilEscortDead(true);
		return spawn;
	}

	private static RaidDefinition.Spawn transformingBoss(String entityId, double health, double melee, double ki, int aiTier) {
		RaidDefinition.Spawn spawn = boss(entityId, health, melee, ki, aiTier);
		spawn.setCanTransform(true);
		return spawn;
	}

	private static RaidDefinition.EffectReward effect(String id, int seconds, int amplifier) {
		RaidDefinition.EffectReward reward = new RaidDefinition.EffectReward();
		reward.setId(id);
		reward.setSeconds(seconds);
		reward.setAmplifier(amplifier);
		return reward;
	}

	private static RaidDefinition.ItemReward item(String id, int count) {
		RaidDefinition.ItemReward reward = new RaidDefinition.ItemReward();
		reward.setId(id);
		reward.setCount(count);
		return reward;
	}
}
