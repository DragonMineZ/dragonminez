package com.dragonminez.common.stats.techniques;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Getter
@Setter
public class KiAttackData extends TechniqueData {
	public enum KiType { SMALL_BALL, MEDIUM_BALL, GIANT_BALL, WAVE, LASER, BEAM, DISK, EXPLOSION, SHIELD, BARRAGE, AREA }
	public enum Utility { DAMAGE, HEAL }

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
			case EXPLOSION -> "ki.explosion";
			case BEAM, LASER -> "ki.makkako";
			default -> "ki.kameha";
		};
	}

	private static float getUtilityMultiplier(Utility util) {
		return switch (util) {
			case DAMAGE -> 1.0f;
			case HEAL -> 1.25f;
		};
	}

	private float getWeightedComplexity() {
		float maxStat = 20.0f;
		int maxArmorPen = 100;

		float damageWeight = 10.0f;
		float sizeWeight = 4.0f;
		float speedWeight = 3.0f;
		float armorPenWeight = 2.0f;

		float damageRatio = damageMultiplier / maxStat;
		float sizeRatio = size / maxStat;
		float speedRatio = speed / maxStat;
		float armorPenRatio = (float) armorPenetration / maxArmorPen;

		return (damageRatio * damageWeight) +
				(sizeRatio * sizeWeight) +
				(speedRatio * speedWeight) +
				(armorPenRatio * armorPenWeight);
	}

	public float getActualDamageMultiplier() { return damageMultiplier * (1.0f + (damageLevel * 0.1f)); }
	public float getActualSpeed() { return speed * (1.0f + (speedLevel * 0.05f)); }
	public float getActualSize() { return size * (1.0f + (sizeLevel * 0.05f)); }
	public int getActualArmorPenetration() { return Math.min(100, armorPenetration + (armorPenLevel * 2)); }
	public int getActualCastTime() { return (int) (castTime * (1.0f - (castTimeLevel * 0.05f))); }
	public int getActualCooldown() { return (int) (cooldown * (1.0f - (cooldownLevel * 0.05f))); }

	@Override
	public double getCalculatedCost(com.dragonminez.common.stats.StatsData statsData) {
		double damageDone = statsData.getKiDamage() * getActualDamageMultiplier();
		double complexityFactor = (getActualSize() * 5.0) + (getActualSpeed() * 5.0) + (getActualArmorPenetration() * 0.2);

		float typeMult = getTypeMultiplier(this.kiType != null ? this.kiType : KiType.SMALL_BALL);
		float utilMult = getUtilityMultiplier(this.utility != null ? this.utility : Utility.DAMAGE);

		return Math.max(5.0, (damageDone * 0.5 + complexityFactor) * typeMult * utilMult);
	}

	public void calculateDerivedValues() {
		float typeMult = getTypeMultiplier(kiType != null ? kiType : KiType.SMALL_BALL);
		float utilMult = getUtilityMultiplier(utility != null ? utility : Utility.DAMAGE);

		float flatDamage = getActualDamageMultiplier() * 10.0f;
		float flatSize = getActualSize() * 4.0f;
		float flatSpeed = getActualSpeed() * 3.0f;
		float flatPen = (getActualArmorPenetration() / 100.0f) * 2.0f;

		float complexity = flatDamage + flatSize + flatSpeed + flatPen;

		float tpBase = (80.0f + complexity * 15.0f) * typeMult * utilMult;
		this.tpCost = Math.max(10, Math.round(tpBase));

		if (!PredefinedTechniques.isPredefinedTechniqueId(this.id)) {
			if (this.kiType == KiType.SMALL_BALL) {
				this.castTime = 0;
			} else {
				float castBase = (8.0f + complexity * 1.5f) * (float) Math.sqrt(typeMult) * utilMult;
				this.castTime = Math.max(5, Math.min(200, Math.round(castBase)));
			}
			float cdBase = (20.0f + complexity * 3.0f) * typeMult * utilMult;
			this.cooldown = Math.max(10, Math.min(600, Math.round(cdBase)));
		}
	}

	public String generateExportCode() {
		try {
			CompoundTag tag = this.save();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(baos);
			NbtIo.write(tag, new DataOutputStream(gzip));
			gzip.close();
			return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static KiAttackData importFromCode(String code) {
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
	}

	public static float[] previewDerivedValues(KiType type, Utility util, float damage, float size, float speed, int armorPen) {
		float maxStat = 20.0f;
		int maxArmorPen = 100;

		float damageRatio = damage / maxStat;
		float sizeRatio = size / maxStat;
		float speedRatio = speed / maxStat;
		float armorPenRatio = (float) armorPen / maxArmorPen;

		float complexity = (damageRatio * 10.0f) + (sizeRatio * 4.0f) +
				(speedRatio * 3.0f) + (armorPenRatio * 2.0f);

		float typeMult = getTypeMultiplier(type != null ? type : KiType.SMALL_BALL);
		float utilMult = getUtilityMultiplier(util != null ? util : Utility.DAMAGE);

		float kiCost = Math.max(5, (float) ((10.0 + complexity * 40.0) * typeMult * utilMult));
		float tpCostVal = Math.max(10, Math.round((80.0f + complexity * 200.0f) * typeMult * utilMult));

		float castVal;
		if (type == KiType.SMALL_BALL) castVal = 0;
		else {
			castVal = Math.max(5, Math.min(200, Math.round((8.0f + complexity * 12.0f) * (float) Math.sqrt(typeMult) * utilMult)));
		}

		float cdVal = Math.max(10, Math.min(600, Math.round((20.0f + complexity * 30.0f) * typeMult * utilMult)));

		return new float[]{kiCost, tpCostVal, castVal, cdVal};
	}
}