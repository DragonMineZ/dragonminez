package com.dragonminez.compat.capabilities;

import net.minecraft.core.Direction;
import com.dragonminez.compat.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility shim for Forge ICapabilityProvider.
 */
public interface ICapabilityProvider {
	@NotNull
	<T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side);

	@NotNull
	default <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
		return getCapability(cap, null);
	}
}
