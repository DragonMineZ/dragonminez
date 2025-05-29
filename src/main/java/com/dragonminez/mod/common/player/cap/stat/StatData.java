package com.dragonminez.mod.common.player.cap.stat;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class StatData extends CapDataHolder {

  private int strength = 5;
  private int strikePower = 5;
  private int energy = 5;
  private int vitality = 5;
  private int resistance = 5;
  private int kiPower = 5;
  private int alignment = 100;

  public StatData() {
    super(StatDataType.ID);
  }

  public StatData(int strength, int strikePower, int energy, int vitality,
      int resistance,
      int kiPower, int alignment) {
    super(StatDataType.ID);
    this.strength = strength;
    this.strikePower = strikePower;
    this.energy = energy;
    this.vitality = vitality;
    this.resistance = resistance;
    this.kiPower = kiPower;
    this.alignment = alignment;
  }

  @Override
  public CompoundTag serialize(CompoundTag nbt) {
    nbt.putInt(StatDataType.STRENGTH.id(), this.strength);
    nbt.putInt(StatDataType.STRIKE_POWER.id(), this.strikePower);
    nbt.putInt(StatDataType.ENERGY.id(), this.energy);
    nbt.putInt(StatDataType.VITALITY.id(), this.vitality);
    nbt.putInt(StatDataType.RESISTANCE.id(), this.resistance);
    nbt.putInt(StatDataType.KI_POWER.id(), this.kiPower);
    nbt.putInt(StatDataType.ALIGNMENT.id(), this.alignment);
    return nbt;
  }

  @Override
  public void deserialize(CompoundTag nbt) {
    this.strength = nbt.getInt(StatDataType.STRENGTH.id());
    this.strikePower = nbt.getInt(StatDataType.STRIKE_POWER.id());
    this.energy = nbt.getInt(StatDataType.ENERGY.id());
    this.vitality = nbt.getInt(StatDataType.VITALITY.id());
    this.resistance = nbt.getInt(StatDataType.RESISTANCE.id());
    this.kiPower = nbt.getInt(StatDataType.KI_POWER.id());
    this.alignment = nbt.getInt(StatDataType.ALIGNMENT.id());
  }

  public int getStrength() {
    return strength;
  }

  public void setStrength(int strength) {
    this.strength = strength;
  }

  public int getStrikePower() {
    return strikePower;
  }

  public void setStrikePower(int strikePower) {
    this.strikePower = strikePower;
  }

  public int getEnergy() {
    return energy;
  }

  public void setEnergy(int energy) {
    this.energy = energy;
  }

  public int getVitality() {
    return vitality;
  }

  public void setVitality(int vitality) {
    this.vitality = vitality;
  }

  public int getResistance() {
    return resistance;
  }

  public void setResistance(int resistance) {
    this.resistance = resistance;
  }

  public int getKiPower() {
    return kiPower;
  }

  public void setKiPower(int kiPower) {
    this.kiPower = kiPower;
  }

  public int getAlignment() {
    return alignment;
  }

  public void setAlignment(int alignment) {
    this.alignment = alignment;
  }

  public static class StatDataType {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "stat");
    public static final CapDataType STRENGTH = CapDataType.of("strength", "STR",
        false);
    public static final CapDataType STRIKE_POWER = CapDataType.of("strike_power", "SKP",
        false);
    public static final CapDataType ENERGY = CapDataType.of("energy", "ENE",
        false);
    public static final CapDataType VITALITY = CapDataType.of("vitality", "VIT",
        false);
    public static final CapDataType RESISTANCE = CapDataType.of("resistance", "RES",
        false);
    public static final CapDataType KI_POWER = CapDataType.of("ki_power", "PWR",
        false);
    public static final CapDataType ALIGNMENT = CapDataType.of("alignment", false);

  }
}
