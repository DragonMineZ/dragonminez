package com.dragonminez.common.stats.character;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.server.dynamicgrowth.DynamicGrowthService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public class Resources {
    private float currentEnergy;
    private float currentStamina;
    private float currentPoise;
    private float surgeCharge;
    private float rage;
    private int release;
    private int releaseLimit;
    private int flightSpeedLimit;
    private int actionCharge;
    private int alignment;
    private double trainingPoints;
    private int pendingAttributePoints;
    private int racialSkillCount;
    private Player player;
    private transient StatsData statsData;

    public Resources() {
        this.currentEnergy = 0;
        this.currentStamina = 0;
        this.currentPoise = 0;
        this.surgeCharge = 0;
        this.rage = 0;
        this.release = 5;
        this.releaseLimit = 0;
        this.flightSpeedLimit = 100;
        this.actionCharge = 0;
        this.alignment = 100;
        this.trainingPoints = 0;
        this.pendingAttributePoints = 0;
        this.racialSkillCount = 0;
    }

    public void reset() {
        this.currentEnergy = 0;
        this.currentStamina = 0;
        this.currentPoise = 0;
        this.surgeCharge = 0;
        this.rage = 0;
        this.release = 5;
        this.releaseLimit = 0;
        this.flightSpeedLimit = 100;
        this.actionCharge = 0;
        this.alignment = 100;
        this.racialSkillCount = 0;
    }

    private static float roundToQuarter(float value) {
        return Math.round(value * 4.0F) / 4.0F;
    }

    private static float truncateToInt(float value) {
        return (float) Math.floor(value);
    }

    private static final float LARGE_POOL_THRESHOLD = 1_000_000.0F;

    /**
     * Clamp ki/stamina with double math so pools at high ENE/RES (1e8–1e9 max) can reach max.
     * Quarter steps only below {@link #LARGE_POOL_THRESHOLD}.
     */
    private static float clampPool(double value, float max) {
        if (!(max > 0.0F) || !Double.isFinite(value)) {
            return 0.0F;
        }
        double v = Math.min(Math.max(0.0, value), (double) max);
        if (v >= (double) max - Math.max(4.0 * Math.ulp(max), 1.0)) {
            return max;
        }
        if (v < LARGE_POOL_THRESHOLD) {
            return roundToQuarter((float) v);
        }
        return (float) v;
    }

    public int getPowerRelease() {
        return release;
    }

    public void setCurrentEnergy(float energy) {
        if (energy <= 1) setPowerRelease(0);
        float max = statsData != null ? statsData.getMaxEnergy() : 0.0F;
        this.currentEnergy = clampPool(energy, max);
    }

    public void setCurrentStamina(float stamina) {
        float max = statsData != null ? Math.max(0.0F, statsData.getMaxStamina()) : 0.0F;
        this.currentStamina = clampPool(stamina, max);
    }

    public void setCurrentPoise(float poise) {
        float max = statsData != null ? statsData.getMaxPoise() : 0.0F;
        this.currentPoise = roundToQuarter((float) Math.min(Math.max(0.0, (double) poise), Math.max(0.0, (double) max)));
    }

    public void setSurgeCharge(float surgeCharge) {
        this.surgeCharge = Math.min(100.0f, Math.max(0.0f, surgeCharge));
    }

    public float getSurgeCharge() {
        return surgeCharge;
    }

    public boolean isSurgeFull() {
        return surgeCharge >= 100.0f;
    }

    public void setRage(float rage) {
        this.rage = Float.isFinite(rage) ? Math.min(100.0f, Math.max(0.0f, rage)) : 0.0f;
    }

    public float getRage() {
        return rage;
    }

    public boolean isRageFull() {
        return rage >= 100.0f;
    }

    public void setPowerRelease(int release) {
        this.release = Math.max(0, release);
    }

    public void setReleaseLimit(int releaseLimit) {
        this.releaseLimit = Math.max(0, releaseLimit);
    }

    public void setActionCharge(int actionCharge) {
        this.actionCharge = Math.max(0, Math.min(100, actionCharge));
    }

    public void setAlignment(int alignment) {
        if (statsData != null && statsData.getEffects().hasEffect("majin")) {
            this.alignment = 0;
            return;
        }
        this.alignment = Math.max(0, Math.min(100, alignment));
    }

    public float getTrainingPoints() {
        return (float) Math.min(trainingPoints, Float.MAX_VALUE);
    }

    public double getTrainingPointsExact() {
        return trainingPoints;
    }

    public void setTrainingPoints(float points) {
        setTrainingPointsExact(points);
    }

    public void setTrainingPointsExact(double points) {
        if (Double.isNaN(points)) return;
        this.trainingPoints = Math.floor(Math.max(0.0, Math.min(Double.MAX_VALUE, points)));
    }

    public void setPendingAttributePoints(int points) {
        this.pendingAttributePoints = Math.max(0, points);
    }

    public void addPendingAttributePoints(int amount) {
        setPendingAttributePoints(pendingAttributePoints + amount);
    }

    public void removePendingAttributePoints(int amount) {
        setPendingAttributePoints(pendingAttributePoints - amount);
    }

    public void setRacialSkillCount(int count) {
        this.racialSkillCount = Math.max(0, count);
    }

    public void addEnergy(float amount) {
        setCurrentEnergy((float) ((double) currentEnergy + (double) amount));
    }

    public void addStamina(float amount) {
        setCurrentStamina((float) ((double) currentStamina + (double) amount));
    }

    public void addPoise(float amount) {
        setCurrentPoise(currentPoise + amount);
    }

    /**
     * When max energy/stamina grows (VIT-style heal for pools): grant the delta with double math,
     * or snap full if the bar was full before.
     */
    public void grantMaxPoolIncrease(float oldMax, float newMax, boolean energy) {
        if (!(newMax > oldMax)) return;
        double cur = energy ? currentEnergy : currentStamina;
        boolean wasFull = oldMax <= 0.0F || cur >= (double) oldMax - Math.max(4.0 * Math.ulp(oldMax), 1.0);
        if (wasFull) {
            if (energy) setCurrentEnergy(newMax);
            else setCurrentStamina(newMax);
            return;
        }
        double next = cur + ((double) newMax - (double) oldMax);
        if (energy) setCurrentEnergy((float) Math.min(newMax, next));
        else setCurrentStamina((float) Math.min(newMax, next));
    }

    /** After login / max recompute: snap near-full float pools to exact max; never invent fill. */
    public void reclampToCurrentMax() {
        if (statsData == null) return;
        float maxE = statsData.getMaxEnergy();
        float maxS = statsData.getMaxStamina();
        float maxP = statsData.getMaxPoise();
        if (maxE > 0.0F) this.currentEnergy = clampPool(this.currentEnergy, maxE);
        if (maxS > 0.0F) this.currentStamina = clampPool(this.currentStamina, maxS);
        if (maxP > 0.0F) {
            this.currentPoise = roundToQuarter((float) Math.min(Math.max(0.0, (double) this.currentPoise), (double) maxP));
        }
    }

    public void addAlignment(int amount) {
        setAlignment(alignment + amount);
    }

    public void addTrainingPoints(float amount) {
        addTrainingPoints(amount, true);
    }

    public void addTrainingPoints(float amount, boolean shareWithParty) {
        addTrainingPoints(amount, shareWithParty, false);
    }

    public void addBoostedTrainingPoints(double amount) {
        addTrainingPoints(amount, true, true);
    }

    public void addTrainingPoints(double amount, boolean shareWithParty, boolean preBoosted) {
        if (amount <= 0 || player == null) {
            setTrainingPointsExact(trainingPoints + amount);
            return;
        }
        double oldValue = this.trainingPoints;
        if (amount >= Integer.MAX_VALUE) {
            setTrainingPointsExact(oldValue + amount);
            return;
        }
        DMZEvent.TPGainEvent event = new DMZEvent.TPGainEvent(player, (int) Math.min(oldValue, Integer.MAX_VALUE), (int) amount, shareWithParty, preBoosted);

        if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
            setTrainingPointsExact(oldValue + event.getTpGain());
        }
    }

    public void addRacialSkillCount(int amount) {
        setRacialSkillCount(racialSkillCount + amount);
    }

    public void removeEnergy(float amount) {
        removeEnergy(amount, true);
    }

    public void removeEnergy(float amount, boolean drainsSurge) {
        boolean taxed = drainsSurge && amount > 0 && isSurgeTaxActive();
        float requested = taxed
                ? (float) (amount * ConfigManager.getCombatConfig().getSurgeKiCostMultiplier())
                : amount;

        float before = currentEnergy;
        setCurrentEnergy(currentEnergy - requested);
        float spent = before - currentEnergy;

        awardDynamicGrowthEnergy(spent);
        if (drainsSurge && spent > 0) drainSurgeFromKiSpent(spent);
    }

    private boolean isSurgeTaxActive() {
        return statsData != null && statsData.getStatus().isSurgeActive();
    }

    private void drainSurgeFromKiSpent(float spent) {
        if (surgeCharge <= 0 || statsData == null) return;
        if (!(player instanceof ServerPlayer)) return;

        float maxEnergy = statsData.getMaxEnergy();
        if (maxEnergy <= 0) return;

        double factor = ConfigManager.getCombatConfig().getSurgeDrainPerKiFraction();
        setSurgeCharge(surgeCharge - (float) ((spent / maxEnergy) * 100.0 * factor));
    }

    public void removeStamina(float amount) {
        float before = currentStamina;
        setCurrentStamina(currentStamina - amount);
        awardDynamicGrowthStamina(before - currentStamina);
    }

    private void awardDynamicGrowthStamina(float spent) {
        if (spent > 0 && statsData != null && player instanceof ServerPlayer serverPlayer) {
            DynamicGrowthService.awardStaminaSpent(serverPlayer, statsData, spent);
        }
    }

    private void awardDynamicGrowthEnergy(float spent) {
        if (spent > 0 && statsData != null && player instanceof ServerPlayer serverPlayer) {
            DynamicGrowthService.awardEnergySpent(serverPlayer, statsData, spent);
        }
    }
    public void removePoise(float amount) {
        setCurrentPoise(currentPoise - amount);
    }

    public void removeAlignment(int amount) {
        setAlignment(alignment - amount);
    }

    public void removeTrainingPoints(float amount) {
        setTrainingPointsExact(trainingPoints - amount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("CurrentEnergy", currentEnergy);
        tag.putFloat("CurrentStamina", currentStamina);
        tag.putFloat("CurrentPoise", currentPoise);
        tag.putFloat("SurgeCharge", surgeCharge);
        tag.putFloat("Rage", rage);
        tag.putInt("Release", release);
        tag.putInt("ReleaseLimit", releaseLimit);
        tag.putInt("FlightSpeed", flightSpeedLimit);
        tag.putInt("FormRelease", actionCharge);
        tag.putInt("Alignment", alignment);
        tag.putDouble("TrainingPointsD", trainingPoints);
        tag.putFloat("TrainingPointsF", (float) trainingPoints);
        tag.putInt("PendingAttributePoints", pendingAttributePoints);
        tag.putInt("ZenkaiCount", racialSkillCount);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("CurrentEnergy", 5)) this.currentEnergy = tag.getFloat("CurrentEnergy");
         else this.currentEnergy = tag.getInt("CurrentEnergy");
        if (tag.contains("CurrentStamina", 5)) this.currentStamina = tag.getFloat("CurrentStamina");
         else this.currentStamina = tag.getInt("CurrentStamina");
        if (tag.contains("CurrentPoise", 5)) this.currentPoise = tag.getFloat("CurrentPoise");
         else this.currentPoise = tag.getInt("CurrentPoise");

        this.surgeCharge = tag.getFloat("SurgeCharge");
        setRage(tag.getFloat("Rage"));
        this.release = tag.getInt("Release");
        this.releaseLimit = tag.getInt("ReleaseLimit");
        this.flightSpeedLimit = tag.getInt("FlightSpeed");
        this.actionCharge = tag.getInt("FormRelease");
        this.alignment = tag.getInt("Alignment");
        if (tag.contains("TrainingPointsD", 6)) this.trainingPoints = tag.getDouble("TrainingPointsD");
        else if (tag.contains("TrainingPointsF", 5)) this.trainingPoints = tag.getFloat("TrainingPointsF");
        else this.trainingPoints = tag.getInt("TrainingPoints");
        this.pendingAttributePoints = tag.getInt("PendingAttributePoints");
        this.racialSkillCount = tag.getInt("ZenkaiCount");
    }

    public void copyFrom(Resources other) {
        this.currentEnergy = other.currentEnergy;
        this.currentStamina = other.currentStamina;
        this.currentPoise = other.currentPoise;
        this.surgeCharge = other.surgeCharge;
        this.rage = other.rage;
        this.release = other.release;
        this.releaseLimit = other.releaseLimit;
        this.flightSpeedLimit = other.flightSpeedLimit;
        this.actionCharge = other.actionCharge;
        this.alignment = other.alignment;
        this.trainingPoints = other.trainingPoints;
        this.pendingAttributePoints = other.pendingAttributePoints;
        this.racialSkillCount = other.racialSkillCount;
    }

    @java.lang.SuppressWarnings("all")
    public float getCurrentEnergy() {
        return this.currentEnergy;
    }

    @java.lang.SuppressWarnings("all")
    public float getCurrentStamina() {
        return this.currentStamina;
    }

    @java.lang.SuppressWarnings("all")
    public float getCurrentPoise() {
        return this.currentPoise;
    }

    @java.lang.SuppressWarnings("all")
    public int getRelease() {
        return this.release;
    }

    @java.lang.SuppressWarnings("all")
    public int getReleaseLimit() {
        return this.releaseLimit;
    }

    public int getFlightSpeedLimit() {
        return this.flightSpeedLimit;
    }

    public void setFlightSpeedLimit(int flightSpeedLimit) {
        this.flightSpeedLimit = flightSpeedLimit;
    }

    @java.lang.SuppressWarnings("all")
    public int getActionCharge() {
        return this.actionCharge;
    }

    @java.lang.SuppressWarnings("all")
    public int getAlignment() {
        return this.alignment;
    }

    @java.lang.SuppressWarnings("all")
    public int getPendingAttributePoints() {
        return this.pendingAttributePoints;
    }

    @java.lang.SuppressWarnings("all")
    public int getRacialSkillCount() {
        return this.racialSkillCount;
    }

    @java.lang.SuppressWarnings("all")
    public Player getPlayer() {
        return this.player;
    }

    @java.lang.SuppressWarnings("all")
    public StatsData getStatsData() {
        return this.statsData;
    }

    @java.lang.SuppressWarnings("all")
    public void setRelease(final int release) {
        this.release = release;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlayer(final Player player) {
        this.player = player;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatsData(final StatsData statsData) {
        this.statsData = statsData;
    }
}
