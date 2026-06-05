package com.dragonminez.client.render.util;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Resolves the active per-form transformation outline configuration for a
 * player. Reuses the same {@link FormConfig.FormData.OutlineShaderConfig}
 * values the PostChain outline uses, but feeds the geometry-based inverted-hull
 * outline that is drawn when a shaderpack is active.
 */
public final class OutlineConfigResolver {
	private OutlineConfigResolver() {}

	public record OutlineData(float[] primary, float[] secondary, float thickness, float colorMixSpeed, float noiseScale) {}

	@Nullable
	public static OutlineData resolve(Player player) {
		StatsData data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
		if (data == null) return null;
		Character character = data.getCharacter();
		if (character == null) return null;

		FormConfig.FormData activeFormData = character.getActiveFormData();
		FormConfig.FormData activeStackFormData = character.getActiveStackFormData();

		FormConfig.FormData.OutlineShaderConfig selected = null;

		if (activeFormData != null) {
			FormConfig.FormData.OutlineShaderConfig formConfig = activeFormData.getOutlineShader();
			if (formConfig != null && formConfig.isEnabled()) selected = formConfig;
		}

		// A stacked form, when present, takes priority over the base form.
		if (activeStackFormData != null) {
			FormConfig.FormData.OutlineShaderConfig stackConfig = activeStackFormData.getOutlineShader();
			if (stackConfig != null && stackConfig.isEnabled()) selected = stackConfig;
		}

		if (selected == null) return null;

		return new OutlineData(
				ColorUtils.hexToRgb(selected.getPrimaryColor()),
				ColorUtils.hexToRgb(selected.getSecondaryColor()),
				(float) selected.getOutlineThickness(),
				(float) selected.getColorMixSpeed(),
				(float) selected.getNoiseScale()
		);
	}
}
