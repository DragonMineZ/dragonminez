package com.dragonminez.common.init.menu.menutypes;

import com.dragonminez.common.init.MainBlocks;
import com.dragonminez.common.init.MainMenus;
import com.dragonminez.common.init.MainRecipes;
import com.dragonminez.server.recipes.PatternRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

import java.util.Optional;

public class PatternStationMenu extends AbstractContainerMenu {
	public static final int GRID_SIZE = 6;
	public static final int RESULT_SLOT = 0;
	public static final int CRAFT_SLOT_START = 1;
	public static final int CRAFT_SLOT_END = CRAFT_SLOT_START + GRID_SIZE * GRID_SIZE;
	public static final int INV_SLOT_START = CRAFT_SLOT_END;
	public static final int INV_SLOT_END = INV_SLOT_START + 27;
	public static final int USE_ROW_SLOT_START = INV_SLOT_END;
	public static final int USE_ROW_SLOT_END = USE_ROW_SLOT_START + 9;

	public static final int GRID_X = 8;
	public static final int GRID_Y = 24;
	public static final int RESULT_X = 217;
	public static final int RESULT_Y = 24;
	public static final int INV_X = 130;
	public static final int INV_Y = 64;
	public static final int HOTBAR_Y = 122;

	private final CraftingContainer craftSlots = new TransientCraftingContainer(this, GRID_SIZE, GRID_SIZE);
	private final ResultContainer resultSlots = new ResultContainer();
	private final ContainerLevelAccess access;
	private final Player player;

	public PatternStationMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
		this(containerId, inv, ContainerLevelAccess.NULL);
	}

	public PatternStationMenu(int containerId, Inventory inv, ContainerLevelAccess access) {
		super(MainMenus.PATTERN_STATION_MENU.get(), containerId);
		this.access = access;
		this.player = inv.player;

		this.addSlot(new PatternResultSlot(inv.player, this.craftSlots, this.resultSlots, 0, RESULT_X, RESULT_Y));

		for (int row = 0; row < GRID_SIZE; ++row) {
			for (int col = 0; col < GRID_SIZE; ++col) {
				this.addSlot(new Slot(this.craftSlots, col + row * GRID_SIZE, GRID_X + col * 18, GRID_Y + row * 18));
			}
		}

		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				this.addSlot(new Slot(inv, col + (row + 1) * 9, INV_X + col * 18, INV_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; ++col) {
			this.addSlot(new Slot(inv, col, INV_X + col * 18, HOTBAR_Y));
		}
	}

	protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots) {
		if (level.isClientSide) return;
		ServerPlayer serverPlayer = (ServerPlayer) player;
		ItemStack result = ItemStack.EMPTY;
		Optional<PatternRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(MainRecipes.PATTERN_TYPE.get(), craftSlots, level);
		if (optional.isPresent()) {
			PatternRecipe recipe = optional.get();
			if (resultSlots.setRecipeUsed(level, serverPlayer, recipe)) {
				ItemStack assembled = recipe.assemble(craftSlots, level.registryAccess());
				if (assembled.isItemEnabled(level.enabledFeatures())) {
					result = assembled;
				}
			}
		}

		resultSlots.setItem(0, result);
		menu.setRemoteSlot(0, result);
		serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, result));
	}

	@Override
	public void slotsChanged(Container container) {
		this.access.execute((level, pos) -> slotChangedCraftingGrid(this, level, this.player, this.craftSlots, this.resultSlots));
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, MainBlocks.PATTERN_STATION.get());
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack copy = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack stack = slot.getItem();
			copy = stack.copy();
			if (index == RESULT_SLOT) {
				this.access.execute((level, pos) -> stack.getItem().onCraftedBy(stack, level, player));
				if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
					return ItemStack.EMPTY;
				}
				slot.onQuickCraft(stack, copy);
			} else if (index >= INV_SLOT_START && index < USE_ROW_SLOT_END) {
				if (!this.moveItemStackTo(stack, CRAFT_SLOT_START, CRAFT_SLOT_END, false)) {
					if (index < INV_SLOT_END) {
						if (!this.moveItemStackTo(stack, USE_ROW_SLOT_START, USE_ROW_SLOT_END, false)) {
							return ItemStack.EMPTY;
						}
					} else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
						return ItemStack.EMPTY;
					}
				}
			} else if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}

			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (stack.getCount() == copy.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, stack);
			if (index == RESULT_SLOT) {
				player.drop(stack, false);
			}
		}
		return copy;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
	}

	public CraftingContainer getCraftSlots() {
		return this.craftSlots;
	}

	private static class PatternResultSlot extends ResultSlot {
		private final CraftingContainer craftSlots;
		private final Player player;

		public PatternResultSlot(Player player, CraftingContainer craftSlots, Container resultContainer, int slot, int x, int y) {
			super(player, craftSlots, resultContainer, slot, x, y);
			this.craftSlots = craftSlots;
			this.player = player;
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			this.checkTakeAchievements(stack);
			ForgeHooks.setCraftingPlayer(player);
			NonNullList<ItemStack> remaining = player.level().getRecipeManager()
					.getRemainingItemsFor(MainRecipes.PATTERN_TYPE.get(), this.craftSlots, player.level());
			ForgeHooks.setCraftingPlayer(null);

			for (int i = 0; i < remaining.size(); ++i) {
				ItemStack current = this.craftSlots.getItem(i);
				ItemStack leftover = remaining.get(i);
				if (!current.isEmpty()) {
					this.craftSlots.removeItem(i, 1);
					current = this.craftSlots.getItem(i);
				}

				if (!leftover.isEmpty()) {
					if (current.isEmpty()) {
						this.craftSlots.setItem(i, leftover);
					} else if (ItemStack.isSameItemSameTags(current, leftover)) {
						leftover.grow(current.getCount());
						this.craftSlots.setItem(i, leftover);
					} else if (!this.player.getInventory().add(leftover)) {
						this.player.drop(leftover, false);
					}
				}
			}
		}
	}
}
