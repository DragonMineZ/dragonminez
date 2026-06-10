package com.dragonminez.common.stats.techniques;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.TechniqueConfig;
import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Mth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@Getter
@Setter
public class KiAttackData extends TechniqueData {
	public enum KiType { SMALL_BALL, MEDIUM_BALL, GIANT_BALL, WAVE, LASER, BEAM, DISK, EXPLOSION, SHIELD, BARRAGE, AREA }
	public enum Utility { DAMAGE, HEAL }

	public enum SecondaryEffectType { NONE, BUFF, DEBUFF }
	public enum AffectedStat { STR, SKP, DEF, STM_REGEN, HP_REGEN, ENE_REGEN, PWR }

	public static final int MIN_SECONDARY_INTENSITY = 5;
	public static final int MAX_SECONDARY_INTENSITY = 50;
	public static final int MIN_SECONDARY_DURATION = 1;
	public static final int MAX_SECONDARY_DURATION = 8;
	public static final float SECONDARY_COST_FACTOR = 0.25f;

	private static final String CODE_PREFIX = "DMZK1:";
	private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private List<String> allowedRaces = new ArrayList<>();
	private KiType kiType;
	private String animation;
	private Utility utility;

	private int colorInterior;
	private int colorExterior;
	private int colorOutline;

	private float damageMultiplier;
	private float speed;
	private float size;
	private int armorPenetration;

	private SecondaryEffectType secondaryEffectType = SecondaryEffectType.NONE;
	private AffectedStat affectedStat;
	private float secondaryIntensity;
	private int secondaryDuration;

	private int damageLevel = 0;
	private int castTimeLevel = 0;
	private int cooldownLevel = 0;
	private int speedLevel = 0;
	private int sizeLevel = 0;
	private int armorPenLevel = 0;

	public KiAttackData() { super(); }

	@Override
	public TechniqueType getType() { return TechniqueType.KI_ATTACK; }

	private static float getTypeMultiplier(KiType type) {
		return switch (type) {
			case SMALL_BALL -> 1.0f;
			case MEDIUM_BALL -> 1.4f;
			case GIANT_BALL -> 2.2f;
			case WAVE -> 1.6f;
			case LASER -> 0.8f;
			case BEAM -> 1.4f;
			case DISK -> 1.2f;
			case EXPLOSION -> 2.6f;
			case SHIELD -> 1.6f;
			case BARRAGE -> 1.4f;
			case AREA -> 1.8f;
		};
	}

	public String getAnimationPrefix() {
		if (this.animation != null && !this.animation.isEmpty()) return this.animation;

		if (PredefinedTechniques.isPredefinedTechniqueId(this.id)) {
			KiAttackData predefined = PredefinedTechniques.REGISTRY.get(this.id);
			if (predefined != null && predefined.getAnimation() != null && !predefined.getAnimation().isEmpty()) return predefined.getAnimation();
		}

		return switch (kiType != null ? kiType : KiType.SMALL_BALL) {
			case BARRAGE -> "ki.barrage";
			case GIANT_BALL -> "ki.large_ball";
			case WAVE -> "ki.kameha";
			case DISK -> "ki.kienzan";
			case EXPLOSION, SHIELD -> "ki.explosion";
			case BEAM, LASER -> "ki.makkako";
			case SMALL_BALL, MEDIUM_BALL -> "ki.bigbang";
			default -> "ki.kameha";
		};
	}

	private static float getUtilityMultiplier(Utility util) {
		return switch (util) {
			case DAMAGE -> 1.0f;
			case HEAL -> 1.25f;
		};
	}

	public static boolean allowsHealUtility(KiType type) {
		return type == KiType.AREA || type == KiType.SMALL_BALL || type == KiType.SHIELD;
	}

	public Utility getEffectiveUtility() {
		Utility u = this.utility != null ? this.utility : Utility.DAMAGE;
		if (u == Utility.HEAL && !allowsHealUtility(this.kiType != null ? this.kiType : KiType.SMALL_BALL)) return Utility.DAMAGE;
		return u;
	}

	public static final int KI_TIME_MULTIPLIER = 20;
	public float getActualDamageMultiplier() { return damageMultiplier * (1.0f + (damageLevel * 0.1f)); }
	public float getActualSpeed() { return speed * (1.0f + (speedLevel * 0.05f)); }
	public float getActualSize() { return size * (1.0f + (sizeLevel * 0.05f)); }
	public int getActualArmorPenetration() { return Math.min(100, armorPenetration + (armorPenLevel * 2)); }
	public int getActualCastTime() { return getBaseChargeTicks(); }
	public int getActualCooldown() { return cooldown * KI_TIME_MULTIPLIER; }

