package com.dragonminez.common.config;

import java.util.ArrayList;
import java.util.List;

public class RaidDefinition {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	public static final String DEFAULT_PREPARATION_MUSIC = "";
	public static final String DEFAULT_BATTLE_MUSIC = "dragonminez:raid_ost_1";
	private String configVersion;
	private Boolean enabled;
	private String displayName;
	private Double activationRadius;
	private Double leashDistance;
	private Integer interWaveDelaySeconds;
	private Music music;
	private Trigger trigger;
	private List<Wave> waves = new ArrayList<>();
	private Rewards rewards;
	private Defeat defeat;

	public boolean isEnabled() {
		return enabled == null || enabled;
	}

	public String displayNameOr(String fallbackKey) {
		return displayName != null && !displayName.isBlank() ? displayName : fallbackKey;
	}

	public double activationRadiusOr(double fallback) {
		return activationRadius != null ? Math.max(1.0, activationRadius) : fallback;
	}

	public double leashDistanceOr(double fallback) {
		return leashDistance != null ? Math.max(1.0, leashDistance) : fallback;
	}

	public int interWaveDelayTicksOr(int fallbackTicks) {
		return interWaveDelaySeconds != null ? Math.max(0, interWaveDelaySeconds) * 20 : fallbackTicks;
	}

	public String preparationMusic() {
		return music != null && music.preparation != null ? music.preparation : DEFAULT_PREPARATION_MUSIC;
	}

	public String battleMusic() {
		return music != null && music.battle != null ? music.battle : DEFAULT_BATTLE_MUSIC;
	}

	public boolean hasTrigger() {
		return trigger != null && trigger.entityId != null && !trigger.entityId.isBlank();
	}


	public static class Music {
		private String preparation;
		private String battle;

		public String getPreparation() {
			return this.preparation;
		}

		public String getBattle() {
			return this.battle;
		}

		public void setPreparation(final String preparation) {
			this.preparation = preparation;
		}

		public void setBattle(final String battle) {
			this.battle = battle;
		}

		public Music() {
		}
	}


	public static class Trigger {
		private String entityId;
		private String dimension;
		private Double spawnChance;
		private Boolean supervillain;
		private Integer preparationSeconds;
		private String omenMessage;
		private String startMessage;
		private String announceMessage;
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;
		private Integer aiTier;

		public double spawnChanceOr(double fallback) {
			return spawnChance != null ? Math.max(0.0, Math.min(1.0, spawnChance)) : fallback;
		}

		public boolean isSupervillain() {
			return supervillain == null || supervillain;
		}

		public int preparationTicksOr(int fallbackTicks) {
			return preparationSeconds != null ? Math.max(1, preparationSeconds) * 20 : fallbackTicks;
		}

		public String getEntityId() {
			return this.entityId;
		}

		public String getDimension() {
			return this.dimension;
		}

		public Double getSpawnChance() {
			return this.spawnChance;
		}

		public Boolean getSupervillain() {
			return this.supervillain;
		}

		public Integer getPreparationSeconds() {
			return this.preparationSeconds;
		}

		public String getOmenMessage() {
			return this.omenMessage;
		}

		public String getStartMessage() {
			return this.startMessage;
		}

		public String getAnnounceMessage() {
			return this.announceMessage;
		}

		public Double getHealth() {
			return this.health;
		}

		public Double getMeleeDamage() {
			return this.meleeDamage;
		}

		public Double getKiDamage() {
			return this.kiDamage;
		}

		public Integer getAiTier() {
			return this.aiTier;
		}

		public void setEntityId(final String entityId) {
			this.entityId = entityId;
		}

		public void setDimension(final String dimension) {
			this.dimension = dimension;
		}

		public void setSpawnChance(final Double spawnChance) {
			this.spawnChance = spawnChance;
		}

		public void setSupervillain(final Boolean supervillain) {
			this.supervillain = supervillain;
		}

		public void setPreparationSeconds(final Integer preparationSeconds) {
			this.preparationSeconds = preparationSeconds;
		}

		public void setOmenMessage(final String omenMessage) {
			this.omenMessage = omenMessage;
		}

		public void setStartMessage(final String startMessage) {
			this.startMessage = startMessage;
		}

		public void setAnnounceMessage(final String announceMessage) {
			this.announceMessage = announceMessage;
		}

