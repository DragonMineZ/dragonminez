package com.dragonminez.mixin.client;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldOpenFlows.class})
public class WorldOpenFlowsMixin {
	@Inject(method = {"confirmWorldCreation"}, at = {@At("HEAD")}, cancellable = true)
	private static void alwaysTreatAsStable(Minecraft pMinecraft, CreateWorldScreen pScreen, Lifecycle pLifecycle, Runnable pLoadWorld, boolean pSkipWarnings, CallbackInfo ci) {
		if (pLifecycle != Lifecycle.stable()) {
			BooleanConsumer booleanconsumer = (p_233154_) -> {
				if (p_233154_) {
					pLoadWorld.run();
				} else {
					pMinecraft.setScreen(pScreen);
				}

			};
			pLoadWorld.run();
			ci.cancel();
		}
	}
}
