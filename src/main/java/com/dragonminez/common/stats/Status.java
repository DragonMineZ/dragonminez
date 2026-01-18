package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

public class Status {
    private boolean isAlive;
    private boolean hasCreatedCharacter;
    private boolean isAuraActive;
    private boolean isTurboActive;
    private boolean isTransforming;
    private boolean isTailVisible;
    private boolean isDescending;
    private boolean isInKaioPlanet;
    private boolean compactMenu;
	private boolean isChargingKi;
	private boolean isKaiokenActive;
	private boolean isInCombat;
	private boolean isBlocking;
	private long lastBlockTime;
	private boolean isStunned;

    public Status() {
        this.isAlive = true;
        this.hasCreatedCharacter = false;
        this.isAuraActive = false;
        this.isTurboActive = false;
        this.isTransforming = false;
        this.isTailVisible = false;
        this.isDescending = false;
        this.isInKaioPlanet = false;
        this.compactMenu = false;
		this.isChargingKi = false;
		this.isKaiokenActive = false;
		this.isInCombat = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.isStunned = false;
    }

    public boolean isAlive() { return isAlive; }
    public boolean hasCreatedCharacter() { return hasCreatedCharacter; }
    public boolean isAuraActive() { return isAuraActive; }
    public boolean isTurboActive() { return isTurboActive; }
    public boolean isTransforming() { return isTransforming; }
    public boolean isTailVisible() { return isTailVisible; }
    public boolean isDescending() { return isDescending; }
    public boolean isInKaioPlanet() { return isInKaioPlanet; }
    public boolean isCompactMenu() { return compactMenu; }
	public boolean isChargingKi() { return isChargingKi; }
	public boolean isKaiokenActive() { return isKaiokenActive; }
	public boolean isInCombat() { return isInCombat; }
	public boolean isBlocking() { return isBlocking; }
	public long getLastBlockTime() { return lastBlockTime; }
	public boolean isStunned() { return isStunned; }

    public void setAlive(boolean alive) { this.isAlive = alive; }
    public void setCreatedCharacter(boolean created) { this.hasCreatedCharacter = created; }
    public void setAuraActive(boolean active) { this.isAuraActive = active; }
    public void setTurboActive(boolean active) { this.isTurboActive = active; }
    public void setTransforming(boolean transforming) { this.isTransforming = transforming; }
    public void setTailVisible(boolean visible) { this.isTailVisible = visible; }
    public void setDescending(boolean descending) { this.isDescending = descending; }
    public void setInKaioPlanet(boolean inKaio) { this.isInKaioPlanet = inKaio; }
    public void setCompactMenu(boolean compact) { this.compactMenu = compact; }
	public void setChargingKi(boolean charging) { this.isChargingKi = charging; }
	public void setKaiokenActive(boolean active) { this.isKaiokenActive = active; }
	public void setInCombat(boolean inCombat) { this.isInCombat = inCombat; }
	public void setBlocking(boolean blocking) { this.isBlocking = blocking; }
	public void setLastBlockTime(long time) { this.lastBlockTime = time; }
	public void setStunned(boolean stunned) { this.isStunned = stunned; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsAlive", isAlive);
        tag.putBoolean("HasCreatedChar", hasCreatedCharacter);
        tag.putBoolean("AuraActive", isAuraActive);
        tag.putBoolean("TurboActive", isTurboActive);
        tag.putBoolean("Transforming", isTransforming);
        tag.putBoolean("TailVisible", isTailVisible);
        tag.putBoolean("Descending", isDescending);
        tag.putBoolean("InKaioPlanet", isInKaioPlanet);
        tag.putBoolean("CompactMenu", compactMenu);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putBoolean("IsKaiokenActive", isKaiokenActive);
		tag.putBoolean("IsInCombat", isInCombat);
		tag.putBoolean("IsBlocking", isBlocking);
		tag.putLong("LastBlockTime", lastBlockTime);
		tag.putBoolean("IsStunned", isStunned);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.isAlive = tag.getBoolean("IsAlive");
        this.hasCreatedCharacter = tag.getBoolean("HasCreatedChar");
        this.isAuraActive = tag.getBoolean("AuraActive");
        this.isTurboActive = tag.getBoolean("TurboActive");
        this.isTransforming = tag.getBoolean("Transforming");
        this.isTailVisible = tag.getBoolean("TailVisible");
        this.isDescending = tag.getBoolean("Descending");
        this.isInKaioPlanet = tag.getBoolean("InKaioPlanet");
        this.compactMenu = tag.getBoolean("CompactMenu");
		this.isChargingKi = tag.getBoolean("IsChargingKi");
		this.isKaiokenActive = tag.getBoolean("IsKaiokenActive");
		this.isInCombat = tag.getBoolean("IsInCombat");
		this.isBlocking = tag.getBoolean("IsBlocking");
		this.lastBlockTime = tag.getLong("LastBlockTime");
		this.isStunned = tag.getBoolean("IsStunned");
    }

    public void copyFrom(Status other) {
        this.isAlive = other.isAlive;
        this.hasCreatedCharacter = other.hasCreatedCharacter;
        this.isAuraActive = other.isAuraActive;
        this.isTurboActive = other.isTurboActive;
        this.isTransforming = other.isTransforming;
        this.isTailVisible = other.isTailVisible;
        this.isDescending = other.isDescending;
        this.isInKaioPlanet = other.isInKaioPlanet;
        this.compactMenu = other.compactMenu;
		this.isChargingKi = other.isChargingKi;
		this.isKaiokenActive = other.isKaiokenActive;
		this.isInCombat = other.isInCombat;
		this.isBlocking = other.isBlocking;
		this.lastBlockTime = other.lastBlockTime;
		this.isStunned = other.isStunned;
    }
}

