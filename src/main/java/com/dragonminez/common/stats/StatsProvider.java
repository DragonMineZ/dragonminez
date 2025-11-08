package com.dragonminez.common.stats;

import com.dragonminez.Reference;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "stats");

    private final StatsData data;
    private final LazyOptional<StatsData> optional;

    public StatsProvider(Player player) {
        this.data = new StatsData(player);
        this.optional = LazyOptional.of(() -> data);
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
        return entity.getCapability(cap);
    }

    void invalidate() {
        this.optional.invalidate();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.load(nbt);
    }
}

