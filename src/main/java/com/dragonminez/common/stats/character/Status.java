package com.dragonminez.common.stats.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.extras.ActionMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class Status {
	public static final int FLIGHT_SEARCH = 0;
	public static final int FLIGHT_COMBAT = 1;
	public static final int GOD_RITUAL_NONE = 0;
	public static final int GOD_RITUAL_DONOR = 1;
	public static final int GOD_RITUAL_RECIPIENT = 2;
	private boolean isAlive;
	private boolean forceHalo;
	private int tempReturnTimer;
	private int tempReturnsUsed;
	private long tempReturnReadyAt;
	private int deathCount;
	private boolean isHasCreatedCharacter;
	private boolean isAuraActive;
	private boolean isActionCharging;
	private boolean isTailVisible;
	private boolean isDescending;
	private boolean isInKaioPlanet;
	private boolean isChargingKi;
	private boolean kiBurstArmed;
	private boolean surgeActive;
	private boolean rageActive;
	private boolean isBlocking;
	private long lastBlockTime;
	private long lastHurtTime;
	private boolean friendlyFistEnabled;
	private boolean stunEffect;
	private boolean isKnockedDown;
	private ActionMode selectedAction;
	private String kiWeaponType;
	private int drainingTargetId;
	private boolean isFused;
	private boolean isFusionLeader;
	private UUID fusionPartnerUUID;
	private int fusionTimer;
	private String fusionType;
	private String fusionName;
	private boolean fusionPartyManaged;
	private UUID fusionPrevPartyId;
	private boolean fusionPrevPartyLeader;
	private int potaraPoseTimer;
	private int evasionLockTicks;
	private UUID potaraPartnerUUID;
	private boolean potaraLeader;
	private boolean potaraBeetle;
	private CompoundTag originalAppearance;
	private boolean androidUpgraded;
	private boolean renderKatana;
	private String backWeapon;
	private String scouterItem;
	private String pothalaColor;
	private boolean isPermanentAura;
	private boolean forcedAura;
	private boolean forcedCharge;
	private boolean isStrikeLocked;
	private boolean matchFrozen;
	private boolean sleepRecovering;
	private int flightMode;
	private boolean flightModeLocked;
	private int lockedFlightMode;
	private final Set<String> visitedDimensions;
	private UUID activeShadowDummyUUID;
	private int shadowDummyPercent;
	private int shadowDummyKillCount;
	private int kiTransferTarget;
	private int kiTransferDonors;
	private int kiDrainStacks;
	private int kiDrainTicks;
	private int godRitualRole;
	private UUID godRitualAnchor;
	private int godRitualTicks;
	private long godRitualGlowStart;

	public Status() {
		this.isAlive = true;
		this.forceHalo = false;
		this.tempReturnTimer = 0;
		this.tempReturnsUsed = 0;
		this.tempReturnReadyAt = 0L;
		this.deathCount = 0;
		this.isHasCreatedCharacter = false;
		this.isAuraActive = false;
		this.isActionCharging = false;
		this.isTailVisible = true;
		this.isDescending = false;
		this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.kiBurstArmed = false;
		this.surgeActive = false;
		this.rageActive = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.lastHurtTime = 0;
		this.friendlyFistEnabled = false;
		this.stunEffect = false;
		this.isKnockedDown = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
		this.drainingTargetId = -1;
		this.isFused = false;
		this.isFusionLeader = false;
		this.fusionPartnerUUID = null;
		this.fusionTimer = 0;
		this.fusionType = "";
		this.fusionName = "";
		this.fusionPartyManaged = false;
		this.fusionPrevPartyId = null;
		this.fusionPrevPartyLeader = false;
		this.potaraPoseTimer = 0;
		this.evasionLockTicks = 0;
		this.potaraPartnerUUID = null;
		this.potaraLeader = false;
		this.potaraBeetle = false;
		this.originalAppearance = new CompoundTag();
		this.androidUpgraded = false;
		this.renderKatana = false;
		this.backWeapon = "";
		this.scouterItem = "";
		this.pothalaColor = "";
		this.isPermanentAura = false;
		this.forcedAura = false;
		this.forcedCharge = false;
		this.isStrikeLocked = false;
		this.matchFrozen = false;
		this.sleepRecovering = false;
		this.flightMode = FLIGHT_SEARCH;
		this.flightModeLocked = false;
		this.lockedFlightMode = FLIGHT_SEARCH;
		this.visitedDimensions = new LinkedHashSet<>();
		this.activeShadowDummyUUID = null;
		this.shadowDummyPercent = 0;
		this.shadowDummyKillCount = 0;
		this.kiTransferTarget = -1;
		this.kiTransferDonors = 0;
		this.kiDrainStacks = 0;
		this.kiDrainTicks = 0;
		this.godRitualRole = GOD_RITUAL_NONE;
		this.godRitualAnchor = null;
		this.godRitualTicks = 0;
		this.godRitualGlowStart = 0L;
	}

	public void reset() {
		this.isAlive = true;
		this.forceHalo = false;
		this.tempReturnTimer = 0;
		this.tempReturnsUsed = 0;
		this.tempReturnReadyAt = 0L;
		this.deathCount = 0;
		this.isHasCreatedCharacter = false;
		this.isAuraActive = false;
		this.isActionCharging = false;
		this.isTailVisible = true;
		this.isDescending = false;
		this.isInKaioPlanet = false;
		this.isChargingKi = false;
		this.kiBurstArmed = false;
		this.surgeActive = false;
		this.rageActive = false;
		this.isBlocking = false;
		this.lastBlockTime = 0;
		this.lastHurtTime = 0;
		this.friendlyFistEnabled = false;
		this.stunEffect = false;
		this.isKnockedDown = false;
		this.selectedAction = ActionMode.FORM;
		this.kiWeaponType = "blade";
		this.drainingTargetId = -1;
		this.isFused = false;
		this.isFusionLeader = false;
		this.fusionPartnerUUID = null;
		this.fusionTimer = 0;
		this.fusionType = "";
		this.fusionName = "";
		this.fusionPartyManaged = false;
		this.fusionPrevPartyId = null;
		this.fusionPrevPartyLeader = false;
		this.potaraPoseTimer = 0;
		this.evasionLockTicks = 0;
		this.potaraPartnerUUID = null;
		this.potaraLeader = false;
		this.potaraBeetle = false;
		this.originalAppearance = new CompoundTag();
		this.androidUpgraded = false;
		this.renderKatana = false;
		this.backWeapon = "";
		this.scouterItem = "";
		this.pothalaColor = "";
		this.isPermanentAura = false;
		this.forcedAura = false;
		this.forcedCharge = false;
		this.isStrikeLocked = false;
		this.matchFrozen = false;
		this.sleepRecovering = false;
		this.flightMode = FLIGHT_SEARCH;
		this.flightModeLocked = false;
		this.lockedFlightMode = FLIGHT_SEARCH;
		this.visitedDimensions.clear();
		this.activeShadowDummyUUID = null;
		this.shadowDummyPercent = 0;
		this.shadowDummyKillCount = 0;
		this.kiTransferTarget = -1;
		this.kiTransferDonors = 0;
		this.kiDrainStacks = 0;
		this.kiDrainTicks = 0;
		this.godRitualRole = GOD_RITUAL_NONE;
		this.godRitualAnchor = null;
		this.godRitualTicks = 0;
		this.godRitualGlowStart = 0L;
	}

	public boolean isStunned() {
		return stunEffect || isKnockedDown || isStrikeLocked || matchFrozen || sleepRecovering || isInGodRitual();
	}

	public boolean isSharingKi() {
		return kiTransferTarget >= 0;
	}

	public boolean isInGodRitual() {
		return godRitualRole != GOD_RITUAL_NONE;
	}

	public boolean isKiShareLocked() {
		return isSharingKi() || isInGodRitual();
	}

	public void validateKiWeaponType() {
		var types = ConfigManager.getCombatConfig().getKiWeaponTypes();
		if (types.isEmpty()) return;
		if (kiWeaponType == null || !types.contains(kiWeaponType.toLowerCase())) kiWeaponType = types.get(0);
	}

	public boolean hasActiveShadowDummy() {
		return activeShadowDummyUUID != null;
	}

	public void markVisitedDimension(String dimensionId) {
		if (dimensionId == null || dimensionId.isBlank() || ResourceLocation.tryParse(dimensionId) == null) return;
		this.visitedDimensions.add(dimensionId);
	}

	public boolean hasVisitedDimension(String dimensionId) {
		return dimensionId != null && this.visitedDimensions.contains(dimensionId);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("IsAlive", isAlive);
		tag.putBoolean("ForceHalo", forceHalo);
		tag.putInt("TempReturnTimer", tempReturnTimer);
		tag.putInt("TempReturnsUsed", tempReturnsUsed);
		tag.putLong("TempReturnReadyAt", tempReturnReadyAt);
		tag.putInt("DeathCount", deathCount);
		tag.putBoolean("HasCreatedChar", isHasCreatedCharacter);
		tag.putBoolean("AuraActive", isAuraActive);
		tag.putBoolean("Transforming", isActionCharging);
		tag.putBoolean("TailVisible", isTailVisible);
		tag.putBoolean("Descending", isDescending);
		tag.putBoolean("InKaioPlanet", isInKaioPlanet);
		tag.putBoolean("IsChargingKi", isChargingKi);
		tag.putBoolean("KiBurstArmed", kiBurstArmed);
		tag.putBoolean("SurgeActive", surgeActive);
		tag.putBoolean("RageActive", rageActive);
		tag.putBoolean("IsBlocking", isBlocking);
		tag.putLong("LastBlockTime", lastBlockTime);
		tag.putLong("LastHurtTime", lastHurtTime);
		tag.putBoolean("FriendlyFistEnabled", friendlyFistEnabled);
		tag.putBoolean("IsStunned", stunEffect);
		tag.putBoolean("IsKnockedDown", isKnockedDown);
		tag.putInt("SelectedAction", selectedAction.ordinal());
		tag.putString("KiWeaponType", kiWeaponType);
		tag.putInt("DrainingTargetId", drainingTargetId);
		tag.putBoolean("IsFused", isFused);
		tag.putBoolean("IsFusionLeader", isFusionLeader);
		if (fusionPartnerUUID != null) tag.putUUID("FusionPartnerUUID", fusionPartnerUUID);
		tag.putInt("FusionTimer", fusionTimer);
		tag.putString("FusionType", fusionType);
		tag.putString("FusionName", fusionName);
		tag.putBoolean("FusionPartyManaged", fusionPartyManaged);
		if (fusionPrevPartyId != null) tag.putUUID("FusionPrevPartyId", fusionPrevPartyId);
		tag.putBoolean("FusionPrevPartyLeader", fusionPrevPartyLeader);
		tag.putInt("PotaraPoseTimer", potaraPoseTimer);
		tag.putInt("EvasionLockTicks", evasionLockTicks);
		if (potaraPartnerUUID != null) tag.putUUID("PotaraPartnerUUID", potaraPartnerUUID);
		tag.putBoolean("PotaraLeader", potaraLeader);
		tag.putBoolean("PotaraBeetle", potaraBeetle);
		tag.put("OriginalAppearance", originalAppearance);
		tag.putBoolean("AndroidUpgraded", androidUpgraded);
		tag.putBoolean("RenderKatana", renderKatana);
		tag.putString("BackWeapon", backWeapon);
		tag.putString("ScouterItem", scouterItem);
		tag.putString("PothalaColor", pothalaColor);
		tag.putBoolean("IsPermanentAura", isPermanentAura);
		tag.putBoolean("ForcedAura", forcedAura);
		tag.putBoolean("ForcedCharge", forcedCharge);
		tag.putBoolean("IsStrikeLocked", isStrikeLocked);
		tag.putBoolean("MatchFrozen", matchFrozen);
		tag.putBoolean("SleepRecovering", sleepRecovering);
		tag.putInt("FlightMode", flightMode);
		tag.putBoolean("FlightModeLocked", flightModeLocked);
		tag.putInt("LockedFlightMode", lockedFlightMode);
		ListTag visitedDimensionsTag = new ListTag();
		for (String dimensionId : visitedDimensions) visitedDimensionsTag.add(StringTag.valueOf(dimensionId));
		tag.put("VisitedDimensions", visitedDimensionsTag);
		if (activeShadowDummyUUID != null) tag.putUUID("ActiveShadowDummyUUID", activeShadowDummyUUID);
		tag.putInt("ShadowDummyPercent", shadowDummyPercent);
		tag.putInt("ShadowDummyKillCount", shadowDummyKillCount);
		tag.putInt("KiTransferTarget", kiTransferTarget);
		tag.putInt("KiTransferDonors", kiTransferDonors);
		tag.putInt("KiDrainStacks", kiDrainStacks);
		tag.putInt("KiDrainTicks", kiDrainTicks);
		tag.putInt("GodRitualRole", godRitualRole);
		if (godRitualAnchor != null) tag.putUUID("GodRitualAnchor", godRitualAnchor);
		tag.putInt("GodRitualTicks", godRitualTicks);
		tag.putLong("GodRitualGlowStart", godRitualGlowStart);
		return tag;
	}

	public void load(CompoundTag tag) {
		this.isAlive = tag.getBoolean("IsAlive");
		this.forceHalo = tag.getBoolean("ForceHalo");
		this.tempReturnTimer = tag.getInt("TempReturnTimer");
		this.tempReturnsUsed = tag.getInt("TempReturnsUsed");
		this.tempReturnReadyAt = tag.getLong("TempReturnReadyAt");
		this.deathCount = tag.getInt("DeathCount");
		this.isHasCreatedCharacter = tag.getBoolean("HasCreatedChar");
		this.isAuraActive = tag.getBoolean("AuraActive");
		this.isActionCharging = tag.getBoolean("Transforming");
		this.isTailVisible = tag.getBoolean("TailVisible");
		this.isDescending = tag.getBoolean("Descending");
		this.isInKaioPlanet = tag.getBoolean("InKaioPlanet");
		this.isChargingKi = tag.getBoolean("IsChargingKi");
		this.kiBurstArmed = tag.getBoolean("KiBurstArmed");
		this.surgeActive = tag.getBoolean("SurgeActive");
		this.rageActive = tag.getBoolean("RageActive");
		this.isBlocking = tag.getBoolean("IsBlocking");
		this.lastBlockTime = tag.getLong("LastBlockTime");
		this.lastHurtTime = tag.getLong("LastHurtTime");
		this.friendlyFistEnabled = tag.getBoolean("FriendlyFistEnabled");
		this.stunEffect = tag.getBoolean("IsStunned");
		this.isKnockedDown = tag.getBoolean("IsKnockedDown");
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
		this.fusionName = tag.getString("FusionName");
		this.fusionPartyManaged = tag.getBoolean("FusionPartyManaged");
		this.fusionPrevPartyId = tag.hasUUID("FusionPrevPartyId") ? tag.getUUID("FusionPrevPartyId") : null;
		this.fusionPrevPartyLeader = tag.getBoolean("FusionPrevPartyLeader");
		this.potaraPoseTimer = tag.getInt("PotaraPoseTimer");
		this.evasionLockTicks = tag.getInt("EvasionLockTicks");
		this.potaraPartnerUUID = tag.hasUUID("PotaraPartnerUUID") ? tag.getUUID("PotaraPartnerUUID") : null;
		this.potaraLeader = tag.getBoolean("PotaraLeader");
		this.potaraBeetle = tag.getBoolean("PotaraBeetle");
		if (tag.contains("OriginalAppearance")) this.originalAppearance = tag.getCompound("OriginalAppearance");
		 else this.originalAppearance = new CompoundTag();
		this.androidUpgraded = tag.getBoolean("AndroidUpgraded");
		this.renderKatana = tag.getBoolean("RenderKatana");
		this.backWeapon = tag.getString("BackWeapon");
		this.scouterItem = tag.getString("ScouterItem");
		this.pothalaColor = tag.getString("PothalaColor");
		this.isPermanentAura = tag.getBoolean("IsPermanentAura");
		this.forcedAura = tag.getBoolean("ForcedAura");
		this.forcedCharge = tag.getBoolean("ForcedCharge");
		this.isStrikeLocked = tag.getBoolean("IsStrikeLocked");
		this.matchFrozen = tag.getBoolean("MatchFrozen");
		this.sleepRecovering = tag.getBoolean("SleepRecovering");
		this.flightMode = tag.getInt("FlightMode");
		this.flightModeLocked = tag.getBoolean("FlightModeLocked");
		this.lockedFlightMode = tag.getInt("LockedFlightMode");
		this.visitedDimensions.clear();
		if (tag.contains("VisitedDimensions", Tag.TAG_LIST)) {
			ListTag visitedDimensionsTag = tag.getList("VisitedDimensions", Tag.TAG_STRING);
			for (Tag dimensionTag : visitedDimensionsTag) this.markVisitedDimension(dimensionTag.getAsString());
		}
		this.activeShadowDummyUUID = tag.hasUUID("ActiveShadowDummyUUID") ? tag.getUUID("ActiveShadowDummyUUID") : null;
		this.shadowDummyPercent = tag.getInt("ShadowDummyPercent");
		this.shadowDummyKillCount = tag.contains("ShadowDummyKillCount") ? tag.getInt("ShadowDummyKillCount") : 0;
		this.kiTransferTarget = tag.contains("KiTransferTarget") ? tag.getInt("KiTransferTarget") : -1;
		this.kiTransferDonors = tag.getInt("KiTransferDonors");
		this.kiDrainStacks = tag.getInt("KiDrainStacks");
		this.kiDrainTicks = tag.getInt("KiDrainTicks");
		this.godRitualRole = tag.getInt("GodRitualRole");
		this.godRitualAnchor = tag.hasUUID("GodRitualAnchor") ? tag.getUUID("GodRitualAnchor") : null;
		this.godRitualTicks = tag.getInt("GodRitualTicks");
		this.godRitualGlowStart = tag.getLong("GodRitualGlowStart");
	}

	public void copyFrom(Status other) {
		this.isAlive = other.isAlive;
		this.forceHalo = other.forceHalo;
		this.tempReturnTimer = other.tempReturnTimer;
		this.tempReturnsUsed = other.tempReturnsUsed;
		this.tempReturnReadyAt = other.tempReturnReadyAt;
		this.deathCount = other.deathCount;
		this.isHasCreatedCharacter = other.isHasCreatedCharacter;
		this.isAuraActive = other.isAuraActive;
		this.isActionCharging = other.isActionCharging;
		this.isTailVisible = other.isTailVisible;
		this.isDescending = other.isDescending;
		this.isInKaioPlanet = other.isInKaioPlanet;
		this.isChargingKi = other.isChargingKi;
		this.kiBurstArmed = other.kiBurstArmed;
		this.surgeActive = other.surgeActive;
		this.rageActive = other.rageActive;
		this.isBlocking = other.isBlocking;
		this.lastBlockTime = other.lastBlockTime;
		this.lastHurtTime = other.lastHurtTime;
		this.friendlyFistEnabled = other.friendlyFistEnabled;
		this.stunEffect = other.stunEffect;
		this.isKnockedDown = other.isKnockedDown;
		this.selectedAction = other.selectedAction;
		this.kiWeaponType = other.kiWeaponType;
		this.drainingTargetId = other.drainingTargetId;
		this.isFused = other.isFused;
		this.isFusionLeader = other.isFusionLeader;
		this.fusionPartnerUUID = other.fusionPartnerUUID;
		this.fusionTimer = other.fusionTimer;
		this.fusionType = other.fusionType;
		this.fusionName = other.fusionName;
		this.fusionPartyManaged = other.fusionPartyManaged;
		this.fusionPrevPartyId = other.fusionPrevPartyId;
		this.fusionPrevPartyLeader = other.fusionPrevPartyLeader;
		this.potaraPoseTimer = other.potaraPoseTimer;
		this.evasionLockTicks = other.evasionLockTicks;
		this.potaraPartnerUUID = other.potaraPartnerUUID;
		this.potaraLeader = other.potaraLeader;
		this.potaraBeetle = other.potaraBeetle;
		this.originalAppearance = other.originalAppearance.copy();
		this.androidUpgraded = other.androidUpgraded;
		this.renderKatana = other.renderKatana;
		this.backWeapon = other.backWeapon;
		this.pothalaColor = other.pothalaColor;
		this.scouterItem = other.scouterItem;
		this.isPermanentAura = other.isPermanentAura;
		this.forcedAura = other.forcedAura;
		this.forcedCharge = other.forcedCharge;
		this.isStrikeLocked = other.isStrikeLocked;
		this.matchFrozen = other.matchFrozen;
		this.sleepRecovering = other.sleepRecovering;
		this.flightMode = other.flightMode;
		this.flightModeLocked = other.flightModeLocked;
		this.lockedFlightMode = other.lockedFlightMode;
		this.visitedDimensions.clear();
		this.visitedDimensions.addAll(other.visitedDimensions);
		this.activeShadowDummyUUID = other.activeShadowDummyUUID;
		this.shadowDummyPercent = other.shadowDummyPercent;
		this.shadowDummyKillCount = other.shadowDummyKillCount;
		this.kiTransferTarget = other.kiTransferTarget;
		this.kiTransferDonors = other.kiTransferDonors;
		this.kiDrainStacks = other.kiDrainStacks;
		this.kiDrainTicks = other.kiDrainTicks;
		this.godRitualRole = other.godRitualRole;
		this.godRitualAnchor = other.godRitualAnchor;
		this.godRitualTicks = other.godRitualTicks;
		this.godRitualGlowStart = other.godRitualGlowStart;
	}

	public boolean isAlive() {
		return this.isAlive;
	}

	public boolean isForceHalo() {
		return this.forceHalo;
	}

	public int getTempReturnTimer() {
		return this.tempReturnTimer;
	}

	public int getTempReturnsUsed() {
		return this.tempReturnsUsed;
	}

	public long getTempReturnReadyAt() {
		return this.tempReturnReadyAt;
	}

	public int getDeathCount() {
		return this.deathCount;
	}

	public boolean isHasCreatedCharacter() {
		return this.isHasCreatedCharacter;
	}

	public boolean isAuraActive() {
		return this.isAuraActive;
	}

	public boolean isActionCharging() {
		return this.isActionCharging;
	}

	public boolean isTailVisible() {
		return this.isTailVisible;
	}

	public boolean isDescending() {
		return this.isDescending;
	}

	public boolean isInKaioPlanet() {
		return this.isInKaioPlanet;
	}

	public boolean isChargingKi() {
		return this.isChargingKi;
	}

	public boolean isKiBurstArmed() {
		return this.kiBurstArmed;
	}

	public boolean isSurgeActive() {
		return this.surgeActive;
	}

	public boolean isRageActive() {
		return this.rageActive;
	}

	public boolean isBlocking() {
		return this.isBlocking;
	}

	public long getLastBlockTime() {
		return this.lastBlockTime;
	}

	public long getLastHurtTime() {
		return this.lastHurtTime;
	}

	public boolean isFriendlyFistEnabled() {
		return this.friendlyFistEnabled;
	}

	public boolean isStunEffect() {
		return this.stunEffect;
	}

	public boolean isKnockedDown() {
		return this.isKnockedDown;
	}

	public ActionMode getSelectedAction() {
		return this.selectedAction;
	}

	public String getKiWeaponType() {
		return this.kiWeaponType;
	}

	public int getDrainingTargetId() {
		return this.drainingTargetId;
	}

	public boolean isFused() {
		return this.isFused;
	}

	public boolean isFusionLeader() {
		return this.isFusionLeader;
	}

	public UUID getFusionPartnerUUID() {
		return this.fusionPartnerUUID;
	}

	public int getFusionTimer() {
		return this.fusionTimer;
	}

	public String getFusionType() {
		return this.fusionType;
	}

	public String getFusionName() {
		return this.fusionName;
	}

	public boolean isFusionPartyManaged() {
		return this.fusionPartyManaged;
	}

	public UUID getFusionPrevPartyId() {
		return this.fusionPrevPartyId;
	}

	public boolean isFusionPrevPartyLeader() {
		return this.fusionPrevPartyLeader;
	}

	public int getPotaraPoseTimer() {
		return this.potaraPoseTimer;
	}

	public int getEvasionLockTicks() {
		return this.evasionLockTicks;
	}

	public UUID getPotaraPartnerUUID() {
		return this.potaraPartnerUUID;
	}

	public boolean isPotaraLeader() {
		return this.potaraLeader;
	}

	public boolean isPotaraBeetle() {
		return this.potaraBeetle;
	}

	public CompoundTag getOriginalAppearance() {
		return this.originalAppearance;
	}

	public boolean isAndroidUpgraded() {
		return this.androidUpgraded;
	}

	public boolean isRenderKatana() {
		return this.renderKatana;
	}

	public String getBackWeapon() {
		return this.backWeapon;
	}

	public String getScouterItem() {
		return this.scouterItem;
	}

	public String getPothalaColor() {
		return this.pothalaColor;
	}

	public boolean isPermanentAura() {
		return this.isPermanentAura;
	}

	public boolean isForcedAura() {
		return this.forcedAura;
	}

	public boolean isForcedCharge() {
		return this.forcedCharge;
	}

	public boolean isStrikeLocked() {
		return this.isStrikeLocked;
	}

	public boolean isMatchFrozen() {
		return this.matchFrozen;
	}

	public boolean isSleepRecovering() {
		return this.sleepRecovering;
	}

	public int getFlightMode() {
		return this.flightMode;
	}

	public boolean isFlightModeLocked() {
		return this.flightModeLocked;
	}

	public int getLockedFlightMode() {
		return this.lockedFlightMode;
	}

	public Set<String> getVisitedDimensions() {
		return this.visitedDimensions;
	}

	public UUID getActiveShadowDummyUUID() {
		return this.activeShadowDummyUUID;
	}

	public int getShadowDummyPercent() {
		return this.shadowDummyPercent;
	}

	public int getShadowDummyKillCount() {
		return this.shadowDummyKillCount;
	}

	public int getKiTransferTarget() {
		return this.kiTransferTarget;
	}

	public int getKiTransferDonors() {
		return this.kiTransferDonors;
	}

	public int getKiDrainStacks() {
		return this.kiDrainStacks;
	}

	public int getKiDrainTicks() {
		return this.kiDrainTicks;
	}

	public int getGodRitualRole() {
		return this.godRitualRole;
	}

	public UUID getGodRitualAnchor() {
		return this.godRitualAnchor;
	}

	public int getGodRitualTicks() {
		return this.godRitualTicks;
	}

	public long getGodRitualGlowStart() {
		return this.godRitualGlowStart;
	}

	public void setAlive(final boolean isAlive) {
		this.isAlive = isAlive;
	}

	public void setForceHalo(final boolean forceHalo) {
		this.forceHalo = forceHalo;
	}

	public void setTempReturnTimer(final int tempReturnTimer) {
		this.tempReturnTimer = tempReturnTimer;
	}

	public void setTempReturnsUsed(final int tempReturnsUsed) {
		this.tempReturnsUsed = tempReturnsUsed;
	}

	public void setTempReturnReadyAt(final long tempReturnReadyAt) {
		this.tempReturnReadyAt = tempReturnReadyAt;
	}

	public void setDeathCount(final int deathCount) {
		this.deathCount = deathCount;
	}

	public void setHasCreatedCharacter(final boolean isHasCreatedCharacter) {
		this.isHasCreatedCharacter = isHasCreatedCharacter;
	}

	public void setAuraActive(final boolean isAuraActive) {
		this.isAuraActive = isAuraActive;
	}

	public void setActionCharging(final boolean isActionCharging) {
		this.isActionCharging = isActionCharging;
	}

	public void setTailVisible(final boolean isTailVisible) {
		this.isTailVisible = isTailVisible;
	}

	public void setDescending(final boolean isDescending) {
		this.isDescending = isDescending;
	}

	public void setInKaioPlanet(final boolean isInKaioPlanet) {
		this.isInKaioPlanet = isInKaioPlanet;
	}

	public void setChargingKi(final boolean isChargingKi) {
		this.isChargingKi = isChargingKi;
	}

	public void setKiBurstArmed(final boolean kiBurstArmed) {
		this.kiBurstArmed = kiBurstArmed;
	}

	public void setSurgeActive(final boolean surgeActive) {
		this.surgeActive = surgeActive;
	}

	public void setRageActive(final boolean rageActive) {
		this.rageActive = rageActive;
	}

	public void setBlocking(final boolean isBlocking) {
		this.isBlocking = isBlocking;
	}

	public void setLastBlockTime(final long lastBlockTime) {
		this.lastBlockTime = lastBlockTime;
	}

	public void setLastHurtTime(final long lastHurtTime) {
		this.lastHurtTime = lastHurtTime;
	}

	public void setFriendlyFistEnabled(final boolean friendlyFistEnabled) {
		this.friendlyFistEnabled = friendlyFistEnabled;
	}

	public void setStunEffect(final boolean stunEffect) {
		this.stunEffect = stunEffect;
	}

	public void setKnockedDown(final boolean isKnockedDown) {
		this.isKnockedDown = isKnockedDown;
	}

	public void setSelectedAction(final ActionMode selectedAction) {
		this.selectedAction = selectedAction;
	}

	public void setKiWeaponType(final String kiWeaponType) {
		this.kiWeaponType = kiWeaponType;
	}

	public void setDrainingTargetId(final int drainingTargetId) {
		this.drainingTargetId = drainingTargetId;
	}

	public void setFused(final boolean isFused) {
		this.isFused = isFused;
	}

	public void setFusionLeader(final boolean isFusionLeader) {
		this.isFusionLeader = isFusionLeader;
	}

	public void setFusionPartnerUUID(final UUID fusionPartnerUUID) {
		this.fusionPartnerUUID = fusionPartnerUUID;
	}

	public void setFusionTimer(final int fusionTimer) {
		this.fusionTimer = fusionTimer;
	}

	public void setFusionType(final String fusionType) {
		this.fusionType = fusionType;
	}

	public void setFusionName(final String fusionName) {
		this.fusionName = fusionName;
	}

	public void setFusionPartyManaged(final boolean fusionPartyManaged) {
		this.fusionPartyManaged = fusionPartyManaged;
	}

	public void setFusionPrevPartyId(final UUID fusionPrevPartyId) {
		this.fusionPrevPartyId = fusionPrevPartyId;
	}

	public void setFusionPrevPartyLeader(final boolean fusionPrevPartyLeader) {
		this.fusionPrevPartyLeader = fusionPrevPartyLeader;
	}

	public void setPotaraPoseTimer(final int potaraPoseTimer) {
		this.potaraPoseTimer = potaraPoseTimer;
	}

	public void setEvasionLockTicks(final int evasionLockTicks) {
		this.evasionLockTicks = evasionLockTicks;
	}

	public void setPotaraPartnerUUID(final UUID potaraPartnerUUID) {
		this.potaraPartnerUUID = potaraPartnerUUID;
	}

	public void setPotaraLeader(final boolean potaraLeader) {
		this.potaraLeader = potaraLeader;
	}

	public void setPotaraBeetle(final boolean potaraBeetle) {
		this.potaraBeetle = potaraBeetle;
	}

	public void setOriginalAppearance(final CompoundTag originalAppearance) {
		this.originalAppearance = originalAppearance;
	}

	public void setAndroidUpgraded(final boolean androidUpgraded) {
		this.androidUpgraded = androidUpgraded;
	}

	public void setRenderKatana(final boolean renderKatana) {
		this.renderKatana = renderKatana;
	}

	public void setBackWeapon(final String backWeapon) {
		this.backWeapon = backWeapon;
	}

	public void setScouterItem(final String scouterItem) {
		this.scouterItem = scouterItem;
	}

	public void setPothalaColor(final String pothalaColor) {
		this.pothalaColor = pothalaColor;
	}

	public void setPermanentAura(final boolean isPermanentAura) {
		this.isPermanentAura = isPermanentAura;
	}

	public void setForcedAura(final boolean forcedAura) {
		this.forcedAura = forcedAura;
	}

	public void setForcedCharge(final boolean forcedCharge) {
		this.forcedCharge = forcedCharge;
	}

	public void setStrikeLocked(final boolean isStrikeLocked) {
		this.isStrikeLocked = isStrikeLocked;
	}

	public void setMatchFrozen(final boolean matchFrozen) {
		this.matchFrozen = matchFrozen;
	}

	public void setSleepRecovering(final boolean sleepRecovering) {
		this.sleepRecovering = sleepRecovering;
	}

	public void setFlightMode(final int flightMode) {
		this.flightMode = flightMode;
	}

	public void setFlightModeLocked(final boolean flightModeLocked) {
		this.flightModeLocked = flightModeLocked;
	}

	public void setLockedFlightMode(final int lockedFlightMode) {
		this.lockedFlightMode = lockedFlightMode;
	}

	public void setActiveShadowDummyUUID(final UUID activeShadowDummyUUID) {
		this.activeShadowDummyUUID = activeShadowDummyUUID;
	}

	public void setShadowDummyPercent(final int shadowDummyPercent) {
		this.shadowDummyPercent = shadowDummyPercent;
	}

	public void setShadowDummyKillCount(final int shadowDummyKillCount) {
		this.shadowDummyKillCount = shadowDummyKillCount;
	}

	public void setKiTransferTarget(final int kiTransferTarget) {
		this.kiTransferTarget = kiTransferTarget;
	}

	public void setKiTransferDonors(final int kiTransferDonors) {
		this.kiTransferDonors = kiTransferDonors;
	}

	public void setKiDrainStacks(final int kiDrainStacks) {
		this.kiDrainStacks = kiDrainStacks;
	}

	public void setKiDrainTicks(final int kiDrainTicks) {
		this.kiDrainTicks = kiDrainTicks;
	}

	public void setGodRitualRole(final int godRitualRole) {
		this.godRitualRole = godRitualRole;
	}

	public void setGodRitualAnchor(final UUID godRitualAnchor) {
		this.godRitualAnchor = godRitualAnchor;
	}

	public void setGodRitualTicks(final int godRitualTicks) {
		this.godRitualTicks = godRitualTicks;
	}

	public void setGodRitualGlowStart(final long godRitualGlowStart) {
		this.godRitualGlowStart = godRitualGlowStart;
	}
}
