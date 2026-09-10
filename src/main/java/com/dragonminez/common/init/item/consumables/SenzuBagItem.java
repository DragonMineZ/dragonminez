package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.init.MainTags;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SenzuBagItem extends Item {

	private static final String TAG_ITEMS = "Items";
	public static final int MAX_BEANS = 64;
	private static final int BAR_COLOR = Mth.color(0.4F, 0.8F, 0.4F);

	public SenzuBagItem(Properties properties) {
		super(properties);
	}

	public static boolean isAllowed(ItemStack stack) {
		return !stack.isEmpty() && stack.is(MainTags.Items.SENZU_BEANS);
	}

	public static int getContentCount(ItemStack bag) {
		return getContents(bag).mapToInt(ItemStack::getCount).sum();
	}

	public static Stream<ItemStack> getContents(ItemStack bag) {
		CompoundTag tag = bag.getTag();
		if (tag == null || !tag.contains(TAG_ITEMS)) return Stream.empty();
		return tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND).stream()
				.map(CompoundTag.class::cast)
				.map(ItemStack::of);
	}

	private static int add(ItemStack bag, ItemStack inserted) {
		if (inserted.isEmpty() || !isAllowed(inserted)) return 0;

		int space = MAX_BEANS - getContentCount(bag);
		if (space <= 0) return 0;
		int moved = Math.min(space, inserted.getCount());

		CompoundTag tag = bag.getOrCreateTag();
		if (!tag.contains(TAG_ITEMS)) tag.put(TAG_ITEMS, new ListTag());
		ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);

		Optional<CompoundTag> existing = findStackableEntry(list, inserted);
		if (existing.isPresent()) {
			CompoundTag entry = existing.get();
			ItemStack stored = ItemStack.of(entry);
			stored.grow(moved);
			stored.save(entry);
		} else {
			ItemStack copy = inserted.copyWithCount(moved);
			CompoundTag entry = new CompoundTag();
			copy.save(entry);
			list.add(0, entry);
		}
		return moved;
	}

	private static Optional<CompoundTag> findStackableEntry(ListTag list, ItemStack inserted) {
		if (inserted.isDamaged()) return Optional.empty();
		for (Tag raw : list) {
			CompoundTag entry = (CompoundTag) raw;
			ItemStack stored = ItemStack.of(entry);
			if (ItemStack.isSameItemSameTags(stored, inserted)) return Optional.of(entry);
		}
		return Optional.empty();
	}

	private static Optional<ItemStack> removeOne(ItemStack bag) {
		CompoundTag tag = bag.getOrCreateTag();
		if (!tag.contains(TAG_ITEMS)) return Optional.empty();
		ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
		if (list.isEmpty()) return Optional.empty();

		CompoundTag entry = list.getCompound(0);
		ItemStack stored = ItemStack.of(entry);
		int taken = Math.min(stored.getCount(), stored.getMaxStackSize());
		ItemStack out = stored.copyWithCount(taken);
		stored.shrink(taken);

		if (stored.isEmpty()) {
			list.remove(0);
			if (list.isEmpty()) tag.remove(TAG_ITEMS);
		} else {
			stored.save(entry);
		}
		return Optional.of(out);
	}

	private static boolean dropContents(ItemStack bag, Player player) {
		CompoundTag tag = bag.getOrCreateTag();
		if (!tag.contains(TAG_ITEMS)) return false;

		if (player instanceof net.minecraft.server.level.ServerPlayer) {
			ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				ItemStack stored = ItemStack.of(list.getCompound(i));
				while (!stored.isEmpty()) {
					int chunk = Math.min(stored.getCount(), stored.getMaxStackSize());
					player.drop(stored.copyWithCount(chunk), true);
					stored.shrink(chunk);
				}
			}
		}
		bag.removeTagKey(TAG_ITEMS);
		return true;
	}

	@Override
	public boolean overrideStackedOnOther(@NotNull ItemStack bag, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
		if (action != ClickAction.SECONDARY) return false;

		ItemStack other = slot.getItem();
		if (other.isEmpty()) {
			removeOne(bag).ifPresent(stack -> {
				playRemoveOneSound(player);
				add(bag, slot.safeInsert(stack));
			});
			return true;
		}
		if (isAllowed(other)) {
			int space = MAX_BEANS - getContentCount(bag);
			if (space > 0 && add(bag, slot.safeTake(other.getCount(), space, player)) > 0) {
				playInsertSound(player);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean overrideOtherStackedOnMe(@NotNull ItemStack bag, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull net.minecraft.world.entity.SlotAccess access) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

		if (other.isEmpty()) {
			removeOne(bag).ifPresent(stack -> {
				playRemoveOneSound(player);
				access.set(stack);
			});
			return true;
		}
		if (isAllowed(other)) {
			int moved = add(bag, other);
			if (moved > 0) {
				playInsertSound(player);
				other.shrink(moved);
			}
			return true;
		}
		return false;
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack bag = player.getItemInHand(hand);
		if (dropContents(bag, player)) {
			playDropContentsSound(player);
			player.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResultHolder.sidedSuccess(bag, level.isClientSide());
		}
		return InteractionResultHolder.fail(bag);
	}

	@Override
	public boolean isBarVisible(@NotNull ItemStack stack) {
		return getContentCount(stack) > 0;
	}

	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		return Math.min(1 + 12 * getContentCount(stack) / MAX_BEANS, 13);
	}

	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public boolean isEnchantable(@NotNull ItemStack stack) {
		return false;
	}

	@Override
	public void onDestroyed(@NotNull net.minecraft.world.entity.item.ItemEntity itemEntity) {
		net.minecraft.world.item.ItemUtils.onContainerDestroyed(itemEntity, getContents(itemEntity.getItem()));
	}

	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
		List<ItemStack> contents = getContents(stack).toList();
		if (contents.isEmpty()) {
			tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.empty").withStyle(ChatFormatting.GRAY));
		} else {
			for (ItemStack stored : new ArrayList<>(contents)) {
				tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.entry",
						stored.getHoverName(), stored.getCount()).withStyle(ChatFormatting.GRAY));
			}
		}
		tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.fullness",
				getContentCount(stack), MAX_BEANS).withStyle(ChatFormatting.GRAY));
	}

	private void playRemoveOneSound(Player player) {
		player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}

	private void playInsertSound(Player player) {
		player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}

	private void playDropContentsSound(Player player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS,
				SoundSource.PLAYERS, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}
}
