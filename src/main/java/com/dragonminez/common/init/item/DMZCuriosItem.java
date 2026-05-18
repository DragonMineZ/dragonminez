package com.dragonminez.common.init.item;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

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
}