		public void setHealth(final Double health) {
			this.health = health;
		}

		public void setMeleeDamage(final Double meleeDamage) {
			this.meleeDamage = meleeDamage;
		}

		public void setKiDamage(final Double kiDamage) {
			this.kiDamage = kiDamage;
		}

		public void setAiTier(final Integer aiTier) {
			this.aiTier = aiTier;
		}

		public Trigger() {
		}
	}


	public static class Wave {
		private List<Spawn> spawns = new ArrayList<>();

		public int totalMobCount() {
			if (spawns == null) return 0;
			int total = 0;
			for (Spawn spawn : spawns) total += spawn.countOr(1);
			return total;
		}

		public boolean isBossWave() {
			if (spawns == null) return false;
			for (Spawn spawn : spawns) if (spawn.isDormantUntilEscortDead()) return true;
			return false;
		}

		public List<Spawn> getSpawns() {
			return this.spawns;
		}

		public void setSpawns(final List<Spawn> spawns) {
			this.spawns = spawns;
		}

		public Wave() {
		}
	}


	public static class Spawn {
		private String entityId;
		private Integer count;
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;
		private Integer aiTier;
		private Integer textureVariant;
		private Boolean canTransform;
		private Boolean dormantUntilEscortDead;
		private Double transformHealth;
		private Double transformMeleeDamage;
		private Double transformKiDamage;

		public int countOr(int fallback) {
			return count != null ? Math.max(0, count) : fallback;
		}

		public boolean hasAbsoluteStats() {
			return health != null;
		}

		public double healthOr(double fallback) {
			return health != null ? Math.max(1.0, health) : fallback;
		}

		public double meleeDamageOr(double fallback) {
			return meleeDamage != null ? Math.max(0.0, meleeDamage) : fallback;
		}

		public double kiDamageOr(double fallback) {
			return kiDamage != null ? Math.max(0.0, kiDamage) : fallback;
		}

		public int aiTierOr(int fallback) {
			return aiTier != null ? aiTier : fallback;
		}

		public boolean canTransformOr(boolean fallback) {
			return canTransform != null ? canTransform : fallback;
		}

		public boolean isDormantUntilEscortDead() {
			return dormantUntilEscortDead != null && dormantUntilEscortDead;
		}

		public String getEntityId() {
			return this.entityId;
		}

		public Integer getCount() {
			return this.count;
		}

		public Double getHealth() {
			return this.health;
		}

		public Double getMeleeDamage() {
			return this.meleeDamage;
		}

		public Double getKiDamage() {
			return this.kiDamage;
		}

		public Integer getAiTier() {
			return this.aiTier;
		}

		public Integer getTextureVariant() {
			return this.textureVariant;
		}

		public Boolean getCanTransform() {
			return this.canTransform;
		}

		public Boolean getDormantUntilEscortDead() {
			return this.dormantUntilEscortDead;
		}

		public Double getTransformHealth() {
			return this.transformHealth;
		}

		public Double getTransformMeleeDamage() {
			return this.transformMeleeDamage;
		}

		public Double getTransformKiDamage() {
			return this.transformKiDamage;
		}

		public void setEntityId(final String entityId) {
			this.entityId = entityId;
		}

		public void setCount(final Integer count) {
			this.count = count;
		}

		public void setHealth(final Double health) {
			this.health = health;
		}

		public void setMeleeDamage(final Double meleeDamage) {
			this.meleeDamage = meleeDamage;
		}

		public void setKiDamage(final Double kiDamage) {
			this.kiDamage = kiDamage;
		}

		public void setAiTier(final Integer aiTier) {
			this.aiTier = aiTier;
		}

		public void setTextureVariant(final Integer textureVariant) {
			this.textureVariant = textureVariant;
		}

		public void setCanTransform(final Boolean canTransform) {
			this.canTransform = canTransform;
		}

		public void setDormantUntilEscortDead(final Boolean dormantUntilEscortDead) {
			this.dormantUntilEscortDead = dormantUntilEscortDead;
		}

		public void setTransformHealth(final Double transformHealth) {
			this.transformHealth = transformHealth;
		}

		public void setTransformMeleeDamage(final Double transformMeleeDamage) {
			this.transformMeleeDamage = transformMeleeDamage;
		}

		public void setTransformKiDamage(final Double transformKiDamage) {
			this.transformKiDamage = transformKiDamage;
		}

