package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.init.item.PothalaPairItem;
import com.dragonminez.server.util.BeetleFusionTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MediBugBeetleItem extends FoodItem {
	public static final int PAIRED_EAT_LOCK_TICKS = 30 * 20;

	public MediBugBeetleItem() {
		super(2, 1.2f, 64);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!level.isClientSide) PothalaPairItem.reservePairId(stack, level);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		int pairId = PothalaPairItem.getPairId(stack);
		ItemStack result = super.finishUsingItem(stack, level, entity);
		if (pairId != 0 && entity instanceof ServerPlayer player) {
			player.getCooldowns().addCooldown(this, PAIRED_EAT_LOCK_TICKS);
			BeetleFusionTracker.onBeetleEaten(player, pairId);
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		if (PothalaPairItem.getPairId(stack) != 0) {
			tooltip.add(Component.translatable("item.dragonminez.medi_bug_beetle.tooltip.paired").withStyle(ChatFormatting.GRAY));
			PothalaPairItem.appendPairIdTooltip(stack, tooltip);
		}
		super.appendHoverText(stack, level, tooltip, flag);
	}
}
