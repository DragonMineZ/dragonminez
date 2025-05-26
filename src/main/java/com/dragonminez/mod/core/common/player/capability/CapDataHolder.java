package com.dragonminez.mod.core.common.player.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Abstract base class for capability data holders.
 * <p>
 * Each capability that stores data should extend this class and implement
 * serialization logic using the {@link INBTSerializable} interface.
 * This class also provides a unique identifier used for capability registration
 * and retrieval.
 */
public abstract class CapDataHolder implements INBTSerializable<CompoundTag> {

  /**
   * The unique identifier for this capability.
   */
  private final ResourceLocation identifier;

  /**
   * Constructs a new capability data holder with the given identifier.
   *
   * @param identifier the {@link ResourceLocation} that uniquely identifies this capability
   */
  public CapDataHolder(ResourceLocation identifier) {
    this.identifier = identifier;
  }

  /**
   * Returns the unique identifier associated with this capability.
   *
   * @return the {@link ResourceLocation} used to identify this capability
   */
  public ResourceLocation identifier() {
    return identifier;
  }
}
