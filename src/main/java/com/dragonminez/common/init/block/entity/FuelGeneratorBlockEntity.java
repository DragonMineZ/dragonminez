package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.block.custom.FuelGeneratorBlock;
import com.dragonminez.common.init.menu.menutypes.FuelGeneratorMenu;
import com.dragonminez.server.energy.StarEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FuelGeneratorBlockEntity extends BlockEntity implements MenuProvider {
	private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) { setChanged(); }
		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
		}
	};

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(1000, 20) {
		@Override
		public void onEnergyChanged() { setChanged(); }
		@Override
		public boolean canReceive() { return false; }
	};

	private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
	private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

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

	@Override
	public void onLoad() {
		super.onLoad();
		lazyItemHandler = LazyOptional.of(() -> itemHandler);
		lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		lazyItemHandler.invalidate();
		lazyEnergyHandler.invalidate();
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel.isClientSide) return;

		boolean isBurning = burnTime > 0;
		boolean changed = false;

		if (isBurning) {
			--burnTime;
			if (burnTime % 4 == 0) {
				int current = energyStorage.getEnergyStored();
				int max = energyStorage.getMaxEnergyStored();
				energyStorage.setEnergy(Math.min(current + 1, max));
			}
			changed = true;
		}

		if (burnTime <= 0 && energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored()) {
			ItemStack fuel = itemHandler.getStackInSlot(0);
			if (!fuel.isEmpty()) {
				int fuelTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
				if (fuelTime > 0) {
					this.burnTime = fuelTime;
					this.maxBurnTime = fuelTime;
					fuel.shrink(1);
					itemHandler.setStackInSlot(0, fuel);
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

	private boolean canBurn() {
		return energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored();
	}

	private void startBurn() {
		ItemStack fuel = itemHandler.getStackInSlot(0);
		if (!fuel.isEmpty()) {
			int time = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
			if (time > 0) {
				burnTime = time;
				maxBurnTime = time;
				fuel.shrink(1);
				setChanged();
			}
		}
	}

	private void distributeEnergy() {
		if(energyStorage.getEnergyStored() <= 0) return;

		for (Direction dir : Direction.values()) {
			BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
			if (be != null) {
				be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(e -> {
					if (e.canReceive()) {
						int sent = e.receiveEnergy(Math.min(energyStorage.getEnergyStored(), 256), false);
						energyStorage.extractEnergy(sent, false);
					}
				});
			}
		}
	}

	@Override
	protected void saveAdditional(CompoundTag pTag) {
		pTag.put("inventory", itemHandler.serializeNBT());
		pTag.putInt("burnTime", burnTime);
		energyStorage.saveNBT(pTag);
		super.saveAdditional(pTag);
	}

	@Override
	public void load(CompoundTag pTag) {
		super.load(pTag);
		itemHandler.deserializeNBT(pTag.getCompound("inventory"));
		burnTime = pTag.getInt("burnTime");
		energyStorage.loadNBT(pTag);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.dragonminez.fuel_generator");
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
		if(cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
		return super.getCapability(cap, side);
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new FuelGeneratorMenu(pContainerId, pPlayerInventory, this, this.data);
	}
}