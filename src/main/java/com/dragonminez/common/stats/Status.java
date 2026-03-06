package com.dragonminez.common.stats;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

@Getter
@Setter
public class Status {
	private boolean isAlive;
	private boolean isHasCreatedCharacter;
	private boolean isAuraActive;
	private boolean isActionCharging;
	private boolean isTailVisible;
	private boolean isDescending;
	private boolean isInKaioPlanet;
	private boolean isChargingKi;
	private boolean isBlocking;
	private long lastBlockTime;
	private long lastHurtTime;
	private boolean isStunned;
	private ActionMode selectedAction;
	private String kiWeaponType;
	private int drainingTargetId;
	private boolean isFused;
	private boolean isFusionLeader;
	private UUID fusionPartnerUUID;
	private int fusionTimer;
	private String fusionType;
	private CompoundTag originalAppearance;
	private boolean androidUpgraded;
	private boolean renderKatana;
	private String backWeapon;
	private String scouterItem;
	private String pothalaColor;
	private boolean isPermanentAura;

	public Status() {
		this.isAlive = true;
		this.isHasCreatedCharacter = false;
		this.isAuraActive = false;
		this.isActionCharging = false;
		this.isTailVisible = false;
		this.isDescending = false;
		this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.lastHurtTime = 0;
		this.isStunned = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
		this.drainingTargetId = -1;
		this.isFused = false;
		this.isFusionLeader = false;
		this.fusionPartnerUUID = null;
		this.fusionTimer = 0;
		this.fusionType = "";
		this.originalAppearance = new CompoundTag();
		this.androidUpgraded = false;
		this.renderKatana = false;
		this.backWeapon = "";
		this.scouterItem = "";
		this.pothalaColor = "";
		this.isPermanentAura = false;
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("IsAlive", isAlive);
		tag.putBoolean("HasCreatedChar", isHasCreatedCharacter);
		tag.putBoolean("AuraActive", isAuraActive);
		tag.putBoolean("Transforming", isActionCharging);
		tag.putBoolean("TailVisible", isTailVisible);
		tag.putBoolean("Descending", isDescending);
		tag.putBoolean("InKaioPlanet", isInKaioPlanet);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putBoolean("IsBlocking", isBlocking);
		tag.putLong("LastBlockTime", lastBlockTime);
		tag.putLong("LastHurtTime", lastHurtTime);
		tag.putBoolean("IsStunned", isStunned);
		tag.putInt("SelectedAction", selectedAction.ordinal());
		tag.putString("KiWeaponType", kiWeaponType);
		tag.putInt("DrainingTargetId", drainingTargetId);
		tag.putBoolean("IsFused", isFused);
		tag.putBoolean("IsFusionLeader", isFusionLeader);
		if (fusionPartnerUUID != null) tag.putUUID("FusionPartnerUUID", fusionPartnerUUID);
		tag.putInt("FusionTimer", fusionTimer);
		tag.putString("FusionType", fusionType);
		tag.put("OriginalAppearance", originalAppearance);
		tag.putBoolean("AndroidUpgraded", androidUpgraded);
		tag.putBoolean("RenderKatana", renderKatana);
		tag.putString("BackWeapon", backWeapon);
		tag.putString("ScouterItem", scouterItem);
		tag.putString("PothalaColor", pothalaColor);
		tag.putBoolean("IsPermanentAura", isPermanentAura);
		return tag;
	}

	public void load(CompoundTag tag) {
		this.isAlive = tag.getBoolean("IsAlive");
		this.isHasCreatedCharacter = tag.getBoolean("HasCreatedChar");
		this.isAuraActive = tag.getBoolean("AuraActive");
		this.isActionCharging = tag.getBoolean("Transforming");
		this.isTailVisible = tag.getBoolean("TailVisible");
		this.isDescending = tag.getBoolean("Descending");
		this.isInKaioPlanet = tag.getBoolean("InKaioPlanet");
		this.isChargingKi = tag.getBoolean("IsChargingKi");
		this.isBlocking = tag.getBoolean("IsBlocking");
		this.lastBlockTime = tag.getLong("LastBlockTime");
		this.lastHurtTime = tag.getLong("LastHurtTime");
		this.isStunned = tag.getBoolean("IsStunned");
		if (tag.contains("SelectedAction")) this.selectedAction = ActionMode.values()[tag.getInt("SelectedAction")];
		else this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = tag.getString("KiWeaponType");
		this.drainingTargetId = tag.getInt("DrainingTargetId");
		this.isFused = tag.getBoolean("IsFused");
		this.isFusionLeader = tag.getBoolean("IsFusionLeader");
		if (tag.hasUUID("FusionPartnerUUID")) this.fusionPartnerUUID = tag.getUUID("FusionPartnerUUID");
		else this.fusionPartnerUUID = null;
		this.fusionTimer = tag.getInt("FusionTimer");
		this.fusionType = tag.getString("FusionType");
		if (tag.contains("OriginalAppearance")) this.originalAppearance = tag.getCompound("OriginalAppearance");
		else this.originalAppearance = new CompoundTag();
		this.androidUpgraded = tag.getBoolean("AndroidUpgraded");
		this.renderKatana = tag.getBoolean("RenderKatana");
		this.backWeapon = tag.getString("BackWeapon");
		this.scouterItem = tag.getString("ScouterItem");
		this.pothalaColor = tag.getString("PothalaColor");
		this.isPermanentAura = tag.getBoolean("IsPermanentAura");
	}

	public void copyFrom(Status other) {
		this.isAlive = other.isAlive;
		this.isHasCreatedCharacter = other.isHasCreatedCharacter;
		this.isAuraActive = other.isAuraActive;
		this.isActionCharging = other.isActionCharging;
		this.isTailVisible = other.isTailVisible;
		this.isDescending = other.isDescending;
		this.isInKaioPlanet = other.isInKaioPlanet;
		this.isChargingKi = other.isChargingKi;
		this.isBlocking = other.isBlocking;
		this.lastBlockTime = other.lastBlockTime;
		this.lastHurtTime = other.lastHurtTime;
		this.isStunned = other.isStunned;
		this.selectedAction = other.selectedAction;
		this.kiWeaponType = other.kiWeaponType;
		this.drainingTargetId = other.drainingTargetId;
		this.isFused = other.isFused;
		this.isFusionLeader = other.isFusionLeader;
		this.fusionPartnerUUID = other.fusionPartnerUUID;
		this.fusionTimer = other.fusionTimer;
		this.fusionType = other.fusionType;
		this.originalAppearance = other.originalAppearance.copy();
		this.androidUpgraded = other.androidUpgraded;
		this.renderKatana = other.renderKatana;
		this.backWeapon = other.backWeapon;
		this.pothalaColor = other.pothalaColor;
		this.scouterItem = other.scouterItem;
		this.isPermanentAura = other.isPermanentAura;
	}
}

