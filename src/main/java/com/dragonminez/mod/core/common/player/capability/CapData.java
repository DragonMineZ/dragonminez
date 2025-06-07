package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.core.common.util.JavaUtil;
import com.dragonminez.mod.core.common.util.JavaUtil.DataType;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a single piece of data stored in a capability, including its type, identifier,
 * access logic, and metadata such as display names and visibility.
 * <p>
 * {@code CapData} acts as a descriptor for a field within a {@link CapDataHolder}, providing
 * typed access (getter/setter) to the data through functional interfaces.
 * The field may represent numeric, boolean, string, or other supported types as defined in {@link DataType}.
 * </p>
 * <p>
 * These descriptors are typically used to define which values a specific capability supports
 * and are registered once during capability initialization via {@link CapDataHolder#acceptedData()}.
 * </p>
 *
 * <p><b>Usage example:</b></p>
 * <pre>{@code
 * CapData<MyCapImpl, Integer> power = CapData.of("power", DataType.INTEGER,
 *     MyCapImpl::setPower, MyCapImpl::getPower, true);
 * }</pre>
 *
 * @param <T> the capability holder type (must implement {@link ICap})
 * @param <V> the value type of this data field
 *
 * @see CapDataHolder
 * @see DataType
 */
public final class CapData<T extends ICap, V> {

  private final String id;
  private final String legibleId;
  private final String abbreviation;
  private final DataType type;
  private final BiConsumer<T, V> setter;
  private final Function<T, V> getter;
  private final boolean isPublic;

  /**
   * Constructs a new capability data descriptor.
   *
   * @param id           the unique ID (lowercase string)
   * @param type         the value type (numeric, boolean, string, etc.)
   * @param setter       setter function for the value on a given holder
   * @param getter       getter function to read the value from a holder
   * @param abbreviation optional short label (for UI/logs)
   * @param isPublic     whether the value is externally visible (e.g., shown in commands)
   */
  public CapData(String id, DataType type, BiConsumer<T, V> setter, Function<T, V> getter,
      String abbreviation, boolean isPublic) {
    this.id = id.toLowerCase(Locale.ROOT);
    this.type = type;
    this.legibleId = JavaUtil.toLegible(this.id);
    this.abbreviation = abbreviation;
    this.setter = setter;
    this.getter = getter;
    this.isPublic = isPublic;
  }

  /**
   * Creates a capability data descriptor without an abbreviation.
   *
   * @param id       the unique ID
   * @param type     the value type
   * @param setter   setter for the value
   * @param getter   getter for the value
   * @param isPublic visibility flag
   * @return a new {@link CapData} instance
   */
  public static <T extends ICap, V> CapData<T, V> of(String id, DataType type,
      BiConsumer<T, V> setter, Function<T, V> getter, boolean isPublic) {
    return new CapData<>(id, type, setter, getter, "", isPublic);
  }

  /**
   * Creates a capability data descriptor with an abbreviation.
   *
   * @param id           the unique ID
   * @param type         the value type
   * @param abbreviation short-form display label
   * @param setter       setter for the value
   * @param getter       getter for the value
   * @param isPublic     visibility flag
   * @return a new {@link CapData} instance
   */
  public static <T extends ICap, V> CapData<T, V> of(String id, DataType type,
      String abbreviation, BiConsumer<T, V> setter, Function<T, V> getter, boolean isPublic) {
    return new CapData<>(id, type, setter, getter, abbreviation, isPublic);
  }

  /**
   * @return the internal string ID (lowercase)
   */
  public String id() {
    return this.id;
  }

  /**
   * @return the data type for this field (numeric, boolean, etc.)
   */
  public DataType type() {
    return this.type;
  }

  /**
   * Sets the value on a given capability holder instance.
   * <p>
   * This method performs an unchecked cast and should only be used with compatible holders.
   *
   * @param holder the capability data holder
   * @param value  the value to assign
   */
  @SuppressWarnings("unchecked")
  public void set(ICap cap, Object value) {
    this.setter.accept((T) cap, (V) value);
  }

  /**
   * Retrieves the current value from the given holder.
   * <p>
   * This method performs an unchecked cast and should only be used with compatible holders.
   *
   * @param holder the capability data holder
   * @return the current value of this field
   */
  @SuppressWarnings("unchecked")
  public Object get(ICap cap) {
    return this.getter.apply((T) cap);
  }

  /**
   * @return true if this data is numeric and supports operations like increment/decrement
   */
  public boolean isNumeric() {
    return this.type().isNumeric();
  }

  /**
   * @return a user-friendly version of the ID (e.g., "Power Level")
   */
  public String legibleId() {
    return this.legibleId;
  }

  /**
   * @return the short-form label for this data, used in compact displays
   */
  public String abbreviation() {
    return this.abbreviation;
  }

  /**
   * @return true if this data is publicly visible (e.g., shown in UI/commands)
   */
  public boolean isPublic() {
    return this.isPublic;
  }
}
