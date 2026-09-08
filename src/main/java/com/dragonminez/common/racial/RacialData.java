package com.dragonminez.common.racial;

import com.dragonminez.common.racial.capture.PendingCapture;
import com.dragonminez.common.stats.character.BonusStats;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Getter
@Setter
public class RacialData {

	private int zenkaiPermanentUses;
	private double zenkaiReleaseBonus;
	private float peakRawHitWindow;
	private int peakRawHitTicksLeft;

	private String tempZenkaiBuffName = "";

	private final List<AssimilationSlot> assimilations = new ArrayList<>();

	private final List<AbsorptionSlot> absorptions = new ArrayList<>();

	private int absorptionSlotCounter;

	private boolean adrenalineArmed = true;

	private float energyReserve;
	private boolean reserveActive;

	public static final String BIO_SKILL_DRAIN = "drain";
	public static final String BIO_SKILL_EXPLODE = "explode";

	private final List<UUID> cellJrs = new ArrayList<>();
	private double cellJrStatPenalty;

	private String bioSelectedSkill = BIO_SKILL_DRAIN;

	private float bioSwell;
	private int bioChargeTicks;
	private transient Vec3 bioBlastCenter;
	private transient float bioBlastMaxRadius;
	private transient int bioBlastTick;

	private boolean bioSwellLocked;

	private final List<String> ownedBonusNames = new ArrayList<>();

	public void addOwnedBonusName(String bonusName) {
		if (!ownedBonusNames.contains(bonusName)) ownedBonusNames.add(bonusName);
	}

	public void removeOwnedBonusName(String bonusName) {
		ownedBonusNames.remove(bonusName);
	}

	public void addAssimilation(AssimilationSlot slot) {
		assimilations.add(slot);
	}

	public void addAbsorption(AbsorptionSlot slot) {
		absorptions.add(slot);
	}

