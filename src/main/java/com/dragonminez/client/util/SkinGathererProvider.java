package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;
import java.util.function.BiConsumer;

public class SkinGathererProvider {

	public static SkinGathererProvider INSTANCE = new SkinGathererProvider();

	public void gatherBodyLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		var character = stats.getCharacter();
		String raceName = character.getRaceName().toLowerCase();
		int bodyType = character.getBodyType();
		String currentForm = character.getActiveForm();

		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
		if (raceConfig == null) return;

		String raceCustomModel = raceConfig.getCustomModel() != null ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = "";

		boolean hasStackForm = character.hasActiveStackForm() && character.getActiveStackFormData() != null;
		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;

		if (hasStackForm && character.getActiveStackFormData().hasCustomModel()) {
			formCustomModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (hasForm && character.getActiveFormData().hasCustomModel()) {
			formCustomModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}

		String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (key.isEmpty()) key = raceName;

		String logicKey = key;
		if (key.equals("human_slim") || key.equals("majin_slim") || key.equals("base_slim")) {
			logicKey = raceName;
		}

		float[] b1 = ColorUtils.hexToRgb(character.getBodyColor());
		float[] b2 = ColorUtils.hexToRgb(character.getBodyColor2());
		float[] b3 = ColorUtils.hexToRgb(character.getBodyColor3());
		float[] hair = ColorUtils.hexToRgb(character.getHairColor());

		if (hasForm) {
			var f = character.getActiveFormData();
			if (!f.getBodyColor1().isEmpty()) b1 = ColorUtils.hexToRgb(f.getBodyColor1());
			if (!f.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(f.getBodyColor2());
			if (!f.getBodyColor3().isEmpty()) b3 = ColorUtils.hexToRgb(f.getBodyColor3());
			if (!f.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(f.getHairColor());
		}

		if (hasStackForm) {
			var sf = character.getActiveStackFormData();
			if (!sf.getBodyColor1().isEmpty()) b1 = ColorUtils.hexToRgb(sf.getBodyColor1());
			if (!sf.getBodyColor2().isEmpty()) b2 = ColorUtils.hexToRgb(sf.getBodyColor2());
			if (!sf.getBodyColor3().isEmpty()) b3 = ColorUtils.hexToRgb(sf.getBodyColor3());
			if (!sf.getHairColor().isEmpty()) hair = ColorUtils.hexToRgb(sf.getHairColor());
		}

		if (stats.getStatus().isActionCharging()) {
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) {
				var nextForm = TransformationsHelper.getNextAvailableForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getBodyColor1().isEmpty()) b1 = DMZSkinLayer.lerpColor(factor, b1, ColorUtils.hexToRgb(nextForm.getBodyColor1()));
					if (!nextForm.getBodyColor2().isEmpty()) b2 = DMZSkinLayer.lerpColor(factor, b2, ColorUtils.hexToRgb(nextForm.getBodyColor2()));
					if (!nextForm.getBodyColor3().isEmpty()) b3 = DMZSkinLayer.lerpColor(factor, b3, ColorUtils.hexToRgb(nextForm.getBodyColor3()));
					if (!nextForm.getHairColor().isEmpty()) hair = DMZSkinLayer.lerpColor(factor, hair, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			} else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) {
				var nextForm = TransformationsHelper.getNextAvailableStackForm(stats);
				if (nextForm != null) {
					float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
					if (!nextForm.getBodyColor1().isEmpty()) b1 = DMZSkinLayer.lerpColor(factor, b1, ColorUtils.hexToRgb(nextForm.getBodyColor1()));
					if (!nextForm.getBodyColor2().isEmpty()) b2 = DMZSkinLayer.lerpColor(factor, b2, ColorUtils.hexToRgb(nextForm.getBodyColor2()));
					if (!nextForm.getBodyColor3().isEmpty()) b3 = DMZSkinLayer.lerpColor(factor, b3, ColorUtils.hexToRgb(nextForm.getBodyColor3()));
					if (!nextForm.getHairColor().isEmpty()) hair = DMZSkinLayer.lerpColor(factor, hair, ColorUtils.hexToRgb(nextForm.getHairColor()));
				}
			}
		}

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

		if (logicKey.equals("oozaru") || isOozaruForm) {
			resolveBodyOozaru(b1, b2, consumer);
			return;
		}

		boolean isSaiyanLogic = logicKey.equals("saiyan") || logicKey.equals("saiyan_ssj4") || raceName.equals("saiyan");
		boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();

		if ((isSaiyanLogic || hasSaiyanTail) && stats.getStatus().isTailVisible() && character.isHasSaiyanTail()) {
			float[] tailColor;
			if (hasStackForm && character.getActiveStackFormData().getBodyColor2() != null && !character.getActiveStackFormData().getBodyColor2().isEmpty()) {
				tailColor = ColorUtils.hexToRgb(character.getActiveStackFormData().getBodyColor2());
			} else if (hasForm && character.getActiveFormData().getBodyColor2() != null && !character.getActiveFormData().getBodyColor2().isEmpty()) {
				tailColor = ColorUtils.hexToRgb(character.getActiveFormData().getBodyColor2());
			} else if (character.getBodyColor2() != null && !character.getBodyColor2().isEmpty()) {
				tailColor = ColorUtils.hexToRgb(character.getBodyColor2());
			} else {
				tailColor = ColorUtils.hexToRgb("#572117");
			}
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tail1.png")), tailColor);
		}

		boolean isHumanoid = logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.equals("saiyan_ssj4") || logicKey.equals("buffed");
		if (isHumanoid && bodyType == 0) {
			consumer.accept(player.getSkinTextureLocation(), new float[]{1.0f, 1.0f, 1.0f});
			return;
		}

		switch (logicKey) {
			case "human", "saiyan", "saiyan_ssj4", "buffed" -> resolveBodyHumanSaiyan(character, b1, consumer);
			case "namekian", "namekian_orange" -> resolveBodyNamekian(character, b1, b2, b3, consumer);
			case "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid" -> resolveBodyMajin(character, logicKey, b1, consumer);
			case "frostdemon", "frostdemon_second", "frostdemon_final", "frostdemon_fifth", "frostdemon_third", "frostdemon_fp" -> resolveBodyFrostDemon(character, logicKey, b1, b2, b3, hair, consumer);
			case "bioandroid", "bioandroid_semi", "bioandroid_perfect", "bioandroid_base", "bioandroid_ultra" -> resolveBodyBioAndroid(character, logicKey, b1, b2, b3, hair, consumer);
			default -> {
				String gender = raceConfig.getHasGender() ? "_" + character.getGender().toLowerCase() : "";
				boolean isFormLayered = hasForm && Boolean.TRUE.equals(character.getActiveFormData().getIsLayered());
				boolean isBaseLayered = Boolean.TRUE.equals(raceConfig.getIsLayered());

				if (isFormLayered || isBaseLayered) {
					String prefix = "textures/entity/races/" + logicKey + gender + "_";
					resolveDynamicLayers(character, raceConfig, prefix, prefix, b1, b2, b3, consumer);
				} else {
					ResourceLocation customTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + logicKey + gender + ".png");
					consumer.accept(DMZSkinLayer.getSafeTexture(customTex), b1);
				}
			}
		}
	}

	protected void resolveDynamicLayers(Character character, RaceCharacterConfig raceConfig, String prefix, String fallbackPrefix, float[] b1, float[] b2, float[] b3, BiConsumer<ResourceLocation, float[]> consumer) {
		if (raceConfig == null) return;

		boolean hasForm = character.hasActiveForm() && character.getActiveFormData() != null;
		boolean isFormLayered = hasForm && Boolean.TRUE.equals(character.getActiveFormData().getIsLayered());

		int layerAmount = isFormLayered ? character.getActiveFormData().getLayerAmount() : raceConfig.getLayerAmount();

		for (int i = 1; i <= layerAmount; i++) {
			float[] color;
			if (i == 1) color = b1;
			else if (i == 2) color = b2;
			else if (i == 3) color = b3;
			else {
				String hex = isFormLayered ? character.getActiveFormData().getExtraLayerColor(i - 4) : raceConfig.getExtraLayerColor(i - 4);
				color = ColorUtils.hexToRgb(hex);
			}

			ResourceLocation texture = DMZSkinLayer.getSafeTexture(
					ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer" + i + ".png"),
					ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer" + i + ".png")
			);
			consumer.accept(texture, color);
		}
	}

	public void gatherAndroidLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		var character = stats.getCharacter();
		String raceName = character.getRace().toLowerCase();
		boolean canBeUpgraded = ConfigManager.getRaceCharacter(raceName) != null && ConfigManager.getRaceCharacter(raceName).getFormSkillTpCosts("androidforms").length > 0;
		if (!canBeUpgraded || !stats.getStatus().isAndroidUpgraded()) return;

		String androidPath = character.getGender().equals(Character.GENDER_FEMALE) ? "textures/entity/races/female_android.png" : "textures/entity/races/male_android.png";
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, androidPath)), new float[]{1.0f, 1.0f, 1.0f});
	}

	public void gatherTattooLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/majinm.png")), new float[]{1.0f, 1.0f, 1.0f});
		}
		int tattooType = stats.getCharacter().getTattooType();
		if (tattooType == 0) return;

		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tattoos/tattoo_" + tattooType + ".png")), new float[]{1.0f, 1.0f, 1.0f});
	}

	protected void resolveBodyHumanSaiyan(Character character, float[] bodyColor, BiConsumer<ResourceLocation, float[]> consumer) {
		int bodyType = character.getBodyType();
		String gender = character.getGender().toLowerCase().trim();
		String genderPart = (gender.equals("female") || gender.equals("mujer")) ? "_female" : "_male";
		String path = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_" + bodyType + ".png";
		String fallbackPath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_0.png";
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPath)), bodyColor);
	}

	protected void resolveBodyOozaru(float[] bodyColor, float[] bodyColor2, BiConsumer<ResourceLocation, float[]> consumer) {
		String basePath = "textures/entity/races/humansaiyan/oozaru_";
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer1.png")), bodyColor2);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer2.png")), bodyColor);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer3.png")), new float[]{1f, 1f, 1f});
	}

	protected void resolveBodyNamekian(Character character, float[] c1, float[] c2, float[] c3, BiConsumer<ResourceLocation, float[]> consumer) {
		int bodyType = character.getBodyType();
		String basePath = "textures/entity/races/namekian/bodytype_" + bodyType + "_";
		String fallbackPath = "textures/entity/races/namekian/bodytype_0_";

		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPath + "layer1.png")), c1);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPath + "layer2.png")), c2);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, basePath + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPath + "layer3.png")), c3);
	}

	protected void resolveBodyFrostDemon(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<ResourceLocation, float[]> consumer) {
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();
		float[] orangeColor = ColorUtils.hexToRgb("#e67d40");
		String folder = "textures/entity/races/frostdemon/";
		String prefix, fallbackPrefix;

		boolean isSecondForm = Objects.equals(currentForm, FrostDemonForms.SECOND_FORM);
		boolean isBase = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");
		boolean isBulky = (key.equals("frostdemon") && (isBase || isSecondForm) || key.equals("frostdemon_second"))
                || key.equals("frostdemon_third");

		if (isBulky) {
			prefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_" + bodyType + "_" : folder + "bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_0_" : folder + "bodytype_0_";
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer1.png")), b1);
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer2.png")), b2);
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer3.png")), b3);
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer4.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer4.png")), hair);
			if (bodyType == 0)
				consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer5.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer5.png")), orangeColor);
		} else {
			prefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_" + bodyType + "_" : folder + "finalform_bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_0_" : folder + "finalform_bodytype_0_";

			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer1.png")), b1);
			consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer2.png")), (bodyType == 0 || bodyType == 2) ? hair : b2);
			if (bodyType == 1) {
				consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer3.png")), b3);
				consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer4.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer4.png")), hair);
			} else if (bodyType == 2) {
				consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer3.png")), hair);
				consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer2.png")), b2);
			}
		}
	}

	protected void resolveBodyBioAndroid(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<ResourceLocation, float[]> consumer) {
		String phase = switch (key) {
			case "bioandroid_semi" -> "semiperfect";
			case "bioandroid_perfect", "bioandroid_ultra" -> "perfect";
			case "bioandroid_base" -> "base";
			case "bioandroid" -> character.hasActiveForm() ? "perfect" : "base";
			default -> "perfect";
		};

		int bodyType = character.getBodyType();
		String prefix = "textures/entity/races/bioandroid/" + phase + "_" + bodyType + "_";
		String fallbackPrefix = "textures/entity/races/bioandroid/" + phase + "_0_";

		float[] stinger = ColorUtils.hexToRgb("#D9B28D");

		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer1.png")), b1);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer2.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer2.png")), b2);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer3.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer3.png")), b3);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer4.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer4.png")), hair);
		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer5.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer5.png")), stinger);
	}

	protected void resolveBodyMajin(Character character, String key, float[] b1, BiConsumer<ResourceLocation, float[]> consumer) {
		String currentForm = character.getActiveForm();
		String gender = character.getGender().toLowerCase().trim();
		String genderSuffix = (gender.equals("female") || gender.equals("mujer")) ? "female" : "male";
		boolean isFemale = genderSuffix.equals("female");
		String phase;

		if (Objects.equals(currentForm, MajinForms.KID) || key.equals("majin_kid")) phase = "kid";
		else if (Objects.equals(currentForm, MajinForms.EVIL) || key.equals("majin_evil")) phase = "evil";
		else if (Objects.equals(currentForm, MajinForms.SUPER) || key.equals("majin_super")) phase = "super";
		else if (Objects.equals(currentForm, MajinForms.ULTRA) || key.equals("majin_ultra")) phase = "ultra";
		else if (character.hasActiveForm()) phase = "super";
		else phase = "base";

		int bodyType = character.getBodyType();
		String prefix = "textures/entity/races/majin/" + phase + "_" + bodyType + "_" + genderSuffix + "_";
		String fallbackPrefix = "textures/entity/races/majin/" + phase + "_0_" + genderSuffix + "_";

		consumer.accept(DMZSkinLayer.getSafeTexture(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, prefix + "layer1.png"), ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, fallbackPrefix + "layer1.png")), b1);

		if (isFemale && (phase.equals("super") || phase.equals("ultra"))) {
			ResourceLocation tailLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tail1.png");
			consumer.accept(DMZSkinLayer.getSafeTexture(tailLoc, tailLoc), b1);
		}
	}
}