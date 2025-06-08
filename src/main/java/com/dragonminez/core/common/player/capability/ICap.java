package com.dragonminez.core.common.player.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a serializable capability that can store and load its data via NBT, and provide access
 * to its data holder.
 */
public interface ICap {

  /**
   * Serializes this capability’s data into the given {@link CompoundTag}.
   * <p>
   * Subclasses should override this method to write their custom data. The default implementation
   * returns the tag unchanged.
   *
   * @param nbt the tag to write data into
   * @return the modified or original {@link CompoundTag}
   */
  default CompoundTag serialize(CompoundTag nbt) {
    return nbt;
  }

  /**
   * Deserializes this capability’s data from the given {@link CompoundTag}.
   * <p>
   * Subclasses should override this method to read their custom data. The {@code cloned} parameter
   * can be used to distinguish between normal deserialization and clone/sync scenarios.
   *
   * @param nbt    the tag to read data from
   * @param cloned whether the data is being loaded into a cloned or synced instance
   */
  default void deserialize(CompoundTag nbt, boolean cloned) {
  }

  /**
   * Randomizes this capability's internal data, if applicable.
   * <p>
   * Intended to be overridden for capabilities that support randomized initial states.
   */
  default void randomize() {
  }

  /**
   * Returns the associated {@link CapDataHolder} of this capability.
   *
   * @return the capability data holder
   */
  CapDataHolder holder();
}
