package com.dragonminez.common.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RaidDefaults {

	public static final String FRIEZA_INVASION = "frieza_invasion";
	public static final String SAIYAN_ASSAULT = "saiyan_assault";
	public static final String GOMAH_ASSAULT = "gomah_assault";

	private RaidDefaults() {}

	public static Map<String, RaidDefinition> create() {
		Map<String, RaidDefinition> defaults = new LinkedHashMap<>();
		defaults.put(FRIEZA_INVASION, friezaInvasion());
		defaults.put(SAIYAN_ASSAULT, saiyanAssault());
		defaults.put(GOMAH_ASSAULT, gomahAssault());
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
		trigger.setSpawnChance(0.0D);
		trigger.setSupervillain(true);
		trigger.setPreparationSeconds(60);
		trigger.setAnnounceMessage("raid.dragonminez.frieza_invasion.ruins");
		trigger.setHealth(400.0D);
		trigger.setMeleeDamage(18.0D);
		trigger.setKiDamage(12.0D);
		trigger.setAiTier(1);
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
						boss("dragonminez:saga_dodoria", 7000, 60, 50, 2),
						transformingBoss("dragonminez:saga_zarbon", 6800, 65, 55, 2, 10200)),
				wave(mob(soldiers, 15, 220, 30, 20, 1),
						boss("dragonminez:saga_guldo", 6000, 55, 60, 2),
						boss("dragonminez:saga_recoome", 10000, 90, 50, 2),
						boss("dragonminez:saga_burter", 10000, 70, 55, 3),
						boss("dragonminez:saga_jeice", 10000, 70, 65, 3),
						boss("dragonminez:saga_ginyu", 14000, 110, 85, 3))));

		RaidDefinition.Rewards rewards = new RaidDefinition.Rewards();
		rewards.setTrainingPoints(30_000F);
		rewards.setEffects(List.of(
				effect("minecraft:hero_of_the_village", 600, 0),
				effect("dragonminez:world_hero", 600, 1)));
		rewards.setItems(List.of(
				item("minecraft:golden_carrot", 16),
				item("dragonminez:senzu_bean", 3),
				item("dragonminez:blaster_cannon", 1)));
		def.setRewards(rewards);
		def.setDefeat(defeat("raid.dragonminez.frieza_invasion.defeat", -20));

		return def;
	}

	private static RaidDefinition saiyanAssault() {
		RaidDefinition def = new RaidDefinition();
		def.setConfigVersion(RaidDefinition.CURRENT_VERSION);
		def.setDisplayName("raid.dragonminez.saiyan_assault");
		def.setEnabled(true);
		def.setActivationRadius(48.0D);
		def.setLeashDistance(80.0D);
		def.setInterWaveDelaySeconds(5);

		RaidDefinition.Music music = new RaidDefinition.Music();
		music.setPreparation("dragonminez:menu_music_11");
		music.setBattle("dragonminez:raid_ost_1");
		def.setMusic(music);

		String soldier = "dragonminez:saga_saiyan_soldier";
		String elite = "dragonminez:saga_saiyan_elite";

		RaidDefinition.Trigger trigger = new RaidDefinition.Trigger();
		trigger.setEntityId(soldier);
		trigger.setDimension("minecraft:overworld");
		trigger.setSpawnChance(0.0D);
		trigger.setSupervillain(true);
		trigger.setPreparationSeconds(60);
		trigger.setAnnounceMessage("raid.dragonminez.saiyan_assault.crater");
		trigger.setOmenMessage("raid.dragonminez.saiyan_assault.omen");
		trigger.setStartMessage("raid.dragonminez.saiyan_assault.start");
		trigger.setHealth(500.0D);
		trigger.setMeleeDamage(12.0D);
		trigger.setKiDamage(10.0D);
		trigger.setAiTier(1);
		def.setTrigger(trigger);

		def.setWaves(List.of(
				wave(mob(soldier, 4, 500, 12, 10, 1)),
				wave(mob(soldier, 6, 550, 14, 11, 1)),
				wave(mob(soldier, 6, 600, 15, 12, 1),
						noTransform(mob(elite, 2, 700, 17, 14, 2))),
				wave(mob(soldier, 6, 650, 16, 13, 1),
						noTransform(mob(elite, 4, 750, 18, 15, 2))),
				wave(mob(soldier, 6, 700, 17, 14, 1),
						moonTransformer(elite, 3, 800, 20, 16, 2, 1500, 55, 45))));

		RaidDefinition.Rewards rewards = new RaidDefinition.Rewards();
		rewards.setTrainingPoints(10_000F);
		rewards.setEffects(List.of(
				effect("minecraft:hero_of_the_village", 600, 0),
				effect("dragonminez:world_hero", 600, 0)));
		rewards.setItems(List.of(
				item("minecraft:golden_carrot", 8),
				item("dragonminez:senzu_bean", 2),
				item("dragonminez:green_scouter", 1)));
		def.setRewards(rewards);
		def.setDefeat(defeat("raid.dragonminez.saiyan_assault.defeat", -10));

		return def;
	}

	private static RaidDefinition gomahAssault() {
		RaidDefinition def = new RaidDefinition();
		def.setConfigVersion(RaidDefinition.CURRENT_VERSION);
		def.setDisplayName("raid.dragonminez.gomah_assault");
		def.setEnabled(true);
		def.setActivationRadius(48.0D);
		def.setLeashDistance(80.0D);
		def.setInterWaveDelaySeconds(5);

		RaidDefinition.Music music = new RaidDefinition.Music();
		music.setPreparation("dragonminez:menu_music_11");
		music.setBattle("dragonminez:raid_ost_1");
		def.setMusic(music);

		String soldiers = "#dragonminez:gomah_soldiers";

		RaidDefinition.Trigger trigger = new RaidDefinition.Trigger();
		trigger.setEntityId(soldiers);
		trigger.setDimension("dragonminez:demon_realm");
		trigger.setSpawnChance(0.0D);
		trigger.setSupervillain(true);
		trigger.setPreparationSeconds(60);
		trigger.setAnnounceMessage("raid.dragonminez.gomah_assault.camp");
		trigger.setOmenMessage("raid.dragonminez.gomah_assault.omen");
		trigger.setStartMessage("raid.dragonminez.gomah_assault.start");
		trigger.setHealth(10000.0D);
		trigger.setMeleeDamage(900.0D);
		trigger.setKiDamage(800.0D);
		trigger.setAiTier(2);
		def.setTrigger(trigger);

		def.setWaves(List.of(
				wave(mob(soldiers, 6, 8000, 750, 650, 1)),
				wave(mob(soldiers, 8, 10000, 900, 800, 1)),
				wave(mob(soldiers, 10, 12000, 1100, 950, 2)),
				wave(mob(soldiers, 10, 14000, 1300, 1100, 2),
						boss("dragonminez:saga_glorio", 450000, 19000, 17000, 3)),
				wave(mob(soldiers, 10, 16000, 1500, 1300, 2),
						boss("dragonminez:saga_majin_kuu", 750000, 31000, 28000, 3),
						boss("dragonminez:saga_majin_duu", 800000, 33000, 30000, 3))));

		RaidDefinition.Rewards rewards = new RaidDefinition.Rewards();
		rewards.setTrainingPoints(350_000F);
		rewards.setEffects(List.of(
				effect("minecraft:hero_of_the_village", 600, 0),
				effect("dragonminez:world_hero", 600, 2)));
		rewards.setItems(List.of(
				item("minecraft:golden_carrot", 16),
				item("dragonminez:medi_bug_ant", 4)));
		def.setRewards(rewards);
		def.setDefeat(defeat("raid.dragonminez.gomah_assault.defeat", -30));

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

	private static RaidDefinition.Spawn transformingBoss(String entityId, double health, double melee, double ki, int aiTier,
														double transformedHealth) {
		RaidDefinition.Spawn spawn = transformingBoss(entityId, health, melee, ki, aiTier);
		spawn.setTransformHealth(transformedHealth);
		return spawn;
	}

	private static RaidDefinition.Spawn noTransform(RaidDefinition.Spawn spawn) {
		spawn.setCanTransform(false);
		return spawn;
	}

	private static RaidDefinition.Spawn moonTransformer(String entityId, int count, double health, double melee, double ki, int aiTier,
														double oozaruHealth, double oozaruMelee, double oozaruKi) {
		RaidDefinition.Spawn spawn = mob(entityId, count, health, melee, ki, aiTier);
		spawn.setDormantUntilEscortDead(true);
		spawn.setCanTransform(true);
		spawn.setTransformHealth(oozaruHealth);
		spawn.setTransformMeleeDamage(oozaruMelee);
		spawn.setTransformKiDamage(oozaruKi);
		return spawn;
	}

	private static RaidDefinition.Defeat defeat(String subtitle, int alignment) {
		RaidDefinition.Defeat defeat = new RaidDefinition.Defeat();
		defeat.setTitle(RaidDefinition.Defeat.DEFAULT_TITLE);
		defeat.setSubtitle(subtitle);
		defeat.setAlignment(alignment);
		return defeat;
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
