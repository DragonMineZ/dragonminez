package com.dragonminez.common.init.block.entity;

import com.dragonminez.Reference;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.server.energy.StarEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.RenderUtil;

public class EnergyCableBlockEntity extends BlockEntity implements GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(150, 15) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = super.receiveEnergy(maxReceive, simulate);
			if (received > 0 && !simulate) {
				onEnergyChanged();
			}
			return received;
		}
		@Override
		public void onEnergyChanged() { setChanged(); }
	};

	public EnergyCableBlockEntity(BlockPos pPos, BlockState pState) {
		super(MainBlockEntities.ENERGY_CABLE_BE.get(), pPos, pState);
	}

	public IEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel == null || pLevel.isClientSide) return;
		if (energyStorage.getEnergyStored() <= 0) return;

		int toDistribute = energyStorage.getEnergyStored();
		int distributed = 0;

		for (Direction dir : Direction.values()) {
			BlockPos neighborPos = pPos.relative(dir);
			BlockEntity be = pLevel.getBlockEntity(neighborPos);
			if (be != null && !(be instanceof EnergyCableBlockEntity) && isDMZBlock(pLevel.getBlockState(neighborPos).getBlock())) {
				distributed += pushTo(pLevel, neighborPos, dir.getOpposite(), toDistribute - distributed);
				if (distributed >= toDistribute) break;
			}
		}

		if (distributed < toDistribute) {
			for (Direction dir : Direction.values()) {
				BlockEntity be = pLevel.getBlockEntity(pPos.relative(dir));
				if (be instanceof EnergyCableBlockEntity) {
					distributed += pushTo(pLevel, pPos.relative(dir), dir.getOpposite(), toDistribute - distributed);
					if (distributed >= toDistribute) break;
				}
			}
		}

		energyStorage.extractEnergy(distributed, false);
	}

	private boolean isDMZBlock(Block block) {
		var key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null && key.getNamespace().equals(Reference.MOD_ID);
	}

	private int pushTo(Level level, BlockPos pos, Direction side, int amount) {
		IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
		if (energy == null) return 0;
		return energy.receiveEnergy(amount, false);
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
		return RenderUtil.getCurrentTick();
	}

}
