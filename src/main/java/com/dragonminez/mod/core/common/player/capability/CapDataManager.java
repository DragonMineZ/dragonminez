package com.dragonminez.mod.core.common.player.capability;

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
 * Generic manager class for player capability data.
 * <p>
 * Handles the creation, update, and retrieval of a specific capability type {@code D}, which must
 * extend {@link CapDataHolder}. This class also implements {@link ICapabilityProvider} so it can be
 * registered and attached to player entities.
 *
 * @param <D> the type of capability data this manager handles
 */
public abstract class CapDataManager<D extends CapDataHolder> implements ICapabilityProvider {

  /**
   * The capability reference registered through Forge's capability system.
   */
  protected final Capability<D> capability;

  /**
   * Constructs a new data manager for the given capability type.
   *
   * @param capability the capability being managed
   */
  protected CapDataManager(Capability<D> capability) {
    this.capability = capability;
  }

  /**
   * Constructs and returns a new instance of the capability data. This is used when attaching the
   * capability to a player.
   *
   * @return a new instance of the capability data
   */
  public abstract D buildCap();

  /**
   * Replaces the existing capability data on a player with the data from another instance.
   * Typically used for syncing or loading saved data.
   *
   * @param player  the player whose data should be updated
   * @param newData the new data to apply to the player
   */
  public void update(Player player, D newData) {
    this.retrieveData(player, oldData ->
        oldData.deserialize(newData.serialize(new CompoundTag())));
  }

  /**
   * Retrieves the capability data from the player and applies the given consumer if present.
   *
   * @param player   the player whose data is being accessed
   * @param consumer the logic to apply to the capability if found
   */
  public void retrieveData(Player player, Consumer<D> consumer) {
    player.getCapability(this.capability)
        .ifPresent(consumer::accept);
  }

  /**
   * Retrieves the capability data from the player.
   * <p>
   * If the capability is not present, returns {@code null}.
   *
   * @param player the player whose data is being accessed
   * @return the capability data if present, otherwise {@code null}
   */
  @SuppressWarnings("all")
  public D retrieveData(Player player) {
    return player.getCapability(this.capability)
        .orElse(null);
  }

  /**
   * Provides access to the capability instance.
   * <p>
   * Called by Forge when another system requests the capability from this provider.
   *
   * @param cap  the capability being requested
   * @param side the direction the capability is being requested from (usually null for players)
   * @param <T>  the type of the requested capability
   * @return a {@link LazyOptional} containing the capability if it matches, otherwise empty
   */
  @Override
  public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
      @Nullable Direction side) {
    return this.capability.orEmpty(cap, LazyOptional.of(this::buildCap));
  }
}
