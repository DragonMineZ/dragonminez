package com.dragonminez.mixin.client;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientBundleTooltip.class)
public interface BundleTooltipComponentAccessor {
	@Accessor("contents")
	BundleContents getContents();
}
