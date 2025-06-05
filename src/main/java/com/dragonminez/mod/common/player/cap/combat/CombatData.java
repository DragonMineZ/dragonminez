package com.dragonminez.mod.common.player.cap.combat;

import com.dragonminez.mod.common.Reference;
import com.dragonminez.mod.core.common.player.capability.CapDataHolder;
import com.dragonminez.mod.core.common.player.capability.CapDataType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class CombatData extends CapDataHolder {

  private boolean isInCombatMode = false;
  private boolean isBlocking = false;

  public CombatData() {
    super(CombatDataType.ID);
  }

  public CombatData(boolean isInCombatMode, boolean isBlocking) {
    super(CombatDataType.ID);
    this.isInCombatMode = isInCombatMode;
    this.isBlocking = isBlocking;
  }

  @Override
  public CompoundTag serialize(CompoundTag tag) {
    tag.putBoolean(CombatDataType.COMBAT_MODE.id(), this.isInCombatMode);
    tag.putBoolean(CombatDataType.BLOCKING.id(), this.isBlocking);
    return tag;
  }

  @Override
  public void deserialize(CompoundTag nbt, boolean cloned) {
    if (cloned) {
      return;
    }
    this.isInCombatMode = nbt.getBoolean(CombatDataType.COMBAT_MODE.id());
    this.isBlocking = nbt.getBoolean(CombatDataType.BLOCKING.id());
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

  public static class CombatDataType {

    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "combat");
    public static final CapDataType COMBAT_MODE = CapDataType.of("combat_mode", true);
    public static final CapDataType BLOCKING = CapDataType.of("blocking", true);

  }
}
