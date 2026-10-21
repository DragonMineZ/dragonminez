package com.dragonminez.client.gui.hud;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public record HudPlayerState(float health, float maxHealth, float energy, float maxEnergy, float stamina, float maxStamina,
							 int powerRelease, float formCharge, boolean chargingKi, float[] auraRgb, int auraColor) {

	public static HudPlayerState of(Player player, StatsData data) {
		Character character = data.getCharacter();
		Resources resources = data.getResources();

		String auraHex = character.getAuraColor();
		FormConfig.FormData formData = character.getActiveStackForm() != null && !character.getActiveStackForm().isEmpty() ? character.getActiveStackFormData() :
				character.getActiveForm() != null && !character.getActiveForm().isEmpty() ? character.getActiveFormData() : null;
		if (formData != null && formData.getAuraColor() != null && !formData.getAuraColor().isEmpty()) auraHex = formData.getAuraColor();
		float[] auraRgb = ColorUtils.hexToRgb(auraHex);

		int actionCharge = resources.getActionCharge();
		float formCharge = actionCharge <= 0 ? 0.0f : Mth.clamp((actionCharge < 10 ? 10 + actionCharge : actionCharge) / 100.0f, 0.0f, 1.0f);

		return new HudPlayerState(
				player.getHealth(), Math.max(1.0f, (float) player.getAttributeValue(Attributes.MAX_HEALTH)),
				resources.getCurrentEnergy(), Math.max(1.0f, data.getMaxEnergy()),
				resources.getCurrentStamina(), Math.max(1.0f, data.getMaxStamina()),
				resources.getPowerRelease(), formCharge, data.getStatus().isChargingKi(),
				auraRgb, HudRender.rgb(auraRgb, 1.0f));
	}

	public static int healthColor(float fraction) {
		float toYellow = smoothStep(0.22f, 0.38f, fraction);
		float toGreen = smoothStep(0.52f, 0.70f, fraction);
		return HudRender.mix(HudRender.mix(0xE8392F, 0xF2C230, toYellow), 0x35D46A, toGreen);
	}

	private static float smoothStep(float edge0, float edge1, float value) {
		float t = Mth.clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
		return t * t * (3.0f - 2.0f * t);
	}
}
