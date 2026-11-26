package com.dragonminez.common.util.types.items;

import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;

/**
 * One custom effect on a potion, splash potion, lingering potion or tipped arrow.
 *
 * <p>{@link #fromLegacyEntry} reads the older map form, where an effect was a single
 * number meaning its duration.
 */
public class PotionEffectDTO {
	private ResourceLocation effect;
	private Integer duration = 1;
	private Integer amplifier = 0;
	private Boolean ambient = false;
	private Boolean visible = true;
	private Boolean showIcon = true;

	public PotionEffectDTO(ResourceLocation effect, int duration, int amplifier) {
		this.effect = effect;
		this.duration = duration;
		this.amplifier = amplifier;
	}

	/**
	 * Reads a legacy {@code {"<effect id>": <duration>}} entry.
	 */
	public static PotionEffectDTO fromLegacyEntry(String effectId, int duration) {
		ResourceLocation parsed = ResourceLocation.tryParse(effectId);
		if (parsed == null) {
			throw new JsonSyntaxException("Invalid mob effect id: \'" + effectId + "\'");
		}
		return new PotionEffectDTO(parsed, duration, 0);
	}

	public void validate(String errorPrefix) {
		if (this.effect == null) {
			throw new JsonSyntaxException(errorPrefix + " a mob effect entry has no \'effect\' id.");
		}
		if (this.duration == null || this.duration < 1) {
			throw new JsonSyntaxException(errorPrefix + " mob effect \'" + this.effect + "\' has an invalid duration: " + this.duration + ".");
		}
		if (this.amplifier == null || this.amplifier < 0) {
			throw new JsonSyntaxException(errorPrefix + " mob effect \'" + this.effect + "\' has an invalid amplifier: " + this.amplifier + ".");
		}
	}

	public ResourceLocation getEffect() {
		return this.effect;
	}

	public Integer getDuration() {
		return this.duration;
	}

	public Integer getAmplifier() {
		return this.amplifier;
	}

	public Boolean getAmbient() {
		return this.ambient;
	}

	public Boolean getVisible() {
		return this.visible;
	}

	public Boolean getShowIcon() {
		return this.showIcon;
	}

	public void setEffect(final ResourceLocation effect) {
		this.effect = effect;
	}

	public void setDuration(final Integer duration) {
		this.duration = duration;
	}

	public void setAmplifier(final Integer amplifier) {
		this.amplifier = amplifier;
	}

	public void setAmbient(final Boolean ambient) {
		this.ambient = ambient;
	}

	public void setVisible(final Boolean visible) {
		this.visible = visible;
	}

	public void setShowIcon(final Boolean showIcon) {
		this.showIcon = showIcon;
	}

	public PotionEffectDTO() {
	}
}
