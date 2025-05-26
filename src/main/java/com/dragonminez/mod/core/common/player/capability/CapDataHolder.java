package com.dragonminez.mod.core.common.player.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

public abstract class CapDataHolder implements INBTSerializable<CompoundTag> {

  private final ResourceLocation identifier;

  public CapDataHolder(ResourceLocation identifier) {
    this.identifier = identifier;
  }

  public ResourceLocation identifier() {
    return identifier;
  }
}
