package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.compat.capabilities.Capability;
import com.dragonminez.compat.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.INBTSerializable;
import com.dragonminez.compat.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Player stats provider backed by a serializable NeoForge data attachment.
 */
public class StatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = ResourceLocation.parse(Reference.MOD_ID);

    private final StatsData data;
    private final LazyOptional<StatsData> optional;

    public StatsProvider(Player player) {
        this.data = new StatsData(player);
        this.optional = LazyOptional.of(() -> data);
    }

	public static StatsProvider getOrCreate(Player player) {
		return player.getData(StatsCapability.PLAYER_STATS.get());
	}

	public static void remove(Player player) {
		// Attachments are owned by the entity and discarded with it.
	}

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == StatsCapability.INSTANCE) {
            return this.optional.cast();
        }
        return LazyOptional.empty();
    }

    public static @NotNull <T> LazyOptional<T> get(Capability<T> cap, Entity entity) {
		if (!(entity instanceof Player player)) {
			return LazyOptional.empty();
		}
		if (cap != StatsCapability.INSTANCE) {
			return LazyOptional.empty();
		}
		@SuppressWarnings("unchecked")
		LazyOptional<T> result = (LazyOptional<T>) LazyOptional.of(() -> getOrCreate(player).data);
		return result;
    }

    void invalidate() {
        this.optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return data.save();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
		try {
			data.load(nbt);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
