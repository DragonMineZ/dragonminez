package com.dragonminez.common.stats.character;

import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.stats.StatsData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

@Getter
@Setter
public class Resources {
    private float currentEnergy;
    private float currentStamina;
    private float currentPoise;
    private int release;
    private int actionCharge;
    private int alignment;
    private float trainingPoints;
    private int racialSkillCount;
    private Player player;
    private transient StatsData statsData;

    public Resources() {
        this.currentEnergy = 0;
        this.currentStamina = 0;
        this.currentPoise = 0;
        this.release = 5;
        this.actionCharge = 0;
        this.alignment = 100;
        this.trainingPoints = 0;
        this.racialSkillCount = 0;
    }

    public void reset() {
        this.currentEnergy = 0;
        this.currentStamina = 0;
        this.currentPoise = 0;
        this.release = 5;
        this.actionCharge = 0;
        this.alignment = 100;
        this.racialSkillCount = 0;
    }

    private static float roundToQuarter(float value) {
        return Math.round(value * 4.0f) / 4.0f;
    }

    private static float truncateToInt(float value) {
        return (float) Math.floor(value);
    }

    public int getPowerRelease() { return release; }

    public void setCurrentEnergy(float energy) {
        if (energy <= 1) setPowerRelease(0);
        this.currentEnergy = roundToQuarter(Math.min(Math.max(0, energy), statsData.getMaxEnergy()));
    }

    public void setCurrentStamina(float stamina) {
        this.currentStamina = roundToQuarter(Math.min(Math.max(0, stamina), statsData.getMaxStamina()));
    }

    public void setCurrentPoise(float poise) {
        this.currentPoise = roundToQuarter(Math.min(Math.max(0, poise), statsData.getMaxPoise()));
    }

    public void setPowerRelease(int release) {
        this.release = Math.max(0, release);
    }

    public void setActionCharge(int actionCharge) {
        this.actionCharge = Math.max(0, Math.min(100, actionCharge));
    }

    public void setAlignment(int alignment) {
        this.alignment = Math.max(0, Math.min(100, alignment));
    }

    public void setTrainingPoints(float points) {
        float clamped = Math.max(0, Math.min(Float.MAX_VALUE - 1, points));
        this.trainingPoints = truncateToInt(clamped);
    }

    public void setRacialSkillCount(int count) {
        this.racialSkillCount = Math.max(0, count);
    }

    public void addEnergy(float amount) { setCurrentEnergy(currentEnergy + amount); }
    public void addStamina(float amount) { setCurrentStamina(currentStamina + amount); }
    public void addPoise(float amount) { setCurrentPoise(currentPoise + amount); }
    public void addAlignment(int amount) { setAlignment(alignment + amount); }

    public void addTrainingPoints(float amount) {
        addTrainingPoints(amount, true);
    }
    public void addTrainingPoints(float amount, boolean shareWithParty) {
        if (amount <= 0 || player == null) {
            setTrainingPoints(trainingPoints + amount);
            return;
        }

        float oldValue = this.trainingPoints;
        DMZEvent.TPGainEvent event = new DMZEvent.TPGainEvent(player, (int) oldValue, (int) amount, shareWithParty);

        if (!MinecraftForge.EVENT_BUS.post(event)) {
            setTrainingPoints(oldValue + event.getTpGain());
        }
    }

    public void addRacialSkillCount(int amount) { setRacialSkillCount(racialSkillCount + amount); }

    public void removeEnergy(float amount) { setCurrentEnergy(currentEnergy - amount); }
    public void removeStamina(float amount) { setCurrentStamina(currentStamina - amount); }
    public void removePoise(float amount) { setCurrentPoise(currentPoise - amount); }
    public void removeAlignment(int amount) { setAlignment(alignment - amount); }
    public void removeTrainingPoints(float amount) { setTrainingPoints(trainingPoints - amount); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("CurrentEnergy", currentEnergy);
        tag.putFloat("CurrentStamina", currentStamina);
        tag.putFloat("CurrentPoise", currentPoise);
        tag.putInt("Release", release);
        tag.putInt("FormRelease", actionCharge);
        tag.putInt("Alignment", alignment);
        tag.putFloat("TrainingPointsF", trainingPoints);
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

        this.release = tag.getInt("Release");
        this.actionCharge = tag.getInt("FormRelease");
        this.alignment = tag.getInt("Alignment");

        if (tag.contains("TrainingPointsF", 5)) this.trainingPoints = tag.getFloat("TrainingPointsF");
        else this.trainingPoints = tag.getInt("TrainingPoints");

        this.racialSkillCount = tag.getInt("ZenkaiCount");
    }

    public void copyFrom(Resources other) {
        this.currentEnergy = other.currentEnergy;
        this.currentStamina = other.currentStamina;
        this.currentPoise = other.currentPoise;
        this.release = other.release;
        this.actionCharge = other.actionCharge;
        this.alignment = other.alignment;
        this.trainingPoints = other.trainingPoints;
        this.racialSkillCount = other.racialSkillCount;
    }
}