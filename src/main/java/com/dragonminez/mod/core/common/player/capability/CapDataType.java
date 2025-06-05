package com.dragonminez.mod.core.common.player.capability;

import java.util.Locale;

/**
 * Represents metadata about a capability type, including its string ID,
 * human-readable name, abbreviation, and whether it's considered public (visible to others).
 * <p>
 * This class is immutable and provides factory methods for easy creation.
 */
public final class CapDataType {

  /**
   * The lowercase identifier of this data type (e.g. "genetic_data").
   */
  private final String id;

  /**
   * A human-readable version of the ID with capitalization and spacing (e.g. "Genetic Data").
   */
  private final String legibleId;

  /**
   * A short string abbreviation, typically used for UI or internal references.
   */
  private final String abbreviation;

  /**
   * Whether this capability is public (i.e., visible or shared externally).
   */
  private final boolean isPublic;

  /**
   * Constructs a new {@code CapDataType}.
   *
   * @param id           the lowercase ID of the capability type
   * @param abbreviation an optional abbreviation for the capability
   * @param isPublic     whether the capability is considered public
   */
  public CapDataType(String id, String abbreviation, boolean isPublic) {
    this.id = id.toLowerCase(Locale.ROOT);
    this.legibleId = toLegible(this.id);
    this.abbreviation = abbreviation;
    this.isPublic = isPublic;
  }

  /**
   * Creates a new {@code CapDataType} without abbreviation.
   *
   * @param id       the ID of the capability
   * @param isPublic whether the capability is public
   * @return a new {@code CapDataType} instance
   */
  public static CapDataType of(String id, boolean isPublic) {
    return new CapDataType(id, "", isPublic);
  }

  /**
   * Creates a new {@code CapDataType} with an abbreviation.
   *
   * @param id           the ID of the capability
   * @param abbreviation the abbreviation for the capability
   * @param isPublic     whether the capability is public
   * @return a new {@code CapDataType} instance
   */
  public static CapDataType of(String id, String abbreviation, boolean isPublic) {
    return new CapDataType(id, abbreviation, isPublic);
  }

  /**
   * Converts an underscore-separated ID into a legible name.
   * For example, {@code "combat_stats"} becomes {@code "Combat Stats"}.
   *
   * @param id the raw ID to convert
   * @return the legible form of the ID
   */
  public static String toLegible(String id) {
    final StringBuilder result = new StringBuilder();
    boolean capitalizeNext = true;

    for (char c : id.toCharArray()) {
      if (c == '_') {
        result.append(" ");
        capitalizeNext = true;
      } else {
        result.append(capitalizeNext ? Character.toUpperCase(c) : c);
        capitalizeNext = false;
      }
    }

    return result.toString();
  }

  /**
   * Gets the raw ID of the capability type.
   *
   * @return the ID string (e.g. "stat_data")
   */
  public String id() {
    return id;
  }

  /**
   * Gets a human-readable version of the capability ID.
   *
   * @return the legible ID string (e.g. "Stat Data")
   */
  public String legibleId() {
    return legibleId;
  }

  /**
   * Gets the short abbreviation for this capability.
   *
   * @return the abbreviation string
   */
  public String abbreviation() {
    return abbreviation;
  }

  /**
   * Indicates whether this capability type is public.
   *
   * @return true if the capability is public, false if it is private
   */
  public boolean isPublic() {
    return isPublic;
  }
}
