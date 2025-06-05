package com.dragonminez.mod.core.common.player.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Abstract base class for capability data holders.
 * <p>
 * This class represents the base abstraction for all capability data objects in the mod. It defines
 * a standard structure that includes a unique identifier and basic (de)serialization hooks.
 * Subclasses should implement their own serialization logic as needed.
 * </p>
 */
public abstract class CapDataHolder {

  /**
   * The unique {@link ResourceLocation} used to identify this capability type.
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
   * Returns the identifier associated with this capability.
   *
   * @return the {@link ResourceLocation} used to identify this capability
   */
  public ResourceLocation identifier() {
    return identifier;
  }

  /**
   * Serializes this capability’s data into the given {@link CompoundTag}.
   * <p>
   * This method is intended to be overridden by subclasses to store their specific data. The
   * default implementation returns the tag unchanged.
   *
   * @param nbt the tag to write data into
   * @return the modified or same {@link CompoundTag}
   */
  public CompoundTag serialize(CompoundTag nbt) {
    return nbt;
  }

  /**
   * Deserializes this capability’s data from the given {@link CompoundTag}.
   * <p>
   * This method is intended to be overridden by subclasses to read their specific data. The
   * {@code clonned} parameter can be used to differentiate logic during cloning or sync
   * operations.
   *
   * @param nbt     the tag to read data from
   * @param clonned whether the data is being loaded into a cloned or synced instance
   */
  public void deserialize(CompoundTag nbt, boolean clonned) {
  }
}
