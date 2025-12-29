package com.dragonminez.common.init.block.entity;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.server.energy.StarEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class EnergyCableBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(50, 5) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = super.receiveEnergy(maxReceive, simulate);
			if(received > 0 && !simulate) {
				onEnergyChanged();
			}
			return received;
		}
		@Override
		public void onEnergyChanged() { setChanged(); }
	};

	private final LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.of(() -> energyStorage);

	public EnergyCableBlockEntity(BlockPos pPos, BlockState pState) {
		super(MainBlockEntities.ENERGY_CABLE_BE.get(), pPos, pState);
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if(pLevel == null || pLevel.isClientSide) return;
		if(energyStorage.getEnergyStored() <= 0) return;

		int toDistribute = energyStorage.getEnergyStored();
		int distributed = 0;

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pPos.relative(dir);
			BlockEntity be = pLevel.getBlockEntity(neighborPos);
			if (be != null && !(be instanceof EnergyCableBlockEntity) && isDMZBlock(pLevel.getBlockState(neighborPos).getBlock())) {
				distributed += pushTo(be, dir.getOpposite(), toDistribute - distributed);
				if(distributed >= toDistribute) break;
			}
		}

		if(distributed < toDistribute) {
			for (Direction dir : Direction.values()) {
				BlockEntity be = pLevel.getBlockEntity(pPos.relative(dir));
				if (be instanceof EnergyCableBlockEntity) {
					distributed += pushTo(be, dir.getOpposite(), toDistribute - distributed);
					if(distributed >= toDistribute) break;
				}
			}
		}

		energyStorage.extractEnergy(distributed, false);
	}

	private boolean isDMZBlock(Block block) {
		var key = ForgeRegistries.BLOCKS.getKey(block);
		return key != null && key.getNamespace().equals(Reference.MOD_ID);
	}

	private int pushTo(BlockEntity be, Direction side, int amount) {
		return be.getCapability(ForgeCapabilities.ENERGY, side)
				.map(e -> e.receiveEnergy(amount, false))
				.orElse(0);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		return tAnimationState.setAndContinue(RawAnimation.begin().then("animation.energy_cable.idle", Animation.LoopType.LOOP));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtils.getCurrentTick();
	}
}