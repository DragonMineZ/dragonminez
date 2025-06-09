package com.dragonminez.core.common.keybind.model;

/**
 * Common keybind data model used for referencing keybind metadata across client and server logic.
 * <p>
 * This class defines the structure of a keybind entry, including identification, categorization,
 * and behavioral flags, without being tied to client input directly.
 */
public abstract class Keybind {

  /**
   * Unique ID for this keybind. Typically used in translation keys, data registries, or network
   * identifiers.
   *
   * @return the unique keybind ID
   */
  public abstract String id();

  /**
   * Logical grouping category for this keybind (e.g., "dragonminez.powers"). Used for organizing
   * keybinds in control menus or for display purposes.
   *
   * @return the category string
   */
  public abstract String category();

  /**
   * Whether holding this key down should repeatedly trigger the associated action.
   *
   * @return true if the action repeats while held, false if it triggers once per press
   */
  public abstract boolean canBeHeldDown();

  /**
   * Whether activating this keybind should notify the server. Used for actions that must be handled
   * or validated server-side.
   *
   * @return true if server sync is required, false otherwise
   */
  public abstract boolean notifyServer();

  /**
   * Whether this keybind is intended only for debugging or development purposes. Can be used to
   * filter out keybinds from production UIs or configuration.
   *
   * @return true if debug-only, false otherwise
   */
  public boolean isDebugKey() {
    return false;
  }
}
