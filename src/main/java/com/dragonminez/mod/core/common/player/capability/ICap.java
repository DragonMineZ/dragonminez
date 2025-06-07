package com.dragonminez.mod.core.common.player.capability;

import net.minecraft.nbt.CompoundTag;

public interface ICap {

  /**
   * Serializes this capability’s data into the given {@link CompoundTag}.
   * <p>
   * This method is intended to be overridden by subclasses to store their specific data. The
   * default implementation returns the tag unchanged.
   *
   * @param nbt the tag to write data into
   * @return the modified or same {@link CompoundTag}
   */
  default CompoundTag serialize(CompoundTag nbt) {
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
  default void deserialize(CompoundTag nbt, boolean clonned) {
  }

  CapDataHolder holder();
}
