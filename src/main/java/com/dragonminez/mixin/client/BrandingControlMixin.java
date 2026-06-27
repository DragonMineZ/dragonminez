package com.dragonminez.mixin.client;

import com.dragonminez.Reference;
import com.dragonminez.client.title.TitleFooterText;
import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = BrandingControl.class, remap = false)
public abstract class BrandingControlMixin {
	@Shadow
	private static List<String> brandings;
	@Shadow
	private static List<String> brandingsNoMC;
	@Shadow
	private static List<String> overCopyrightBrandings;

	@Inject(method = "computeBranding", at = @At("TAIL"))
	private static void dragonminez$addBuildBranding(CallbackInfo info) {
		String buildLine = TitleFooterText.buildVersionLine(Reference.VERSION);
		if (brandings == null || brandings.contains(buildLine)) return;

		List<String> updatedBrandings = new ArrayList<>(brandings);
		updatedBrandings.add(dragonminez$findMinecraftBrandingIndex(updatedBrandings) + 1, buildLine);
		brandings = List.copyOf(updatedBrandings);
		brandingsNoMC = brandings.subList(1, brandings.size());
	}

	@Inject(method = "computeOverCopyrightBrandings", at = @At("TAIL"))
	private static void dragonminez$addTrademarkBranding(CallbackInfo info) {
		if (overCopyrightBrandings == null) return;

		List<String> updatedBrandings = new ArrayList<>(overCopyrightBrandings);

		if (!updatedBrandings.contains(TitleFooterText.DRAGON_BALL_TRADEMARK_LINE_1)) {
			updatedBrandings.add(0, TitleFooterText.DRAGON_BALL_TRADEMARK_LINE_2);
			updatedBrandings.add(1, TitleFooterText.DRAGON_BALL_TRADEMARK_LINE_1);
		}

		overCopyrightBrandings = List.copyOf(updatedBrandings);
	}

	@Unique
	private static int dragonminez$findMinecraftBrandingIndex(List<String> brandingLines) {
		for (int i = 0; i < brandingLines.size(); i++) {
			if (brandingLines.get(i).startsWith("Minecraft ")) return i;
		}
		return Math.max(0, brandingLines.size() - 1);
	}
}
