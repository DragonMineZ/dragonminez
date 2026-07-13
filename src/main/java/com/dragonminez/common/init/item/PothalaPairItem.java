package com.dragonminez.common.init.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

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
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!level.isClientSide) ensurePairId(stack, level.random);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack pair = player.getItemInHand(hand);
		if (!level.isClientSide) {
			int pairId = ensurePairId(pair, level.random);

			ItemStack left = new ItemStack(leftEarring.get());
			ItemStack right = new ItemStack(rightEarring.get());
			setPairId(left, pairId);
			setPairId(right, pairId);

			// Give the earrings while the pair item still occupies the hand slot; shrinking
			// first frees that slot, addItem places the left earring there, and vanilla's
			// useItem then wipes the hand slot because the returned stack is empty.
			giveOrDrop(player, left);
			giveOrDrop(player, right);
			if (!player.getAbilities().instabuild) pair.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(pair, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		appendPairIdTooltip(stack, tooltip);
		super.appendHoverText(stack, level, tooltip, flag);
	}

	private static void giveOrDrop(Player player, ItemStack stack) {
		if (!player.addItem(stack)) player.drop(stack, false);
	}

	public static int ensurePairId(ItemStack stack, RandomSource random) {
		int pairId = getPairId(stack);
		if (pairId == 0) {
			pairId = random.nextInt();
			if (pairId == 0) pairId = 1;
			setPairId(stack, pairId);
		}
		return pairId;
	}

	public static void setPairId(ItemStack stack, int id) {
		stack.getOrCreateTag().putInt(PAIR_ID_KEY, id);
	}

	public static int getPairId(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null ? tag.getInt(PAIR_ID_KEY) : 0;
	}

	public static void appendPairIdTooltip(ItemStack stack, List<Component> tooltip) {
		int pairId = getPairId(stack);
		if (pairId != 0) {
			tooltip.add(Component.translatable("item.dragonminez.pothala.pair_id",
					String.format("%08X", pairId)).withStyle(ChatFormatting.GRAY));
		}
	}
}
