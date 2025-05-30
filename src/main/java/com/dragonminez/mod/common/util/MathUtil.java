package com.dragonminez.mod.common.util;

/**
 * Utility class responsible for value validation and numeric clamping.
 * Designed for static use in mathematical and logical operations.
 */
public class MathUtil {

  /**
   * Checks if the given string matches any of the provided valid values (case-insensitive).
   *
   * @param value  the value to validate
   * @param values the valid options to compare against
   * @return the matched value if valid, otherwise null
   */
  public static String isValueValid(String value, String... values) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    for (String validValue : values) {
      if (validValue.equalsIgnoreCase(value)) {
        return validValue;
      }
    }
    return null;
  }

  /**
   * Clamps an integer between the given minimum and maximum values.
   *
   * @param value the value to clamp
   * @param min   the minimum allowed value
   * @param max   the maximum allowed value
   * @return the clamped value
   */
  public static int rangeValue(int value, int min, int max) {
    if (value < min) {
      value = min;
    } else if (value > max) {
      value = max;
    }
    return value;
  }

  /**
   * Clamps a float between the given minimum and maximum values.
   *
   * @param value the value to clamp
   * @param min   the minimum allowed value
   * @param max   the maximum allowed value
   * @return the clamped value
   */
  public static float rangeValue(float value, float min, float max) {
    if (value < min) {
      value = min;
    } else if (value > max) {
      value = max;
    }
    return value;
  }

  /**
   * Clamps a double between the given minimum and maximum values.
   *
   * @param value the value to clamp
   * @param min   the minimum allowed value
   * @param max   the maximum allowed value
   * @return the clamped value
   */
  public static double rangeValue(double value, double min, double max) {
    if (value < min) {
      value = min;
    } else if (value > max) {
      value = max;
    }
    return value;
  }
}
