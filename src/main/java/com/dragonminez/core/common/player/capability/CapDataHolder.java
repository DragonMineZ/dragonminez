package com.dragonminez.core.common.player.capability;

import java.util.HashMap;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Abstract base class for capability data holders.
 * <p>
 * Represents a container for capability data within the mod, uniquely identified by a
 * {@link ResourceLocation}. This class maintains a mapping of capability data entries indexed by
 * their string IDs, providing a standardized structure for storing and managing capability data.
 * </p>
 * <p>
 * Subclasses must implement {@link #acceptedData()} to define which capability data types are
 * supported. This class handles initializing the data map based on those accepted data entries.
 * Serialization and deserialization logic should be implemented by subclasses as needed.
 * </p>
 *
 * @see CapData
 * @see ResourceLocation
 */
public abstract class CapDataHolder {

  /**
   * Unique identifier for this capability type.
   */
  private final ResourceLocation identifier;

  /**
   * Map of capability data entries, keyed by their string IDs.
   */
  private final HashMap<String, CapData<?, ?>> datas;

  /**
   * Constructs a capability data holder with the specified identifier.
   *
   * @param identifier the unique {@link ResourceLocation} identifying this capability
   */
  public CapDataHolder(ResourceLocation identifier) {
    this.identifier = identifier;
    this.datas = new HashMap<>();
  }

  /**
   * Gets the unique identifier associated with this capability.
   *
   * @return the capability's {@link ResourceLocation} identifier
   */
  public ResourceLocation identifier() {
    return identifier;
  }

  /**
   * Returns the list of accepted capability data entries for this holder.
   * <p>
   * Subclasses must implement this to define all {@link CapData} objects supported by this
   * capability.
   *
   * @return list of accepted capability data types
   */
  public abstract List<CapData<?, ?>> acceptedData();

  /**
   * Retrieves the capability data entry associated with the specified ID.
   *
   * @param id the string ID of the capability data
   * @return the corresponding {@link CapData} instance, or {@code null} if not found
   */
  public CapData<?, ?> data(String id) {
    return this.datas.get(id);
  }

  /**
   * Returns the internal map of all capability data entries by their IDs.
   *
   * @return a map of capability data keyed by string IDs
   */
  public HashMap<String, CapData<?, ?>> datas() {
    if (this.datas.isEmpty()) {
      for (CapData<?, ?> type : this.acceptedData()) {
        this.datas.put(type.id(), type);
      }
    }
    return this.datas;
  }
}
