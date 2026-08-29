package com.dragonminez.common.init.block.entity;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.init.MainRecipes;
import com.dragonminez.common.init.MainBlockEntities;
import com.dragonminez.common.init.menu.menutypes.KikonoStationMenu;
import com.dragonminez.server.energy.StarEnergyStorage;
import com.dragonminez.server.recipes.KikonoRecipe;
import com.dragonminez.server.recipes.KikonoRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.RenderUtil;

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

	public IItemHandler getItemHandler() {
		return itemHandler;
	}

	public IEnergyStorage getEnergyStorage() {
		return energyStorage;
	}

	public void tick(Level pLevel, BlockPos pPos, BlockState pState) {
		if (pLevel.isClientSide) return;

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
					pLevel.playSound(null, pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
					craftItem(recipe.get());
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
		if (level == null) return Optional.empty();
		NonNullList<ItemStack> items = NonNullList.withSize(itemHandler.getSlots(), ItemStack.EMPTY);
		for (int i = 0; i < itemHandler.getSlots(); i++) {
			items.set(i, itemHandler.getStackInSlot(i));
		}
		KikonoRecipeInput input = new KikonoRecipeInput(items);
		return level.getRecipeManager().getRecipeFor(MainRecipes.KIKONO_TYPE.get(), input, level)
				.map(holder -> holder.value());
	}

	private boolean canOutput(KikonoRecipe recipe) {
		ItemStack result = recipe.getResultItem(level != null ? level.registryAccess() : null);
		ItemStack outputSlot = itemHandler.getStackInSlot(11);
		if (outputSlot.isEmpty()) return true;
		if (!outputSlot.is(result.getItem())) return false;
		return outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize();
	}

	private void craftItem(KikonoRecipe recipe) {
		ItemStack result = getOutputWithEnchantments(
				recipe.getResultItem(level != null ? level.registryAccess() : null),
				itemHandler.getStackInSlot(10)
		);

		for (int i = 0; i <= 10; i++) itemHandler.extractItem(i, 1, false);
		itemHandler.insertItem(11, result, false);
	}

	@Override
	protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
		pTag.put("inventory", itemHandler.serializeNBT(registries));
		pTag.putInt("progress", progress);
		energyStorage.saveNBT(pTag);
		super.saveAdditional(pTag, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider registries) {
		super.loadAdditional(pTag, registries);
		if (pTag.contains("inventory")) {
			itemHandler.deserializeNBT(registries, pTag.getCompound("inventory"));
		}
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
	public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
		controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
	}

	private <T extends GeoAnimatable> PlayState predicate(AnimationState<T> tAnimationState) {
		if (this.progress > 0) {
			return tAnimationState.setAndContinue(RawAnimation.begin().then("work", Animation.LoopType.LOOP));
		}
		return tAnimationState.setAndContinue(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
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
		return new KikonoStationMenu(pContainerId, pPlayerInventory, this, this.data);
	}

	private ItemStack getOutputWithEnchantments(ItemStack itemStack, ItemStack template) {
		var output = itemStack.copy();

		// Soft-deps for WeaponLeveling/Apotheosis removed in NeoForge port.
		// Preserve vanilla enchantment copy from template when enabled.
		if (ConfigManager.getServerConfig().getCrafting().getCopyEnchantmentsFromTemplate()) {
			ItemEnchantments enchantments = template.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
			if (!enchantments.isEmpty()) {
				output.set(DataComponents.ENCHANTMENTS, enchantments);
			}
		}

		return output;
	}

}
