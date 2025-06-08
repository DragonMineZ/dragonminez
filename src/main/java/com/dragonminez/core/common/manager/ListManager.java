package com.dragonminez.core.common.manager;

import com.dragonminez.mod.common.util.LogUtil;
import com.google.common.collect.HashMultimap;
import java.util.Collection;
import java.util.Set;

/**
 * Generic manager class for managing a mapping of keys to multiple values. Uses
 * {@link HashMultimap} to allow multiple values per key unless uniqueness is enforced.
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public abstract class ListManager<K, V> {

  private final HashMultimap<K, V> map = HashMultimap.create();

  /**
   * Adds a value to a given key. If {@link #uniqueKeys()} is true, only one value per key is
   * allowed. If the key already exists, it logs an error and skips addition.
   *
   * @param key   The key to add
   * @param value The value to associate with the key
   */
  public void register(K key, V value) {
    if (this.uniqueKeys() && this.map.containsKey(key)) {
      LogUtil.error("Duplicated key %s on manager %s".formatted(key, this.identifier()));
      return;
    }
    this.update(key, value);
  }

  /**
   * Forcefully adds a value to a key, regardless of existing associations. Logging behavior depends
   * on {@link #logMode()}.
   *
   * @param key   The key to update
   * @param value The value to associate
   */
  public void update(K key, V value) {
    this.map.put(key, value);
    if (this.logMode() == LogMode.LOG_ALL || this.logMode() == LogMode.LOG_ADDITION) {
      LogUtil.info("Added %s to %s".formatted(value, key));
    }
  }

  /**
   * Removes a value associated with a given key. Logging behavior depends on {@link #logMode()}.
   *
   * @param key   The key to remove the value from
   * @param value The value to remove
   */
  public void remove(K key, V value) {
    this.map.remove(key, value);
    if (this.logMode() == LogMode.LOG_ALL || this.logMode() == LogMode.LOG_REMOVAL) {
      LogUtil.info("Removed %s from %s".formatted(value, key));
    }
  }

  /**
   * Gets all values associated with a given key.
   *
   * @param key The key to retrieve values for
   * @return A set of values associated with the key
   */
  public Set<V> get(K key) {
    return this.map.get(key);
  }

  /**
   * Retrieves all keys present in the manager.
   *
   * @return A set of keys
   */
  public Set<K> keys() {
    return this.map.keySet();
  }

  /**
   * Retrieves all values stored in the manager.
   *
   * @return A collection of all values
   */
  public Collection<V> values() {
    return this.map.values();
  }

  /**
   * Provides a string identifier for logging and debugging purposes.
   *
   * @return The identifier of the manager
   */
  public abstract String identifier();

  /**
   * Determines if the manager enforces unique keys (i.e., one value per key).
   *
   * @return true if keys must be unique, false otherwise
   */
  public abstract boolean uniqueKeys();

  /**
   * Returns the logging mode used by this manager.
   *
   * @return The current log mode
   */
  public abstract LogMode logMode();

  /**
   * Enum representing the different logging modes supported.
   */
  public enum LogMode {
    /**
     * Logs all operations: additions and removals
     */
    LOG_ALL,

    /**
     * Logs only removals
     */
    LOG_REMOVAL,

    /**
     * Logs only additions
     */
    LOG_ADDITION,
  }
}
