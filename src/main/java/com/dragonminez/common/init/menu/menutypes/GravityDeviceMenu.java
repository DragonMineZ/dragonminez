package com.dragonminez.common.init.menu.menutypes;

import com.dragonminez.common.init.MainMenus;
import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.block.entity.GravityDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GravityDeviceMenu extends AbstractContainerMenu {
	public final GravityDeviceBlockEntity blockEntity;
	private final Level level;
	private final ContainerData data;

	public GravityDeviceMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
		this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(6));
	}

	public GravityDeviceMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
		super(MainMenus.GRAVITY_DEVICE_MENU.get(), pContainerId);
		this.blockEntity = ((GravityDeviceBlockEntity) entity);
		this.level = inv.player.level();
		this.data = data;

		addPlayerInv(inv);
		addPlayerHotbar(inv);

		addDataSlots(data);
	}

	public BlockPos getBlockPos() {
		return blockEntity.getBlockPos();
	}

	public boolean isActive() { return data.get(0) > 0; }
	public int getTargetGravity() { return data.get(1); }
	public int getEnergy() { return data.get(2); }
	public int getMaxEnergy() { return data.get(3); }
	public boolean isRoomValid() { return data.get(4) > 0; }
	public boolean isRunning() { return data.get(5) > 0; }

	public int getScaledEnergy() {
		int energy = this.data.get(2);
		int maxEnergy = this.data.get(3);
		int barHeight = 60;
		return maxEnergy != 0 && energy != 0 ? energy * barHeight / maxEnergy : 0;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int pIndex) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
				pPlayer, MainBlocks.GRAVITY_DEVICE.get());
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
