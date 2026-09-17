package com.dragonminez.client.render.effects;

import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.FormConfig;
import net.minecraft.util.Mth;

public final class AuraStyle {
	private static final float CORE_LIGHTEN = 0.45f;
	private static final float[] WHITE = {1.0f, 1.0f, 1.0f};

	public float sizeX, sizeY, sizeZ;
	public float peaks;
	public float waveFrequency, waveSpeed, waveAmplitude;
	public float noiseDetail, upwardBias;
	public float coreAlpha, rimAlpha, rimPower, rimThreshold;
	public float noiseFactor;
	public float bloomIntensity;
	public float spikeDensity, spikeBurstRate, spikeRarity, bandStart;
	public final float[] coreColor = new float[3];
	public final float[] rimColor = new float[3];
	public final float[] noiseColor = new float[3];

	private static final float BRIGHTNESS = 0.85f;

	private static void dim(float[] color) {
		for (int i = 0; i < 3; i++) color[i] *= BRIGHTNESS;
	}

	public static AuraStyle resolve(FormConfig.Aura3DStyle config, float[] auraColor) {
		FormConfig.Aura3DStyle c = config != null ? config : FormConfig.Aura3DStyle.DEFAULT;
		float[] base = auraColor != null && auraColor.length >= 3 ? auraColor : WHITE;

		AuraStyle style = new AuraStyle();
		style.sizeX = c.getSizeX();
		style.sizeY = c.getSizeY();
		style.sizeZ = c.getSizeZ();
		style.peaks = c.getFlamePeaks();
		style.waveFrequency = c.getWaveFrequency();
		style.waveSpeed = c.getWaveSpeed();
		style.waveAmplitude = c.getWaveAmplitude();
		style.noiseDetail = c.getNoiseDetail();
		style.upwardBias = c.getUpwardBias();
		style.coreAlpha = c.getCoreAlpha();
		style.rimAlpha = c.getRimAlpha();
		style.rimPower = c.getRimPower();
		style.rimThreshold = c.getRimThreshold();
		style.bloomIntensity = c.getBloomIntensity();
		style.spikeDensity = c.getSpikeDensity();
		style.spikeBurstRate = c.getSpikeBurstRate();
		style.spikeRarity = c.getSpikeRarity();
		style.bandStart = c.getBandStart();

		if (c.getRimColor().isEmpty()) copy(base, style.rimColor);
		else copy(ColorUtils.hexToRgb(c.getRimColor()), style.rimColor);

		if (c.getCoreColor().isEmpty()) {
			float peak = Math.max(base[0], Math.max(base[1], base[2]));
			for (int i = 0; i < 3; i++) {
				float brightest = peak > 1.0e-4f ? base[i] / peak : base[i];
				style.coreColor[i] = Mth.lerp(CORE_LIGHTEN, base[i], brightest);
			}
		} else copy(ColorUtils.hexToRgb(c.getCoreColor()), style.coreColor);

		if (c.getNoiseColor().isEmpty()) {
			copy(style.rimColor, style.noiseColor);
			style.noiseFactor = 0.0f;
		} else {
			copy(ColorUtils.hexToRgb(c.getNoiseColor()), style.noiseColor);
			style.noiseFactor = 1.0f;
		}

		dim(style.coreColor);
		dim(style.rimColor);
		dim(style.noiseColor);
		return style;
	}

	public static AuraStyle mix(AuraStyle from, AuraStyle to, float t) {
		return new AuraStyle().copyFrom(from).blendTowards(to, t);
	}

	public AuraStyle copyFrom(AuraStyle other) {
		sizeX = other.sizeX;
		sizeY = other.sizeY;
		sizeZ = other.sizeZ;
		peaks = other.peaks;
		waveFrequency = other.waveFrequency;
		waveSpeed = other.waveSpeed;
		waveAmplitude = other.waveAmplitude;
		noiseDetail = other.noiseDetail;
		upwardBias = other.upwardBias;
		coreAlpha = other.coreAlpha;
		rimAlpha = other.rimAlpha;
		rimPower = other.rimPower;
		rimThreshold = other.rimThreshold;
		noiseFactor = other.noiseFactor;
		bloomIntensity = other.bloomIntensity;
		spikeDensity = other.spikeDensity;
		spikeBurstRate = other.spikeBurstRate;
		spikeRarity = other.spikeRarity;
		bandStart = other.bandStart;
		copy(other.coreColor, coreColor);
		copy(other.rimColor, rimColor);
		copy(other.noiseColor, noiseColor);
		return this;
	}

	public AuraStyle blendTowards(AuraStyle target, float t) {
		if (t <= 0.0f) return this;
		if (t >= 1.0f) return copyFrom(target);
		sizeX = Mth.lerp(t, sizeX, target.sizeX);
		sizeY = Mth.lerp(t, sizeY, target.sizeY);
		sizeZ = Mth.lerp(t, sizeZ, target.sizeZ);
		peaks = Mth.lerp(t, peaks, target.peaks);
		waveFrequency = Mth.lerp(t, waveFrequency, target.waveFrequency);
		waveSpeed = Mth.lerp(t, waveSpeed, target.waveSpeed);
		waveAmplitude = Mth.lerp(t, waveAmplitude, target.waveAmplitude);
		noiseDetail = Mth.lerp(t, noiseDetail, target.noiseDetail);
		upwardBias = Mth.lerp(t, upwardBias, target.upwardBias);
		coreAlpha = Mth.lerp(t, coreAlpha, target.coreAlpha);
		rimAlpha = Mth.lerp(t, rimAlpha, target.rimAlpha);
		rimPower = Mth.lerp(t, rimPower, target.rimPower);
		rimThreshold = Mth.lerp(t, rimThreshold, target.rimThreshold);
		noiseFactor = Mth.lerp(t, noiseFactor, target.noiseFactor);
		bloomIntensity = Mth.lerp(t, bloomIntensity, target.bloomIntensity);
		spikeDensity = Mth.lerp(t, spikeDensity, target.spikeDensity);
		spikeBurstRate = Mth.lerp(t, spikeBurstRate, target.spikeBurstRate);
		spikeRarity = Mth.lerp(t, spikeRarity, target.spikeRarity);
		bandStart = Mth.lerp(t, bandStart, target.bandStart);
		for (int i = 0; i < 3; i++) {
			coreColor[i] = Mth.lerp(t, coreColor[i], target.coreColor[i]);
			rimColor[i] = Mth.lerp(t, rimColor[i], target.rimColor[i]);
			noiseColor[i] = Mth.lerp(t, noiseColor[i], target.noiseColor[i]);
		}
		return this;
	}

	private static void copy(float[] from, float[] to) {
		to[0] = from[0];
		to[1] = from[1];
		to[2] = from[2];
	}
}
