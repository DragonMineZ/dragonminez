package com.dragonminez.mod.common.player.cap.combat;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapData;
import com.dragonminez.mod.core.common.player.capability.ICap;
import com.dragonminez.mod.core.common.util.JavaUtil.DataType;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
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
  public CompoundTag serialize(CompoundTag tag) {
    tag.putBoolean(CombatDataHolder.COMBAT_MODE.id(), this.isInCombatMode);
    tag.putBoolean(CombatDataHolder.BLOCKING.id(), this.isBlocking);
    return tag;
  }

  @Override
  public void deserialize(CompoundTag nbt, boolean cloned) {
    if (cloned) {
      return;
    }
    this.isInCombatMode = nbt.getBoolean(CombatDataHolder.COMBAT_MODE.id());
    this.isBlocking = nbt.getBoolean(CombatDataHolder.BLOCKING.id());
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
        CombatData::isInCombatMode, true
    );

    public static final CapData<CombatData, Boolean> BLOCKING = CapData.of("blocking",
        DataType.BOOLEAN, CombatData::setBlocking, CombatData::isBlocking, true
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
