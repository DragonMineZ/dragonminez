package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.core.common.network.capability.PacketS2CCapSync;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic manager for a player capability of type {@code D}.
 * <p>
 * Provides methods to create, retrieve, update, and serialize capability data attached to players.
 *
 * @param <D> the capability data type, extending {@link ICap}
 */
public abstract class CapDataManager<D extends ICap> implements ICapabilityProvider {

  protected final Capability<D> capability;

  /**
   * Creates a manager for the given capability.
   *
   * @param capability the Forge capability instance managed by this class
   */
  protected CapDataManager(Capability<D> capability) {
    this.capability = capability;
  }

  /**
   * Creates a new instance of the capability data.
   *
   * @return a new capability data instance
   */
  public abstract D buildCap();

  /**
   * Builds a sync packet containing capability data to send to clients.
   *
   * @param player   the player whose data is being synced, or null
   * @param data     the capability data to send, or null
   * @param isPublic whether the data should be visible to other players
   * @return a capability sync packet, or null if unavailable
   */
  public abstract PacketS2CCapSync<D> buildSyncPacket(@Nullable Player player, @Nullable D data,
      @Nullable Boolean isPublic);

  /**
   * Builds a mock sync packet with null data for network registration.
   *
   * @param isPublic visibility flag for the mock packet
   * @return a mock capability sync packet, or null
   */
  public PacketS2CCapSync<D> buildMockSyncPacket(boolean isPublic) {
    return this.buildSyncPacket(null, null, isPublic);
  }

  /**
   * Updates the capability data of a player by copying from another instance.
   *
   * @param player  the player whose data to update
   * @param newData the new capability data to apply
   * @param cloned  whether {@code newData} is cloned (affects deserialization)
   */
  public void update(Player player, D newData, boolean cloned) {
    this.retrieveData(player, oldData ->
        oldData.deserialize(newData.serialize(new CompoundTag()), cloned));
  }

  /**
   * Updates the capability data of a player by copying from another instance. Assumes
   * {@code newData} is not cloned.
   *
   * @param player  the player whose data to update
   * @param newData the new capability data to apply
   */
  public void update(Player player, ICap newData) {
    this.update(player, (D) newData, false);
  }

  /**
   * Copies capability data from one player to another.
   *
   * @param reference player to copy data from
   * @param target    player to copy data to
   */
  public void update(Player reference, Player target) {
    final D data = this.retrieveData(reference);
    if (data != null) {
      this.update(target, data, true);
    }
  }

  /**
   * Retrieves a value by key from the player's capability data.
   *
   * @param player the player whose data to read
   * @param key    the data key
   * @return the value associated with the key, or {@code null} if not found
   */
  public Object get(Player player, String key) {
    final D cap = this.retrieveData(player);
    if (cap == null) {
      return null;
    }
    final CapDataHolder holder = cap.holder();
    if (holder == null) {
      return null;
    }
    final CapData<?, ?> data = holder.datas().get(key);
    if (data == null) {
      return null;
    }
    return data.get(cap);
  }

  /**
   * Executes a consumer with the player's capability data if present.
   *
   * @param player   the player whose capability to access
   * @param consumer the consumer to apply on the capability data
   */
  public void retrieveData(Player player, Consumer<D> consumer) {
    player.getCapability(this.capability)
        .ifPresent(consumer::accept);
  }

  /**
   * Retrieves the capability data from the player, or {@code null} if not present.
   *
   * @param player the player whose capability to retrieve
   * @return the capability data or {@code null}
   */
  @SuppressWarnings("all")
  public D retrieveData(Player player) {
    return player.getCapability(this.capability)
        .orElse(null);
  }

  /**
   * Provides the capability instance to other systems.
   *
   * @param cap  the capability requested
   * @param side the direction (usually {@code null} for players)
   * @param <T>  the capability type
   * @return a {@link LazyOptional} containing the capability if matched, else empty
   */
  @Override
  public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
      @Nullable Direction side) {
    return this.capability.orEmpty(cap, LazyOptional.of(this::buildCap));
  }
}
