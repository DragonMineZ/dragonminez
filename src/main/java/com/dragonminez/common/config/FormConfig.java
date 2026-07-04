package com.dragonminez.common.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
@NoArgsConstructor
public class FormConfig {
	public static final double CURRENT_VERSION = ConfigManager.CONFIG_VERSION;
	private double configVersion;

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
		private Integer auraLayer = 0;
		private String auraColor = "";
		private Integer extraAuraLayer = -1;
		private String extraAuraColor = "#FFFFFF";
		private String extraAuraType = "kakarot";
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