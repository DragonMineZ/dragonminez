package com.dragonminez.common.stats;

import com.dragonminez.common.events.DMZEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

public class Resources {
    private int currentEnergy;
    private int currentStamina;
	private int currentPoise;
    private int release;
    private int actionCharge;
    private int alignment;
    private int trainingPoints;
    private int racialSkillCount;
    private Player player;

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

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getCurrentEnergy() { return currentEnergy; }
    public int getCurrentStamina() { return currentStamina; }
	public int getCurrentPoise() { return currentPoise; }
    public int getPowerRelease() { return release; }
    public int getActionCharge() { return actionCharge; }
    public int getAlignment() { return alignment; }
    public int getTrainingPoints() { return trainingPoints; }
    public int getRacialSkillCount() { return racialSkillCount; }

    public void setCurrentEnergy(int energy) { this.currentEnergy = Math.max(0, energy); }
    public void setCurrentStamina(int stamina) { this.currentStamina = Math.max(0, stamina); }
	public void setCurrentPoise(int poise) { this.currentPoise = Math.max(0, poise); }
    public void setPowerRelease(int release) { this.release = Math.max(0, release); }
    public void setActionCharge(int actionCharge) { this.actionCharge = Math.max(0, Math.min(100, actionCharge)); }
    public void setAlignment(int alignment) { this.alignment = Math.max(0, Math.min(100, alignment)); }
    public void setTrainingPoints(int points) { this.trainingPoints = Math.max(0, points); }
    public void setRacialSkillCount(int count) { this.racialSkillCount = Math.max(0, count); }

    public void addEnergy(int amount) { setCurrentEnergy(Math.max(0, currentEnergy + amount)); }
    public void addStamina(int amount) { setCurrentStamina(Math.max(0, currentStamina + amount)); }
	public void addPoise(int amount) { setCurrentPoise(Math.max(0, currentPoise + amount)); }
    public void addAlignment(int amount) { setAlignment(Math.max(0, alignment + amount)); }

    public void addTrainingPoints(int amount) {
        if (amount <= 0 || player == null) {
            setTrainingPoints(trainingPoints + amount);
            return;
        }

        int oldValue = this.trainingPoints;
        DMZEvent.TPGainEvent event = new DMZEvent.TPGainEvent(player, oldValue, amount);

        if (!MinecraftForge.EVENT_BUS.post(event)) {
            setTrainingPoints(oldValue + event.getTpGain());
        }
    }

    public void addRacialSkillCount(int amount) { setRacialSkillCount(racialSkillCount + amount); }

    public void removeEnergy(int amount) { setCurrentEnergy(Math.max(0, currentEnergy - amount)); }
    public void removeStamina(int amount) { setCurrentStamina(Math.max(0, currentStamina - amount)); }
	public void removePoise(int amount) { setCurrentPoise(Math.max(0, currentPoise - amount)); }
    public void removeAlignment(int amount) { setAlignment(Math.max(0, alignment - amount)); }
    public void removeTrainingPoints(int amount) { setTrainingPoints(Math.max(0, trainingPoints - amount)); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CurrentEnergy", currentEnergy);
        tag.putInt("CurrentStamina", currentStamina);
		tag.putInt("CurrentPoise", currentPoise);
        tag.putInt("Release", release);
        tag.putInt("FormRelease", actionCharge);
        tag.putInt("Alignment", alignment);
        tag.putInt("TrainingPoints", trainingPoints);
        tag.putInt("ZenkaiCount", racialSkillCount);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.currentEnergy = tag.getInt("CurrentEnergy");
        this.currentStamina = tag.getInt("CurrentStamina");
		this.currentPoise = tag.getInt("CurrentPoise");
        this.release = tag.getInt("Release");
        this.actionCharge = tag.getInt("FormRelease");
        this.alignment = tag.getInt("Alignment");
        this.trainingPoints = tag.getInt("TrainingPoints");
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

