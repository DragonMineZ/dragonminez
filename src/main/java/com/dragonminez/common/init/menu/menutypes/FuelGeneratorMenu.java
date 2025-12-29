package com.dragonminez.common.init.menu.menutypes;

import com.dragonminez.common.init.MainMenus;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.entity.FuelGeneratorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class FuelGeneratorMenu extends AbstractContainerMenu {
	public final FuelGeneratorBlockEntity blockEntity;
	private final Level level;
	private final ContainerData data;

	public FuelGeneratorMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
		this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
	}

	public FuelGeneratorMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
		super(MainMenus.FUEL_GENERATOR_MENU.get(), pContainerId);
		checkContainerSize(inv, 1);
		this.blockEntity = ((FuelGeneratorBlockEntity) entity);
		this.level = inv.player.level();
		this.data = data;

		addPlayerInv(inv);
		addPlayerHotbar(inv);

		this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
			this.addSlot(new SlotItemHandler(handler, 0, 80, 38));
		});

		addDataSlots(data);
	}

	public boolean isBurning() {
		return data.get(0) > 0;
	}

	public int getScaledBurnTime() {
		int burnTime = this.data.get(0);
		int maxBurnTime = this.data.get(1);
		int flameSize = 14;
		if (maxBurnTime == 0) return 0;
		return (maxBurnTime - burnTime) * flameSize / maxBurnTime;
	}

	public int getScaledEnergy() {
		int energy = this.data.get(2);
		int maxEnergy = this.data.get(3);
		int barHeight = 60;
		return maxEnergy != 0 && energy != 0 ? energy * barHeight / maxEnergy : 0;
	}

	public int getEnergy() { return this.data.get(2); }
	public int getMaxEnergy() { return this.data.get(3); }

	private static final int VANILLA_SLOT_COUNT = 36;
	private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_SLOT_COUNT;
	private static final int TE_INVENTORY_SLOT_COUNT = 1;

	@Override
	public ItemStack quickMoveStack(Player playerIn, int pIndex) {
		Slot sourceSlot = slots.get(pIndex);
		if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
		ItemStack sourceStack = sourceSlot.getItem();
		ItemStack copyOfSourceStack = sourceStack.copy();

		if (pIndex < VANILLA_SLOT_COUNT) {
			if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
				return ItemStack.EMPTY;
			}
		} else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
			if (!moveItemStackTo(sourceStack, 0, VANILLA_SLOT_COUNT, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (sourceStack.getCount() == 0) {
			sourceSlot.set(ItemStack.EMPTY);
		} else {
			sourceSlot.setChanged();
		}
		sourceSlot.onTake(playerIn, sourceStack);
		return copyOfSourceStack;
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
				pPlayer, MainBlocks.FUEL_GENERATOR.get());
	}

	private void addPlayerInv(Inventory playerInv) {
		for (int i = 0; i < 3; ++i) {
			for (int l = 0; l < 9; ++l) {
				this.addSlot(new Slot(playerInv, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
			}
		}
	}

	private void addPlayerHotbar(Inventory playerInv) {
		for (int i = 0; i < 9; ++i) {
			this.addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
		}
	}

	public ContainerData getData() {
		return data;
	}
}