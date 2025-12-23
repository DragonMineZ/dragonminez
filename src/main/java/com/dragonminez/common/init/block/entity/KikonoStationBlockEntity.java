package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.init.MainRecipes;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import com.dragonminez.server.energy.StarEnergyStorage;
import com.dragonminez.server.recipes.KikonoRecipe;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Optional;

public class KikonoStationBlockEntity extends BlockEntity implements MenuProvider, GeoBlockEntity {
	private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
	// 0-8 Input, 9 Pattern, 10 Template, 11 Output
	private final ItemStackHandler itemHandler = new ItemStackHandler(12) {
		@Override
		protected void onContentsChanged(int slot) { setChanged(); }
	};

	private final StarEnergyStorage energyStorage = new StarEnergyStorage(2000, 20) {
		@Override
		public void onEnergyChanged() { setChanged(); }
		@Override
		public boolean canExtract() { return false; }
	};

	private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
	private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

	protected final ContainerData data;
	private int progress = 0;
	private int maxProgress = 0;

	public KikonoStationBlockEntity(BlockPos pPos, BlockState pBlockState) {
		super(MainBlockEntities.KIKONO_STATION_BE.get(), pPos, pBlockState);
		this.data = new ContainerData() {
			@Override
			public int get(int pIndex) {
				return switch (pIndex) {
					case 0 -> progress;
					case 1 -> maxProgress;
					case 2 -> energyStorage.getEnergyStored();
					case 3 -> energyStorage.getMaxEnergyStored();
					default -> 0;
				};
			}
			@Override
			public void set(int pIndex, int pValue) {
				switch (pIndex) {
					case 0 -> progress = pValue;
					case 1 -> maxProgress = pValue;
					case 2 -> energyStorage.setEnergy(pValue);
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
		if(pLevel.isClientSide) return;

		Optional<KikonoRecipe> recipe = getCurrentRecipe();

		if (recipe.isPresent() && canOutput(recipe.get())) {
			this.maxProgress = recipe.get().getCraftingTime();

			int totalEnergyCost = recipe.get().getEnergyCost();
			int costPerTick = 0;

			if (totalEnergyCost > 0 && this.maxProgress > 0) {
				costPerTick = (totalEnergyCost + this.maxProgress - 1) / this.maxProgress;
			}

			int currentEnergy = energyStorage.getEnergyStored();

			if (currentEnergy >= costPerTick) {
				energyStorage.setEnergy(currentEnergy - costPerTick);

				progress++;
				if (progress >= maxProgress) {
					pLevel.playSound(null, pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);craftItem(recipe.get());
					progress = 0;
				}
			} else {
				if (progress > 0) progress--;
			}
		} else {
			progress = 0;
		}
		setChanged();
	}

	private Optional<KikonoRecipe> getCurrentRecipe() {
		SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
		for (int i = 0; i < itemHandler.getSlots(); i++) inventory.setItem(i, itemHandler.getStackInSlot(i));
		return level.getRecipeManager().getRecipeFor(MainRecipes.KIKONO_TYPE.get(), inventory, level);
	}

	private boolean canOutput(KikonoRecipe recipe) {
		ItemStack result = recipe.getResultItem(null);
		ItemStack outputSlot = itemHandler.getStackInSlot(11);
		if (outputSlot.isEmpty()) return true;
		if (!outputSlot.is(result.getItem())) return false;
		return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
	}

	private void craftItem(KikonoRecipe recipe) {
		ItemStack result = recipe.getResultItem(null);
		for (int i = 0; i <= 10; i++) itemHandler.extractItem(i, 1, false);
		itemHandler.insertItem(11, result.copy(), false);
	}

	@Override
	protected void saveAdditional(CompoundTag pTag) {
		pTag.put("inventory", itemHandler.serializeNBT());
		pTag.putInt("progress", progress);
		energyStorage.saveNBT(pTag);
		super.saveAdditional(pTag);
	}

	@Override
	public void load(CompoundTag pTag) {
		super.load(pTag);
		itemHandler.deserializeNBT(pTag.getCompound("inventory"));
		progress = pTag.getInt("progress");
		energyStorage.loadNBT(pTag);
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.dragonminez.kikono_station");
	}

	public void drops() {
		SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
		for (int i = 0; i < itemHandler.getSlots(); i++) inventory.setItem(i, itemHandler.getStackInSlot(i));
		Containers.dropContents(this.level, this.worldPosition, inventory);
	}

	@Override
	public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if(cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
		if(cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		if (this.progress > 0) {
			return tAnimationState.setAndContinue(RawAnimation.begin().then("animation.kikono_station.working", Animation.LoopType.LOOP));
		}
		return tAnimationState.setAndContinue(RawAnimation.begin().then("animation.kikono_station.idle", Animation.LoopType.LOOP));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtils.getCurrentTick();
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new KikonoStationMenu(pContainerId, pPlayerInventory, this, this.data);
	}
}
