package com.dragonminez.common.stats;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class Status {
    private boolean isAlive;
    private boolean hasCreatedCharacter;
    private boolean isAuraActive;
    private boolean isActionCharging;
    private boolean isTailVisible;
    private boolean isDescending;
    private boolean isInKaioPlanet;
	private boolean isChargingKi;
	private int activeKaiokenPhase;
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

    public Status() {
        this.isAlive = true;
        this.hasCreatedCharacter = false;
        this.isAuraActive = false;
        this.isActionCharging = false;
        this.isTailVisible = false;
        this.isDescending = false;
        this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.activeKaiokenPhase = 0;
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
    }

    public boolean isAlive() { return isAlive; }
    public boolean hasCreatedCharacter() { return hasCreatedCharacter; }
    public boolean isAuraActive() { return isAuraActive; }
    public boolean isActionCharging() { return isActionCharging; }
    public boolean isTailVisible() { return isTailVisible; }
    public boolean isDescending() { return isDescending; }
    public boolean isInKaioPlanet() { return isInKaioPlanet; }
	public boolean isChargingKi() { return isChargingKi; }
	public int getActiveKaiokenPhase() { return activeKaiokenPhase; }
	public boolean isBlocking() { return isBlocking; }
	public long getLastBlockTime() { return lastBlockTime; }
	public long getLastHurtTime() { return lastHurtTime; }
	public boolean isStunned() { return isStunned; }
	public ActionMode getSelectedAction() { return selectedAction; }
	public String getKiWeaponType() { return kiWeaponType; }
	public int getDrainingTargetId() { return drainingTargetId; }
	public boolean isFused() { return isFused; }
	public boolean isFusionLeader() { return isFusionLeader; }
	public UUID getFusionPartnerUUID() { return fusionPartnerUUID; }
	public int getFusionTimer() { return fusionTimer; }
	public String getFusionType() { return fusionType; }
	public CompoundTag getOriginalAppearance() { return originalAppearance; }
	public boolean isAndroidUpgraded() { return androidUpgraded; }
	public boolean isRenderKatana() { return renderKatana; }
	public String getBackWeapon() { return backWeapon; }
	public String getScouterItem() { return scouterItem; }
	public String getPothalaColor() { return pothalaColor; }

    public void setAlive(boolean alive) { this.isAlive = alive; }
    public void setCreatedCharacter(boolean created) { this.hasCreatedCharacter = created; }
    public void setAuraActive(boolean active) { this.isAuraActive = active; }
    public void setActionCharging(boolean actionCharging) { this.isActionCharging = actionCharging; }
    public void setTailVisible(boolean visible) { this.isTailVisible = visible; }
    public void setDescending(boolean descending) { this.isDescending = descending; }
    public void setInKaioPlanet(boolean inKaio) { this.isInKaioPlanet = inKaio; }
	public void setChargingKi(boolean charging) { this.isChargingKi = charging; }
	public void setActiveKaiokenPhase(int phase) { this.activeKaiokenPhase = Math.max(0, phase); }
	public void setBlocking(boolean blocking) { this.isBlocking = blocking; }
	public void setLastBlockTime(long time) { this.lastBlockTime = time; }
	public void setLastHurtTime(long time) { this.lastHurtTime = time; }
	public void setStunned(boolean stunned) { this.isStunned = stunned; }
	public void setSelectedAction(ActionMode action) { this.selectedAction = action; }
	public void setKiWeaponType(String type) { this.kiWeaponType = type; }
	public void setDrainingTargetId(int id) { this.drainingTargetId = id; }
	public void setFused(boolean fused) { this.isFused = fused; }
	public void setFusionLeader(boolean leader) { this.isFusionLeader = leader; }
	public void setFusionPartnerUUID(UUID uuid) { this.fusionPartnerUUID = uuid; }
	public void setFusionTimer(int timer) { this.fusionTimer = timer; }
	public void setFusionType(String type) { this.fusionType = type; }
	public void setOriginalAppearance(CompoundTag tag) { this.originalAppearance = tag; }
	public void setAndroidUpgraded(boolean upgraded) { this.androidUpgraded = upgraded; }
	public void setRenderKatana(boolean render) { this.renderKatana = render; }
	public void setBackWeapon(String weapon) { this.backWeapon = weapon; }
	public void setScouterItem(String item) { this.scouterItem = item; }
	public void setPothalaColor(String color) { this.pothalaColor = color; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsAlive", isAlive);
        tag.putBoolean("HasCreatedChar", hasCreatedCharacter);
        tag.putBoolean("AuraActive", isAuraActive);
        tag.putBoolean("Transforming", isActionCharging);
        tag.putBoolean("TailVisible", isTailVisible);
        tag.putBoolean("Descending", isDescending);
        tag.putBoolean("InKaioPlanet", isInKaioPlanet);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putInt("ActiveKaiokenPhase", activeKaiokenPhase);
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
        return tag;
    }

    public void load(CompoundTag tag) {
        this.isAlive = tag.getBoolean("IsAlive");
        this.hasCreatedCharacter = tag.getBoolean("HasCreatedChar");
        this.isAuraActive = tag.getBoolean("AuraActive");
        this.isActionCharging = tag.getBoolean("Transforming");
        this.isTailVisible = tag.getBoolean("TailVisible");
        this.isDescending = tag.getBoolean("Descending");
        this.isInKaioPlanet = tag.getBoolean("InKaioPlanet");
		this.isChargingKi = tag.getBoolean("IsChargingKi");
		this.activeKaiokenPhase = tag.getInt("ActiveKaiokenPhase");
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
    }

    public void copyFrom(Status other) {
        this.isAlive = other.isAlive;
        this.hasCreatedCharacter = other.hasCreatedCharacter;
        this.isAuraActive = other.isAuraActive;
        this.isActionCharging = other.isActionCharging;
        this.isTailVisible = other.isTailVisible;
        this.isDescending = other.isDescending;
        this.isInKaioPlanet = other.isInKaioPlanet;
		this.isChargingKi = other.isChargingKi;
		this.activeKaiokenPhase = other.activeKaiokenPhase;
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
    }
}

