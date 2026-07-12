package com.dragonminez.common.init.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * A "pair of Pothalas" held item. Right-clicking splits it into a matched left and right earring,
 * both stamped with the same random pair id (stored in NBT). Two players wearing the two halves of
 * the same pair (opposite sides, same id) are the ones the proximity fusion pairs together.
 */
public class PothalaPairItem extends Item {

	public static final String PAIR_ID_KEY = "PothalaPairId";

	private final Supplier<Item> leftEarring;
	private final Supplier<Item> rightEarring;

	public PothalaPairItem(Properties properties, Supplier<Item> leftEarring, Supplier<Item> rightEarring) {
		super(properties);
		this.leftEarring = leftEarring;
		this.rightEarring = rightEarring;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack pair = player.getItemInHand(hand);
		if (!level.isClientSide) {
			int pairId = level.random.nextInt();
			if (pairId == 0) pairId = 1; // 0 means "unpaired", so never hand it out

			ItemStack left = new ItemStack(leftEarring.get());
			ItemStack right = new ItemStack(rightEarring.get());
			setPairId(left, pairId);
			setPairId(right, pairId);

			pair.shrink(1);
			giveOrDrop(player, left);
			giveOrDrop(player, right);
		}
		return InteractionResultHolder.sidedSuccess(pair, level.isClientSide());
	}

	private static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.addItem(stack)) player.drop(stack, false);
	}

	public static void setPairId(ItemStack stack, int id) {
		stack.getOrCreateTag().putInt(PAIR_ID_KEY, id);
	}

	public static int getPairId(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null ? tag.getInt(PAIR_ID_KEY) : 0;
	}
}
