package com.dragonminez.common.init.item;

import com.dragonminez.common.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.Locale;

public class DMZCuriosItem extends Item implements ICurioItem {

	public enum CurioType {
		HEAD_TECH,
		WEIGHTS
	}

	private final CurioType curioType;

	public DMZCuriosItem(Properties properties, CurioType curioType) {
		super(properties);
		this.curioType = curioType;
	}

	public CurioType getCurioType() {
		return curioType;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!level.isClientSide) PothalaPairItem.reservePairId(stack, level);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		String id = stack.getItem().getDescriptionId();
		if (id.contains("pothala_right")) {
			tooltip.add(Component.translatable("item.dragonminez.pothala.right.tooltip").withStyle(ChatFormatting.GRAY));
		} else if (id.contains("pothala_left")) {
			tooltip.add(Component.translatable("item.dragonminez.pothala.left.tooltip").withStyle(ChatFormatting.GRAY));
		} else if (id.contains("demon_eye")) {
			var gameplay = ConfigManager.getServerConfig().getGameplay();
			tooltip.add(Component.translatable("item.dragonminez.demon_eye.tooltip",
					String.format(Locale.US, "%.2f", gameplay.getDemonEyeMultiplier())).withStyle(ChatFormatting.LIGHT_PURPLE));
			tooltip.add(Component.translatable("item.dragonminez.demon_eye.tooltip2",
					String.format(Locale.US, "%.0f", gameplay.getDemonEyeKiDrainPercent() * 100),
					String.format(Locale.US, "%.0f", gameplay.getDemonEyeHealthDrainPercent() * 100)).withStyle(ChatFormatting.RED));
			tooltip.add(Component.translatable("item.dragonminez.demon_eye.tooltip3").withStyle(ChatFormatting.DARK_RED));
			tooltip.add(Component.translatable("item.dragonminez.demon_eye.tooltip4",
					String.format(Locale.US, "%.2f", 1.0 + gameplay.getDemonEyeMaxGrowth())).withStyle(ChatFormatting.DARK_PURPLE));
			if (gameplay.getDemonEyeBackHitsToRemove() > 0) {
				tooltip.add(Component.translatable("item.dragonminez.demon_eye.tooltip5", gameplay.getDemonEyeBackHitsToRemove())
						.withStyle(ChatFormatting.GRAY));
			}
		}
		PothalaPairItem.appendPairIdTooltip(stack, tooltip);
		super.appendHoverText(stack, level, tooltip, flag);
	}
}