	public boolean isInstantCast() {
		return kiType == KiType.SMALL_BALL || kiType == KiType.LASER;
	}

	public static final int OVERCHARGE_MAX_PERCENT = 175;
	public static final int OVERCHARGE_TIER_PERCENT = 25;

	public int getBaseChargeTicks() {
		if (isInstantCast()) return 0;
		int configured = ConfigManager.getTechniqueConfig().getKiTypeConfig(kiType != null ? kiType : KiType.SMALL_BALL).getCastTimeTicks();
		return Math.max(0, configured);
	}

	public static float costMultiplier(float percent) {
		if (percent <= 100.0f) return Math.max(0.0f, percent) / 100.0f;
		return 1.0f + (percent - 100.0f) / (OVERCHARGE_MAX_PERCENT - 100.0f);
	}

	public void setSecondaryIntensity(float value) {
		this.secondaryIntensity = value <= 0 ? 0 : Mth.clamp(value, MIN_SECONDARY_INTENSITY, MAX_SECONDARY_INTENSITY);
	}

	public void setSecondaryDuration(int value) {
		this.secondaryDuration = value <= 0 ? 0 : Mth.clamp(value, MIN_SECONDARY_DURATION, MAX_SECONDARY_DURATION);
	}

	public boolean hasValidSecondaryEffect() {
		if (secondaryEffectType == null || secondaryEffectType == SecondaryEffectType.NONE) return false;
		if (affectedStat == null) return false;
		if (secondaryIntensity <= 0 || secondaryDuration <= 0) return false;
		Utility u = this.utility != null ? this.utility : Utility.DAMAGE;
		return (secondaryEffectType == SecondaryEffectType.BUFF && u == Utility.HEAL)
				|| (secondaryEffectType == SecondaryEffectType.DEBUFF && u == Utility.DAMAGE);
	}

	public float secondaryCostWeight() {
		return secondaryCostWeight(hasValidSecondaryEffect() ? secondaryEffectType : SecondaryEffectType.NONE, secondaryIntensity, secondaryDuration);
	}

	private static float secondaryCostWeight(SecondaryEffectType type, float intensity, int duration) {
		if (type == null || type == SecondaryEffectType.NONE) return 0f;
		float intensityNorm = Mth.clamp((intensity - MIN_SECONDARY_INTENSITY) / (float) (MAX_SECONDARY_INTENSITY - MIN_SECONDARY_INTENSITY), 0f, 1f);
		float durationNorm = Mth.clamp((duration - MIN_SECONDARY_DURATION) / (float) (MAX_SECONDARY_DURATION - MIN_SECONDARY_DURATION), 0f, 1f);
		return intensityNorm * 0.6f + durationNorm * 0.4f;
	}

	public float secondaryCostMultiplier() {
		return 1f + SECONDARY_COST_FACTOR * secondaryCostWeight();
	}

