package com.dragonminez.mod.common.player.cap.stat;

import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.ICap;
import com.dragonminez.core.common.util.JavaUtil.DataType;
import com.dragonminez.core.common.util.MathUtil;
import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.common.config.GeneralConfig;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class StatData implements ICap {

  // STATS
  private int strength = 5;
  private int strikePower = 5;
  private int energy = 5;
  private int vitality = 5;
  private int resistance = 5;
  private int kiPower = 5;
  private int alignment = 100;

  // INFERRED STATS
  private float health = 20.0F;
  private int ki = 5;
  private int stamina = 5;

  public StatData() {
    super();
  }

  @Override
  public void randomize() {
    final int max = GeneralConfig.attributes().maxAttributes;
    this.setStrength(MathUtil.randomInt(1, max));
    this.setStrikePower(MathUtil.randomInt(1, max));
    this.setEnergy(MathUtil.randomInt(1, max));
    this.setVitality(MathUtil.randomInt(1, max));
    this.setResistance(MathUtil.randomInt(1, max));
    this.setKiPower(MathUtil.randomInt(1, max));
    this.setAlignment(MathUtil.randomInt(0, 100));
    this.setHealth(MathUtil.randomFloat(1.0F, 20.0F));
    this.setKi(MathUtil.randomInt(1, max));
    this.setStamina(MathUtil.randomInt(1, max));
  }

  @Override
  public CapDataHolder holder() {
    return StatDataHolder.INSTANCE;
  }

  public void setStrength(int strength) {
    this.strength = MathUtil.rangeValue(strength, 1,
        GeneralConfig.attributes().maxAttributes);
  }

  public int getStrength() {
    return strength;
  }

  public void setStrikePower(int strikePower) {
    this.strikePower = MathUtil.rangeValue(strikePower, 1,
        GeneralConfig.attributes().maxAttributes);
  }

  public int getStrikePower() {
    return strikePower;
  }

  public void setEnergy(int energy) {
    this.energy = MathUtil.rangeValue(energy, 1, GeneralConfig.attributes().maxAttributes);
  }

  public int getEnergy() {
    return energy;
  }

  public void setVitality(int vitality) {
    this.vitality = MathUtil.rangeValue(vitality, 1, GeneralConfig.attributes().maxAttributes);
  }

  public int getVitality() {
    return vitality;
  }

  public void setResistance(int resistance) {
    this.resistance = MathUtil.rangeValue(resistance, 1, GeneralConfig.attributes().maxAttributes);
  }

  public int getResistance() {
    return resistance;
  }

  public void setKiPower(int kiPower) {
    this.kiPower = MathUtil.rangeValue(kiPower, 1, GeneralConfig.attributes().maxAttributes);
  }

  public int getKiPower() {
    return kiPower;
  }

  public void setAlignment(int alignment) {
    this.alignment = MathUtil.rangeValue(alignment, 0, 100);
  }

  public int getAlignment() {
    return alignment;
  }

  public void setHealth(float health) {
    this.health = MathUtil.rangeValue(health, 1.0F,
        GeneralConfig.attributes().maxAttributes);
  }

  public float getHealth() {
    return this.health;
  }

  private void setKi(int ki) {
    this.ki = MathUtil.rangeValue(ki, 1,
        GeneralConfig.attributes().maxAttributes);
  }

  private int getKi() {
    return this.ki;
  }

  private void setStamina(int stamina) {
    this.stamina = MathUtil.rangeValue(stamina, 1,
        GeneralConfig.attributes().maxAttributes);
  }

  private int getStamina() {
    return this.stamina;
  }

  public static class StatDataHolder extends CapDataHolder {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "stat");
    public static final StatDataHolder INSTANCE = new StatDataHolder();

    public static final CapData<StatData, Integer> STRENGTH = CapData.of(
        "strength", DataType.INTEGER, "STR", StatData::setStrength,
        StatData::getStrength, false, true);

    public static final CapData<StatData, Integer> STRIKE_POWER = CapData.of(
        "strike_power", DataType.INTEGER, "SKP", StatData::setStrikePower,
        StatData::getStrikePower, false, true);

    public static final CapData<StatData, Integer> ENERGY = CapData.of(
        "energy", DataType.INTEGER, "ENE", StatData::setEnergy,
        StatData::getEnergy, false, true);

    public static final CapData<StatData, Integer> VITALITY = CapData.of(
        "vitality", DataType.INTEGER, "VIT", StatData::setVitality,
        StatData::getVitality, false, true);

    public static final CapData<StatData, Integer> RESISTANCE = CapData.of(
        "resistance", DataType.INTEGER, "RES", StatData::setResistance,
        StatData::getResistance, false, true);

    public static final CapData<StatData, Integer> KI_POWER = CapData.of(
        "ki_power", DataType.INTEGER, "PWR", StatData::setKiPower,
        StatData::getKiPower, false, true);

    public static final CapData<StatData, Integer> ALIGNMENT = CapData.of(
        "alignment", DataType.INTEGER, StatData::setAlignment,
        StatData::getAlignment, false, true);

    public static final CapData<StatData, Float> HEALTH = CapData.of(
        "health", DataType.FLOAT, StatData::setHealth,
        StatData::getHealth, false, true);

    public static final CapData<StatData, Integer> KI = CapData.of(
        "ki", DataType.INTEGER, StatData::setKi,
        StatData::getKi, false, true);

    public static final CapData<StatData, Integer> STAMINA = CapData.of(
        "stamina", DataType.INTEGER, StatData::setStamina,
        StatData::getStamina, false, true);

    public StatDataHolder() {
      super(ID);
    }

    @Override
    public List<CapData<?, ?>> acceptedData() {
      return List.of(
          STRENGTH,
          STRIKE_POWER,
          ENERGY,
          VITALITY,
          RESISTANCE,
          KI_POWER,
          ALIGNMENT,
          HEALTH,
          KI,
          STAMINA
      );
    }
  }
}
