package com.dragonminez.common.init.item.tools;

import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainTags;
import com.dragonminez.common.init.item.weapons.BlankWeaponTier;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ToolTiers {
	public static final Tier BLANK_WEAPON_TIER = new BlankWeaponTier(0, 0, -1, 0, 15, Ingredient.EMPTY);
	public static final Tier GETE_TIER = new SimpleTier(
			MainTags.Blocks.NEEDS_GETE_TOOL,
			3250,
			12.0f,
			6.0f,
			25,
			() -> Ingredient.of(MainItems.GETE_INGOT.get())
	);
}
