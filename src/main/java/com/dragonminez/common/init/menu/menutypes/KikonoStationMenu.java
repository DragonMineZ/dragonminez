package com.dragonminez.common.init.menu.menutypes;

import com.dragonminez.common.init.MainMenus;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.entity.KikonoStationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class KikonoStationMenu extends AbstractContainerMenu {
	public final KikonoStationBlockEntity blockEntity;
	private final Level level;
	private final ContainerData data;

	public KikonoStationMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
		this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
	}

	public KikonoStationMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
		super(MainMenus.KIKONO_STATION_MENU.get(), pContainerId);
		checkContainerSize(inv, 12);
		this.blockEntity = ((KikonoStationBlockEntity) entity);
		this.level = inv.player.level();
		this.data = data;

		addPlayerInv(inv);
		addPlayerHotbar(inv);

		this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
			int startX = 28;
			int startY = 17;
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					this.addSlot(new SlotItemHandler(handler, col + row * 3, startX + col * 18, startY + row * 18));
				}
			}

			this.addSlot(new SlotItemHandler(handler, 9, 89, 17));  // PATTERN (Slot 9)
			this.addSlot(new SlotItemHandler(handler, 10, 89, 53)); // TEMPLATE/ARMOR (Slot 10)
			this.addSlot(new SlotItemHandler(handler, 11, 141, 35)); // OUTPUT (Slot 11)
		});

		addDataSlots(data);
	}

	public boolean isCrafting() {
		return data.get(0) > 0;
	}

	public int getScaledProgress() {
		int progress = this.data.get(0);
		int maxProgress = this.data.get(1);
		int arrowSize = 26;
		return maxProgress != 0 && progress != 0 ? progress * arrowSize / maxProgress : 0;
	}

	public int getEnergy() { return this.data.get(2); }
	public int getMaxEnergy() { return this.data.get(3); }

	public int getScaledEnergy() {
		int energy = this.data.get(2);
		int maxEnergy = this.data.get(3);
		int barHeight = 60;
		return maxEnergy != 0 && energy != 0 ? energy * barHeight / maxEnergy : 0;
	}

	private static final int VANILLA_SLOT_COUNT = 36;
	private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_SLOT_COUNT;
	private static final int TE_INVENTORY_SLOT_COUNT = 12;

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
				pPlayer, MainBlocks.KIKONO_STATION.get());
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
}