		public Spawn() {
		}
	}


	public static class Rewards {
		private Float trainingPoints;
		private List<EffectReward> effects = new ArrayList<>();
		private List<ItemReward> items = new ArrayList<>();

		public float trainingPointsOr(float fallback) {
			return trainingPoints != null ? Math.max(0.0F, trainingPoints) : fallback;
		}

		public Float getTrainingPoints() {
			return this.trainingPoints;
		}

		public List<EffectReward> getEffects() {
			return this.effects;
		}

		public List<ItemReward> getItems() {
			return this.items;
		}

		public void setTrainingPoints(final Float trainingPoints) {
			this.trainingPoints = trainingPoints;
		}

		public void setEffects(final List<EffectReward> effects) {
			this.effects = effects;
		}

		public void setItems(final List<ItemReward> items) {
			this.items = items;
		}

		public Rewards() {
		}
	}


	public static class Defeat {
		public static final String DEFAULT_TITLE = "raid.dragonminez.defeat.title";
		private String title;
		private String subtitle;
		private Integer alignment;

		public String titleOr(String fallback) {
			return title != null && !title.isBlank() ? title : fallback;
		}

		public int alignmentOr(int fallback) {
			return alignment != null ? alignment : fallback;
		}

		public String getTitle() {
			return this.title;
		}

		public String getSubtitle() {
			return this.subtitle;
		}

		public Integer getAlignment() {
			return this.alignment;
		}

		public void setTitle(final String title) {
			this.title = title;
		}

		public void setSubtitle(final String subtitle) {
			this.subtitle = subtitle;
		}

		public void setAlignment(final Integer alignment) {
			this.alignment = alignment;
		}

		public Defeat() {
		}
	}


	public static class EffectReward {
		private String id;
		private Integer seconds;
		private Integer amplifier;

		public int durationTicksOr(int fallbackTicks) {
			return seconds != null ? Math.max(1, seconds) * 20 : fallbackTicks;
		}

		public int amplifierOr(int fallback) {
			return amplifier != null ? Math.max(0, amplifier) : fallback;
		}

		public String getId() {
			return this.id;
		}

		public Integer getSeconds() {
			return this.seconds;
		}

		public Integer getAmplifier() {
			return this.amplifier;
		}

		public void setId(final String id) {
			this.id = id;
		}

		public void setSeconds(final Integer seconds) {
			this.seconds = seconds;
		}

		public void setAmplifier(final Integer amplifier) {
			this.amplifier = amplifier;
		}

		public EffectReward() {
		}
	}


	public static class ItemReward {
		private String id;
		private Integer count;

		public int countOr(int fallback) {
			return count != null ? Math.max(1, count) : fallback;
		}

		public String getId() {
			return this.id;
		}

		public Integer getCount() {
			return this.count;
		}

		public void setId(final String id) {
			this.id = id;
		}

		public void setCount(final Integer count) {
			this.count = count;
		}

		public ItemReward() {
		}
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public Boolean getEnabled() {
		return this.enabled;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public Double getActivationRadius() {
		return this.activationRadius;
	}

	public Double getLeashDistance() {
		return this.leashDistance;
	}

	public Integer getInterWaveDelaySeconds() {
		return this.interWaveDelaySeconds;
	}

	public Music getMusic() {
		return this.music;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public List<Wave> getWaves() {
		return this.waves;
	}

	public Rewards getRewards() {
		return this.rewards;
	}

	public Defeat getDefeat() {
		return this.defeat;
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	public void setActivationRadius(final Double activationRadius) {
		this.activationRadius = activationRadius;
	}

	public void setLeashDistance(final Double leashDistance) {
		this.leashDistance = leashDistance;
	}

	public void setInterWaveDelaySeconds(final Integer interWaveDelaySeconds) {
		this.interWaveDelaySeconds = interWaveDelaySeconds;
	}

	public void setMusic(final Music music) {
		this.music = music;
	}

	public void setTrigger(final Trigger trigger) {
		this.trigger = trigger;
	}

	public void setWaves(final List<Wave> waves) {
		this.waves = waves;
	}

	public void setRewards(final Rewards rewards) {
		this.rewards = rewards;
	}

	public void setDefeat(final Defeat defeat) {
		this.defeat = defeat;
	}

	public RaidDefinition() {
	}
}
