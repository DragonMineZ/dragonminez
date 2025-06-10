package com.dragonminez.mod.common.player.cap.power;

import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.ICap;
import com.dragonminez.core.common.util.JavaUtil.DataType;
import com.dragonminez.core.common.util.MathUtil;
import com.dragonminez.mod.common.Reference;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PowerData implements ICap {

  private int powerRelease = 50;
  private int formRelease = 0;
  private boolean isAuraOn = false;

  public PowerData() {
    super();
  }

  @Override
  public void randomize() {
    final int max = 100;
    this.setPowerRelease(MathUtil.randomInt(0, max));
    this.setFormRelease(MathUtil.randomInt(0, max));
  }

  @Override
  public CapDataHolder holder() {
    return PowerDataHolder.INSTANCE;
  }

  public void setPowerRelease(int powerRelease) {
    this.powerRelease = powerRelease;
  }

  public int getPowerRelease() {
    return this.powerRelease;
  }

  public void setFormRelease(int formRelease) {
    this.formRelease = formRelease;
  }

  public int getFormRelease() {
    return this.formRelease;
  }

  public void setAuraState(boolean auraState) {
    this.isAuraOn = auraState;
  }

  public boolean isAuraOn() {
    return this.isAuraOn;
  }

  public static class PowerDataHolder extends CapDataHolder {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID,
        "power");
    public static final PowerDataHolder INSTANCE = new PowerDataHolder();

    public static final CapData<PowerData, Integer> POWER_RELEASE = CapData.of("power_release",
        DataType.INTEGER, PowerData::setPowerRelease,
        PowerData::getPowerRelease, false, true
    );

    public static final CapData<PowerData, Integer> FORM_RELEASE = CapData.of("form_release",
        DataType.INTEGER, PowerData::setFormRelease,
        PowerData::getFormRelease, false, false
    );

    public static final CapData<PowerData, Boolean> AURA_STATE = CapData.of("aura_state",
        DataType.BOOLEAN, PowerData::setAuraState,
        PowerData::isAuraOn, true, false
    );

    public PowerDataHolder() {
      super(ID);
    }

    @Override
    public List<CapData<?, ?>> acceptedData() {
      return List.of(PowerDataHolder.POWER_RELEASE, PowerDataHolder.FORM_RELEASE,
          PowerDataHolder.AURA_STATE);
    }
  }
}
