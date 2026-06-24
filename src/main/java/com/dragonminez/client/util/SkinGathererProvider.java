package com.dragonminez.client.util;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.DMZSkinLayer;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.extras.ActionMode;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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
		if (key == null || key.isEmpty()) return "human";
		String k = key.toLowerCase();
		if (k.startsWith("oozaru")) return "oozaru";
		if (k.startsWith("namekian")) return "namekian";
		if (k.startsWith("frostdemon")) return "frostdemon";
		if (k.startsWith("bioandroid")) return "bioandroid";
		if (k.startsWith("majin") || k.startsWith("janemba")) return "majin";
		if (k.equals("human") || k.equals("saiyan") || k.equals("ssj4d") || k.equals("ssj4gt")
				|| k.equals("buffed") || k.equals("4arms")) return "human";
		return "custom";
	}

	public interface BodyLayerSink extends BiConsumer<ResourceLocation, float[]> {
		@Override
		default void accept(ResourceLocation texture, float[] color) {
			base(texture, color);
		}

		void base(ResourceLocation texture, float[] color);

		void fading(String layerId, ResourceLocation texture, float[] color);
	}

	private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();

	private static final float[] WHITE_COLOR = {1.0f, 1.0f, 1.0f};
	private static final float[] DEFAULT_TAIL_COLOR = ColorUtils.hexToRgb("#572117");
	private static final float[] DEFAULT_ORANGE_COLOR = ColorUtils.hexToRgb("#e67d40");
	private static final float[] DEFAULT_STINGER_COLOR = ColorUtils.hexToRgb("#D9B28D");

	public static ResourceLocation getCachedTexture(String path) {
		return TEXTURE_CACHE.computeIfAbsent(path, p -> ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, p));
	}

	private void emitFadingLayer(BiConsumer<ResourceLocation, float[]> consumer, String layerId, ResourceLocation texture, float[] color) {
		if (consumer instanceof BodyLayerSink sink) sink.fading(layerId, texture, color);
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

		float[] b1 = character.getRgbBodyColor();
		float[] b2 = character.getRgbBodyColor2();
		float[] b3 = character.getRgbBodyColor3();
		float[] hair = character.getRgbHairColor();

		if (hasForm) {
			var f = character.getActiveFormData();
			if (f.getRgbBodyColor1() != null) b1 = f.getRgbBodyColor1();
			if (f.getRgbBodyColor2() != null) b2 = f.getRgbBodyColor2();
			if (f.getRgbBodyColor3() != null) b3 = f.getRgbBodyColor3();
			if (f.getRgbHairColor() != null) hair = f.getRgbHairColor();
		}

		if (hasStackForm) {
			var sf = character.getActiveStackFormData();
			if (sf.getRgbBodyColor1() != null) b1 = sf.getRgbBodyColor1();
			if (sf.getRgbBodyColor2() != null) b2 = sf.getRgbBodyColor2();
			if (sf.getRgbBodyColor3() != null) b3 = sf.getRgbBodyColor3();
			if (sf.getRgbHairColor() != null) hair = sf.getRgbHairColor();
		}

		if (stats.getStatus().isActionCharging()) {
			FormConfig.FormData nextForm = null;
			if (stats.getStatus().getSelectedAction() == ActionMode.FORM) nextForm = TransformationsHelper.getNextAvailableForm(stats);
			else if (stats.getStatus().getSelectedAction() == ActionMode.STACK) nextForm = TransformationsHelper.getNextAvailableStackForm(stats);

			if (nextForm != null) {
				float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);
				if (nextForm.getRgbBodyColor1() != null) b1 = DMZSkinLayer.lerpColor(factor, b1, nextForm.getRgbBodyColor1());
				if (nextForm.getRgbBodyColor2() != null) b2 = DMZSkinLayer.lerpColor(factor, b2, nextForm.getRgbBodyColor2());
				if (nextForm.getRgbBodyColor3() != null) b3 = DMZSkinLayer.lerpColor(factor, b3, nextForm.getRgbBodyColor3());
				if (nextForm.getRgbHairColor() != null) hair = DMZSkinLayer.lerpColor(factor, hair, nextForm.getRgbHairColor());
			}
		}

		if (hasForm && character.getActiveFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_form", character.getActiveFormData());
		}
		if (hasStackForm && character.getActiveStackFormData().hasExtraFormLayer()) {
			emitExtraFormLayer(consumer, "extraform_stack", character.getActiveStackFormData());
		}

		boolean isOozaruForm = raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

		if (logicKey.equals("oozaru") || isOozaruForm) {
			resolveBodyOozaru(b1, b2, consumer);
			return;
		}

		boolean isSaiyanLogic = logicKey.equals("saiyan") || logicKey.equals("ssj4gt") || logicKey.equals("ssj4d") || raceName.equals("saiyan");
		boolean hasSaiyanTail = raceConfig.getHasSaiyanTail() != null && raceConfig.getHasSaiyanTail();
		boolean renderSaiyanTail = (isSaiyanLogic || hasSaiyanTail) && stats.getStatus().isTailVisible() && character.isHasSaiyanTail();

		boolean isHumanoid = logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.equals("ssj4d")
                || logicKey.equals("ssj4gt") || logicKey.equals("buffed") || logicKey.equals("4arms");

		if (isHumanoid && bodyType == 0) {
			consumer.accept(player.getSkinTextureLocation(), WHITE_COLOR);
		} else {
            switch (logicKey) {
                case "human", "saiyan", "ssj4gt", "ssj4d", "buffed", "4arms" -> resolveBodyHumanSaiyan(character, logicKey, b1, b2, b3, consumer);
                case "namekian", "namekian_orange", "namekian_buffed" -> resolveBodyNamekian(character, b1, b2, b3, consumer);
                case "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid", "janemba_fat","janemba_super" -> resolveBodyMajin(character, logicKey, b1, b2, b3, consumer);
                case "frostdemon", "frostdemon_second", "frostdemon_final", "frostdemon_fifth", "frostdemon_third", "frostdemon_fp", "frostdemon_mecha", "frostdemon_metalcore" -> resolveBodyFrostDemon(character, logicKey, b1, b2, b3, hair, consumer);
                case "bioandroid", "bioandroid_semi", "bioandroid_perfect", "bioandroid_base", "bioandroid_ultra", "bioandroid_xeno" -> resolveBodyBioAndroid(character, logicKey, b1, b2, b3, hair, consumer);
                default -> {
                    boolean hasGender = Boolean.TRUE.equals(raceConfig.getHasGender());
                    String genSuffix = hasGender ? ((character.getGender().equalsIgnoreCase("female") || character.getGender().equalsIgnoreCase("mujer")) ? "_female" : "_male") : "";

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

		if (renderSaiyanTail) {
			float[] tailColor;
			if (hasStackForm && character.getActiveStackFormData().getRgbBodyColor2() != null) tailColor = character.getActiveStackFormData().getRgbBodyColor2();
			else if (hasForm && character.getActiveFormData().getRgbBodyColor2() != null) tailColor = character.getActiveFormData().getRgbBodyColor2();
			else if (character.getRgbBodyColor2() != null) tailColor = character.getRgbBodyColor2();
			else tailColor = DEFAULT_TAIL_COLOR;
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/tail1.png")), tailColor);
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
		if (tattooType == 0) return;

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/tattoos/tattoo_" + tattooType + ".png")), WHITE_COLOR);
	}

	public void gatherEffectLayers(AbstractClientPlayer player, StatsData stats, float partialTick, BiConsumer<ResourceLocation, float[]> consumer) {
		if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/majinm.png")), WHITE_COLOR);
		}
	}

	protected void resolveBodyHumanSaiyan(Character character, String key, float[] bodyColor, float[] bodyColor2, float[] bodyColor3, BiConsumer<ResourceLocation, float[]> consumer) {
		int bodyType = character.getBodyType();
        var legendaryGroup = character.getActiveFormGroup().equals("legendaryforms");
        String gender = character.getGender().toLowerCase().trim();
        String genderPart = (gender.equals("female") || gender.equals("mujer")) ? "_female" : "_male";
		String path = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_" + bodyType + ".png";
		String fallbackPath = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_0.png";

        float[] finalBodyColor = bodyColor;
        if(legendaryGroup && (character.getActiveForm().equals("shiyoken") || character.getActiveForm().equals("shin_shiyoken") || character.getActiveForm().equals("chou_shiyoken"))){

            float redness = 0.5F;

            float newR = Math.min(1.0F, bodyColor[0] + redness);
            float newG = bodyColor[1] * (1.0F - (redness * 0.5F));
            float newB = bodyColor[2] * (1.0F - (redness * 0.5F));

            finalBodyColor = new float[]{newR, newG, newB};
        }

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(path), getCachedTexture(fallbackPath)), finalBodyColor);
	}

	protected void resolveBodyOozaru(float[] bodyColor, float[] bodyColor2, BiConsumer<ResourceLocation, float[]> consumer) {
		String basePath = "textures/entity/races/humansaiyan/oozaru_";
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(basePath + "layer1.png")), bodyColor2);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png"), getCachedTexture(basePath + "layer2.png")), bodyColor);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer3.png"), getCachedTexture(basePath + "layer3.png")), WHITE_COLOR);
	}

	protected void resolveBodyNamekian(Character character, float[] c1, float[] c2, float[] c3, BiConsumer<ResourceLocation, float[]> consumer) {
		int bodyType = character.getBodyType();
		String basePath = "textures/entity/races/namekian/bodytype_" + bodyType + "_";
		String fallbackPath = "textures/entity/races/namekian/bodytype_0_";

		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer1.png"), getCachedTexture(fallbackPath + "layer1.png")), c1);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer2.png"), getCachedTexture(fallbackPath + "layer2.png")), c2);
		consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(basePath + "layer3.png"), getCachedTexture(fallbackPath + "layer3.png")), c3);
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
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			if (bodyType == 0)
				consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_ORANGE_COLOR);
		} else {
			prefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_" + bodyType + "_" : folder + "finalform_bodytype_" + bodyType + "_";
			fallbackPrefix = key.equals("frostdemon_fifth") ? folder + "fifth_bodytype_0_" : folder + "finalform_bodytype_0_";

			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
			consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), (bodyType == 0 || bodyType == 2) ? hair : b2);
			if (bodyType == 1) {
				consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
				consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);
			} else if (bodyType == 2) {
				consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), hair);
				consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
			}
		}

        if(key.equals("frostdemon_mecha")){
            prefix = folder + "mechaform_";
            fallbackPrefix = folder + "mechaform_";

            consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), b1);
        }

        if(key.equals("frostdemon_metalcore")){
            prefix = folder + "metalcore_";
            fallbackPrefix = folder + "metalcore_";

            emitFadingLayer(consumer, "metalcore_1", DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), ColorUtils.hexToRgb("#9BA377"));
            emitFadingLayer(consumer, "metalcore_2", DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), ColorUtils.hexToRgb("#20211A"));

        }
	}

    protected void resolveBodyBioAndroid(Character character, String key, float[] b1, float[] b2, float[] b3, float[] hair, BiConsumer<ResourceLocation, float[]> consumer) {
        String phase = switch (key) {
            case "bioandroid_semi" -> "semiperfect";
            case "bioandroid_perfect", "bioandroid_ultra", "bioandroid_xeno" -> "perfect";
            case "bioandroid_base" -> "base";
            case "bioandroid" -> character.hasActiveForm() ? "perfect" : "base";
            default -> "perfect";
        };

        int bodyType = character.getBodyType();

        // Validaciones seguras
        String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";
        String formGroup = character.getActiveFormGroup() != null ? character.getActiveFormGroup() : "";
        boolean legendaryGroup = formGroup.equals("legendaryforms");

        String prefix = "textures/entity/races/bioandroid/" + phase + "_" + bodyType + "_";
        String fallbackPrefix = "textures/entity/races/bioandroid/" + phase + "_0_";

        float[] finalBodyColor = b1;
        if(legendaryGroup){
            finalBodyColor = ColorUtils.darkenColor(b1, 0.4f);
        }

        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer1.png"), getCachedTexture(fallbackPrefix + "layer1.png")), finalBodyColor);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer2.png"), getCachedTexture(fallbackPrefix + "layer2.png")), b2);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer3.png"), getCachedTexture(fallbackPrefix + "layer3.png")), b3);
        consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer4.png"), getCachedTexture(fallbackPrefix + "layer4.png")), hair);

        if (!currentForm.equals("xenomax") && !currentForm.equals("xenofp")) {
            consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture(prefix + "layer5.png"), getCachedTexture(fallbackPrefix + "layer5.png")), DEFAULT_STINGER_COLOR);
        }

        if(legendaryGroup){
            consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer1.png"), getCachedTexture("textures/entity/races/bioandroid/xenoform_layer1.png")), ColorUtils.hexToRgb("#FFFFFF"));

            if (currentForm.equals("xenomax")){
                consumer.accept(DMZSkinLayer.getSafeTexture(getCachedTexture("textures/entity/races/bioandroid/xenoform_layer2.png"), getCachedTexture("textures/entity/races/bioandroid/xenoform_layer2.png")), ColorUtils.hexToRgb("#FFFFFF"));
            }
        }
    }

    protected void resolveBodyMajin(Character character, String key, float[] b1, float[] b2, float[] b3, BiConsumer<ResourceLocation, float[]> consumer) {

        if ("janemba_super".equals(key)) {
            String path = "textures/entity/races/majin/janembasuper_0_male_";
            consumer.accept(getCachedTexture(path + "layer1.png"), b1);
            consumer.accept(getCachedTexture(path + "layer2.png"), b2);
            consumer.accept(getCachedTexture(path + "layer3.png"), b3);
            return;
        }

        if ("janemba_fat".equals(key)) {
            String path = "textures/entity/races/majin/janemba_0_male_";
            consumer.accept(getCachedTexture(path + "layer1.png"), b1);
            return;
        }

        String currentForm = character.getActiveForm();
        String gender = character.getGender().toLowerCase().trim();
        String genderSuffix = (gender.equals("female") || gender.equals("mujer")) ? "female" : "male";
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
        if (l1 != null) consumer.accept(l1, b1);

        tryLoadOptionalLayer(basePath + "layer2.png", b2, consumer);
        tryLoadOptionalLayer(basePath + "layer3.png", b3, consumer);

        if (genderSuffix.equals("female") && (phase.equals("super") || phase.equals("ultra"))) {
            ResourceLocation tailLoc = getCachedTexture("textures/entity/races/tail1.png");
            consumer.accept(DMZSkinLayer.getSafeTexture(tailLoc, tailLoc), b1);
        }
    }

    private void tryLoadOptionalLayer(String path, float[] color, BiConsumer<ResourceLocation, float[]> consumer) {
        ResourceLocation loc = getCachedTexture(path);
        if (Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()) {
            consumer.accept(loc, color);
        }
    }
}