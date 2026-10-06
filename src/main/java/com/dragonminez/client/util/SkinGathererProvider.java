package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.SkinPixels;
import com.dragonminez.client.render.util.SkinPixelTextures;
import com.dragonminez.client.systems.FormVisualTransition;
import com.dragonminez.common.stats.FusedData;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.FusionForms;
import com.dragonminez.common.util.FusionTraits;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class SkinGathererProvider {

	public static SkinGathererProvider INSTANCE = new SkinGathererProvider();

	private static final Set<String> BUILTIN_RACES = Set.of(
			"human", "saiyan", "namekian", "majin", "frostdemon", "bioandroid"
	);

	public static boolean isBuiltInRace(String race) {
		return race != null && BUILTIN_RACES.contains(race.toLowerCase());
	}

	public static String modelFamily(String key) {
		return FusionTraits.modelFamily(key);
	}

	public static boolean isHumanoidKey(String logicKey) {
		return logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.contains("ssj4d")
				|| logicKey.contains("ssj4gt") || logicKey.equals("buffed") || logicKey.equals("buffedg3") || logicKey.equals("4arms");
	}

	public static boolean rendersPlayerSkin(Character character) {
		if (character == null || character.getBodyType() != 0) return false;
		String raceName = character.getRaceName().toLowerCase();
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
		if (raceConfig == null) return false;

		String raceCustomModel = raceConfig.getCustomModel() != null ? raceConfig.getCustomModel().toLowerCase() : "";
		String formCustomModel = "";
		if (character.hasActiveStackForm() && character.getActiveStackFormData() != null && character.getActiveStackFormData().hasCustomModel()) {
			formCustomModel = character.getActiveStackFormData().getCustomModel().toLowerCase();
		} else if (character.hasActiveForm() && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel()) {
			formCustomModel = character.getActiveFormData().getCustomModel().toLowerCase();
		}
		String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
		if (key.isEmpty()) key = isBuiltInRace(raceName) ? raceName : "human";
		String logicKey = key.equals("human_slim") || key.equals("majin_slim") || key.equals("base_slim") ? raceName : key;

		String currentForm = character.getActiveForm();
		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));
		if (logicKey.equals("oozaru") || isOozaruForm) return false;
		if (isHumanoidKey(logicKey)) return true;
		if (isBuiltInRace(raceName) || !modelFamily(logicKey).equals("custom")) return false;
		return Boolean.TRUE.equals(raceConfig.getUseVanillaSkin());
	}

	public interface BodyLayerSink extends BiConsumer<ResourceLocation, float[]> {
		@Override
		default void accept(ResourceLocation texture, float[] color) {
			base(texture, color);
		}

		void base(ResourceLocation texture, float[] color);

		default void overlay(ResourceLocation texture, float[] color) {
			base(texture, color);
		}

		default void translucent(ResourceLocation texture, float[] color) {
			base(texture, color);
		}

		default void overlayTranslucent(ResourceLocation texture, float[] color) {
			translucent(texture, color);
		}

		void fading(String layerId, ResourceLocation texture, float[] color, float targetAlpha);

		default void fading(String layerId, ResourceLocation texture, float[] color) {
			fading(layerId, texture, color, 1.0f);
		}

		default void saiyanTail(float[] color) {
			base(DMZSkinLayer.getSafeTexture(getCachedTexture(SAIYAN_TAIL_TEXTURE)), color);
		}
	}

	private static final String SAIYAN_TAIL_TEXTURE = "textures/entity/races/tail1.png";

	private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();

	private static final float[] WHITE_COLOR = {1.0f, 1.0f, 1.0f};
	private static final float[] DEFAULT_TAIL_COLOR = ColorUtils.hexToRgb("#572117");
	private static final float[] DEFAULT_ORANGE_COLOR = ColorUtils.hexToRgb("#e67d40");
	private static final float[] DEFAULT_STINGER_COLOR = ColorUtils.hexToRgb("#EDD747");

	private Character donorScratch;
	private Character fusionScratch;

	public static ResourceLocation getCachedTexture(String path) {
		return TEXTURE_CACHE.computeIfAbsent(path, p -> ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, p));
	}

	private void emitFadingLayer(BiConsumer<ResourceLocation, float[]> consumer, String layerId, ResourceLocation texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.fading(layerId, texture, color);
		else consumer.accept(texture, color);
	}

	private void emitOverlayLayer(BiConsumer<ResourceLocation, float[]> consumer, ResourceLocation texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.overlay(texture, color);
		else consumer.accept(texture, color);
	}

	private void emitTranslucentLayer(BiConsumer<ResourceLocation, float[]> consumer, ResourceLocation texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.translucent(texture, color);
		else consumer.accept(texture, color);
	}

	private void emitExtraFormLayer(BiConsumer<ResourceLocation, float[]> consumer, String layerId, FormConfig.FormData form) {
		ResourceLocation extraTex = resolveConfiguredTexture(form.getExtraFormLayer());
		if (extraTex == null) return;
		float[] extraColor = form.getRgbExtraFormColor() != null ? form.getRgbExtraFormColor() : WHITE_COLOR;
		emitFadingLayer(consumer, layerId, DMZSkinLayer.getSafeTexture(extraTex), extraColor);
	}

	private ResourceLocation resolveConfiguredTexture(String path) {
		if (path.contains(":")) return ResourceLocation.tryParse(path);
		return getCachedTexture(path);
	}

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
		if (key.isEmpty()) key = isBuiltInRace(raceName) ? raceName : "human";

		String logicKey = key;
		if (key.equals("human_slim") || key.equals("majin_slim") || key.equals("base_slim")) {
			logicKey = raceName;
		}

		String modelKey = logicKey;
		FusedData fused = stats.getFusedData();
		boolean partnerModel = fused != null && fused.usesPartnerModel(character);
		Character look = character;
		if (partnerModel) {
			look = baseLook(character);
			logicKey = raceCustomModel.isEmpty() ? (isBuiltInRace(raceName) ? raceName : "human") : raceCustomModel;
			if (logicKey.equals("human_slim") || logicKey.equals("majin_slim") || logicKey.equals("base_slim")) logicKey = raceName;
		}

		float[][] colors = resolveBodyColors(stats);
		float[] b1 = colors[0];
		float[] b2 = colors[1];
		float[] b3 = colors[2];
		float[] hair = colors[3];

		if (hasForm && character.getActiveFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_form", character.getActiveFormData());
		}
		if (hasStackForm && character.getActiveStackFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_stack", character.getActiveStackFormData());
		}

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

		if (modelKey.equals("oozaru") || isOozaruForm) {
			resolveBodyOozaru(b1, b2, consumer);
			return;
		}

		boolean isSaiyanLogic = modelKey.equals("saiyan") || modelKey.contains("ssj4gt") || modelKey.contains("ssj4d") || raceName.equals("saiyan");
		boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();
		boolean renderSaiyanTail = (isSaiyanLogic || hasSaiyanTail) && stats.getStatus().isTailVisible() && SaiyanTailRules.hasTail(character);

		boolean isHumanoid = isHumanoidKey(logicKey);

		if (isHumanoid && look.getBodyType() == 0) {
			consumer.accept(player.getSkinTextureLocation(), WHITE_COLOR);
		} else if (isHumanoid) {
			resolveBodyHumanSaiyan(look, logicKey, b1, b2, b3, consumer);
		} else {
            switch (logicKey) {
                case "namekian", "namekian_orange", "namekian_buffed" -> resolveBodyNamekian(look, b1, b2, b3, consumer);
                case "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid", "janemba_imperfect", "janemba_fat", "janemba_super" -> resolveBodyMajin(look, logicKey, b1, b2, b3, consumer);
                case "frostdemon", "frostdemon_second", "frostdemon_final", "frostdemon_fifth", "frostdemon_third", "frostdemon_fp", "frostdemon_mecha", "frostdemon_metalcore" -> resolveBodyFrostDemon(look, logicKey, b1, b2, b3, hair, consumer);
                case "bioandroid", "bioandroid_semi", "bioandroid_perfect", "bioandroid_base", "bioandroid_ultra", "bioandroid_xeno", "bioandroid_xenofp" -> resolveBodyBioAndroid(look, logicKey, b1, b2, b3, hair, consumer);
                case "glindtrueform" -> resolveBodyGlindTrueForm(b1, b2, consumer);
                case "trascended" -> resolveBodyGlindTranscended(look, b1, b2, consumer);
				default -> {
					boolean hasGender = Boolean.TRUE.equals(raceConfig.getHasGender());
					String genSuffix = hasGender ? (character.getGender().equals(Character.GENDER_FEMALE) ? "_female" : "_male") : "";

					if (Boolean.TRUE.equals(raceConfig.getUseVanillaSkin()) && bodyType == 0) {
						consumer.accept(player.getSkinTextureLocation(), WHITE_COLOR);
						break;
					}

					if (Boolean.TRUE.equals(raceConfig.getIsLayered())) {
						String prefix = "textures/entity/races/" + raceName + "/" + logicKey + genSuffix + "_" + bodyType + "_";
						String fallbackPrefix = "textures/entity/races/" + raceName + "/" + logicKey + genSuffix + "_0_";

						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png")), b2);
						consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png")), b3);
					} else {
						ResourceLocation customTex = getCachedTexture("textures/entity/races/" + raceName + "/" + logicKey + genSuffix + ".png");
						consumer.accept(DMZSkinLayer.getSafeTexture(customTex), b1);
					}
				}
            }
        }

		if (partnerModel) {
			switch (modelFamily(modelKey)) {
				case "frostdemon" -> emitFrostDemonFormLayers(modelKey, b1, consumer);
				case "bioandroid" -> emitBioAndroidFormLayers(modelKey, FusionForms.baseGroup(character.getActiveFormGroup()), consumer);
				default -> {}
			}
		}

		if (renderSaiyanTail) {
			float[] tailColor = b2 != null ? b2 : DEFAULT_TAIL_COLOR;
			if (consumer instanceof BodyLayerSink sink) sink.saiyanTail(tailColor);
			else consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(SAIYAN_TAIL_TEXTURE)), tailColor);
		}
    }

	public static float[][] resolveBodyColors(StatsData stats) {
		var character = stats.getCharacter();
		return new float[][]{
				FormVisualTransition.color(stats, character.getRgbBodyColor(), FormConfig.FormData::getRgbBodyColor1),
				FormVisualTransition.color(stats, character.getRgbBodyColor2(), FormConfig.FormData::getRgbBodyColor2),
				FormVisualTransition.color(stats, character.getRgbBodyColor3(), FormConfig.FormData::getRgbBodyColor3),
				FormVisualTransition.color(stats, character.getRgbHairColor(), FormConfig.FormData::getRgbHairColor)
		};
	}

	private Character baseLook(Character character) {
		if (fusionScratch == null) fusionScratch = new Character();
		fusionScratch.setRace(character.getRaceName());
		fusionScratch.setGender(character.getGender());
		fusionScratch.setBodyType(character.getBodyType());
		fusionScratch.setHairColor(character.getHairColor());
		fusionScratch.clearActiveForm();
		fusionScratch.clearActiveStackForm();
		return fusionScratch;
	}

	public void gatherDonorLayers(String race, int bodyType, String gender, float[][] colors, BiConsumer<ResourceLocation, float[]> consumer) {
		if (donorScratch == null) donorScratch = new Character();
		donorScratch.setRace(race);
		donorScratch.setGender(gender);
		donorScratch.setBodyType(bodyType);
		switch (donorScratch.getRaceName()) {
			case "frostdemon" -> resolveBodyFrostDemon(donorScratch, "frostdemon", colors[0], colors[1], colors[2], colors[3], consumer);
			case "bioandroid" -> resolveBodyBioAndroid(donorScratch, "bioandroid", colors[0], colors[1], colors[2], colors[3], consumer);
			default -> {}
		}
	}

	public void gatherAndroidLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		var character = stats.getCharacter();
		String raceName = character.getRace().toLowerCase();
		boolean canBeUpgraded = ConfigManager.getRaceCharacter(raceName) != null && ConfigManager.getRaceCharacter(raceName).getFormSkillTpCosts("androidforms").length > 0;
		if (!canBeUpgraded || !stats.getStatus().isAndroidUpgraded()) return;

		String androidPath = character.getGender().equals(Character.GENDER_FEMALE) ? "textures/entity/races/female_android.png" : "textures/entity/races/male_android.png";
		emitFadingLayer(consumer, "android", DMZSkinLayer.getSafeTexture(getCachedTexture(androidPath)), WHITE_COLOR);
	}

	public void gatherTattooLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		int tattooType = stats.getCharacter().getTattooType();
		if (tattooType == Character.TATTOO_CUSTOM) {
			SkinPixels pixels = stats.getCharacter().getSkinPixels();
			if (!pixels.hasTattoo() || !SkinPixels.customTattooEnabled()) return;
			ResourceLocation custom = SkinPixelTextures.tattoo(player, pixels);
			if (custom != null) consumer.accept(custom, WHITE_COLOR);
			return;
		}
		if (tattooType <= 0) return;

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/tattoos/tattoo_" + tattooType + ".png")), WHITE_COLOR);
	}

	public void gatherEffectLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/majinm.png")), WHITE_COLOR);
		}
	}

	protected void resolveBodyHumanSaiyan(Character character, String key, float[] bodyColor, float[] bodyColor2, float[] bodyColor3, BiConsumer<ResourceLocation, float[]> consumer) {
		int bodyType = character.getBodyType();
        String gender = character.getGender().toLowerCase().trim();
        String genderPart = (gender.equals(Character.GENDER_FEMALE)) ? "_female" : "_male";
		String basePath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_" + bodyType + "_";
		String fallbackPath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_0_";
		RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(character.getRaceName());
		if (bodyType != 0 && raceConfig != null && raceConfig.isSlimBodyType(bodyType)) {
			ResourceLocation slimLayer = getCachedTexture(basePath + "slim_layer1.png");
			if (DMZSkinLayer.getSafeTexture(slimLayer).equals(slimLayer)) basePath = basePath + "slim_";
		}

		ResourceLocation shadowLayer = getCachedTexture(basePath + "layer2.png");
		boolean shaded = DMZSkinLayer.getSafeTexture(shadowLayer).equals(shadowLayer);

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png")), shaded ? ColorUtils.skinBaseTone(bodyColor) : bodyColor);
		if (shaded) emitTranslucentLayer(consumer, shadowLayer, ColorUtils.skinShadowTone(bodyColor));
	}

	protected void resolveBodyGlindTrueForm(float[] bodyColor, float[] bodyColor2, BiConsumer<ResourceLocation, float[]> consumer) {
		String basePath = "textures/entity/races/glind/trueform_";
		ResourceLocation layer1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"));
		consumer.accept(layer1, ColorUtils.skinBaseTone(bodyColor));
		emitShadowLayer(consumer, layer1, bodyColor);
		acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png")), bodyColor2);
	}

	protected void resolveBodyGlindTranscended(Character character, float[] bodyColor, float[] bodyColor2, BiConsumer<ResourceLocation, float[]> consumer) {
		boolean female = Character.GENDER_FEMALE.equalsIgnoreCase(character.getGender());
		String fallbackPath = female ? "textures/entity/races/humansaiyan/bodytype_female_1_" : "textures/entity/races/humansaiyan/bodytype_male_1_";
		String bodyPath = female ? "textures/entity/races/humansaiyan/bodytype_female_" + Math.max(1, character.getBodyType()) + "_" : fallbackPath;
		ResourceLocation bodyLayer = getCachedTexture(bodyPath + "layer1.png");
		if (!DMZSkinLayer.getSafeTexture(bodyLayer).equals(bodyLayer)) bodyPath = fallbackPath;
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(bodyPath + "layer1.png")), ColorUtils.skinBaseTone(bodyColor));
		ResourceLocation bodyShadow = getCachedTexture(bodyPath + "layer2.png");
		if (DMZSkinLayer.getSafeTexture(bodyShadow).equals(bodyShadow)) emitTranslucentLayer(consumer, bodyShadow, ColorUtils.skinShadowTone(bodyColor));
		ResourceLocation overlay = DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/glind/trascended_layer2.png"));
		emitOverlayLayer(consumer, overlay, ColorUtils.skinBaseTone(bodyColor2));
		ResourceLocation overlayShadow = getCachedTexture("textures/entity/races/glind/trascended_layer2_shadow.png");
		if (!DMZSkinLayer.getSafeTexture(overlayShadow).equals(overlayShadow)) return;
		float[] shadowTone = ColorUtils.skinShadowTone(bodyColor2);
		if (consumer instanceof BodyLayerSink sink) sink.overlayTranslucent(overlayShadow, shadowTone);
		else consumer.accept(overlayShadow, shadowTone);
	}

	protected void resolveBodyOozaru(float[] bodyColor, float[] bodyColor2, BiConsumer<ResourceLocation, float[]> consumer) {
		String basePath = "textures/entity/races/humansaiyan/oozaru_";
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(basePath + "layer1.png")), bodyColor2);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png"), getCachedTexture(basePath + "layer2.png")), bodyColor);
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer3.png"), getCachedTexture(basePath + "layer3.png")), WHITE_COLOR);
	}

    protected void resolveBodyNamekian(Character character, float[] c1, float[] c2, float[] c3, BiConsumer<ResourceLocation, float[]> consumer) {
        int bodyType = character.getBodyType();
        var hairColor = character.getRgbHairColor();
        String basePath = "textures/entity/races/namekian/bodytype_" + bodyType + "_";
        String fallbackPath = "textures/entity/races/namekian/bodytype_0_";

        ResourceLocation layer1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png"));
        consumer.accept(layer1, c1);
        emitShadowLayer(consumer, layer1, c1);

        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer2.png", fallbackPath + "layer2.png", c2, consumer), c2);
        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer3.png", fallbackPath + "layer3.png", c3, consumer), c3);
        emitShadowLayer(consumer, tryLoadOptionalLayerWithFallback(basePath + "layer4.png", fallbackPath + "layer4.png", hairColor, consumer), hairColor);
    }

    private void acceptWithShadow(BiConsumer<ResourceLocation, float[]> consumer, ResourceLocation layer, float[] color) {
        consumer.accept(layer, color);
        emitShadowLayer(consumer, layer, color);
    }

    private void emitShadowLayer(BiConsumer<ResourceLocation, float[]> consumer, ResourceLocation layer, float[] color) {
        if (layer == null) return;
        String path = layer.getPath();
        ResourceLocation shadow = getCachedTexture(path.substring(0, path.length() - ".png".length()) + "_shadow.png");
        if (DMZSkinLayer.getSafeTexture(shadow).equals(shadow)) emitTranslucentLayer(consumer, shadow, ColorUtils.skinShadowTone(color));
    }

	protected void resolveBodyFrostDemon(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<ResourceLocation, float[]> consumer) {
		String currentForm = character.getActiveForm();
		int bodyType = character.getBodyType();
		String folder = "textures/entity/races/frostdemon/";
		String prefix, fallbackPrefix;

		boolean isSecondForm = Objects.equals(currentForm, FrostDemonForms.SECOND_FORM);
		boolean isBase = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");
		boolean isBulky = (key.equals("frostdemon") && (isBase || isSecondForm) || key.equals("frostdemon_second"))
				|| key.equals("frostdemon_third");

		if (isBulky) {
			prefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_" + bodyType + "_" : folder + "bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_third") ? folder + "thirdform_bodytype_0_" : folder + "bodytype_0_";
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			if (bodyType == 0)
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_ORANGE_COLOR);
		} else {
			prefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_" + bodyType + "_" : folder + "finalform_bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_0_" : folder + "finalform_bodytype_0_";

			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), (bodyType == 0 || bodyType == 2) ? hair : b2);
			if (bodyType == 1) {
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			} else if (bodyType == 2) {
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), hair);
				acceptWithShadow(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			}
		}

		emitFrostDemonFormLayers(key, b1, consumer);
	}

	private void emitFrostDemonFormLayers(String key, float[] b1, BiConsumer<ResourceLocation, float[]> consumer) {
		String folder = "textures/entity/races/frostdemon/";
		if (key.equals("frostdemon_mecha")) {
			ResourceLocation mecha = getCachedTexture(folder + "mechaform_layer1.png");
			consumer.accept(DMZSkinLayer.getSafeTexture(mecha, mecha), b1);
		}
		if (key.equals("frostdemon_metalcore")) {
			ResourceLocation core1 = getCachedTexture(folder + "metalcore_layer1.png");
			ResourceLocation core2 = getCachedTexture(folder + "metalcore_layer2.png");
			emitFadingLayer(consumer, "metalcore_1", DMZSkinLayer.getSafeTexture(core1, core1), ColorUtils.hexToRgb("#9BA377"));
			emitFadingLayer(consumer, "metalcore_2", DMZSkinLayer.getSafeTexture(core2, core2), ColorUtils.hexToRgb("#20211A"));
		}
	}

    protected void resolveBodyBioAndroid(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<ResourceLocation, float[]> consumer) {
        String phase = switch (key) {
            case "bioandroid_semi" -> "semiperfect";
            case "bioandroid_perfect", "bioandroid_ultra", "bioandroid_xeno", "bioandroid_xenofp" -> "perfect";
            case "bioandroid_base" -> "base";
            case "bioandroid" -> character.hasActiveForm() ? "perfect" : "base";
            default -> "perfect";
        };

        int bodyType = character.getBodyType();

        String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";
        String formGroup = character.getActiveFormGroup() != null ? character.getActiveFormGroup() : "";

        String prefix = "textures/entity/races/bioandroid/" + phase + "_" + bodyType + "_";
        String fallbackPrefix = "textures/entity/races/bioandroid/" + phase + "_0_";

        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);

        if (!currentForm.equals("xenomax") && !currentForm.equals("xenofp")) {
            consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_STINGER_COLOR);
        }

        emitBioAndroidFormLayers(key, formGroup, consumer);
    }

	private void emitBioAndroidFormLayers(String key, String formGroup, BiConsumer<ResourceLocation, float[]> consumer) {
		boolean xenoModel = key.equals("bioandroid_xeno") || key.equals("bioandroid_xenofp");
		if (!formGroup.equals("legendaryforms") && !xenoModel) return;
		emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer1.png")), WHITE_COLOR);
		if (xenoModel) emitOverlayLayer(consumer, DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer2.png")), WHITE_COLOR);
	}

    protected void resolveBodyMajin(Character character, String key, float[] b1, float[] b2, float[] b3, BiConsumer<ResourceLocation, float[]> consumer) {

        if ("janemba_super".equals(key)) {
            String path = "textures/entity/races/majin/janembasuper_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            acceptWithShadow(consumer, getCachedTexture(path + "layer2.png"), b2);
            acceptWithShadow(consumer, getCachedTexture(path + "layer3.png"), b3);
            return;
        }

        if ("janemba_imperfect".equals(key)) {
            String path = "textures/entity/races/majin/janemba_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            return;
        }

        if ("janemba_fat".equals(key)) {
            String path = "textures/entity/races/majin/janembafat_0_male_";
            acceptWithShadow(consumer, getCachedTexture(path + "layer1.png"), b1);
            acceptWithShadow(consumer, getCachedTexture(path + "layer2.png"), b2);
            return;
        }

        String currentForm = character.getActiveForm();
        String gender = character.getGender().toLowerCase().trim();
        String genderSuffix = gender.equals(Character.GENDER_FEMALE) ? "female" : "male";
        String phase;

        if (Objects.equals(currentForm, MajinForms.KID) || key.equals("majin_kid")) phase = "kid";
        else if (Objects.equals(currentForm, MajinForms.EVIL) || key.equals("majin_evil")) phase = "evil";
        else if (Objects.equals(currentForm, MajinForms.SUPER) || key.equals("majin_super")) phase = "super";
        else if (Objects.equals(currentForm, MajinForms.ULTRA) || key.equals("majin_ultra")) phase = "ultra";
        else if (character.hasActiveForm()) phase = "super";
        else phase = "base";

        int bodyType = character.getBodyType();
        String basePath = "textures/entity/races/majin/bodytype_" + genderSuffix + "_" + bodyType + "_";
        String fallbackPath = "textures/entity/races/majin/bodytype_" + genderSuffix + "_0_";

        ResourceLocation l1 = DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png"));
        if (l1 != null) acceptWithShadow(consumer, l1, b1);

        emitShadowLayer(consumer, tryLoadOptionalLayer(basePath + "layer2.png", b2, consumer), b2);
        emitShadowLayer(consumer, tryLoadOptionalLayer(basePath + "layer3.png", b3, consumer), b3);

        if (genderSuffix.equals("female") && (phase.equals("super") || phase.equals("ultra"))) {
            ResourceLocation tailLoc = getCachedTexture("textures/entity/races/tail1.png");
            consumer.accept(DMZSkinLayer.getSafeTexture(tailLoc, tailLoc), b1);
        }
    }

    private ResourceLocation tryLoadOptionalLayer(String path, float[] color, BiConsumer<ResourceLocation, float[]> consumer) {
        ResourceLocation loc = getCachedTexture(path);
        if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            consumer.accept(loc, color);
            return loc;
        }
        return null;
    }

    private ResourceLocation tryLoadOptionalLayerWithFallback(String path, String fallbackPath, float[] color, BiConsumer<ResourceLocation, float[]> consumer) {
        ResourceLocation loc = getCachedTexture(path);
        ResourceLocation fallbackLoc = getCachedTexture(fallbackPath);

        if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            consumer.accept(loc, color);
            return loc;
        }
        else if (Minecraft.getInstance().getResourceManager().getResource(fallbackLoc).isPresent()) {
            consumer.accept(fallbackLoc, color);
            return fallbackLoc;
        }
        return null;
    }
}