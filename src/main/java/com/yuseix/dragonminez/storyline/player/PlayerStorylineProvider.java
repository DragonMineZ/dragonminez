package com.yuseix.dragonminez.storyline.player;

import com.yuseix.dragonminez.DragonMineZ;
import com.yuseix.dragonminez.storyline.StorylineManager;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerStorylineProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

	public static final ResourceLocation ID = new ResourceLocation(DragonMineZ.MOD_ID, "storyline");

	public static Capability<StorylineManager> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
	});
	private final LazyOptional<StorylineManager> optional = LazyOptional.of(this::getStorylineBackend);

	private StorylineManager storylineManager = null;

	private StorylineManager getStorylineBackend() {
		if (this.storylineManager == null) {
			this.storylineManager = new StorylineManager();
		}
		return this.storylineManager;
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == CAPABILITY) {
			return this.optional.cast();
		} else {
			return LazyOptional.empty();
		}
	}

	@Override
	public CompoundTag serializeNBT() {

		CompoundTag nbt = new CompoundTag();
		getStorylineBackend().saveNBTData(nbt);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		getStorylineBackend().loadNBTData(nbt);
	}

}
