package com.dragonminez.common.init.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

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
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		PothalaPairItem.appendPairIdTooltip(stack, tooltip);
		super.appendHoverText(stack, level, tooltip, flag);
	}
}