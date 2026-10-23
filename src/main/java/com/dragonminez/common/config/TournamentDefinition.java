package com.dragonminez.common.config;

import com.dragonminez.common.diagnostics.RemovedConfigKeys;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TournamentDefinition {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	public static final Map<String, String> HOST_NPCS = Map.of("baba_earth", "baba", "cell", "cell", "otherworld_announcer", "otherworld", "world_tournament_announcer", "budokai");
	public static final int CONTENDER_COUNT = 1;


	public enum Format {
		BRACKET, GAUNTLET;
	}

	private String configVersion;
	private Boolean enabled;
	private String displayName;
	private Format format;
	private Integer difficultyStars;
	private Integer qualifierSlots;
	private Integer reentryCooldownSeconds;
	private Integer victoryCooldownSeconds;
	private Integer matchTimeoutSeconds;
	private Integer nextRoundSeconds;
	private Integer rulesAcceptSeconds;
	private Integer returnSeconds;
	private Integer arrivalSeconds;
	private Boolean lethal;
	private RingOffset ring;
	private Fighter champion;
	private Fighter semifinalist;
	private Fighter finalist;
	private List<Fighter> contenders = new ArrayList<>();
	private Rewards rewards;

	public boolean isEnabled() {
		return enabled == null || enabled;
	}

	public Format formatOr(Format fallback) {
		return format != null ? format : fallback;
	}

	public String displayNameOr(String fallbackKey) {
		return displayName != null && !displayName.isBlank() ? displayName : fallbackKey;
	}

	public int difficultyStarsOr(int fallback) {
		return difficultyStars != null ? Math.max(1, Math.min(5, difficultyStars)) : fallback;
	}

	public int qualifierSlotsOr(int fallback) {
		int value = qualifierSlots != null ? qualifierSlots : fallback;
		value = Math.max(2, Math.min(16, value));
		return Integer.highestOneBit(value);
	}

	public int reentryCooldownSecondsOr(int fallback) {
		return reentryCooldownSeconds != null ? Math.max(0, reentryCooldownSeconds) : fallback;
	}

	public int victoryCooldownSecondsOr(int fallback) {
		if (victoryCooldownSeconds != null) return Math.max(0, victoryCooldownSeconds);
		return reentryCooldownSecondsOr(fallback);
	}

	public int matchTimeoutSecondsOr(int fallback) {
		return matchTimeoutSeconds != null ? Math.max(30, matchTimeoutSeconds) : fallback;
	}

	public int nextRoundSecondsOr(int fallback) {
		return nextRoundSeconds != null ? Math.max(5, nextRoundSeconds) : fallback;
	}

	public int rulesAcceptSecondsOr(int fallback) {
		return rulesAcceptSeconds != null ? Math.max(10, rulesAcceptSeconds) : fallback;
	}

	public int returnSecondsOr(int fallback) {
		return returnSeconds != null ? Math.max(3, returnSeconds) : fallback;
	}

	public int arrivalSecondsOr(int fallback) {
		return arrivalSeconds != null ? Math.max(5, arrivalSeconds) : fallback;
	}

	public boolean isLethal() {
		return lethal != null && lethal;
	}

	public boolean hasFinalist() {
		return finalist != null && finalist.isUsable();
	}

	public boolean isUsable() {
		return champion != null && champion.isUsable() && semifinalist != null && semifinalist.isUsable() && contenders != null && contenders.size() >= CONTENDER_COUNT && contenders.stream().allMatch(Fighter::isUsable);
	}


	public static class RingOffset {
		private Integer x;
		private Integer y;
		private Integer z;
		private Integer radius;

		public int x() {
			return x != null ? x : 0;
		}

		public int y() {
			return y != null ? y : 0;
		}

		public int z() {
			return z != null ? z : 0;
		}

		public int radius() {
			return radius != null ? Math.max(1, radius) : 4;
		}

		public boolean isExplicit() {
			return x != null || y != null || z != null;
		}

		public Integer getX() {
			return this.x;
		}

		public Integer getY() {
			return this.y;
		}

		public Integer getZ() {
			return this.z;
		}

		public Integer getRadius() {
			return this.radius;
		}

		public void setX(final Integer x) {
			this.x = x;
		}

		public void setY(final Integer y) {
			this.y = y;
		}

		public void setZ(final Integer z) {
			this.z = z;
		}

		public void setRadius(final Integer radius) {
			this.radius = radius;
		}

		public RingOffset() {
		}
	}


	@RemovedConfigKeys({"displayName"})
	public static class Fighter {
		private String entityId;
		private Double health;
		private Double meleeDamage;
		private Double kiDamage;
		private Integer aiTier;
		private Double perRoundScaling;
		private Boolean halo;

		public boolean isHalo() {
			return halo != null && halo;
		}

		public boolean isUsable() {
			return entityId != null && !entityId.isBlank();
		}

		public boolean isTag() {
			return entityId != null && entityId.startsWith("#");
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
			return aiTier != null ? Math.max(0, aiTier) : fallback;
		}

		public double perRoundScalingOr(double fallback) {
			return perRoundScaling != null ? Math.max(1.0, perRoundScaling) : fallback;
		}

		public String getEntityId() {
			return this.entityId;
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

		public Double getPerRoundScaling() {
			return this.perRoundScaling;
		}

		public Boolean getHalo() {
			return this.halo;
		}

		public void setEntityId(final String entityId) {
			this.entityId = entityId;
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

		public void setPerRoundScaling(final Double perRoundScaling) {
			this.perRoundScaling = perRoundScaling;
		}

		public void setHalo(final Boolean halo) {
			this.halo = halo;
		}

		public Fighter() {
		}
	}


	public static class Rewards {
		private Integer trainingPoints;
		private Integer alignment;
		private List<String> commands = new ArrayList<>();

		public int trainingPointsOr(int fallback) {
			return trainingPoints != null ? Math.max(0, trainingPoints) : fallback;
		}

		public int alignmentOr(int fallback) {
			return alignment != null ? alignment : fallback;
		}

		public Integer getTrainingPoints() {
			return this.trainingPoints;
		}

		public Integer getAlignment() {
			return this.alignment;
		}

		public List<String> getCommands() {
			return this.commands;
		}

		public void setTrainingPoints(final Integer trainingPoints) {
			this.trainingPoints = trainingPoints;
		}

		public void setAlignment(final Integer alignment) {
			this.alignment = alignment;
		}

		public void setCommands(final List<String> commands) {
			this.commands = commands;
		}

		public Rewards() {
		}
	}


	public static final class Defaults {
		public static final String BABA = "baba";
		public static final String CELL = "cell";
		public static final String CELL_POOL = "#dragonminez:z_fighters_cell";
		public static final String OTHERWORLD = "otherworld";
		public static final String BUU_POOL = "#dragonminez:npc_buusaga";
		public static final String BUDOKAI = "budokai";
		public static final String BUDOKAI_POOL = "#dragonminez:npc_budokai";

		private Defaults() {
		}

		public static Map<String, TournamentDefinition> create() {
			Map<String, TournamentDefinition> defaults = new LinkedHashMap<>();
			defaults.put(BABA, baba());
			defaults.put(CELL, cell());
			defaults.put(OTHERWORLD, otherworld());
			defaults.put(BUDOKAI, budokai());
			return defaults;
		}

		private static TournamentDefinition budokai() {
			TournamentDefinition def = new TournamentDefinition();
			def.setConfigVersion(CURRENT_VERSION);
			def.setDisplayName("tournament.dragonminez.budokai");
			def.setEnabled(true);
			def.setDifficultyStars(4);
			def.setFormat(TournamentDefinition.Format.BRACKET);
			def.setQualifierSlots(8);
			def.setLethal(false);
			def.setReentryCooldownSeconds(75 * 60);
			def.setMatchTimeoutSeconds(420);
			def.setNextRoundSeconds(30);
			def.setRulesAcceptSeconds(60);
			def.setReturnSeconds(15);
			def.setArrivalSeconds(60);
			TournamentDefinition.RingOffset ring = new TournamentDefinition.RingOffset();
			ring.setX(86);
			ring.setY(27);
			ring.setZ(258);
			ring.setRadius(8);
			def.setRing(ring);
			def.setSemifinalist(fighter("dragonminez:saga_gohan_end_base", 462000.0, 19200.0, 17400.0, 3, 1.0));
			def.setFinalist(fighter("dragonminez:saga_vegeta_majin", 640000.0, 26700.0, 24300.0, 3, 1.0));
			def.setChampion(fighter("dragonminez:saga_mr_satan", 96000.0, 4200.0, 1500.0, 2, 1.0));
			def.setContenders(List.of(fighter(BUDOKAI_POOL, 147000.0, 6100.0, 5500.0, 2, 1.08), fighter(BUDOKAI_POOL, 168000.0, 7000.0, 6300.0, 2, 1.08), fighter(BUDOKAI_POOL, 189000.0, 7900.0, 7100.0, 2, 1.08), fighter(BUDOKAI_POOL, 213500.0, 8900.0, 8000.0, 2, 1.08), fighter(BUDOKAI_POOL, 245000.0, 10200.0, 9200.0, 3, 1.08), fighter(BUDOKAI_POOL, 280000.0, 11700.0, 10500.0, 3, 1.08), fighter(BUDOKAI_POOL, 315000.0, 13100.0, 11800.0, 3, 1.08), fighter(BUDOKAI_POOL, 350000.0, 14600.0, 13100.0, 3, 1.08), fighter(BUDOKAI_POOL, 392000.0, 16300.0, 14700.0, 3, 1.08)));
			TournamentDefinition.Rewards rewards = new TournamentDefinition.Rewards();
			rewards.setTrainingPoints(1800000);
			rewards.setAlignment(5);
			def.setRewards(rewards);
			return def;
		}

		private static TournamentDefinition cell() {
			TournamentDefinition def = new TournamentDefinition();
			def.setConfigVersion(CURRENT_VERSION);
			def.setDisplayName("tournament.dragonminez.cell");
			def.setEnabled(true);
			def.setDifficultyStars(4);
			def.setFormat(TournamentDefinition.Format.BRACKET);
			def.setQualifierSlots(8);
			def.setLethal(true);
			def.setReentryCooldownSeconds(60 * 60);
			def.setMatchTimeoutSeconds(420);
			def.setNextRoundSeconds(30);
			def.setRulesAcceptSeconds(60);
			def.setReturnSeconds(15);
			def.setArrivalSeconds(60);
			TournamentDefinition.RingOffset ring = new TournamentDefinition.RingOffset();
			ring.setX(37);
			ring.setY(6);
			ring.setZ(37);
			ring.setRadius(6);
			def.setRing(ring);
			def.setChampion(fighter("dragonminez:saga_cell_superperfect", 287000.0, 11700.0, 10540.0, 3, 1.0));
			def.setSemifinalist(fighter("dragonminez:saga_gohan_mid_ssj", 245000.0, 10200.0, 9180.0, 3, 1.0));
			def.setContenders(List.of(fighter(CELL_POOL, 66500.0, 2700.0, 2420.0, 1, 1.08), fighter(CELL_POOL, 70000.0, 2850.0, 2550.0, 1, 1.08), fighter(CELL_POOL, 84000.0, 3400.0, 3060.0, 2, 1.08), fighter(CELL_POOL, 150500.0, 5940.0, 6545.0, 2, 1.08), fighter(CELL_POOL, 168000.0, 6600.0, 5950.0, 2, 1.08), fighter(CELL_POOL, 189000.0, 7500.0, 6800.0, 3, 1.08), fighter(CELL_POOL, 196000.0, 8100.0, 7310.0, 3, 1.08), fighter(CELL_POOL, 203000.0, 8400.0, 7650.0, 3, 1.08), fighter(CELL_POOL, 210000.0, 8700.0, 7820.0, 3, 1.08)));
			TournamentDefinition.Rewards rewards = new TournamentDefinition.Rewards();
			rewards.setTrainingPoints(1000000);
			rewards.setAlignment(10);
			def.setRewards(rewards);
			return def;
		}

		private static TournamentDefinition otherworld() {
			TournamentDefinition def = new TournamentDefinition();
			def.setConfigVersion(CURRENT_VERSION);
			def.setDisplayName("tournament.dragonminez.otherworld");
			def.setEnabled(true);
			def.setDifficultyStars(5);
			def.setFormat(TournamentDefinition.Format.BRACKET);
			def.setQualifierSlots(8);
			def.setLethal(false);
			def.setReentryCooldownSeconds(90 * 60);
			def.setMatchTimeoutSeconds(480);
			def.setNextRoundSeconds(30);
			def.setRulesAcceptSeconds(60);
			def.setReturnSeconds(15);
			def.setArrivalSeconds(60);
			TournamentDefinition.RingOffset ring = new TournamentDefinition.RingOffset();
			ring.setX(110);
			ring.setY(98);
			ring.setZ(110);
			ring.setRadius(8);
			def.setRing(ring);
			def.setSemifinalist(fighter("dragonminez:saga_paikuhan", 805000.0, 33600.0, 30600.0, 3, 1.0));
			TournamentDefinition.Fighter goku = fighter("dragonminez:saga_goku_end_ssj", 900000.0, 37800.0, 34200.0, 3, 1.0);
			goku.setHalo(true);
			def.setFinalist(goku);
			def.setChampion(fighter("dragonminez:saga_gogeta_ssj", 1330000.0, 55800.0, 50700.0, 4, 1.0));
			def.setContenders(List.of(fighter(BUU_POOL, 238000.0, 9900.0, 8840.0, 2, 1.08), fighter(BUU_POOL, 273000.0, 11400.0, 10370.0, 2, 1.08), fighter(BUU_POOL, 308000.0, 12900.0, 11730.0, 2, 1.08), fighter(BUU_POOL, 343000.0, 14400.0, 12920.0, 2, 1.08), fighter(BUU_POOL, 402500.0, 16800.0, 15300.0, 3, 1.08), fighter(BUU_POOL, 472500.0, 19800.0, 17850.0, 3, 1.08), fighter(BUU_POOL, 507500.0, 21300.0, 19380.0, 3, 1.08), fighter(BUU_POOL, 542500.0, 22800.0, 20740.0, 3, 1.08), fighter(BUU_POOL, 612500.0, 25800.0, 23460.0, 3, 1.08)));
			TournamentDefinition.Rewards rewards = new TournamentDefinition.Rewards();
			rewards.setTrainingPoints(3000000);
			rewards.setAlignment(10);
			def.setRewards(rewards);
			return def;
		}

		private static TournamentDefinition baba() {
			TournamentDefinition def = new TournamentDefinition();
			def.setConfigVersion(CURRENT_VERSION);
			def.setDisplayName("tournament.dragonminez.baba");
			def.setEnabled(true);
			def.setDifficultyStars(1);
			def.setFormat(TournamentDefinition.Format.GAUNTLET);
			def.setReentryCooldownSeconds(20 * 60);
			def.setMatchTimeoutSeconds(300);
			def.setNextRoundSeconds(30);
			def.setRulesAcceptSeconds(60);
			def.setReturnSeconds(15);
			def.setArrivalSeconds(60);
			TournamentDefinition.RingOffset ring = new TournamentDefinition.RingOffset();
			ring.setX(70);
			ring.setY(4);
			ring.setZ(61);
			ring.setRadius(4);
			def.setRing(ring);
			def.setChampion(fighter("dragonminez:saga_kid_goku", 400.0, 34.0, 30.0, 3, 1.0));
			def.setSemifinalist(fighter("dragonminez:saga_masked_warrior", 280.0, 26.0, 20.0, 2, 1.0));
			def.setContenders(List.of(fighter("dragonminez:saga_dracula", 130.0, 13.0, 8.0, 1, 1.1), fighter("dragonminez:saga_invisible_man", 140.0, 14.0, 10.0, 1, 1.1), fighter("dragonminez:saga_akkuman", 155.0, 15.0, 14.0, 1, 1.1), fighter("dragonminez:saga_mummy", 175.0, 17.0, 6.0, 1, 1.1)));
			TournamentDefinition.Rewards rewards = new TournamentDefinition.Rewards();
			rewards.setTrainingPoints(10000);
			rewards.setAlignment(5);
			def.setRewards(rewards);
			return def;
		}

		private static TournamentDefinition.Fighter fighter(String entityId, double health, double melee, double ki, int aiTier, double perRoundScaling) {
			TournamentDefinition.Fighter f = new TournamentDefinition.Fighter();
			f.setEntityId(entityId);
			f.setHealth(health);
			f.setMeleeDamage(melee);
			f.setKiDamage(ki);
			f.setAiTier(aiTier);
			f.setPerRoundScaling(perRoundScaling);
			return f;
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

	public Format getFormat() {
		return this.format;
	}

	public Integer getDifficultyStars() {
		return this.difficultyStars;
	}

	public Integer getQualifierSlots() {
		return this.qualifierSlots;
	}

	public Integer getReentryCooldownSeconds() {
		return this.reentryCooldownSeconds;
	}

	public Integer getVictoryCooldownSeconds() {
		return this.victoryCooldownSeconds;
	}

	public Integer getMatchTimeoutSeconds() {
		return this.matchTimeoutSeconds;
	}

	public Integer getNextRoundSeconds() {
		return this.nextRoundSeconds;
	}

	public Integer getRulesAcceptSeconds() {
		return this.rulesAcceptSeconds;
	}

	public Integer getReturnSeconds() {
		return this.returnSeconds;
	}

	public Integer getArrivalSeconds() {
		return this.arrivalSeconds;
	}

	public Boolean getLethal() {
		return this.lethal;
	}

	public RingOffset getRing() {
		return this.ring;
	}

	public Fighter getChampion() {
		return this.champion;
	}

	public Fighter getSemifinalist() {
		return this.semifinalist;
	}

	public Fighter getFinalist() {
		return this.finalist;
	}

	public List<Fighter> getContenders() {
		return this.contenders;
	}

	public Rewards getRewards() {
		return this.rewards;
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

	public void setFormat(final Format format) {
		this.format = format;
	}

	public void setDifficultyStars(final Integer difficultyStars) {
		this.difficultyStars = difficultyStars;
	}

	public void setQualifierSlots(final Integer qualifierSlots) {
		this.qualifierSlots = qualifierSlots;
	}

	public void setReentryCooldownSeconds(final Integer reentryCooldownSeconds) {
		this.reentryCooldownSeconds = reentryCooldownSeconds;
	}

	public void setVictoryCooldownSeconds(final Integer victoryCooldownSeconds) {
		this.victoryCooldownSeconds = victoryCooldownSeconds;
	}

	public void setMatchTimeoutSeconds(final Integer matchTimeoutSeconds) {
		this.matchTimeoutSeconds = matchTimeoutSeconds;
	}

	public void setNextRoundSeconds(final Integer nextRoundSeconds) {
		this.nextRoundSeconds = nextRoundSeconds;
	}

	public void setRulesAcceptSeconds(final Integer rulesAcceptSeconds) {
		this.rulesAcceptSeconds = rulesAcceptSeconds;
	}

	public void setReturnSeconds(final Integer returnSeconds) {
		this.returnSeconds = returnSeconds;
	}

	public void setArrivalSeconds(final Integer arrivalSeconds) {
		this.arrivalSeconds = arrivalSeconds;
	}

	public void setLethal(final Boolean lethal) {
		this.lethal = lethal;
	}

	public void setRing(final RingOffset ring) {
		this.ring = ring;
	}

	public void setChampion(final Fighter champion) {
		this.champion = champion;
	}

	public void setSemifinalist(final Fighter semifinalist) {
		this.semifinalist = semifinalist;
	}

	public void setFinalist(final Fighter finalist) {
		this.finalist = finalist;
	}

	public void setContenders(final List<Fighter> contenders) {
		this.contenders = contenders;
	}

	public void setRewards(final Rewards rewards) {
		this.rewards = rewards;
	}

	public TournamentDefinition() {
	}
}
