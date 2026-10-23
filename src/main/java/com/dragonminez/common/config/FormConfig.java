package com.dragonminez.common.config;

import com.dragonminez.common.racial.impl.GlindDivinity;
import com.dragonminez.common.util.FusionForms;
import java.util.*;

public class FormConfig {
	public static final String CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	public static final String AURA_3D_SMOOTH = "smooth";
	public static final String AURA_3D_SPARKING = "sparking";
	public static final String AURA_3D_GOD = "god";
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
		if (AURA_3D_SPARKING.equalsIgnoreCase(preference)) return AURA_3D_SPARKING;
		if (AURA_3D_GOD.equalsIgnoreCase(preference)) return AURA_3D_GOD;
		return AURA_3D_SMOOTH;
	}

	public static String sanitizeAura3DPreference(String preference, String race) {
		String sanitized = sanitizeAura3DPreference(preference);
		return AURA_3D_GOD.equals(sanitized) && !GlindDivinity.grantsGodAura(race) ? AURA_3D_SMOOTH : sanitized;
	}

	public static String resolveAura3DType(String type, String preference) {
		if (type == null || type.isBlank() || AURA_3D_USER_PREFERENCE.equalsIgnoreCase(type.trim())) {
			return sanitizeAura3DPreference(preference);
		}
		return type;
	}


	public static class Aura3DStyle {
		public static final Aura3DStyle DEFAULT = new Aura3DStyle();
		private Float[] size = {1.1F, 1.17F, 1.02F};
		private Integer flamePeaks = 12;
		private Float waveFrequency = 2.0F;
		private Float waveSpeed = 5.0F;
		private Float waveAmplitude = 0.75F;
		private Float noiseDetail = 0.1F;
		private Float upwardBias = 0.54F;
		private Float coreAlpha = 0.05F;
		private Float rimAlpha = 1.0F;
		private Float rimPower = 4.0F;
		private Float rimThreshold = 0.05F;
		private String coreColor = "";
		private String rimColor = "";
		private String noiseColor = "";
		private Float bloomIntensity = 1.0F;
		private Integer spikeDensity = 32;
		private Float spikeBurstRate = 3.0F;
		private Float spikeRarity = 3.0F;
		private Float bandStart = 0.5F;
		private String secondBorderColor = "";

		public float getSizeX() {
			return size != null && size.length > 0 && size[0] != null ? Math.max(0.05F, size[0]) : 1.1F;
		}

		public float getSizeY() {
			return size != null && size.length > 1 && size[1] != null ? Math.max(0.05F, size[1]) : 1.17F;
		}

		public float getSizeZ() {
			return size != null && size.length > 2 && size[2] != null ? Math.max(0.05F, size[2]) : 1.02F;
		}

		public int getFlamePeaks() {
			return flamePeaks != null ? Math.max(1, Math.min(64, flamePeaks)) : 12;
		}

		public float getWaveFrequency() {
			return waveFrequency != null ? Math.max(0.0F, waveFrequency) : 2.0F;
		}

		public float getWaveSpeed() {
			return waveSpeed != null ? waveSpeed : 5.0F;
		}

		public float getWaveAmplitude() {
			return waveAmplitude != null ? Math.max(0.0F, waveAmplitude) : 0.75F;
		}

		public float getNoiseDetail() {
			return noiseDetail != null ? Math.max(0.0F, noiseDetail) : 0.1F;
		}

		public float getUpwardBias() {
			return upwardBias != null ? Math.max(0.0F, Math.min(1.0F, upwardBias)) : 0.54F;
		}

		public float getCoreAlpha() {
			return coreAlpha != null ? Math.max(0.0F, Math.min(1.0F, coreAlpha)) : 0.05F;
		}

		public float getRimAlpha() {
			return rimAlpha != null ? Math.max(0.0F, Math.min(1.0F, rimAlpha)) : 1.0F;
		}

		public float getRimPower() {
			return rimPower != null ? Math.max(0.1F, rimPower) : 4.0F;
		}

		public float getRimThreshold() {
			return rimThreshold != null ? Math.max(0.001F, rimThreshold) : 0.05F;
		}

		public String getCoreColor() {
			return coreColor != null ? coreColor.trim() : "";
		}

		public String getRimColor() {
			return rimColor != null ? rimColor.trim() : "";
		}

		public String getNoiseColor() {
			return noiseColor != null ? noiseColor.trim() : "";
		}

		public float getBloomIntensity() {
			return bloomIntensity != null ? Math.max(0.0F, bloomIntensity) : 1.0F;
		}

		public int getSpikeDensity() {
			return spikeDensity != null ? Math.max(4, Math.min(32, spikeDensity)) : 32;
		}

		public float getSpikeBurstRate() {
			return spikeBurstRate != null ? Math.max(0.0F, spikeBurstRate) : 3.0F;
		}

		public float getSpikeRarity() {
			return spikeRarity != null ? Math.max(0.1F, spikeRarity) : 3.0F;
		}

		public float getBandStart() {
			return bandStart != null ? Math.max(0.05F, Math.min(0.95F, bandStart)) : 0.5F;
		}

		public String getSecondBorderColor() {
			return secondBorderColor != null ? secondBorderColor.trim() : "";
		}

		public Aura3DStyle size(float x, float y, float z) {
			this.size = new Float[] {x, y, z};
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

		public Aura3DStyle secondBorder(String color) {
			this.secondBorderColor = color;
			return this;
		}

		public void setSize(final Float[] size) {
			this.size = size;
		}

		public void setFlamePeaks(final Integer flamePeaks) {
			this.flamePeaks = flamePeaks;
		}

		public void setWaveFrequency(final Float waveFrequency) {
			this.waveFrequency = waveFrequency;
		}

		public void setWaveSpeed(final Float waveSpeed) {
			this.waveSpeed = waveSpeed;
		}

		public void setWaveAmplitude(final Float waveAmplitude) {
			this.waveAmplitude = waveAmplitude;
		}

		public void setNoiseDetail(final Float noiseDetail) {
			this.noiseDetail = noiseDetail;
		}

		public void setUpwardBias(final Float upwardBias) {
			this.upwardBias = upwardBias;
		}

		public void setCoreAlpha(final Float coreAlpha) {
			this.coreAlpha = coreAlpha;
		}

		public void setRimAlpha(final Float rimAlpha) {
			this.rimAlpha = rimAlpha;
		}

		public void setRimPower(final Float rimPower) {
			this.rimPower = rimPower;
		}

		public void setRimThreshold(final Float rimThreshold) {
			this.rimThreshold = rimThreshold;
		}

		public void setCoreColor(final String coreColor) {
			this.coreColor = coreColor;
		}

		public void setRimColor(final String rimColor) {
			this.rimColor = rimColor;
		}

		public void setNoiseColor(final String noiseColor) {
			this.noiseColor = noiseColor;
		}

		public void setBloomIntensity(final Float bloomIntensity) {
			this.bloomIntensity = bloomIntensity;
		}

		public void setSpikeDensity(final Integer spikeDensity) {
			this.spikeDensity = spikeDensity;
		}

		public void setSpikeBurstRate(final Float spikeBurstRate) {
			this.spikeBurstRate = spikeBurstRate;
		}

		public void setSpikeRarity(final Float spikeRarity) {
			this.spikeRarity = spikeRarity;
		}

		public void setBandStart(final Float bandStart) {
			this.bandStart = bandStart;
		}

		public void setSecondBorderColor(final String secondBorderColor) {
			this.secondBorderColor = secondBorderColor;
		}

		public Float[] getSize() {
			return this.size;
		}

		public Aura3DStyle() {
		}
	}


	public static class FormData {
		private String name = "";
		private Integer unlockOnSkillLevel = 0;
		private String formCombo = "";
		private String customModel = "";
		private boolean keepBaseFormHeadBones = false;
		private Boolean hideTail = false;
		private String transformationAnimation = "transf.generic";
		private String bodyColor1 = "";
		private String bodyColor2 = "";
		private String bodyColor3 = "";
		private String extraFormLayer = "";
		private String extraFormColor = "";
		private String hairType = "";
		private String forcedHairCode = "";
		private String hairColor = "";
		private Boolean furUsesHairColor = false;
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
		private Float[] modelScaling = {0.9375F, 0.9375F, 0.9375F};
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
		private Double otherworldTimeDrainMastered = 1.0;
		private Double attackSpeed = 1.0;
		private Double maxMastery = 100.0;
		private Double masteryPerHitDealt = 0.01;
		private Double masteryPerHitReceived = 0.01;
		private Double passiveMasteryEveryFiveSeconds = 0.001;
		private Double maxCostMultiplier = 0.5;
		private Double maxStatsMultiplier = 1.25;
		private String formRequisite = "";
		private String formRequisiteType = "all";
		private Integer levelRequisite = 0;
		private String sagaRequisite = "";
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

		public Double getOtherworldTimeDrainMastered() {
			return otherworldTimeDrainMastered != null ? Math.max(0, otherworldTimeDrainMastered) : getOtherworldTimeDrain();
		}

		public double getOtherworldTimeDrainAt(double mastery) {
			double max = getMaxMastery();
			double ratio = max > 0 ? Math.max(0.0, Math.min(1.0, mastery / max)) : 1.0;
			double base = getOtherworldTimeDrain();
			return base + (getOtherworldTimeDrainMastered() - base) * ratio;
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

		public int getLevelRequisite() {
			return levelRequisite != null ? Math.max(0, levelRequisite) : 0;
		}

		public String getSagaRequisite() {
			return sagaRequisite != null ? sagaRequisite.trim() : "";
		}

		public String getSagaRequisiteId() {
			String value = getSagaRequisite();
			int separator = value.lastIndexOf(':');
			return separator > 0 ? value.substring(0, separator).trim() : "";
		}

		public int getSagaRequisiteQuest() {
			String value = getSagaRequisite();
			int separator = value.lastIndexOf(':');
			if (separator <= 0) return -1;
			try {
				return Integer.parseInt(value.substring(separator + 1).trim());
			} catch (NumberFormatException e) {
				return -1;
			}
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
			String key = (FusionForms.baseGroup(groupId) + "." + formId).toLowerCase();
			for (String entry : getIncompatibleWith()) {
				if (entry != null && entry.trim().toLowerCase().equals(key)) return true;
			}
			return false;
		}

		public Double getStackDrainMultiplier() {
			return Math.max(0.01, stackDrainMultiplier);
		}

		public boolean hidesTail() {
			return Boolean.TRUE.equals(hideTail);
		}

		public boolean usesHairColorForFur() {
			return Boolean.TRUE.equals(furUsesHairColor);
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

			public void setEffectId(final String effectId) {
				this.effectId = effectId;
			}

			public void setAmplifier(final Integer amplifier) {
				this.amplifier = amplifier;
			}

			public void setDurationTicks(final Integer durationTicks) {
				this.durationTicks = durationTicks;
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

			public Boolean getAmbient() {
				return this.ambient;
			}

			public Boolean getVisible() {
				return this.visible;
			}

			public Boolean getShowIcon() {
				return this.showIcon;
			}

			public MobEffectConfig() {
			}
		}


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

			public void setItemId(final String itemId) {
				this.itemId = itemId;
			}

			public void setItemTag(final String itemTag) {
				this.itemTag = itemTag;
			}

			public void setNbt(final String nbt) {
				this.nbt = nbt;
			}

			public void setCount(final Integer count) {
				this.count = count;
			}

			public void setConsume(final Boolean consume) {
				this.consume = consume;
			}

			public Boolean getConsume() {
				return this.consume;
			}

			public TriggerItemCost() {
			}
		}


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

			public void setItemId(final String itemId) {
				this.itemId = itemId;
			}

			public void setItemTag(final String itemTag) {
				this.itemTag = itemTag;
			}

			public void setNbt(final String nbt) {
				this.nbt = nbt;
			}

			public void setDurationSeconds(final Integer durationSeconds) {
				this.durationSeconds = durationSeconds;
			}

			public DurationItemCost() {
			}
		}


		public static class OutlineShaderConfig {
			public static final float DEFAULT_FIRE_INTENSITY = 0.0F;
			public static final float MAX_FIRE_INTENSITY = 3.0F;
			public static final String COLOR_MODE_PULSE = "pulse";
			public static final String COLOR_MODE_GRADIENT = "gradient";
			private Boolean enabled = false;
			private String primaryColor = "#7FFFFF";
			private String secondaryColor = "#7FFFFF";
			private Double outlineThickness = 1.5;
			private Double fireIntensity = (double) DEFAULT_FIRE_INTENSITY;
			private String colorMode = COLOR_MODE_PULSE;
			private Double bodyGlow = 0.0;

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

			public float getFireIntensity() {
				if (fireIntensity == null || fireIntensity.isNaN()) return DEFAULT_FIRE_INTENSITY;
				return (float) Math.max(0.0, Math.min(MAX_FIRE_INTENSITY, fireIntensity));
			}

			public String getColorMode() {
				return COLOR_MODE_GRADIENT.equalsIgnoreCase(colorMode) ? COLOR_MODE_GRADIENT : COLOR_MODE_PULSE;
			}

			public boolean isGradient() {
				return COLOR_MODE_GRADIENT.equals(getColorMode());
			}

			public float getBodyGlow() {
				if (bodyGlow == null || bodyGlow.isNaN()) return 0.0F;
				return (float) Math.max(0.0, Math.min(1.0, bodyGlow));
			}

			public void setEnabled(final Boolean enabled) {
				this.enabled = enabled;
			}

			public void setPrimaryColor(final String primaryColor) {
				this.primaryColor = primaryColor;
			}

			public void setSecondaryColor(final String secondaryColor) {
				this.secondaryColor = secondaryColor;
			}

			public void setOutlineThickness(final Double outlineThickness) {
				this.outlineThickness = outlineThickness;
			}

			public void setFireIntensity(final Double fireIntensity) {
				this.fireIntensity = fireIntensity;
			}

			public void setColorMode(final String colorMode) {
				this.colorMode = colorMode;
			}

			public void setBodyGlow(final Double bodyGlow) {
				this.bodyGlow = bodyGlow;
			}

			public Boolean getEnabled() {
				return this.enabled;
			}

			public OutlineShaderConfig() {
			}
		}

		public void setName(final String name) {
			this.name = name;
		}

		public void setUnlockOnSkillLevel(final Integer unlockOnSkillLevel) {
			this.unlockOnSkillLevel = unlockOnSkillLevel;
		}

		public void setFormCombo(final String formCombo) {
			this.formCombo = formCombo;
		}

		public void setCustomModel(final String customModel) {
			this.customModel = customModel;
		}

		public void setKeepBaseFormHeadBones(final boolean keepBaseFormHeadBones) {
			this.keepBaseFormHeadBones = keepBaseFormHeadBones;
		}

		public void setHideTail(final Boolean hideTail) {
			this.hideTail = hideTail;
		}

		public void setTransformationAnimation(final String transformationAnimation) {
			this.transformationAnimation = transformationAnimation;
		}

		public void setBodyColor1(final String bodyColor1) {
			this.bodyColor1 = bodyColor1;
		}

		public void setBodyColor2(final String bodyColor2) {
			this.bodyColor2 = bodyColor2;
		}

		public void setBodyColor3(final String bodyColor3) {
			this.bodyColor3 = bodyColor3;
		}

		public void setExtraFormLayer(final String extraFormLayer) {
			this.extraFormLayer = extraFormLayer;
		}

		public void setExtraFormColor(final String extraFormColor) {
			this.extraFormColor = extraFormColor;
		}

		public void setHairType(final String hairType) {
			this.hairType = hairType;
		}

		public void setForcedHairCode(final String forcedHairCode) {
			this.forcedHairCode = forcedHairCode;
		}

		public void setHairColor(final String hairColor) {
			this.hairColor = hairColor;
		}

		public void setFurUsesHairColor(final Boolean furUsesHairColor) {
			this.furUsesHairColor = furUsesHairColor;
		}

		public void setEye1Color(final String eye1Color) {
			this.eye1Color = eye1Color;
		}

		public void setEye2Color(final String eye2Color) {
			this.eye2Color = eye2Color;
		}

		public void setAuraType(final String auraType) {
			this.auraType = auraType;
		}

		public void setAuraType3D(final String auraType3D) {
			this.auraType3D = auraType3D;
		}

		public void setAuraLayer(final Integer auraLayer) {
			this.auraLayer = auraLayer;
		}

		public void setAuraColor(final String auraColor) {
			this.auraColor = auraColor;
		}

		public void setExtraAuraLayer(final Integer extraAuraLayer) {
			this.extraAuraLayer = extraAuraLayer;
		}

		public void setExtraAuraColor(final String extraAuraColor) {
			this.extraAuraColor = extraAuraColor;
		}

		public void setExtraAuraType(final String extraAuraType) {
			this.extraAuraType = extraAuraType;
		}

		public void setExtraAuraType3D(final String extraAuraType3D) {
			this.extraAuraType3D = extraAuraType3D;
		}

		public void setAura3DStyle(final Aura3DStyle aura3DStyle) {
			this.aura3DStyle = aura3DStyle;
		}

		public void setExtraAura3DStyle(final Aura3DStyle extraAura3DStyle) {
			this.extraAura3DStyle = extraAura3DStyle;
		}

		public void setHasLightnings(final Boolean hasLightnings) {
			this.hasLightnings = hasLightnings;
		}

		public void setLightningColor(final String lightningColor) {
			this.lightningColor = lightningColor;
		}

		public void setTintColor(final String tintColor) {
			this.tintColor = tintColor;
		}

		public void setTintIntensity(final Double tintIntensity) {
			this.tintIntensity = tintIntensity;
		}

		public void setModelScaling(final Float[] modelScaling) {
			this.modelScaling = modelScaling;
		}

		public void setStrMultiplier(final Double strMultiplier) {
			this.strMultiplier = strMultiplier;
		}

		public void setSkpMultiplier(final Double skpMultiplier) {
			this.skpMultiplier = skpMultiplier;
		}

		public void setStmMultiplier(final Double stmMultiplier) {
			this.stmMultiplier = stmMultiplier;
		}

		public void setDefMultiplier(final Double defMultiplier) {
			this.defMultiplier = defMultiplier;
		}

		public void setVitMultiplier(final Double vitMultiplier) {
			this.vitMultiplier = vitMultiplier;
		}

		public void setPwrMultiplier(final Double pwrMultiplier) {
			this.pwrMultiplier = pwrMultiplier;
		}

		public void setEneMultiplier(final Double eneMultiplier) {
			this.eneMultiplier = eneMultiplier;
		}

		public void setSpeedMultiplier(final Double speedMultiplier) {
			this.speedMultiplier = speedMultiplier;
		}

		public void setStaminaDrainMultiplier(final Double staminaDrainMultiplier) {
			this.staminaDrainMultiplier = staminaDrainMultiplier;
		}

		public void setEnergyDrain(final Double energyDrain) {
			this.energyDrain = energyDrain;
		}

		public void setStaminaDrain(final Double staminaDrain) {
			this.staminaDrain = staminaDrain;
		}

		public void setHealthDrain(final Double healthDrain) {
			this.healthDrain = healthDrain;
		}

		public void setOtherworldTimeDrain(final Double otherworldTimeDrain) {
			this.otherworldTimeDrain = otherworldTimeDrain;
		}

		public void setOtherworldTimeDrainMastered(final Double otherworldTimeDrainMastered) {
			this.otherworldTimeDrainMastered = otherworldTimeDrainMastered;
		}

		public void setAttackSpeed(final Double attackSpeed) {
			this.attackSpeed = attackSpeed;
		}

		public void setMaxMastery(final Double maxMastery) {
			this.maxMastery = maxMastery;
		}

		public void setMasteryPerHitDealt(final Double masteryPerHitDealt) {
			this.masteryPerHitDealt = masteryPerHitDealt;
		}

		public void setMasteryPerHitReceived(final Double masteryPerHitReceived) {
			this.masteryPerHitReceived = masteryPerHitReceived;
		}

		public void setPassiveMasteryEveryFiveSeconds(final Double passiveMasteryEveryFiveSeconds) {
			this.passiveMasteryEveryFiveSeconds = passiveMasteryEveryFiveSeconds;
		}

		public void setMaxCostMultiplier(final Double maxCostMultiplier) {
			this.maxCostMultiplier = maxCostMultiplier;
		}

		public void setMaxStatsMultiplier(final Double maxStatsMultiplier) {
			this.maxStatsMultiplier = maxStatsMultiplier;
		}

		public void setFormRequisite(final String formRequisite) {
			this.formRequisite = formRequisite;
		}

		public void setFormRequisiteType(final String formRequisiteType) {
			this.formRequisiteType = formRequisiteType;
		}

		public void setLevelRequisite(final Integer levelRequisite) {
			this.levelRequisite = levelRequisite;
		}

		public void setSagaRequisite(final String sagaRequisite) {
			this.sagaRequisite = sagaRequisite;
		}

		public void setUnlockOnMastery(final Double unlockOnMastery) {
			this.unlockOnMastery = unlockOnMastery;
		}

		public void setStackOnMastery(final Double stackOnMastery) {
			this.stackOnMastery = stackOnMastery;
		}

		public void setInstantTransformOnMastery(final Double instantTransformOnMastery) {
			this.instantTransformOnMastery = instantTransformOnMastery;
		}

		public void setAllowFreeTransformOnMastery(final Double allowFreeTransformOnMastery) {
			this.allowFreeTransformOnMastery = allowFreeTransformOnMastery;
		}

		public void setFormStackable(final Boolean formStackable) {
			this.formStackable = formStackable;
		}

		public void setStackDrainMultiplier(final Double stackDrainMultiplier) {
			this.stackDrainMultiplier = stackDrainMultiplier;
		}

		public void setIncompatibleWith(final List<String> incompatibleWith) {
			this.incompatibleWith = incompatibleWith;
		}

		public void setShareMasteryWith(final List<String> shareMasteryWith) {
			this.shareMasteryWith = shareMasteryWith;
		}

		public void setShareMasteryMultiplier(final Double shareMasteryMultiplier) {
			this.shareMasteryMultiplier = shareMasteryMultiplier;
		}

		public void setOutlineShader(final OutlineShaderConfig outlineShader) {
			this.outlineShader = outlineShader;
		}

		public void setTriggerItemCosts(final List<TriggerItemCost> triggerItemCosts) {
			this.triggerItemCosts = triggerItemCosts;
		}

		public void setDurationItemCosts(final List<DurationItemCost> durationItemCosts) {
			this.durationItemCosts = durationItemCosts;
		}

		public void setMobEffects(final List<MobEffectConfig> mobEffects) {
			this.mobEffects = mobEffects;
		}

		public void setRgbBodyColor1(final float[] rgbBodyColor1) {
			this.rgbBodyColor1 = rgbBodyColor1;
		}

		public void setRgbBodyColor2(final float[] rgbBodyColor2) {
			this.rgbBodyColor2 = rgbBodyColor2;
		}

		public void setRgbBodyColor3(final float[] rgbBodyColor3) {
			this.rgbBodyColor3 = rgbBodyColor3;
		}

		public void setRgbHairColor(final float[] rgbHairColor) {
			this.rgbHairColor = rgbHairColor;
		}

		public void setRgbEye1Color(final float[] rgbEye1Color) {
			this.rgbEye1Color = rgbEye1Color;
		}

		public void setRgbEye2Color(final float[] rgbEye2Color) {
			this.rgbEye2Color = rgbEye2Color;
		}

		public void setRgbAuraColor(final float[] rgbAuraColor) {
			this.rgbAuraColor = rgbAuraColor;
		}

		public void setRgbExtraFormColor(final float[] rgbExtraFormColor) {
			this.rgbExtraFormColor = rgbExtraFormColor;
		}

		public void setRgbExtraAuraColor(final float[] rgbExtraAuraColor) {
			this.rgbExtraAuraColor = rgbExtraAuraColor;
		}

		public void setRgbTintColor(final float[] rgbTintColor) {
			this.rgbTintColor = rgbTintColor;
		}

		public String getName() {
			return this.name;
		}

		public Integer getUnlockOnSkillLevel() {
			return this.unlockOnSkillLevel;
		}

		public String getFormCombo() {
			return this.formCombo;
		}

		public String getCustomModel() {
			return this.customModel;
		}

		public boolean isKeepBaseFormHeadBones() {
			return this.keepBaseFormHeadBones;
		}

		public Boolean getHideTail() {
			return this.hideTail;
		}

		public String getBodyColor1() {
			return this.bodyColor1;
		}

		public String getBodyColor2() {
			return this.bodyColor2;
		}

		public String getBodyColor3() {
			return this.bodyColor3;
		}

		public String getExtraFormColor() {
			return this.extraFormColor;
		}

		public String getHairType() {
			return this.hairType;
		}

		public String getForcedHairCode() {
			return this.forcedHairCode;
		}

		public String getHairColor() {
			return this.hairColor;
		}

		public Boolean getFurUsesHairColor() {
			return this.furUsesHairColor;
		}

		public String getEye1Color() {
			return this.eye1Color;
		}

		public String getEye2Color() {
			return this.eye2Color;
		}

		public String getAuraType() {
			return this.auraType;
		}

		public String getAuraType3D() {
			return this.auraType3D;
		}

		public Integer getAuraLayer() {
			return this.auraLayer;
		}

		public String getAuraColor() {
			return this.auraColor;
		}

		public String getExtraAuraType3D() {
			return this.extraAuraType3D;
		}

		public Boolean getHasLightnings() {
			return this.hasLightnings;
		}

		public String getLightningColor() {
			return this.lightningColor;
		}

		public Float[] getModelScaling() {
			return this.modelScaling;
		}

		public Double getMaxMastery() {
			return this.maxMastery;
		}

		public Boolean getFormStackable() {
			return this.formStackable;
		}

		public FormData() {
		}
	}

	public void setConfigVersion(final String configVersion) {
		this.configVersion = configVersion;
	}

	public void setGroupName(final String groupName) {
		this.groupName = groupName;
	}

	public void setFormType(final String formType) {
		this.formType = formType;
	}

	public void setForms(final Map<String, FormData> forms) {
		this.forms = forms;
	}

	public String getConfigVersion() {
		return this.configVersion;
	}

	public FormConfig() {
	}
}
