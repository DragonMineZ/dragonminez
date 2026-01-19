package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

public class Status {
    private boolean isAlive;
    private boolean hasCreatedCharacter;
    private boolean isAuraActive;
    private boolean isTurboActive;
    private boolean isActionCharging;
    private boolean isTailVisible;
    private boolean isDescending;
    private boolean isInKaioPlanet;
	private boolean isChargingKi;
	private int activeKaiokenPhase;
	private boolean isInCombat;
	private boolean isBlocking;
	private long lastBlockTime;
	private boolean isStunned;
	private ActionMode selectedAction;
	private String kiWeaponType;

    public Status() {
        this.isAlive = true;
        this.hasCreatedCharacter = false;
        this.isAuraActive = false;
        this.isTurboActive = false;
        this.isActionCharging = false;
        this.isTailVisible = false;
        this.isDescending = false;
        this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.activeKaiokenPhase = 0;
		this.isInCombat = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.isStunned = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
    }

    public boolean isAlive() { return isAlive; }
    public boolean hasCreatedCharacter() { return hasCreatedCharacter; }
    public boolean isAuraActive() { return isAuraActive; }
    public boolean isTurboActive() { return isTurboActive; }
    public boolean isActionCharging() { return isActionCharging; }
    public boolean isTailVisible() { return isTailVisible; }
    public boolean isDescending() { return isDescending; }
    public boolean isInKaioPlanet() { return isInKaioPlanet; }
	public boolean isChargingKi() { return isChargingKi; }
	public int getActiveKaiokenPhase() { return activeKaiokenPhase; }
	public boolean isInCombat() { return isInCombat; }
	public boolean isBlocking() { return isBlocking; }
	public long getLastBlockTime() { return lastBlockTime; }
	public boolean isStunned() { return isStunned; }
	public ActionMode getSelectedAction() { return selectedAction; }
	public String getKiWeaponType() { return kiWeaponType; }

    public void setAlive(boolean alive) { this.isAlive = alive; }
    public void setCreatedCharacter(boolean created) { this.hasCreatedCharacter = created; }
    public void setAuraActive(boolean active) { this.isAuraActive = active; }
    public void setTurboActive(boolean active) { this.isTurboActive = active; }
    public void setActionCharging(boolean actionCharging) { this.isActionCharging = actionCharging; }
    public void setTailVisible(boolean visible) { this.isTailVisible = visible; }
    public void setDescending(boolean descending) { this.isDescending = descending; }
    public void setInKaioPlanet(boolean inKaio) { this.isInKaioPlanet = inKaio; }
	public void setChargingKi(boolean charging) { this.isChargingKi = charging; }
	public void setActiveKaiokenPhase(int phase) { this.activeKaiokenPhase = Math.max(0, Math.min(phase, 5)); }
	public void setInCombat(boolean inCombat) { this.isInCombat = inCombat; }
	public void setBlocking(boolean blocking) { this.isBlocking = blocking; }
	public void setLastBlockTime(long time) { this.lastBlockTime = time; }
	public void setStunned(boolean stunned) { this.isStunned = stunned; }
	public void setSelectedAction(ActionMode action) { this.selectedAction = action; }
	public void setKiWeaponType(String type) { this.kiWeaponType = type; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsAlive", isAlive);
        tag.putBoolean("HasCreatedChar", hasCreatedCharacter);
        tag.putBoolean("AuraActive", isAuraActive);
        tag.putBoolean("TurboActive", isTurboActive);
        tag.putBoolean("Transforming", isActionCharging);
        tag.putBoolean("TailVisible", isTailVisible);
        tag.putBoolean("Descending", isDescending);
        tag.putBoolean("InKaioPlanet", isInKaioPlanet);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putInt("ActiveKaiokenPhase", activeKaiokenPhase);
		tag.putBoolean("IsInCombat", isInCombat);
		tag.putBoolean("IsBlocking", isBlocking);
		tag.putLong("LastBlockTime", lastBlockTime);
		tag.putBoolean("IsStunned", isStunned);
		tag.putInt("SelectedAction", selectedAction.ordinal());
		tag.putString("KiWeaponType", kiWeaponType);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.isAlive = tag.getBoolean("IsAlive");
        this.hasCreatedCharacter = tag.getBoolean("HasCreatedChar");
        this.isAuraActive = tag.getBoolean("AuraActive");
        this.isTurboActive = tag.getBoolean("TurboActive");
        this.isActionCharging = tag.getBoolean("Transforming");
        this.isTailVisible = tag.getBoolean("TailVisible");
        this.isDescending = tag.getBoolean("Descending");
        this.isInKaioPlanet = tag.getBoolean("InKaioPlanet");
		this.isChargingKi = tag.getBoolean("IsChargingKi");
		this.activeKaiokenPhase = tag.getInt("ActiveKaiokenPhase");
		this.isInCombat = tag.getBoolean("IsInCombat");
		this.isBlocking = tag.getBoolean("IsBlocking");
		this.lastBlockTime = tag.getLong("LastBlockTime");
		this.isStunned = tag.getBoolean("IsStunned");
		if (tag.contains("SelectedAction")) {
			this.selectedAction = ActionMode.values()[tag.getInt("SelectedAction")];
		} else {
			this.selectedAction = ActionMode.FORM;
		}
		this.kiWeaponType = tag.getString("KiWeaponType");
    }

    public void copyFrom(Status other) {
        this.isAlive = other.isAlive;
        this.hasCreatedCharacter = other.hasCreatedCharacter;
        this.isAuraActive = other.isAuraActive;
        this.isTurboActive = other.isTurboActive;
        this.isActionCharging = other.isActionCharging;
        this.isTailVisible = other.isTailVisible;
        this.isDescending = other.isDescending;
        this.isInKaioPlanet = other.isInKaioPlanet;
		this.isChargingKi = other.isChargingKi;
		this.activeKaiokenPhase = other.activeKaiokenPhase;
		this.isInCombat = other.isInCombat;
		this.isBlocking = other.isBlocking;
		this.lastBlockTime = other.lastBlockTime;
		this.isStunned = other.isStunned;
		this.selectedAction = other.selectedAction;
		this.kiWeaponType = other.kiWeaponType;
    }
}