	@Override
	public double getCalculatedCost(StatsData statsData) {
		double damageDone = statsData.getKiDamageNoForms() * getActualDamageMultiplier();
		double complexityFactor = (getActualSize() * 5.0) + (getActualSpeed() * 5.0) + (getActualArmorPenetration() * 0.2);

		float typeMult = getTypeMultiplier(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		float utilMult = getUtilityMultiplier(this.utility != null ? this.utility : Utility.DAMAGE);
		TechniqueConfig.TechniqueTypeConfig cfg = ConfigManager.getTechniqueConfig().getKiTypeConfig(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		double configCostMult = Math.max(0.0, cfg.getKiCostMultiplier());
		KiType resolvedType = this.kiType != null ? this.kiType : KiType.SMALL_BALL;
		double overload = largeOverloadKiMultiplier(resolvedType, this.damageMultiplier);
		return Math.max(5.0, ((damageDone * 0.5 + complexityFactor) * typeMult * utilMult * configCostMult * secondaryCostMultiplier() * overload) / 2);
	}

	public int getUpgradeXpCost(String statName) {
		TechniqueConfig.TechniqueTypeConfig cfg = ConfigManager.getTechniqueConfig().getKiTypeConfig(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		int baseMin = Math.max(0, cfg.getMinXPCost());
		double multiplier = Math.max(0.0, cfg.getXpCostMultiplier());

		float initialDamage = Math.max(0.0f, this.damageMultiplier - (this.damageLevel * 0.1f));
		float initialSize = Math.max(0.0f, this.size - (this.sizeLevel * 0.1f));
		float initialSpeed = Math.max(0.0f, this.speed - (this.speedLevel * 0.1f));
		int initialArmorPen = Math.max(0, this.armorPenetration - this.armorPenLevel);

		KiType resolvedType = this.kiType != null ? this.kiType : KiType.SMALL_BALL;
		float initialComplexity = getWeightedComplexity(initialDamage, sizeComplexityRatio(resolvedType, initialSize), initialSpeed, initialArmorPen);
		int totalUpgrades = getTotalUpgradeCount();

		int complexityBase = baseMin + (int) Math.round(initialComplexity * 10.0);
		int scaledBase = (int) Math.round(complexityBase * multiplier);
		int upgradeExtra = (int) Math.round(totalUpgrades * Math.max(0.0, complexityBase * (multiplier - 1.0)));
		int computed = Math.max(0, scaledBase + upgradeExtra);

		int max = cfg.getMaxXPCost();
		if (max >= 0) computed = Math.min(computed, max);
		return Math.max(0, computed);
	}

	public int getXpGainPerHit() {
		TechniqueConfig.TechniqueTypeConfig cfg = ConfigManager.getTechniqueConfig().getKiTypeConfig(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		double gain = Math.max(0.0, cfg.getXpGainPerHit() * cfg.getXpGainMultiplier());
		return Math.max(0, (int) Math.round(gain));
	}

	public int getXpGainPerKill() {
		TechniqueConfig.TechniqueTypeConfig cfg = ConfigManager.getTechniqueConfig().getKiTypeConfig(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		double gain = Math.max(0.0, cfg.getXpGainPerKill() * cfg.getXpGainMultiplier());
		return Math.max(0, (int) Math.round(gain));
	}

	public float getConfiguredDamageMultiplier() {
		TechniqueConfig.TechniqueTypeConfig cfg = ConfigManager.getTechniqueConfig().getKiTypeConfig(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		return (float) Math.max(0.0, cfg.getDamageMultiplier());
	}

	public boolean canUpgradeStat(String statName) {
		KiType type = this.kiType != null ? this.kiType : KiType.SMALL_BALL;
		if ("damage".equals(statName) || "cooldown".equals(statName)) return true;

		return switch (type) {
			case SMALL_BALL -> "speed".equals(statName);
			case MEDIUM_BALL -> "speed".equals(statName) || "armor_pen".equals(statName);
			case GIANT_BALL -> "size".equals(statName) || "armor_pen".equals(statName);
			case WAVE, LASER, BEAM, DISK -> "speed".equals(statName) || "armor_pen".equals(statName);
			case BARRAGE -> "speed".equals(statName);
			case EXPLOSION -> "armor_pen".equals(statName);
			case SHIELD, AREA -> false;
		};
	}

	private int getTotalUpgradeCount() {
		return Math.max(0, damageLevel)
				+ Math.max(0, sizeLevel)
				+ Math.max(0, speedLevel)
				+ Math.max(0, armorPenLevel)
				+ Math.max(0, castTimeLevel)
				+ Math.max(0, cooldownLevel);
	}

	private static final float MAX_DAMAGE_MULT = 2.5f;

	private static float getWeightedComplexity(float damage, float sizeRatio01, float speed, int armorPen) {
		float maxStat = 20.0f;
		int maxArmorPen = 100;

		float damageWeight = 10.0f;
		float sizeWeight = 4.0f;
		float speedWeight = 3.0f;
		float armorPenWeight = 2.0f;

		float damageRatio = Mth.clamp(damage / MAX_DAMAGE_MULT, 0f, 1f);
		float sizeRatio = Mth.clamp(sizeRatio01, 0f, 1f);
		float speedRatio = speed / maxStat;
		float armorPenRatio = (float) armorPen / maxArmorPen;

		return (damageRatio * damageWeight) +
				(sizeRatio * sizeWeight) +
				(speedRatio * speedWeight) +
				(armorPenRatio * armorPenWeight);
	}

	public void calculateDerivedValues() {
		KiType resolvedType = kiType != null ? kiType : KiType.SMALL_BALL;
		Utility resolvedUtil = utility != null ? utility : Utility.DAMAGE;
		float typeMult = getTypeMultiplier(resolvedType);
		float utilMult = getUtilityMultiplier(resolvedUtil);

		float[] normalized = normalizeStatsForType(resolvedType, this.damageMultiplier, this.size, this.speed, this.armorPenetration);

		float complexity = getWeightedComplexity(normalized[0], sizeComplexityRatio(resolvedType, normalized[1]), normalized[2], Math.round(normalized[3]));
		float tpBase = (80.0f + complexity * 100.0f) * typeMult * utilMult * secondaryCostMultiplier();
		this.tpCost = Math.max(10, Math.round(tpBase));

		if (!PredefinedTechniques.isPredefinedTechniqueId(this.id)) {
			float initialDamage = Math.max(0.0f, normalized[0] - (damageLevel * 0.1f));
			float initialSize = Math.max(0.0f, normalized[1] - (sizeLevel * 0.1f));
			float initialSpeed = Math.max(0.0f, normalized[2] - (speedLevel * 0.1f));
			int initialArmorPen = Math.max(0, Math.round(normalized[3]) - armorPenLevel);

			float initialComplexity = getWeightedComplexity(initialDamage, sizeComplexityRatio(resolvedType, initialSize), initialSpeed, initialArmorPen);
			this.castTime = 0;
			int rawCooldown = computeDerivedCooldown(resolvedType, resolvedUtil, initialComplexity, cooldownLevel);
			this.cooldown = Math.max(10, Math.min(600, Math.round(rawCooldown * secondaryCostMultiplier())));
		}
	}

	private static byte[] compressOptimized(byte[] data) throws Exception {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		try (DeflaterOutputStream defOut = new DeflaterOutputStream(byteOut, deflater)) {
			defOut.write(data);
		}
		deflater.end();
		return byteOut.toByteArray();
	}

	private static byte[] decompressOptimized(byte[] data) throws Exception {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
		Inflater inflater = new Inflater(true);
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		try (InflaterInputStream infIn = new InflaterInputStream(byteIn, inflater)) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = infIn.read(buffer)) != -1) {
				byteOut.write(buffer, 0, len);
			}
		}
		inflater.end();
		return byteOut.toByteArray();
	}

	private static String encodeBigInt(byte[] bytes, String alphabet) {
		if (bytes == null || bytes.length == 0) return "";
		BigInteger value = new BigInteger(1, bytes);
		if (value.equals(BigInteger.ZERO)) return String.valueOf(alphabet.charAt(0));

		StringBuilder result = new StringBuilder();
		BigInteger base = BigInteger.valueOf(alphabet.length());

		while (value.compareTo(BigInteger.ZERO) > 0) {
			BigInteger[] divmod = value.divideAndRemainder(base);
			result.append(alphabet.charAt(divmod[1].intValue()));
			value = divmod[0];
		}

		return result.reverse().toString();
	}

	private static byte[] decodeBigInt(String encoded, String alphabet) {
		if (encoded == null || encoded.isEmpty()) return new byte[0];
		BigInteger value = BigInteger.ZERO;
		BigInteger base = BigInteger.valueOf(alphabet.length());

		for (int i = 0; i < encoded.length(); i++) {
			char c = encoded.charAt(i);
			int digit = alphabet.indexOf(c);
			if (digit < 0) throw new IllegalArgumentException("Invalid char: " + c);
			value = value.multiply(base).add(BigInteger.valueOf(digit));
		}

		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {
			byte[] result = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, result, 0, result.length);
			return result;
		}
		return bytes;
	}

	public String generateExportCode() {
		try {
			CompoundTag tag = this.save();
			ByteArrayOutputStream nbtOut = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(nbtOut);
			NbtIo.write(tag, dataOut);
			dataOut.close();

			byte[] compressed = compressOptimized(nbtOut.toByteArray());
			return CODE_PREFIX + encodeBigInt(compressed, BASE62_ALPHABET);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static KiAttackData importFromCode(String code) {
		if (code == null || code.isEmpty()) return null;

		if (!code.startsWith(CODE_PREFIX)) {
			try {
				byte[] data = Base64.getUrlDecoder().decode(code);
				ByteArrayInputStream bais = new ByteArrayInputStream(data);
				GZIPInputStream gzip = new GZIPInputStream(bais);
				CompoundTag tag = NbtIo.read(new DataInputStream(gzip));
				gzip.close();

				KiAttackData attack = new KiAttackData();
				attack.load(tag);
				attack.setId(UUID.randomUUID().toString());
				attack.setExperience(0);
				return attack;
			} catch (Exception e) {
				return null;
			}
		}

		try {
			byte[] bytes = decodeBigInt(code.substring(CODE_PREFIX.length()), BASE62_ALPHABET);
			byte[] decompressed = decompressOptimized(bytes);

			ByteArrayInputStream byteIn = new ByteArrayInputStream(decompressed);
			DataInputStream dataIn = new DataInputStream(byteIn);
			CompoundTag tag = NbtIo.read(dataIn);

			KiAttackData attack = new KiAttackData();
			attack.load(tag);
			attack.setId(UUID.randomUUID().toString());
			attack.setExperience(0);
			return attack;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
		tag.putInt("CastTime", this.castTime);
		tag.putInt("Cooldown", this.cooldown);

		ListTag racesTag = new ListTag();
		for (String race : allowedRaces) racesTag.add(StringTag.valueOf(race));
		tag.put("AllowedRaces", racesTag);

		tag.putString("KiType", kiType != null ? kiType.name() : "");
		tag.putString("Animation", animation != null ? animation : "");
		tag.putString("Utility", utility != null ? utility.name() : "");
		tag.putInt("ColorInterior", colorInterior);
		tag.putInt("ColorExterior", colorExterior);
		tag.putInt("ColorOutline", colorOutline);
		tag.putFloat("DamageMultiplier", damageMultiplier);
		tag.putFloat("Speed", speed);
		tag.putFloat("Size", size);
		tag.putInt("ArmorPenetration", armorPenetration);
		tag.putInt("ArmorPenLevel", armorPenLevel);
		tag.putInt("DamageLevel", damageLevel);
		tag.putInt("CastTimeLevel", castTimeLevel);
		tag.putInt("CooldownLevel", cooldownLevel);
		tag.putInt("SpeedLevel", speedLevel);
		tag.putInt("SizeLevel", sizeLevel);
		tag.putString("SecondaryEffectType", secondaryEffectType != null ? secondaryEffectType.name() : SecondaryEffectType.NONE.name());
		tag.putString("AffectedStat", affectedStat != null ? affectedStat.name() : "");
		tag.putFloat("SecondaryIntensity", secondaryIntensity);
		tag.putInt("SecondaryDuration", secondaryDuration);
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
		this.castTime = tag.getInt("CastTime");
		this.cooldown = tag.getInt("Cooldown");

		this.allowedRaces.clear();
		ListTag racesTag = tag.getList("AllowedRaces", 8);
		for (int i = 0; i < racesTag.size(); i++) {
			this.allowedRaces.add(racesTag.getString(i));
		}

		try { this.kiType = KiType.valueOf(tag.getString("KiType")); } catch (Exception e) { this.kiType = KiType.SMALL_BALL; }
		this.animation = tag.getString("Animation");
		try { this.utility = Utility.valueOf(tag.getString("Utility")); } catch (Exception e) { this.utility = Utility.DAMAGE; }
		if (this.utility == Utility.HEAL && !allowsHealUtility(this.kiType)) this.utility = Utility.DAMAGE;

		if ((this.animation == null || this.animation.isEmpty()) && PredefinedTechniques.isPredefinedTechniqueId(this.id)) {
			KiAttackData predefined = PredefinedTechniques.REGISTRY.get(this.id);
			if (predefined != null) this.animation = predefined.getAnimation();
		}

		this.colorInterior = tag.getInt("ColorInterior");
		this.colorExterior = tag.getInt("ColorExterior");
		this.colorOutline = tag.getInt("ColorOutline");
		this.damageMultiplier = tag.getFloat("DamageMultiplier");
		this.speed = tag.getFloat("Speed");
		this.size = tag.getFloat("Size");
		this.armorPenetration = tag.getInt("ArmorPenetration");
		this.armorPenLevel = tag.getInt("ArmorPenLevel");
		this.damageLevel = tag.getInt("DamageLevel");
		this.castTimeLevel = tag.getInt("CastTimeLevel");
		this.cooldownLevel = tag.getInt("CooldownLevel");
		this.speedLevel = tag.getInt("SpeedLevel");
		this.sizeLevel = tag.getInt("SizeLevel");

		try { this.secondaryEffectType = SecondaryEffectType.valueOf(tag.getString("SecondaryEffectType")); }
		catch (Exception e) { this.secondaryEffectType = SecondaryEffectType.NONE; }
		String affected = tag.getString("AffectedStat");
		if (affected == null || affected.isEmpty()) this.affectedStat = null;
		else try { this.affectedStat = AffectedStat.valueOf(affected); } catch (Exception e) { this.affectedStat = null; }
		setSecondaryIntensity(tag.getFloat("SecondaryIntensity"));
		setSecondaryDuration(tag.getInt("SecondaryDuration"));
	}

	public static float[] previewDerivedValues(KiType type, Utility util, float damage, float size, float speed, int armorPen) {
		return previewDerivedValues(type, util, damage, size, speed, armorPen, SecondaryEffectType.NONE, 0f, 0);
	}

	public static float[] previewDerivedValues(KiType type, Utility util, float damage, float size, float speed, int armorPen,
											   SecondaryEffectType secondaryType, float secondaryIntensity, int secondaryDuration) {
		KiType resolvedType = type != null ? type : KiType.SMALL_BALL;
		Utility resolvedUtil = util != null ? util : Utility.DAMAGE;

		float[] normalized = normalizeStatsForType(resolvedType, damage, size, speed, armorPen);
		float complexity = getWeightedComplexity(normalized[0], sizeComplexityRatio(resolvedType, normalized[1]), normalized[2], Math.round(normalized[3]));

		float typeMult = getTypeMultiplier(resolvedType);
		float utilMult = getUtilityMultiplier(resolvedUtil);
		boolean validSecondary = (secondaryType == SecondaryEffectType.BUFF && resolvedUtil == Utility.HEAL)
				|| (secondaryType == SecondaryEffectType.DEBUFF && resolvedUtil == Utility.DAMAGE);
		float secMult = 1f + SECONDARY_COST_FACTOR * secondaryCostWeight(validSecondary ? secondaryType : SecondaryEffectType.NONE, secondaryIntensity, secondaryDuration);

		float kiCost = Math.max(5, (float) ((10.0 + complexity * 40.0) * typeMult * utilMult * secMult * largeOverloadKiMultiplier(resolvedType, normalized[0])));
		float tpCostVal = Math.max(10, Math.round((80.0f + complexity * 200.0f) * typeMult * utilMult * secMult));
		float castVal = ConfigManager.getTechniqueConfig().getKiTypeConfig(resolvedType).getCastTimeTicks();
		float cdVal = Math.max(10, Math.min(600, Math.round(computeDerivedCooldown(resolvedType, resolvedUtil, complexity, 0) * secMult)));

		return new float[]{kiCost, tpCostVal, castVal, cdVal};
	}

	public static float[] normalizeStatsForType(KiType type, float damage, float size, float speed, int armorPen) {
		KiType resolvedType = type != null ? type : KiType.SMALL_BALL;
		float normalizedDamage = Mth.clamp(damage, getMinDamageForType(resolvedType), getMaxDamageForType(resolvedType));
		float normalizedSize = usesCustomSize(resolvedType)
				? Mth.clamp(size, getMinSizeForType(resolvedType), getMaxSizeForType(resolvedType))
				: getDefaultSizeForType(resolvedType);
		float normalizedSpeed = usesCustomSpeed(resolvedType)
				? Mth.clamp(speed, getMinSpeedForType(resolvedType), getMaxSpeedForType(resolvedType))
				: getDefaultSpeedForType(resolvedType);
		float normalizedArmorPen = usesCustomArmorPen(resolvedType)
				? Mth.clamp(armorPen, 0, getMaxArmorPenForType(resolvedType))
				: getDefaultArmorPenForType(resolvedType);
		return new float[]{normalizedDamage, normalizedSize, normalizedSpeed, normalizedArmorPen};
	}

	public static boolean usesCustomSize(KiType type) {
		return switch (type) {
			case SMALL_BALL, MEDIUM_BALL, GIANT_BALL -> true;
			default -> false;
		};
	}

	public static float getMinSizeForType(KiType type) {
		return switch (type) {
			case SMALL_BALL -> 1.0f;
			case MEDIUM_BALL -> 7.5f;
			case GIANT_BALL -> 15.0f;
			default -> 0.1f;
		};
	}

	public static float getMaxSizeForType(KiType type) {
		return switch (type) {
			case MEDIUM_BALL -> 12.5f;
			case SMALL_BALL -> 5.0f;
			default -> 20.0f;
		};
	}

	public static boolean usesCustomSpeed(KiType type) {
		return switch (type) {
			case SMALL_BALL, MEDIUM_BALL, GIANT_BALL, WAVE, LASER, BEAM, DISK, BARRAGE -> true;
			default -> false;
		};
	}

	public static boolean usesCustomArmorPen(KiType type) {
		return switch (type) {
			case MEDIUM_BALL, GIANT_BALL, WAVE, LASER, BEAM, DISK, EXPLOSION -> true;
			default -> false;
		};
	}

	public static boolean isLargeDamageTier(KiType type) {
		return type == KiType.GIANT_BALL || type == KiType.EXPLOSION;
	}

	private static boolean isMediumDamageTier(KiType type) {
		return switch (type) {
			case MEDIUM_BALL, BEAM, WAVE, SHIELD, AREA -> true;
			default -> false;
		};
	}

	public static float getMinDamageForType(KiType type) {
		if (isLargeDamageTier(type)) return 1.0f;
		if (isMediumDamageTier(type)) return 0.5f;
		return 0.25f;
	}

	public static float getMaxDamageForType(KiType type) {
		if (isLargeDamageTier(type)) return 2.5f;
		if (isMediumDamageTier(type)) return 1.5f;
		return 0.75f;
	}

	public static float getDefaultDamageForType(KiType type) {
		if (isLargeDamageTier(type)) return 1.5f;
		if (isMediumDamageTier(type)) return 1.0f;
		return 0.5f;
	}

	public static float getMinSpeedForType(KiType type) {
		return 0.1f;
	}

	public static float getMaxSpeedForType(KiType type) {
		return switch (type) {
			case SMALL_BALL, LASER -> 2.0f;
			case DISK -> 1.75f;
			case MEDIUM_BALL, BEAM, BARRAGE -> 1.5f;
			case WAVE -> 1.25f;
			case GIANT_BALL -> 0.75f;
			default -> 1.0f;
		};
	}

	public static int getMaxArmorPenForType(KiType type) {
		return switch (type) {
			case DISK, BEAM, GIANT_BALL -> 25;
			default -> 15;
		};
	}

	private static final float LARGE_OVERLOAD_KI_FACTOR = 2.0f;

	private static float largeOverloadKiMultiplier(KiType type, float damage) {
		if (!isLargeDamageTier(type) || damage <= 2.0f) return 1.0f;
		return 1.0f + (damage - 2.0f) * LARGE_OVERLOAD_KI_FACTOR;
	}

	public static float getDefaultSizeForType(KiType type) {
		return switch (type) {
			case GIANT_BALL -> 17.5f;
			case MEDIUM_BALL -> 10.0f;
			case EXPLOSION -> 8.0f;
			case SMALL_BALL -> 3.0f;
			default -> 1.0f;
		};
	}

	private static float sizeComplexityRatio(KiType type, float size) {
		KiType resolved = type != null ? type : KiType.SMALL_BALL;
		if (usesCustomSize(resolved)) {
			float min = getMinSizeForType(resolved);
			float max = getMaxSizeForType(resolved);
			if (max <= min) return 0f;
			return Mth.clamp((size - min) / (max - min), 0f, 1f);
		}
		return Mth.clamp(getDefaultSizeForType(resolved) / 20.0f, 0f, 1f);
	}

	public static float getDefaultSpeedForType(KiType type) {
		return Math.min(1.0f, getMaxSpeedForType(type));
	}

	public static int getDefaultArmorPenForType(KiType type) {
		return 0;
	}

	private static int computeDerivedCooldown(KiType type, Utility util, float initialComplexity, int cooldownLevel) {
		KiType resolved = type != null ? type : KiType.SMALL_BALL;
		float typeMult = getTypeMultiplier(resolved);
		float utilMult = getUtilityMultiplier(util != null ? util : Utility.DAMAGE);
		float cdTypeMult = switch (resolved) {
			case SMALL_BALL, LASER, DISK -> 0.5f;
			default -> 1.0f;
		};
		float base = (20.0f + initialComplexity * 4.0f) * typeMult * utilMult * cdTypeMult;
		float reduced = base * Math.max(0.1f, 1.0f - (Math.max(0, cooldownLevel) * 0.05f));
		return Math.max(10, Math.min(600, Math.round(reduced)));
	}
}