	private transient PendingCapture pendingCaptureRequest;

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("ZenkaiPermanentUses", zenkaiPermanentUses);
		tag.putDouble("ZenkaiReleaseBonus", zenkaiReleaseBonus);
		tag.putFloat("PeakRawHitWindow", peakRawHitWindow);
		tag.putInt("PeakRawHitTicksLeft", peakRawHitTicksLeft);
		tag.putString("TempZenkaiBuffName", tempZenkaiBuffName);
		tag.put("Assimilations", saveSlots(assimilations));
		tag.put("Absorptions", saveSlots(absorptions));
		tag.putInt("AbsorptionSlotCounter", absorptionSlotCounter);
		tag.putBoolean("AdrenalineArmed", adrenalineArmed);
		tag.putFloat("EnergyReserve", energyReserve);
		tag.putBoolean("ReserveActive", reserveActive);
		ListTag cellJrTag = new ListTag();
		for (UUID id : cellJrs) cellJrTag.add(StringTag.valueOf(id.toString()));
		tag.put("CellJrs", cellJrTag);
		tag.putDouble("CellJrStatPenalty", cellJrStatPenalty);
		tag.putString("BioSelectedSkill", bioSelectedSkill);
		tag.putFloat("BioSwell", bioSwell);
		tag.putInt("BioChargeTicks", bioChargeTicks);
		tag.putBoolean("BioSwellLocked", bioSwellLocked);
		ListTag ownedTag = new ListTag();
		for (String name : ownedBonusNames) ownedTag.add(StringTag.valueOf(name));
		tag.put("OwnedBonusNames", ownedTag);
		return tag;
	}

	public void load(CompoundTag tag) {
		zenkaiPermanentUses = tag.getInt("ZenkaiPermanentUses");
		zenkaiReleaseBonus = tag.getDouble("ZenkaiReleaseBonus");
		peakRawHitWindow = tag.getFloat("PeakRawHitWindow");
		peakRawHitTicksLeft = tag.getInt("PeakRawHitTicksLeft");
		tempZenkaiBuffName = tag.getString("TempZenkaiBuffName");
		assimilations.clear();
		assimilations.addAll(loadSlots(tag.getList("Assimilations", Tag.TAG_COMPOUND), AssimilationSlot::fromNbt));
		absorptions.clear();
		absorptions.addAll(loadSlots(tag.getList("Absorptions", Tag.TAG_COMPOUND), AbsorptionSlot::fromNbt));
		absorptionSlotCounter = Math.max(tag.getInt("AbsorptionSlotCounter"), absorptions.size());
		adrenalineArmed = !tag.contains("AdrenalineArmed") || tag.getBoolean("AdrenalineArmed");
		energyReserve = tag.getFloat("EnergyReserve");
		reserveActive = tag.getBoolean("ReserveActive");
		cellJrs.clear();
		ListTag cellJrTag = tag.getList("CellJrs", Tag.TAG_STRING);
		for (int i = 0; i < cellJrTag.size(); i++) cellJrs.add(UUID.fromString(cellJrTag.getString(i)));
		cellJrStatPenalty = tag.getDouble("CellJrStatPenalty");
		String storedSkill = tag.getString("BioSelectedSkill");
		bioSelectedSkill = BIO_SKILL_EXPLODE.equals(storedSkill) ? BIO_SKILL_EXPLODE : BIO_SKILL_DRAIN;
		bioSwell = tag.getFloat("BioSwell");
		bioChargeTicks = tag.getInt("BioChargeTicks");
		bioSwellLocked = tag.getBoolean("BioSwellLocked");
		ownedBonusNames.clear();
		ListTag ownedTag = tag.getList("OwnedBonusNames", Tag.TAG_STRING);
		for (int i = 0; i < ownedTag.size(); i++) ownedBonusNames.add(ownedTag.getString(i));
	}

	private static final String[] CAPTURE_STAT_KEYS = {"STR", "SKP", "PWR"};

	public void migrateFromLegacy(String racialSkill, int legacyCount, BonusStats bonusStats) {
		if (racialSkill == null || legacyCount <= 0) return;
		String prefix = switch (racialSkill) {
			case "saiyan" -> "Zenkai_";
			case "namekian" -> "Assimilation_";
			case "majin" -> "Absorption_";
			default -> null;
		};
		if (prefix == null) return;

		int found = 0;
		for (int i = 1; i <= legacyCount; i++) {
			String bonusName = prefix + i;
			Map<String, Integer> grantedStats = new HashMap<>();
			for (String stat : CAPTURE_STAT_KEYS) {
				int value = findBonusValue(bonusStats, stat, bonusName);
				if (value != 0) grantedStats.put(stat, value);
			}
			if (grantedStats.isEmpty()) continue;

			addOwnedBonusName(bonusName);
			found++;

			if ("namekian".equals(racialSkill)) {
				assimilations.add(new AssimilationSlot(bonusName, null, "", grantedStats, 0L));
			} else if ("majin".equals(racialSkill)) {
				absorptions.add(new AbsorptionSlot(bonusName, null, "", grantedStats, 0L));
			}
		}

		if ("saiyan".equals(racialSkill)) zenkaiPermanentUses = found;
		if ("majin".equals(racialSkill)) absorptionSlotCounter = Math.max(absorptionSlotCounter, found);
	}

	private static int findBonusValue(BonusStats bonusStats, String stat, String bonusName) {
		for (BonusStats.StatBonus bonus : bonusStats.getBonuses(stat)) {
			if (bonus.name.equals(bonusName)) return (int) bonus.value;
		}
		return 0;
	}

	private static ListTag saveSlots(List<? extends CaptureSlot> slots) {
		ListTag list = new ListTag();
		for (CaptureSlot slot : slots) list.add(slot.toNbt());
		return list;
	}

	private static <T> List<T> loadSlots(ListTag list, Function<CompoundTag, T> factory) {
		List<T> result = new ArrayList<>();
		for (int i = 0; i < list.size(); i++) result.add(factory.apply(list.getCompound(i)));
		return result;
	}

	private interface CaptureSlot {
		CompoundTag toNbt();
	}

	public record AssimilationSlot(String bonusName, UUID sourceId, String sourceName,
									Map<String, Integer> grantedStats, long acquiredAtGameTime) implements CaptureSlot {
		@Override
		public CompoundTag toNbt() {
			return CaptureSlotNbt.toNbt(bonusName, sourceId, sourceName, grantedStats, acquiredAtGameTime);
		}

		static AssimilationSlot fromNbt(CompoundTag tag) {
			CaptureSlotNbt.Fields f = CaptureSlotNbt.fromNbt(tag);
			return new AssimilationSlot(f.bonusName(), f.sourceId(), f.sourceName(), f.grantedStats(), f.acquiredAtGameTime());
		}
	}

	public record AbsorptionSlot(String bonusName, UUID sourceId, String sourceName,
								  Map<String, Integer> grantedStats, long acquiredAtGameTime) implements CaptureSlot {
		@Override
		public CompoundTag toNbt() {
			return CaptureSlotNbt.toNbt(bonusName, sourceId, sourceName, grantedStats, acquiredAtGameTime);
		}

		static AbsorptionSlot fromNbt(CompoundTag tag) {
			CaptureSlotNbt.Fields f = CaptureSlotNbt.fromNbt(tag);
			return new AbsorptionSlot(f.bonusName(), f.sourceId(), f.sourceName(), f.grantedStats(), f.acquiredAtGameTime());
		}
	}

	public void copyFrom(RacialData other) {
		this.zenkaiPermanentUses = other.zenkaiPermanentUses;
		this.zenkaiReleaseBonus = other.zenkaiReleaseBonus;
		this.peakRawHitWindow = other.peakRawHitWindow;
		this.peakRawHitTicksLeft = other.peakRawHitTicksLeft;
		this.tempZenkaiBuffName = other.tempZenkaiBuffName;
		this.assimilations.clear();
		this.assimilations.addAll(other.assimilations);
		this.absorptions.clear();
		this.absorptions.addAll(other.absorptions);
		this.absorptionSlotCounter = other.absorptionSlotCounter;
		this.adrenalineArmed = other.adrenalineArmed;
		this.energyReserve = other.energyReserve;
		this.reserveActive = other.reserveActive;
		this.cellJrs.clear();
		this.cellJrs.addAll(other.cellJrs);
		this.cellJrStatPenalty = other.cellJrStatPenalty;
		this.bioSelectedSkill = other.bioSelectedSkill;
		this.bioSwell = other.bioSwell;
		this.bioChargeTicks = other.bioChargeTicks;
		this.bioSwellLocked = other.bioSwellLocked;
		this.ownedBonusNames.clear();
		this.ownedBonusNames.addAll(other.ownedBonusNames);
	}
}
