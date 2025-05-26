package com.dragonminez.mod.core.common.player.capability;

import java.util.Locale;

public final class CapDataType {

  private final String id;
  private final String legibleId;
  private final String abbreviation;
  private final boolean isPublic;

  public CapDataType(String id, String abbreviation, boolean isPublic) {
    this.id = id.toLowerCase(Locale.ROOT);
    this.legibleId = toLegible(this.id);
    this.abbreviation = abbreviation;
    this.isPublic = isPublic;
  }

  public static CapDataType of(String id, boolean isPublic) {
    return new CapDataType(id, "", isPublic);
  }

  public static CapDataType of(String id, String abbreviation, boolean isPublic) {
    return new CapDataType(id, abbreviation, isPublic);
  }

  private static String toLegible(String id) {
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

  public String id() {
    return id;
  }

  public String legibleId() {
    return legibleId;
  }

  public String abbreviation() {
    return abbreviation;
  }

  public boolean isPublic() {
    return isPublic;
  }
}
