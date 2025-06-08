package com.dragonminez.core.common.util;

/**
 * Utility class for general-purpose operations used throughout the mod,
 * such as string formatting and type parsing.
 */
public class JavaUtil {

  /**
   * Parses a raw string into a value of the specified {@link DataType}.
   *
   * <p>If the string is not a valid representation for the given type (e.g., a non-numeric
   * string for a numeric type), this method returns {@code null}.</p>
   *
   * @param type the expected {@link DataType}
   * @param raw  the string to parse
   * @return the parsed object, or {@code null} if parsing fails
   */
  public static Object parseValue(DataType type, String raw) {
    try {
      return switch (type) {
        case INTEGER -> Integer.parseInt(raw);
        case DOUBLE -> Double.parseDouble(raw);
        case FLOAT -> Float.parseFloat(raw);
        case LONG -> Long.parseLong(raw);
        case BOOLEAN -> Boolean.parseBoolean(raw);
        case STRING -> raw;
      };
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Converts an underscore-separated string into a space-separated,
   * capitalized format for display.
   *
   * <p>For example, {@code "combat_stats"} becomes {@code "Combat Stats"}.</p>
   *
   * @param id the raw ID string to convert
   * @return the legible, formatted version of the ID
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
   * Represents the supported data types for capabilities,
   * and provides information on whether the type is numeric.
   */
  public enum DataType {
    STRING(false),
    BOOLEAN(false),
    INTEGER(true),
    FLOAT(true),
    DOUBLE(true),
    LONG(true);

    private final boolean isNumeric;

    DataType(boolean isNumeric) {
      this.isNumeric = isNumeric;
    }

    /**
     * Returns whether this type represents a numeric value.
     *
     * @return {@code true} if numeric, {@code false} otherwise
     */
    public boolean isNumeric() {
      return isNumeric;
    }
  }
}
