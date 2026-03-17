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

	private float damageMultiplier;
	private float speed;
	private float size;
	private int armorPenetration;

	public KiAttackData() { super(); }

	@Override
	public TechniqueType getType() { return TechniqueType.KI_ATTACK; }

	public void calculateAndSetBaseCost() {
		double cost = (damageMultiplier * 50) + (speed * 10) + (size * 5) + (armorPenetration * 2) - (castTime * 2) + (cooldown);
		this.setBaseCost(Math.max(5, cost));
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
		tag.putFloat("DamageMultiplier", damageMultiplier);
		tag.putFloat("Speed", speed);
		tag.putFloat("Size", size);
		tag.putInt("ArmorPenetration", armorPenetration);
		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		this.id = tag.getString("Id");
		this.name = tag.getString("Name");
		this.author = tag.getString("Author");
		this.experience = tag.getInt("Experience");
		this.baseCost = tag.getDouble("BaseCost");
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

		this.colorInterior = tag.getInt("ColorInterior");
		this.colorExterior = tag.getInt("ColorExterior");
		this.damageMultiplier = tag.getFloat("DamageMultiplier");
		this.speed = tag.getFloat("Speed");
		this.size = tag.getFloat("Size");
		this.armorPenetration = tag.getInt("ArmorPenetration");
	}
}