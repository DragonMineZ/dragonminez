package com.dragonminez.mod.core.common.manager;

import com.dragonminez.mod.common.util.LogUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Generic manager class for managing a mapping of {@link Dist} and keys to values.
 * Uses {@link HashBasedTable} to organize values per environment (CLIENT/SERVER).
 *
 * @param <K> The key type
 * @param <V> The value type
 */
public abstract class DistListManager<K, V> {

  private final Table<Dist, K, V> map = HashBasedTable.create();

  /**
   * Adds a value to a given key under the specified {@link Dist}. If {@link #uniqueKeys()} is true,
   * only one value per key per dist is allowed. Logs errors on duplicates.
   *
   * @param dist  The distribution (CLIENT or SERVER)
   * @param key   The key to add
   * @param value The value to associate
   */
  public void register(Dist dist, K key, V value) {
    if (this.uniqueKeys() && this.map.contains(dist, key)) {
      LogUtil.error("Duplicated key %s on manager %s for dist %s".formatted(key, this.identifier(), dist));
      return;
    }
    this.update(dist, key, value);
  }

  /**
   * Forcefully adds or updates a value under the given key and {@link Dist}.
   * Logging behavior depends on {@link #logMode()}.
   *
   * @param dist  The distribution (CLIENT or SERVER)
   * @param key   The key to update
   * @param value The value to associate
   */
  public void update(Dist dist, K key, V value) {
    this.map.put(dist, key, value);
    if (this.logMode() == LogMode.LOG_ALL || this.logMode() == LogMode.LOG_ADDITION) {
      LogUtil.info("Added %s to %s for dist %s".formatted(value, key, dist));
    }
  }

  /**
   * Removes a value under the specified key and {@link Dist}.
   * Logging behavior depends on {@link #logMode()}.
   *
   * @param dist  The distribution (CLIENT or SERVER)
   * @param key   The key to remove the value from
   */
  public void remove(Dist dist, K key) {
    V value = this.map.remove(dist, key);
    if (value != null && (this.logMode() == LogMode.LOG_ALL || this.logMode() == LogMode.LOG_REMOVAL)) {
      LogUtil.info("Removed %s from %s for dist %s".formatted(value, key, dist));
    }
  }

  /**
   * Gets the value associated with a key under the specified {@link Dist}.
   *
   * @param dist The distribution (CLIENT or SERVER)
   * @param key  The key to retrieve the value for
   * @return The value, or null if not found
   */
  public V get(Dist dist, K key) {
    return this.map.get(dist, key);
  }

  /**
   * Returns all keys across all {@link Dist}s.
   *
   * @return A set of all keys
   */
  public Set<K> keys() {
    return new HashSet<>(this.map.columnKeySet());
  }

  /**
   * Returns all values across all {@link Dist}s.
   *
   * @return A collection of all values
   */
  public Collection<V> values() {
    return this.map.values();
  }

  /**
   * Returns all values associated with the given {@link Dist}.
   *
   * @param dist The distribution (CLIENT or SERVER)
   * @return A collection of values for the specified dist
   */
  public Collection<V> values(Dist dist) {
    return this.map.row(dist).values();
  }

  /**
   * Provides a string identifier for logging and debugging purposes.
   *
   * @return The identifier of the manager
   */
  public abstract String identifier();

  /**
   * Determines if the manager enforces unique keys per {@link Dist}.
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
