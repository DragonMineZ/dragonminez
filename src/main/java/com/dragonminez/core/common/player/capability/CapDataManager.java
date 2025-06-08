package com.dragonminez.core.common.player.capability;

import com.dragonminez.core.common.network.capability.PacketS2CCapSync;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic manager for a specific player capability.
 * <p>
 * Handles instance creation, data retrieval, updates, and sync operations for capabilities of type
 * {@code C}, where {@code C} implements {@link ICap}.
 *
 * @param <C> the capability type managed by this instance
 */
public abstract class CapDataManager<C extends ICap> {

  private final ResourceLocation id;
  private final Capability<C> capability;

  /**
   * Constructs a capability data manager.
   *
   * @param id         the unique identifier for the capability
   * @param capability the Forge capability instance
   */
  public CapDataManager(ResourceLocation id, Capability<C> capability) {
    this.id = id;
    this.capability = capability;
  }

  /**
   * Creates a new instance of the capability's backing data.
   *
   * @return a new capability data object
   */
  public abstract C buildCap();

  /**
   * Builds a sync packet to be sent to clients.
   *
   * @param player   the player whose data is being synced, or {@code null} for none
   * @param data     the capability data to send, or {@code null}
   * @param isPublic whether the data is visible to other clients
   * @return a new capability sync packet
   */
  public abstract PacketS2CCapSync<C> buildSyncPacket(@Nullable Player player, @Nullable C data,
      @Nullable Boolean isPublic);

  /**
   * Constructs a mock sync packet for network registration.
   *
   * @param isPublic whether the packet should be marked as public
   * @return a placeholder sync packet
   */
  public PacketS2CCapSync<C> buildMockSyncPacket(boolean isPublic) {
    return this.buildSyncPacket(null, null, isPublic);
  }

  /**
   * Updates the capability data of a player with the given new instance.
   *
   * @param player  the target player
   * @param newData the new capability data to apply
   * @param cloned  whether the data was cloned (affects deserialization)
   */
  public void update(Player player, C newData, boolean cloned) {
    this.retrieveData(player, oldData ->
        oldData.deserialize(newData.serialize(new CompoundTag()), cloned));
  }

  /**
   * Updates the capability data of a player assuming the input data is not cloned.
   *
   * @param player  the target player
   * @param newData the new capability data to apply
   */
  @SuppressWarnings("unchecked")
  public void update(Player player, ICap newData) {
    this.update(player, (C) newData, false);
  }

  /**
   * Copies capability data from one player to another.
   *
   * @param reference the player to copy data from
   * @param target    the player to copy data to
   */
  public void update(Player reference, Player target) {
    final C data = this.retrieveData(reference);
    if (data != null) {
      this.update(target, data, true);
    }
  }

  /**
   * Retrieves a single value from a player's capability data.
   *
   * @param player the player
   * @param key    the data key
   * @return the value, or {@code null} if not found
   */
  public Object get(Player player, String key) {
    final C cap = this.retrieveData(player);
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
   * Runs an operation on a player's capability data if it exists.
   *
   * @param player   the player
   * @param consumer the operation to apply
   */
  public void retrieveData(Player player, NonNullConsumer<C> consumer) {
    player.getCapability(this.capability).ifPresent(consumer);
  }

  /**
   * Retrieves the capability data from a player.
   *
   * @param player the player
   * @return the capability data, or {@code null} if absent
   */
  @SuppressWarnings("all")
  public C retrieveData(Player player) {
    return player.getCapability(this.capability).orElse(null);
  }

  /**
   * Gets the capability ID.
   *
   * @return the resource location identifier
   */
  public ResourceLocation id() {
    return this.id;
  }

  /**
   * Gets the Forge capability instance.
   *
   * @return the capability
   */
  public Capability<C> capability() {
    return this.capability;
  }

  /**
   * A default capability provider that holds a single instance of the capability data.
   *
   * @param <C> the type of capability being held
   */
  public static class CapInstanceProvider<C extends ICap> implements ICapabilityProvider {

    private final ResourceLocation managerId;
    private final C instance;
    private final LazyOptional<C> optional;

    /**
     * Constructs a new capability provider for the given manager.
     *
     * @param manager the capability manager
     */
    public CapInstanceProvider(CapDataManager<C> manager) {
      this.managerId = manager.id();
      this.instance = manager.buildCap();
      this.optional = LazyOptional.of(() -> this.instance);
    }

    /**
     * Provides the capability instance for the given capability type.
     *
     * @param cap  the capability requested
     * @param side the side requested (usually {@code null} for entities)
     * @param <T>  the expected capability type
     * @return the optional containing the instance, or empty if not matched
     */
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,
        @Nullable Direction side) {
      final CapDataManager<?> manager = CapManagerRegistry.INSTANCE.get(FMLEnvironment.dist,
          this.managerId);
      if (manager == null) {
        return LazyOptional.empty();
      }

      if (cap.equals(manager.capability())) {
        @SuppressWarnings("unchecked")
        LazyOptional<T> cast = (LazyOptional<T>) this.optional;
        return cast;
      }

      return LazyOptional.empty();
    }
  }
}
