package com.dragonminez.mod.core.common.player.capability;

import com.dragonminez.mod.common.player.cap.stat.StatData;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CapDataManager<D extends CapDataHolder> implements ICapabilityProvider {

  /**
   * Reference to the registered capability for {@link D}.
   */
  protected final Capability<D> capability;

  protected CapDataManager(Capability<D> capability) {
    this.capability = capability;
  }

  public abstract D buildCap();

  /**
   * Updates a player's {@link StatData} by overwriting it with the contents of another
   * {@link StatData} instance.
   *
   * @param player  The player whose data is being updated.
   * @param newData The new stat data to apply to the player.
   */
  public void update(Player player, D newData) {
    this.retrieveStatData(player, oldData ->
        oldData.deserializeNBT(newData.serializeNBT()));
  }

  /**
   * Retrieves a player's {@link StatData} and applies a consumer to it if present.
   *
   * @param player   The player whose stat data is to be retrieved.
   * @param consumer The consumer to apply if the capability exists.
   */
  public void retrieveStatData(Player player, Consumer<D> consumer) {
    player.getCapability(this.capability)
        .ifPresent(consumer::accept);
  }

  /**
   * Provides the {@link StatData} capability implementation to the capability system. This method
   * is part of Forge's {@link ICapabilityProvider} interface.
   *
   * @param cap  The requested capability type.
   * @param side The direction (not used for player entities).
   * @param <T>  The generic type of the capability.
   * @return A {@link LazyOptional} containing the capability if it matches.
   */
  @Override
  public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap,
      @Nullable Direction side) {
    return this.capability.orEmpty(cap, LazyOptional.of(this::buildCap));
  }
}
