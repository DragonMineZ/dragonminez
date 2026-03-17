package com.dragonminez.common.stats.techniques;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashMap;
import java.util.Map;

public class Techniques {
	@Getter
	private final Map<String, TechniqueData> unlockedTechniques = new HashMap<>();
	private final String[] equippedSlots = new String[8];
	private int selectedSlot = 0;
	private String chargingTechniqueId = "";
	private float techniqueChargePercent = 0.0f;
	private boolean techniqueCharging = false;

	public Techniques() {
		for (int i = 0; i < 8; i++) equippedSlots[i] = "";
	}

	public void unlockTechnique(TechniqueData data) {
		unlockedTechniques.put(data.getId(), data);
	}

	public void equipTechnique(int slotIndex, String techniqueId) {
		if (slotIndex >= 0 && slotIndex < 8 && unlockedTechniques.containsKey(techniqueId)) {
			equippedSlots[slotIndex] = techniqueId;
		}
	}

	public void selectSlot(int slotIndex) {
		if (slotIndex >= 0 && slotIndex < 8) {
			this.selectedSlot = slotIndex;
		}
	}

	public TechniqueData getSelectedTechnique() {
		String id = equippedSlots[selectedSlot];
		return id.isEmpty() ? null : unlockedTechniques.get(id);
	}

	public String[] getEquippedSlots() {
		return this.equippedSlots;
	}

	public String getChargingTechniqueId() {
		return chargingTechniqueId;
	}

	public float getTechniqueChargePercent() {
		return techniqueChargePercent;
	}

	public boolean isTechniqueCharging() {
		return techniqueCharging;
	}

	public boolean isTechniqueChargeActive() {
		return !chargingTechniqueId.isEmpty() && techniqueChargePercent > 0.0f;
	}

	public void startTechniqueCharge(String techniqueId) {
		if (techniqueId == null || techniqueId.isEmpty()) return;
		if (!techniqueId.equals(this.chargingTechniqueId)) {
			this.chargingTechniqueId = techniqueId;
			this.techniqueChargePercent = 0.0f;
		}
		this.techniqueCharging = true;
	}

	public void setTechniqueCharging(boolean charging) {
		this.techniqueCharging = charging;
	}

	public void setTechniqueChargePercent(float percent) {
		this.techniqueChargePercent = Math.max(0.0f, Math.min(200.0f, percent));
	}

	public void clearTechniqueCharge() {
		this.chargingTechniqueId = "";
		this.techniqueChargePercent = 0.0f;
		this.techniqueCharging = false;
	}

	public int getSelectedSlot() {
		return this.selectedSlot;
	}

	public void equipOrSwapTechnique(int slotIndex, String techniqueId) {
		if (slotIndex < 0 || slotIndex >= 8) return;

		int existingSlot = -1;
		for (int i = 0; i < 8; i++) {
			if (equippedSlots[i].equals(techniqueId)) {
				existingSlot = i;
				break;
			}
		}

		if (existingSlot != -1) {
			String temp = equippedSlots[slotIndex];
			equippedSlots[slotIndex] = techniqueId;
			equippedSlots[existingSlot] = temp;
		} else equippedSlots[slotIndex] = techniqueId;
	}

	public void addExperienceToSelected(int amount) {
		TechniqueData active = getSelectedTechnique();
		if (active != null) active.addExperience(amount);
	}

	public void addExperienceToTechnique(String id, int amount) {
		if (unlockedTechniques.containsKey(id)) {
			unlockedTechniques.get(id).addExperience(amount);
		}
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("SelectedSlot", selectedSlot);
		tag.putString("ChargingTechniqueId", chargingTechniqueId);
		tag.putFloat("TechniqueChargePercent", techniqueChargePercent);
		tag.putBoolean("TechniqueCharging", techniqueCharging);

		CompoundTag slotsTag = new CompoundTag();
		for (int i = 0; i < 8; i++) {
			slotsTag.putString("Slot" + i, equippedSlots[i]);
		}
		tag.put("EquippedSlots", slotsTag);

		ListTag unlockedTag = new ListTag();
		for (TechniqueData tech : unlockedTechniques.values()) {
			CompoundTag techTag = tech.save();
			techTag.putString("TechClassType", tech instanceof KiAttackData ? "KI" : "STRIKE");
			unlockedTag.add(techTag);
		}
		tag.put("UnlockedTechniques", unlockedTag);

		return tag;
	}

	public void load(CompoundTag tag) {
		this.selectedSlot = tag.getInt("SelectedSlot");
		this.chargingTechniqueId = tag.getString("ChargingTechniqueId");
		this.techniqueChargePercent = Math.max(0.0f, Math.min(200.0f, tag.getFloat("TechniqueChargePercent")));
		this.techniqueCharging = tag.getBoolean("TechniqueCharging");

		CompoundTag slotsTag = tag.getCompound("EquippedSlots");
		for (int i = 0; i < 8; i++) {
			this.equippedSlots[i] = slotsTag.getString("Slot" + i);
		}

		this.unlockedTechniques.clear();
		ListTag unlockedTag = tag.getList("UnlockedTechniques", 10);
		for (int i = 0; i < unlockedTag.size(); i++) {
			CompoundTag techTag = unlockedTag.getCompound(i);
			String type = techTag.getString("TechClassType");
			TechniqueData tech = type.equals("KI") ? new KiAttackData() : new StrikeAttackData();
			tech.load(techTag);
			this.unlockedTechniques.put(tech.getId(), tech);
		}
	}

	public void copyFrom(Techniques other) {
		this.selectedSlot = other.selectedSlot;
		this.chargingTechniqueId = other.chargingTechniqueId;
		this.techniqueChargePercent = other.techniqueChargePercent;
		this.techniqueCharging = other.techniqueCharging;
		System.arraycopy(other.equippedSlots, 0, this.equippedSlots, 0, 8);
		this.unlockedTechniques.clear();
		for (Map.Entry<String, TechniqueData> entry : other.unlockedTechniques.entrySet()) {
			TechniqueData clone = entry.getValue() instanceof KiAttackData ? new KiAttackData() : new StrikeAttackData();
			clone.load(entry.getValue().save());
			this.unlockedTechniques.put(entry.getKey(), clone);
		}
	}
}