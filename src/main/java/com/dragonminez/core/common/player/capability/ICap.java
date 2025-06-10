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
   * @param nbt    the tag to write data into
   * @param saving true if the data is being saved to disk
   * @return the modified or original {@link CompoundTag}
   */
  default CompoundTag serialize(CompoundTag nbt, boolean saving) {
    for (CapData<?, ?> data : this.holder().acceptedData()) {
      if (!data.isPersistent() && saving) {
        continue;
      }
      final Object value = data.get(this);
      switch (data.type()) {
        case LONG -> nbt.putLong(data.id(), (Long) value);
        case FLOAT -> nbt.putFloat(data.id(), (Float) value);
        case STRING -> nbt.putString(data.id(), (String) value);
        case DOUBLE -> nbt.putDouble(data.id(), (Double) value);
        case BOOLEAN -> nbt.putBoolean(data.id(), (Boolean) value);
        case INTEGER -> nbt.putInt(data.id(), (Integer) value);
      }
    }
    return nbt;
  }

  /**
   * Deserializes this capability’s data from the given {@link CompoundTag}.
   * <p>
   * Subclasses should override this method to read their custom data. The {@code cloned} parameter
   * can be used to distinguish between normal deserialization and clone/sync scenarios.
   *
   * @param nbt    the tag to read data from
   * @param saving whether the data is being saved or loaded into a cloned or synced instance
   */
  default void deserialize(CompoundTag nbt, boolean saving) {
    for (CapData<?, ?> data : this.holder().acceptedData()) {
      if (!data.isPersistent() && saving) {
        continue;
      }
      switch (data.type()) {
        case LONG -> data.set(this, nbt.getLong(data.id()));
        case FLOAT -> data.set(this, nbt.getFloat(data.id()));
        case STRING -> data.set(this, nbt.getString(data.id()));
        case DOUBLE -> data.set(this, nbt.getDouble(data.id()));
        case BOOLEAN -> data.set(this, nbt.getBoolean(data.id()));
        case INTEGER -> data.set(this, nbt.getInt(data.id()));
      }
    }
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
