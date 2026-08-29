package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.FuelGeneratorBlock;
import com.dragonminez.common.init.menu.menutypes.FuelGeneratorMenu;
import com.dragonminez.server.energy.StarEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.RenderUtil;

public class FuelGeneratorBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) { setChanged(); }
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return stack.getBurnTime(RecipeType.SMELTING) > 0;
		}
	};

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(1000, 20) {
		@Override
		public void onEnergyChanged() { setChanged(); }
		@Override
		public boolean canReceive() { return false; }
	};

	protected final ContainerData data;
	private int burnTime;
	private int maxBurnTime;

	public FuelGeneratorBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(MainBlockEntities.FUEL_GENERATOR_BE.get(), pPos, pBlockState);
		this.data = new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> burnTime;
					case 1 -> maxBurnTime;
					case 2 -> energyStorage.getEnergyStored();
					case 3 -> energyStorage.getMaxEnergyStored();
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
				switch (index) {
					case 0 -> burnTime = value;
					case 1 -> maxBurnTime = value;
					case 2 -> energyStorage.setEnergy(value);
				}
			}

			@Override
			public int getCount() { return 4; }
		};
	}

	public IItemHandler getItemHandler() {
		return itemHandler;
	}

	public IEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel.isClientSide) return;

		boolean isBurning = burnTime > 0;
		boolean changed = false;

		if (isBurning) {
			--burnTime;
			// Triple generation speed (not amount): +1 on 3 of every 4 ticks instead of 1 of every 4.
			if (burnTime % 4 != 0) {
				int current = energyStorage.getEnergyStored();
				int max = energyStorage.getMaxEnergyStored();
				energyStorage.setEnergy(Math.min(current + 1, max));
			}
			changed = true;
		}

		if (burnTime <= 0 && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
			ItemStack fuel = itemHandler.getStackInSlot(0);
			Item fuelItem = fuel.getItem();
			if (!fuel.isEmpty()) {
				int fuelTime = fuel.getBurnTime(RecipeType.SMELTING);
				if (fuelTime > 0) {
					this.burnTime = fuelTime;
					this.maxBurnTime = fuelTime;
					fuel.shrink(1);
					if (fuel.isEmpty()) {
						var remainder = fuelItem.getCraftingRemainingItem(fuel);
						itemHandler.setStackInSlot(0, remainder);
					} else {
						itemHandler.setStackInSlot(0, fuel);
					}
					changed = true;
					if (!isBurning) pLevel.setBlock(pPos, pState.setValue(FuelGeneratorBlock.LIT, true), 3);
				}
			} else if (isBurning) {
				pLevel.setBlock(pPos, pState.setValue(FuelGeneratorBlock.LIT, false), 3);
			}
		}

		distributeEnergy();
		if (changed) setChanged();
	}

	private void distributeEnergy() {
		if (energyStorage.getEnergyStored() <= 0 || level == null) return;

		for (Direction dir : Direction.values()) {
			BlockPos neighbor = worldPosition.relative(dir);
			IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite());
			if (target != null && target.canReceive()) {
				int sent = target.receiveEnergy(Math.min(energyStorage.getEnergyStored(), 256), false);
				energyStorage.extractEnergy(sent, false);
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
		pTag.put("inventory", itemHandler.serializeNBT(registries));
		pTag.putInt("burnTime", burnTime);
		energyStorage.saveNBT(pTag);
		super.saveAdditional(pTag, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
		super.loadAdditional(pTag, registries);
		if (pTag.contains("inventory")) {
			itemHandler.deserializeNBT(registries, pTag.getCompound("inventory"));
		}
		burnTime = pTag.getInt("burnTime");
		energyStorage.loadNBT(pTag);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.dragonminez.fuel_generator");
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		return tAnimationState.setAndContinue(RawAnimation.begin().then("animation.fuel_generator.idle", Animation.LoopType.LOOP));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtil.getCurrentTick();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new FuelGeneratorMenu(pContainerId, pPlayerInventory, this, this.data);
	}

}
