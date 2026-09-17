package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class FormConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;

	public static final String AURA_3D_SMOOTH = "smooth";
	public static final String AURA_3D_SPARKING = "sparking";
	public static final String AURA_3D_USER_PREFERENCE = "userPreference";

	private String configVersion;

	private String groupName;
	private String formType = "superforms";
	private Map<String, FormData> forms = new LinkedHashMap<>();

	public String getGroupName() {
		return groupName != null ? groupName : "";
	}

	public String getFormType() {
		return formType != null ? formType : "superforms";
	}

	public Map<String, FormData> getForms() {
		return forms != null ? forms : Collections.emptyMap();
	}

	public FormData getForm(String formName) {
		if (formName == null) return null;
		for (FormData formData : getForms().values()) {
			if (formData != null && formData.getName() != null && formData.getName().equalsIgnoreCase(formName)) return formData;
		}
		return null;
	}

	public FormData getFormByKey(String key) {
		return forms.get(key);
	}

	public static String sanitizeAura3DPreference(String preference) {
		return AURA_3D_SPARKING.equalsIgnoreCase(preference) ? AURA_3D_SPARKING : AURA_3D_SMOOTH;
	}

	public static String resolveAura3DType(String type, String preference) {
		if (type == null || type.isBlank() || AURA_3D_USER_PREFERENCE.equalsIgnoreCase(type.trim())) {
			return sanitizeAura3DPreference(preference);
		}
		return type;
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class Aura3DStyle {
		public static final Aura3DStyle DEFAULT = new Aura3DStyle();

		private Float[] size = {1.10f, 1.17f, 1.02f};
		private Integer flamePeaks = 12;
		private Float waveFrequency = 2.0f;
		private Float waveSpeed = 5.0f;
		private Float waveAmplitude = 0.75f;
		private Float noiseDetail = 0.10f;
		private Float upwardBias = 0.54f;
		private Float coreAlpha = 0.05f;
		private Float rimAlpha = 1.0f;
		private Float rimPower = 4.0f;
		private Float rimThreshold = 0.05f;
		private String coreColor = "";
		private String rimColor = "";
		private String noiseColor = "";
		private Float bloomIntensity = 1.0f;
		private Integer spikeDensity = 32;
		private Float spikeBurstRate = 3.0f;
		private Float spikeRarity = 3.0f;
		private Float bandStart = 0.50f;

		public float getSizeX() { return size != null && size.length > 0 && size[0] != null ? Math.max(0.05f, size[0]) : 1.10f; }
		public float getSizeY() { return size != null && size.length > 1 && size[1] != null ? Math.max(0.05f, size[1]) : 1.17f; }
		public float getSizeZ() { return size != null && size.length > 2 && size[2] != null ? Math.max(0.05f, size[2]) : 1.02f; }
		public int getFlamePeaks() { return flamePeaks != null ? Math.max(1, Math.min(64, flamePeaks)) : 12; }
		public float getWaveFrequency() { return waveFrequency != null ? Math.max(0.0f, waveFrequency) : 2.0f; }
		public float getWaveSpeed() { return waveSpeed != null ? waveSpeed : 5.0f; }
		public float getWaveAmplitude() { return waveAmplitude != null ? Math.max(0.0f, waveAmplitude) : 0.75f; }
		public float getNoiseDetail() { return noiseDetail != null ? Math.max(0.0f, noiseDetail) : 0.10f; }
		public float getUpwardBias() { return upwardBias != null ? Math.max(0.0f, Math.min(1.0f, upwardBias)) : 0.54f; }
		public float getCoreAlpha() { return coreAlpha != null ? Math.max(0.0f, Math.min(1.0f, coreAlpha)) : 0.05f; }
		public float getRimAlpha() { return rimAlpha != null ? Math.max(0.0f, Math.min(1.0f, rimAlpha)) : 1.0f; }
		public float getRimPower() { return rimPower != null ? Math.max(0.1f, rimPower) : 4.0f; }
		public float getRimThreshold() { return rimThreshold != null ? Math.max(0.001f, rimThreshold) : 0.05f; }
		public String getCoreColor() { return coreColor != null ? coreColor.trim() : ""; }
		public String getRimColor() { return rimColor != null ? rimColor.trim() : ""; }
		public String getNoiseColor() { return noiseColor != null ? noiseColor.trim() : ""; }
		public float getBloomIntensity() { return bloomIntensity != null ? Math.max(0.0f, bloomIntensity) : 1.0f; }
		public int getSpikeDensity() { return spikeDensity != null ? Math.max(4, Math.min(32, spikeDensity)) : 32; }
		public float getSpikeBurstRate() { return spikeBurstRate != null ? Math.max(0.0f, spikeBurstRate) : 3.0f; }
		public float getSpikeRarity() { return spikeRarity != null ? Math.max(0.1f, spikeRarity) : 3.0f; }
		public float getBandStart() { return bandStart != null ? Math.max(0.05f, Math.min(0.95f, bandStart)) : 0.50f; }

		public Aura3DStyle size(float x, float y, float z) {
			this.size = new Float[]{x, y, z};
			return this;
		}

		public Aura3DStyle waves(float frequency, float speed, float amplitude) {
			this.waveFrequency = frequency;
			this.waveSpeed = speed;
			this.waveAmplitude = amplitude;
			return this;
		}

		public Aura3DStyle turbulence(float noiseDetail, float upwardBias) {
			this.noiseDetail = noiseDetail;
			this.upwardBias = upwardBias;
			return this;
		}

		public Aura3DStyle rim(float coreAlpha, float rimAlpha, float rimPower, float rimThreshold) {
			this.coreAlpha = coreAlpha;
			this.rimAlpha = rimAlpha;
			this.rimPower = rimPower;
			this.rimThreshold = rimThreshold;
			return this;
		}

		public Aura3DStyle colors(String core, String rim, String noise) {
			this.coreColor = core;
			this.rimColor = rim;
			this.noiseColor = noise;
			return this;
		}

		public Aura3DStyle peaks(int peaks) {
			this.flamePeaks = peaks;
			return this;
		}

		public Aura3DStyle bloom(float intensity) {
			this.bloomIntensity = intensity;
			return this;
		}

		public Aura3DStyle spikes(int density, float burstRate, float rarity, float bandStart) {
			this.spikeDensity = density;
			this.spikeBurstRate = burstRate;
			this.spikeRarity = rarity;
			this.bandStart = bandStart;
			return this;
		}
	}

	@Setter
	@Getter
	@NoArgsConstructor
	public static class FormData {
		private String name = "";
		private Integer unlockOnSkillLevel = 0;
		private String formCombo = "";
		private String customModel = "";
		private boolean keepBaseFormHeadBones = false;
		private String transformationAnimation = "transf.generic";
		private String bodyColor1 = "";
		private String bodyColor2 = "";
		private String bodyColor3 = "";
		private String extraFormLayer = "";
		private String extraFormColor = "";
		private String hairType = "";
		private String forcedHairCode = "";
		private String hairColor = "";
		private String eye1Color = "";
		private String eye2Color = "";
		private String auraType = "kakarot";
		private String auraType3D = AURA_3D_USER_PREFERENCE;
		private Integer auraLayer = 0;
		private String auraColor = "";
		private Integer extraAuraLayer = -1;
		private String extraAuraColor = "#FFFFFF";
		private String extraAuraType = "kakarot";
		private String extraAuraType3D = AURA_3D_USER_PREFERENCE;
		private Aura3DStyle aura3DStyle = new Aura3DStyle();
		private Aura3DStyle extraAura3DStyle = new Aura3DStyle();
		private Boolean hasLightnings = false;
		private String lightningColor = "";
		private String tintColor = "#FF0000";
		private Double tintIntensity = 0.0;
		private Float[] modelScaling = {0.9375f, 0.9375f, 0.9375f};
		private Double strMultiplier = 1.0;
		private Double skpMultiplier = 1.0;
		private Double stmMultiplier = 1.0;
		private Double defMultiplier = 1.0;
		private Double vitMultiplier = 1.0;
		private Double pwrMultiplier = 1.0;
		private Double eneMultiplier = 1.0;
		private Double speedMultiplier = 1.0;
		private Double staminaDrainMultiplier = 1.0;
		private Double energyDrain = 0.0;
		private Double staminaDrain = 0.0;
		private Double healthDrain = 0.0;
		private Double otherworldTimeDrain = 1.0;
		private Double attackSpeed = 1.0;
		private Double maxMastery = 100.0;
		private Double masteryPerHitDealt = 0.01;
		private Double masteryPerHitReceived = 0.01;
		private Double passiveMasteryEveryFiveSeconds = 0.001;
		private Double maxCostMultiplier = 0.5;
		private Double maxStatsMultiplier = 1.25;
		private String formRequisite = "";
		private String formRequisiteType = "all";
		private Double unlockOnMastery = 0.0;
		private Double stackOnMastery = 0.0;
		private Double instantTransformOnMastery = 40.0;
		private Double allowFreeTransformOnMastery = 50.0;
		private Boolean formStackable = true;
		private Double stackDrainMultiplier = 2.0;
		private List<String> incompatibleWith = new ArrayList<>(List.of("ultimate.ultimate"));
		private List<String> shareMasteryWith = new ArrayList<>();
		private Double shareMasteryMultiplier = 1.0;
		private OutlineShaderConfig outlineShader = new OutlineShaderConfig();

		private List<TriggerItemCost> triggerItemCosts = new ArrayList<>();
		private List<DurationItemCost> durationItemCosts = new ArrayList<>();
		private List<MobEffectConfig> mobEffects = new ArrayList<>();

		private transient float[] rgbBodyColor1;
		private transient float[] rgbBodyColor2;
		private transient float[] rgbBodyColor3;
		private transient float[] rgbHairColor;
		private transient float[] rgbEye1Color;
		private transient float[] rgbEye2Color;
		private transient float[] rgbAuraColor;
		private transient float[] rgbExtraFormColor;
		private transient float[] rgbExtraAuraColor;
		private transient float[] rgbTintColor;

		public Double getStrMultiplier() {
			return Math.max(0.01, strMultiplier);
		}

		public Double getSkpMultiplier() {
			return Math.max(0.01, skpMultiplier);
		}

		public Double getStmMultiplier() {
			return Math.max(0.01, stmMultiplier);
		}

		public Double getDefMultiplier() {
			return Math.max(0.01, defMultiplier);
		}

		public Double getVitMultiplier() {
			return Math.max(0.01, vitMultiplier);
		}

		public Double getPwrMultiplier() {
			return Math.max(0.01, pwrMultiplier);
		}

		public Double getEneMultiplier() {
			return Math.max(0.01, eneMultiplier);
		}

		public Double getSpeedMultiplier() {
			return Math.max(0.01, speedMultiplier);
		}

		public Double getStaminaDrainMultiplier() {
			return Math.max(0, staminaDrainMultiplier);
		}

		public Double getEnergyDrain() {
			return Math.max(0, energyDrain);
		}

		public Double getStaminaDrain() {
			return Math.max(0, staminaDrain);
		}

		public Double getHealthDrain() {
			return Math.max(0, healthDrain);
		}

		public Double getOtherworldTimeDrain() {
			return otherworldTimeDrain != null ? Math.max(0, otherworldTimeDrain) : 1.0;
		}

		public Double getAttackSpeed() {
			return Math.max(0.1, attackSpeed);
		}

		public Double getMasteryPerHitDealt() {
			return Math.max(0, masteryPerHitDealt);
		}

		public Double getMasteryPerHitReceived() {
			return Math.max(0, masteryPerHitReceived);
		}

		public Double getPassiveMasteryEveryFiveSeconds() {
			return Math.max(0, passiveMasteryEveryFiveSeconds);
		}

		public Double getMaxCostMultiplier() {
			return Math.max(0, maxCostMultiplier);
		}

		public Double getMaxStatsMultiplier() {
			return Math.max(0, maxStatsMultiplier);
		}

		public String getFormRequisite() {
			return formRequisite != null ? formRequisite.trim() : "";
		}

		public String getFormRequisiteType() {
			return "any".equalsIgnoreCase(formRequisiteType != null ? formRequisiteType.trim() : "") ? "any" : "all";
		}

		public Double getUnlockOnMastery() {
			return Math.max(0, unlockOnMastery);
		}

		public Double getStackOnMastery() {
			return Math.max(0, stackOnMastery);
		}

		public Double getInstantTransformOnMastery() {
			return Math.max(0, instantTransformOnMastery);
		}

		public Double getAllowFreeTransformOnMastery() {
			return Math.max(0, allowFreeTransformOnMastery != null ? allowFreeTransformOnMastery : 50.0);
		}

		public List<String> getIncompatibleWith() {
			return incompatibleWith != null ? incompatibleWith : Collections.emptyList();
		}

		public List<String> getShareMasteryWith() {
			return shareMasteryWith != null ? shareMasteryWith : Collections.emptyList();
		}

		public Double getShareMasteryMultiplier() {
			return Math.max(0, shareMasteryMultiplier);
		}

		public boolean isIncompatibleWith(String groupId, String formId) {
			if (groupId == null || formId == null) return false;
			String key = (groupId + "." + formId).toLowerCase();
			for (String entry : getIncompatibleWith()) {
				if (entry != null && entry.trim().toLowerCase().equals(key)) return true;
			}
			return false;
		}

		public Double getStackDrainMultiplier() {
			return Math.max(0.01, stackDrainMultiplier);
		}

		public Boolean hasCustomModel() {
			return customModel != null && !customModel.isEmpty();
		}

		public boolean hasTransformationAnimation() {
			return transformationAnimation != null && !transformationAnimation.trim().isEmpty();
		}

		public String getTransformationAnimation() {
			return transformationAnimation != null ? transformationAnimation.trim() : "";
		}

		public Boolean hasBodyColorOverride() {
			return !bodyColor1.isEmpty() || !bodyColor2.isEmpty() || !bodyColor3.isEmpty();
		}

		public String getExtraFormLayer() {
			return extraFormLayer != null ? extraFormLayer.trim() : "";
		}

		public boolean hasExtraFormLayer() {
			return !getExtraFormLayer().isEmpty();
		}

		public Boolean hasDefinedHairType() {
			return hairType != null && !hairType.isEmpty();
		}

		public Boolean hasHairCodeOverride() {
			return !forcedHairCode.isEmpty();
		}

		public Boolean hasHairColorOverride() {
			return hairColor != null && !hairColor.isEmpty();
		}

		public Boolean hasEyeColorOverride() {
			return !eye1Color.isEmpty() || !eye2Color.isEmpty();
		}

		public Boolean hasAuraColorOverride() {
			return auraColor != null && !auraColor.isEmpty();
		}

		public int getExtraAuraLayer() {
			return extraAuraLayer != null ? extraAuraLayer : -1;
		}

		public boolean hasExtraAura() {
			int layer = getExtraAuraLayer();
			return layer >= 1 && layer <= 6;
		}

		public String getExtraAuraColor() {
			return extraAuraColor != null && !extraAuraColor.isEmpty() ? extraAuraColor : "#FFFFFF";
		}

		public String getExtraAuraType() {
			return extraAuraType != null && !extraAuraType.isEmpty() ? extraAuraType : "kakarot";
		}

		public Aura3DStyle getAura3DStyle() {
			return aura3DStyle != null ? aura3DStyle : Aura3DStyle.DEFAULT;
		}

		public Aura3DStyle getExtraAura3DStyle() {
			return extraAura3DStyle != null ? extraAura3DStyle : Aura3DStyle.DEFAULT;
		}

		public OutlineShaderConfig getOutlineShader() {
			return outlineShader != null ? outlineShader : new OutlineShaderConfig();
		}

		public List<MobEffectConfig> getMobEffects() {
			return mobEffects != null ? mobEffects : Collections.emptyList();
		}

		public List<TriggerItemCost> getTriggerItemCosts() {
			return triggerItemCosts != null ? triggerItemCosts : Collections.emptyList();
		}

		public List<DurationItemCost> getDurationItemCosts() {
			return durationItemCosts != null ? durationItemCosts : Collections.emptyList();
		}

		public boolean hasTriggerItemCosts() {
			return triggerItemCosts != null && !triggerItemCosts.isEmpty();
		}

		public boolean hasDurationItemCosts() {
			return durationItemCosts != null && !durationItemCosts.isEmpty();
		}

		public float[] getRgbBodyColor1() {
			if (rgbBodyColor1 == null && bodyColor1 != null && !bodyColor1.isEmpty()) rgbBodyColor1 = com.dragonminez.client.util.ColorUtils.hexToRgb(bodyColor1);
			return rgbBodyColor1;
		}

		public float[] getRgbBodyColor2() {
			if (rgbBodyColor2 == null && bodyColor2 != null && !bodyColor2.isEmpty()) rgbBodyColor2 = com.dragonminez.client.util.ColorUtils.hexToRgb(bodyColor2);
			return rgbBodyColor2;
		}

		public float[] getRgbBodyColor3() {
			if (rgbBodyColor3 == null && bodyColor3 != null && !bodyColor3.isEmpty()) rgbBodyColor3 = com.dragonminez.client.util.ColorUtils.hexToRgb(bodyColor3);
			return rgbBodyColor3;
		}

		public float[] getRgbHairColor() {
			if (rgbHairColor == null && hairColor != null && !hairColor.isEmpty()) rgbHairColor = com.dragonminez.client.util.ColorUtils.hexToRgb(hairColor);
			return rgbHairColor;
		}

		public float[] getRgbEye1Color() {
			if (rgbEye1Color == null && eye1Color != null && !eye1Color.isEmpty()) rgbEye1Color = com.dragonminez.client.util.ColorUtils.hexToRgb(eye1Color);
			return rgbEye1Color;
		}

		public float[] getRgbEye2Color() {
			if (rgbEye2Color == null && eye2Color != null && !eye2Color.isEmpty()) rgbEye2Color = com.dragonminez.client.util.ColorUtils.hexToRgb(eye2Color);
			return rgbEye2Color;
		}

		public float[] getRgbAuraColor() {
			if (rgbAuraColor == null && auraColor != null && !auraColor.isEmpty()) rgbAuraColor = com.dragonminez.client.util.ColorUtils.hexToRgb(auraColor);
			return rgbAuraColor;
		}

		public float[] getRgbExtraFormColor() {
			if (rgbExtraFormColor == null && extraFormColor != null && !extraFormColor.isEmpty()) rgbExtraFormColor = com.dragonminez.client.util.ColorUtils.hexToRgb(extraFormColor);
			return rgbExtraFormColor;
		}

		public float[] getRgbExtraAuraColor() {
			if (rgbExtraAuraColor == null) rgbExtraAuraColor = com.dragonminez.client.util.ColorUtils.hexToRgb(getExtraAuraColor());
			return rgbExtraAuraColor;
		}

		public String getTintColor() {
			return tintColor != null ? tintColor : "";
		}

		public double getTintIntensity() {
			return tintIntensity != null ? Math.max(0.0, tintIntensity) : 0.0;
		}

		public float[] getRgbTintColor() {
			if (rgbTintColor == null && tintColor != null && !tintColor.isEmpty()) rgbTintColor = com.dragonminez.client.util.ColorUtils.hexToRgb(tintColor);
			return rgbTintColor;
		}

		public boolean hasTint() {
			return getTintIntensity() > 0.0 && getRgbTintColor() != null;
		}

		@Setter
		@Getter
		@NoArgsConstructor
		public static class MobEffectConfig {
			private String effectId = "";
			private Integer amplifier = 0;
			private Integer durationTicks = -1;
			private Boolean ambient = false;
			private Boolean visible = true;
			private Boolean showIcon = true;

			public String getEffectId() {
				return effectId != null ? effectId.trim() : "";
			}

			public int getAmplifier() {
				return Math.max(0, amplifier != null ? amplifier : 0);
			}

			public int getDurationTicks() {
				return durationTicks != null ? durationTicks : -1;
			}

			public boolean isPersistent() {
				return getDurationTicks() < 0;
			}

			public boolean isAmbient() {
				return Boolean.TRUE.equals(ambient);
			}

			public boolean isVisible() {
				return visible == null || visible;
			}

			public boolean isShowIcon() {
				return showIcon == null || showIcon;
			}
		}

		@Setter
		@Getter
		@NoArgsConstructor
		public static class TriggerItemCost {
			private String itemId = "";
			private String itemTag = "";
			private String nbt = "";
			private Integer count = 1;
			private Boolean consume = true;

			public String getItemId() {
				return itemId != null ? itemId.trim() : "";
			}

			public String getItemTag() {
				return itemTag != null ? itemTag.trim() : "";
			}

			public String getNbt() {
				return nbt != null ? nbt.trim() : "";
			}

			public int getCount() {
				return Math.max(1, count != null ? count : 1);
			}

			public boolean isConsume() {
				return consume == null || consume;
			}

			public boolean hasItemId() {
				return !getItemId().isEmpty();
			}

			public boolean hasItemTag() {
				return !getItemTag().isEmpty();
			}

			public boolean hasNbt() {
				return !getNbt().isEmpty();
			}
		}

		@Setter
		@Getter
		@NoArgsConstructor
		public static class DurationItemCost {
			private String itemId = "";
			private String itemTag = "";
			private String nbt = "";
			private Integer durationSeconds = 1;

			public String getItemId() {
				return itemId != null ? itemId.trim() : "";
			}

			public String getItemTag() {
				return itemTag != null ? itemTag.trim() : "";
			}

			public String getNbt() {
				return nbt != null ? nbt.trim() : "";
			}

			public int getDurationSeconds() {
				return Math.max(1, durationSeconds != null ? durationSeconds : 1);
			}

			public boolean hasItemId() {
				return !getItemId().isEmpty();
			}

			public boolean hasItemTag() {
				return !getItemTag().isEmpty();
			}

			public boolean hasNbt() {
				return !getNbt().isEmpty();
			}
		}

		@Setter
		@Getter
		@NoArgsConstructor
		public static class OutlineShaderConfig {
			private Boolean enabled = false;
			private String primaryColor = "#7FFFFF";
			private String secondaryColor = "#7FFFFF";
			private Double outlineThickness = 1.5;

			public boolean isEnabled() {
				return Boolean.TRUE.equals(enabled);
			}

			public String getPrimaryColor() {
				return primaryColor != null && !primaryColor.isEmpty() ? primaryColor : "#7FFFFF";
			}

			public String getSecondaryColor() {
				return secondaryColor != null && !secondaryColor.isEmpty() ? secondaryColor : "#FFD970";
			}

			public double getOutlineThickness() {
				return Math.max(0.0, outlineThickness != null ? outlineThickness : 1.5);
			}
		}
	}
}