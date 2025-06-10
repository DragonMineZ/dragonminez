package com.dragonminez.mod.common.player.cap.combat;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.core.common.player.capability.CapDataHolder;
import com.dragonminez.core.common.player.capability.CapData;
import com.dragonminez.core.common.player.capability.ICap;
import com.dragonminez.core.common.util.JavaUtil.DataType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class CombatData implements ICap {

  private boolean isInCombatMode = false;
  private boolean isBlocking = false;

  public CombatData() {
    super();
  }

  @Override
  public CapDataHolder holder() {
    return CombatDataHolder.INSTANCE;
  }

  public boolean isInCombatMode() {
    return isInCombatMode;
  }

  public void setCombatMode(boolean inCombatMode) {
    isInCombatMode = inCombatMode;
  }

  public boolean isBlocking() {
    return isBlocking;
  }

  public void setBlocking(boolean blocking) {
    isBlocking = blocking;
  }

  public static class CombatDataHolder extends CapDataHolder {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "combat");
    public static final CombatDataHolder INSTANCE = new CombatDataHolder();

    public static final CapData<CombatData, Boolean> COMBAT_MODE = CapData.of("combat_mode",
        DataType.BOOLEAN, CombatData::setCombatMode,
        CombatData::isInCombatMode, true, false
    );

    public static final CapData<CombatData, Boolean> BLOCKING = CapData.of("blocking",
        DataType.BOOLEAN, CombatData::setBlocking, CombatData::isBlocking, true,
        false
    );

    public CombatDataHolder() {
      super(ID);
    }

    @Override
    public List<CapData<?, ?>> acceptedData() {
      return List.of(CombatDataHolder.COMBAT_MODE, CombatDataHolder.BLOCKING);
    }
  }
}
