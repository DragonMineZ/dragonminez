package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.hair.HairManager;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.*;

public class DMZSkinLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    private int currentKaiokenPhase = 0;

    public DMZSkinLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        var player = (AbstractClientPlayer) animatable;
        if (player == null) return;

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
        var stats = statsCap.orElse(new StatsData(player));

        this.currentKaiokenPhase = TransformationsHelper.getKaiokenPhase(stats);

        float alpha = player.isSpectator() ? 0.15f : 1.0f;

        renderBody(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
        renderHair(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
		renderAndroid(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
        renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
        renderTattoos(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay, alpha);
    }

    private void renderBody(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
        var character = stats.getCharacter();
        String raceName = character.getRaceName().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        String raceCustomModel = (raceConfig != null) ? raceConfig.getCustomModel().toLowerCase() : "";
        String formCustomModel = (character.hasActiveForm() && character.getActiveFormData().hasCustomModel())
                ? character.getActiveFormData().getCustomModel().toLowerCase() : "";

        String key = formCustomModel.isEmpty() ? raceCustomModel : formCustomModel;
        if (key.isEmpty()) key = raceName;

        String logicKey = key;
        if (key.equals("human_slim") || key.equals("majin_slim") || key.equals("base_slim")) {
            logicKey = raceName;
        }

        float[] b1 = hexToRGB(character.getBodyColor());
        float[] b2 = hexToRGB(character.getBodyColor2());
        float[] b3 = hexToRGB(character.getBodyColor3());
        float[] hair = hexToRGB(character.getHairColor());

        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            var f = character.getActiveFormData();
            if (!f.getBodyColor1().isEmpty()) b1 = hexToRGB(f.getBodyColor1());
            if (!f.getBodyColor2().isEmpty()) b2 = hexToRGB(f.getBodyColor2());
            if (!f.getBodyColor3().isEmpty()) b3 = hexToRGB(f.getBodyColor3());
            if (!f.getHairColor().isEmpty()) hair = hexToRGB(f.getHairColor());
        }

        if (stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == com.dragonminez.common.stats.ActionMode.FORM) {
            var nextForm = com.dragonminez.common.util.TransformationsHelper.getNextAvailableForm(stats);
            if (nextForm != null) {
                float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);

                if (!nextForm.getBodyColor1().isEmpty()) {
                    float[] target = hexToRGB(nextForm.getBodyColor1());
                    b1 = new float[]{Mth.lerp(factor, b1[0], target[0]), Mth.lerp(factor, b1[1], target[1]), Mth.lerp(factor, b1[2], target[2])};
                }
                if (!nextForm.getBodyColor2().isEmpty()) {
                    float[] target = hexToRGB(nextForm.getBodyColor2());
                    b2 = new float[]{Mth.lerp(factor, b2[0], target[0]), Mth.lerp(factor, b2[1], target[1]), Mth.lerp(factor, b2[2], target[2])};
                }
                if (!nextForm.getBodyColor3().isEmpty()) {
                    float[] target = hexToRGB(nextForm.getBodyColor3());
                    b3 = new float[]{Mth.lerp(factor, b3[0], target[0]), Mth.lerp(factor, b3[1], target[1]), Mth.lerp(factor, b3[2], target[2])};
                }
                if (!nextForm.getHairColor().isEmpty()) {
                    float[] target = hexToRGB(nextForm.getHairColor());
                    hair = new float[]{Mth.lerp(factor, hair[0], target[0]), Mth.lerp(factor, hair[1], target[1]), Mth.lerp(factor, hair[2], target[2])};
                }
            }
        }

        boolean isOozaruForm = raceName.equals("saiyan") &&
                (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

        if (logicKey.equals("oozaru") || isOozaruForm) {
            renderBodyOozaru(poseStack, animatable, model, bufferSource, player, stats, b1, hair, alpha, partialTick, packedLight, packedOverlay);
            return;
        }

        if (logicKey.equals("saiyan") && stats.getStatus().isTailVisible()) {
            float[] tailColor = character.hasActiveForm() ? hair : hexToRGB("#572117");
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/tail1.png", tailColor, partialTick, packedLight, packedOverlay, alpha);
        }

        boolean isHumanoid = logicKey.equals("human") || logicKey.equals("saiyan") || logicKey.equals("saiyan_ssj4");
        if (isHumanoid && bodyType == 0) {
            ResourceLocation playerSkin = player.getSkinTextureLocation();
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(playerSkin), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha);
            return;
        }

        switch (logicKey) {
            case "bioandroid", "bioandroid_semi", "bioandroid_perfect":
                renderBodyBioAndroid(poseStack, animatable, model, bufferSource, player, stats, b1, b2, b3, hair, alpha, partialTick, packedLight, packedOverlay, key);
                break;

            case "majin", "majin_super", "majin_ultra", "majin_evil", "majin_kid":
                renderBodyMajin(poseStack, animatable, model, bufferSource, player, stats, b1, b2, b3, alpha, partialTick, packedLight, packedOverlay, key);
                break;

            case "frostdemon", "frostdemon_final", "frostdemon_fifth", "frostdemon_third":
                renderBodyFrostDemon(poseStack, animatable, model, bufferSource, player, stats, b1, b2, b3, hair, alpha, partialTick, packedLight, packedOverlay, key);
                break;

            case "namekian", "namekian_orange":
                renderBodyNamekian(poseStack, animatable, model, bufferSource, player, stats, b1, b2, b3, alpha, partialTick, packedLight, packedOverlay);
                break;

            case "human", "saiyan", "saiyan_ssj4":
                renderBodyHumanSaiyan(poseStack, animatable, model, bufferSource, player, stats, b1, hair, alpha, partialTick, packedLight, packedOverlay);
                break;

            default:
                String gender = (raceConfig != null && raceConfig.hasGender()) ? "_" + character.getGender().toLowerCase() : "";
                ResourceLocation customTex = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/" + key + gender + ".png");
                renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(customTex), b1[0], b1[1], b1[2], 1.0f, partialTick, packedLight, packedOverlay, alpha);
                break;
        }
    }

    private void renderBodyHumanSaiyan(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] bodyColor, float[] hairColor, float alpha, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        int bodyType = character.getBodyType();
        String gender = character.getGender().toLowerCase().trim();

        String genderPart = (gender.equals("female") || gender.equals("mujer")) ? "_female" : "_male";

        String path = "textures/entity/races/humansaiyan/bodytype" + genderPart + "_" + bodyType + ".png";

        ResourceLocation textureLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(textureLoc), bodyColor[0], bodyColor[1], bodyColor[2], 1.0f, partialTick, packedLight, packedOverlay, alpha);
    }

    private void renderBodyOozaru(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] bodyColor, float[] hairColor, float alpha, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String currentForm = character.getActiveForm();

        float[] skin = hexToRGB("#FFD7CF");
        float[] furColor = hexToRGB("#572117");

        if (Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU) || (character.hasActiveForm() && !Objects.equals(currentForm, SaiyanForms.OOZARU))) {
            furColor = hairColor;
        }

        String basePath = "textures/entity/races/humansaiyan/oozaru_";

        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer1.png", furColor, partialTick, packedLight, packedOverlay, alpha); // Pelaje
        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer2.png", skin, partialTick, packedLight, packedOverlay, alpha);     // Piel (Hocico/Manos)
        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer3.png", new float[]{1f, 1f, 1f}, partialTick, packedLight, packedOverlay, alpha); // Ojos/Detalles
    }

    private void renderBodyNamekian(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] c1, float[] c2, float[] c3, float alpha, float partialTick, int packedLight, int packedOverlay) {
        int bodyType = stats.getCharacter().getBodyType();

        //String basePath = "textures/entity/races/namekian/bodytype_" + bodyType + "_";
        String basePath = "textures/entity/races/namekian/bodytype_0_";

        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer1.png", c1, partialTick, packedLight, packedOverlay, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer2.png", c2, partialTick, packedLight, packedOverlay, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, basePath + "layer3.png", c3, partialTick, packedLight, packedOverlay, alpha);
    }

    private void renderBodyFrostDemon(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] b1, float[] b2, float[] b3, float[] hair, float alpha, float partialTick, int packedLight, int packedOverlay, String key) {
        var character = stats.getCharacter();
        String currentForm = character.getActiveForm();
        int bodyType = character.getBodyType();
        float[] orangeColor = hexToRGB("#e67d40");
        String folder = "textures/entity/races/frostdemon/";
        String prefix;

        boolean isSecondForm = Objects.equals(currentForm, FrostDemonForms.SECOND_FORM);
        boolean isBase = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");

        boolean isBulky = (key.equals("frostdemon") && (isBase || isSecondForm)) || key.equals("frostdemon_third");

        if (isBulky) {
            prefix = key.equals("frostdemon_third")
                    ? folder + "thirdform_bodytype_" + bodyType + "_"
                    : folder + "bodytype_" + bodyType + "_";

            if (bodyType == 0) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", hair, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer5.png", orangeColor, partialTick, packedLight, packedOverlay, alpha);
            } else {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", hair, partialTick, packedLight, packedOverlay, alpha);
            }
        } else {
            prefix = key.equals("frostdemon_fifth")
                    ? folder + "fifth_bodytype_" + bodyType + "_"
                    : folder + "finalform_bodytype_" + bodyType + "_";

            if (bodyType == 0) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", hair, partialTick, packedLight, packedOverlay, alpha);
            } else if (bodyType == 1) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", hair, partialTick, packedLight, packedOverlay, alpha);
            } else if (bodyType == 2) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, partialTick, packedLight, packedOverlay, alpha);
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", hair, partialTick, packedLight, packedOverlay, alpha);
            }
        }
    }

    private void renderBodyBioAndroid(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] b1, float[] b2, float[] b3, float[] hair, float alpha, float partialTick, int packedLight, int packedOverlay, String key) {
        var character = stats.getCharacter();
        String phase;

        if (key.equals("bioandroid_semi")) {
            phase = "semiperfect";
        }
        else if (key.equals("bioandroid_perfect")) {
            phase = "perfect";
        }
        else if (key.equals("bioandroid_base")) {
            phase = "base";
        }
        else if (key.equals("bioandroid")) {
            phase = character.hasActiveForm() ? "perfect" : "base";
        }
        else {
            phase = "perfect";
        }

        String prefix = "textures/entity/races/bioandroid/" + phase + "_0_";
        float[] stinger = hexToRGB("#D9B28D");
        float[] white = {1.0f, 1.0f, 1.0f};

        float[] layer2Color = phase.equals("perfect") ? white : b2;

        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha); // Piel
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", layer2Color, partialTick, packedLight, packedOverlay, alpha); // Manchas
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, partialTick, packedLight, packedOverlay, alpha); // Exoesqueleto
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", hair, partialTick, packedLight, packedOverlay, alpha); // Alas / Cara
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer5.png", stinger, partialTick, packedLight, packedOverlay, alpha); // Aguijón
    }

    private void renderBodyMajin(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float[] b1, float[] b2, float[] b3, float alpha, float partialTick, int packedLight, int packedOverlay, String key) {
        var character = stats.getCharacter();
        String currentForm = character.getActiveForm();
        String gender = character.getGender().toLowerCase().trim();
        String genderSuffix = (gender.equals("female") || gender.equals("mujer")) ? "female" : "male";
        boolean isFemale = genderSuffix.equals("female");

        String phase;

        if (Objects.equals(currentForm, MajinForms.KID) || key.equals("majin_kid")) phase = "kid";
        else if (Objects.equals(currentForm, MajinForms.EVIL) || key.equals("majin_evil")) phase = "evil";
        else if (Objects.equals(currentForm, MajinForms.SUPER) || key.equals("majin_super")) phase = "super";
        else if (Objects.equals(currentForm, MajinForms.ULTRA) || key.equals("majin_ultra")) phase = "ultra";
        else if (character.hasActiveForm()) {
            phase = "super";
        } else {
            phase = "base";
        }

        String prefix = "textures/entity/races/majin/" + phase + "_0_" + genderSuffix + "_";

        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, partialTick, packedLight, packedOverlay, alpha);

        if (isFemale && (phase.equals("super") || phase.equals("ultra"))) {
            String tailPath = "textures/entity/races/tail1.png";
            renderColoredLayer(model, poseStack, animatable, bufferSource, tailPath, b1, partialTick, packedLight, packedOverlay, alpha);
        }
    }

    private void renderHair(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
        var character = stats.getCharacter();
        String raceName = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();
        int hairId = character.getHairId();

        if (!HairManager.canUseHair(character)) return;
        if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) return;
        if (hairId == 5) return;
        if (hairId == 0 && character.getHairBase().getVisibleStrandCount() == 0) return;

        float[] currentTint = hexToRGB(character.getHairColor());
        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            var activeForm = character.getActiveFormData();
            if (!activeForm.getHairColor().isEmpty()) {
                currentTint = hexToRGB(activeForm.getHairColor());
            }
        }

        float[] finalTint = currentTint;

        if (stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM) {
            var nextForm = com.dragonminez.common.util.TransformationsHelper.getNextAvailableForm(stats);

            if (nextForm != null && !nextForm.getHairColor().isEmpty()) {
                float[] targetTint = hexToRGB(nextForm.getHairColor());

                float chargeProgress = stats.getResources().getActionCharge() / 100.0f;
                chargeProgress = Mth.clamp(chargeProgress, 0.0f, 1.0f);

                finalTint = new float[] {
                        Mth.lerp(chargeProgress, currentTint[0], targetTint[0]),
                        Mth.lerp(chargeProgress, currentTint[1], targetTint[1]),
                        Mth.lerp(chargeProgress, currentTint[2], targetTint[2])
                };
            }
        }

        final float[] hairTint = finalTint;

        model.getBone("head").ifPresent(headBone -> {
            float originalZ = headBone.getPosZ();
            float originalSX = headBone.getScaleX();
            float originalSY = headBone.getScaleY();
            float originalSZ = headBone.getScaleZ();

            float inflation = 0.006f;
            headBone.setPosZ(originalZ - inflation);
            headBone.setScaleX(originalSX + inflation);
            headBone.setScaleY(originalSY + inflation);
            headBone.setScaleZ(originalSZ + inflation);

            List<String> hiddenBones = new ArrayList<>();
            for (GeoBone bone : model.topLevelBones()) {
                if (!bone.isHidden()) {
                    hiddenBones.add(bone.getName());
                    bone.setHidden(true);
                }
            }

            headBone.setHidden(false);
            unhideParents(headBone);

            String hairPath = "textures/entity/races/hair_base.png";

            renderColoredLayer(model, poseStack, animatable, bufferSource, hairPath, hairTint, partialTick, packedLight, packedOverlay, alpha);

            for (GeoBone bone : model.topLevelBones()) {
                if (hiddenBones.contains(bone.getName())) {
                    bone.setHidden(false);
                }
            }

            headBone.setPosZ(originalZ);
            headBone.setScaleX(originalSX);
            headBone.setScaleY(originalSY);
            headBone.setScaleZ(originalSZ);
        });
    }

	private void renderAndroid(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
		var character = stats.getCharacter();
		String raceName = character.getRace().toLowerCase();
		String currentForm = character.getActiveForm();
		// Luego podemos hacer q el FusedAndroid (Super A13) no tenga el layer del Android, si no q tenga directamente otra skin idk

		if (!raceName.equals("human")) return;
		if (!stats.getStatus().isAndroidUpgraded()) return;

		String androidPath = "";
		if (character.getGender().equals(Character.GENDER_FEMALE)) androidPath = "textures/entity/races/female_android.png";
		else androidPath = "textures/entity/races/male_android.png";

		ResourceLocation androidLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, androidPath);
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(androidLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha);
	}

    private void renderTattoos(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {

        if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
            ResourceLocation majinMarkLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/majinm.png");
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(majinMarkLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha);
        }

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType == 0) return;

        ResourceLocation tattooLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/tattoos/tattoo_" + tattooType + ".png");
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(tattooLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay, alpha);

    }

    private void renderFace(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay, float alpha) {
        var character = stats.getCharacter();
        String raceName = character.getRaceName().toLowerCase();
        String currentForm = character.getActiveForm();
        int bodyType = character.getBodyType();

        String customModelValue = (character.hasActiveForm() && character.getActiveFormData().hasCustomModel())
                ? character.getActiveFormData().getCustomModel().toLowerCase()
                : (ConfigManager.getRaceCharacter(raceName) != null ? ConfigManager.getRaceCharacter(raceName).getCustomModel().toLowerCase() : "");

        final boolean isModelEmpty = customModelValue.isEmpty();
        final String finalFaceKey = isModelEmpty ? raceName : customModelValue;

        boolean isOozaruForm = raceName.equals("saiyan") &&
                (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU));

        if (isOozaruForm || finalFaceKey.equals("oozaru")) {
            return;
        }

        boolean isHumanoidModel = finalFaceKey.equals("human") || finalFaceKey.equals("saiyan") || finalFaceKey.equals("saiyan_ssj4");
        if (isHumanoidModel && bodyType == 0) {
            return;
        }

        model.getBone("head").ifPresent(headBone -> {
            float originalZ = headBone.getPosZ();
            headBone.setPosZ(originalZ - 0.001f);

            dispatchFaceRender(model, poseStack, animatable, bufferSource, character, finalFaceKey, isModelEmpty, raceName, partialTick, packedLight, packedOverlay, alpha);

            headBone.setPosZ(originalZ);
        });
    }

    private void dispatchFaceRender(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float pt, int pl, int po, float alpha) {

        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);

        float[] eye1 = hexToRGB(character.getEye1Color());
        float[] eye2 = hexToRGB(character.getEye2Color());
        float[] skin = hexToRGB(character.getBodyColor());
        float[] b2 = hexToRGB(character.getBodyColor2());
        float[] hair = hexToRGB(character.getHairColor());

        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            var f = character.getActiveFormData();
            if (!f.getEye1Color().isEmpty()) eye1 = hexToRGB(f.getEye1Color());
            if (!f.getEye2Color().isEmpty()) eye2 = hexToRGB(f.getEye2Color());
            if (!f.getHairColor().isEmpty()) hair = hexToRGB(f.getHairColor());
            if (!f.getBodyColor1().isEmpty()) skin = hexToRGB(f.getBodyColor1());
            if (!f.getBodyColor2().isEmpty()) b2 = hexToRGB(f.getBodyColor2());
        }

        if (stats != null && stats.getStatus().isActionCharging() && stats.getStatus().getSelectedAction() == ActionMode.FORM) {
            var nextForm = com.dragonminez.common.util.TransformationsHelper.getNextAvailableForm(stats);
            if (nextForm != null) {
                float factor = Mth.clamp(stats.getResources().getActionCharge() / 100.0f, 0.0f, 1.0f);

                if (!nextForm.getEye1Color().isEmpty()) {
                    float[] targetEye1 = hexToRGB(nextForm.getEye1Color());
                    eye1 = new float[]{
                            Mth.lerp(factor, eye1[0], targetEye1[0]),
                            Mth.lerp(factor, eye1[1], targetEye1[1]),
                            Mth.lerp(factor, eye1[2], targetEye1[2])
                    };
                }
                if (!nextForm.getEye2Color().isEmpty()) {
                    float[] targetEye2 = hexToRGB(nextForm.getEye2Color());
                    eye2 = new float[]{
                            Mth.lerp(factor, eye2[0], targetEye2[0]),
                            Mth.lerp(factor, eye2[1], targetEye2[1]),
                            Mth.lerp(factor, eye2[2], targetEye2[2])
                    };
                }
                if (!nextForm.getBodyColor1().isEmpty()) {
                    float[] targetSkin = hexToRGB(nextForm.getBodyColor1());
                    skin = new float[]{
                            Mth.lerp(factor, skin[0], targetSkin[0]),
                            Mth.lerp(factor, skin[1], targetSkin[1]),
                            Mth.lerp(factor, skin[2], targetSkin[2])
                    };
                }
                if (!nextForm.getHairColor().isEmpty()) {
                    float[] targetHair = hexToRGB(nextForm.getHairColor());
                    hair = new float[]{
                            Mth.lerp(factor, hair[0], targetHair[0]),
                            Mth.lerp(factor, hair[1], targetHair[1]),
                            Mth.lerp(factor, hair[2], targetHair[2])
                    };
                }
            }
        }

        if (faceKey.equals("human") || faceKey.equals("saiyan") || faceKey.equals("saiyan_ssj4")) {
            renderHumanFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
            return;
        }
        if (faceKey.equals("namekian") || faceKey.equals("namekian_orange")) {
            renderNamekianFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, pt, pl, po, alpha);
            return;
        }
        if (faceKey.startsWith("frostdemon")) {
            renderFrostFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, skin, b2, pt, pl, po, alpha);
            return;
        }
        if (faceKey.startsWith("bioandroid")) {
            renderBioFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, pt, pl, po, alpha);
            return;
        }

        if (faceKey.equals("majin") || faceKey.equals("majin_super") ||
                faceKey.equals("majin_ultra") || faceKey.equals("majin_evil") ||
                faceKey.equals("majin_kid")) {
            renderMajinFace(model, poseStack, animatable, bufferSource, character, eye1, skin, pt, pl, po, alpha);
            return;
        }

        switch (race) {
            case "human", "saiyan":
                renderHumanFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, hair, pt, pl, po, alpha);
                break;
            case "namekian":
                renderNamekianFace(model, poseStack, animatable, bufferSource, character, eye1, eye2, skin, pt, pl, po, alpha);
                break;
            case "frostdemon":
                renderFrostFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, skin, b2, pt, pl, po, alpha);
                break;
            case "bioandroid":
                renderBioFace(model, poseStack, animatable, bufferSource, character, faceKey, isModelEmpty, race, eye1, eye2, pt, pl, po, alpha);
                break;
            case "majin":
                renderMajinFace(model, poseStack, animatable, bufferSource, character, eye1, skin, pt, pl, po, alpha);
                break;
        }
    }

    private void renderHumanFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float[] hair, float pt, int pl, int po, float alpha) {
        String folder = "textures/entity/races/humansaiyan/faces/";
        String eyeBase = "humansaiyan_eye_" + character.getEyesType();
        float[] white = {1.0f, 1.0f, 1.0f};

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", white, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_3.png", hair, pt, pl, po, alpha);

        if (character.getActiveFormData() != null && character.getActiveFormData().getHairType().equalsIgnoreCase("ssj3")) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "ssj3eyebrows_eye_" + character.getEyesType() + ".png", skin, pt, pl, po, alpha);
        }

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "humansaiyan_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
    }

    private void renderNamekianFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] eye2, float[] skin, float pt, int pl, int po, float alpha) {
        String folder = "textures/entity/races/namekian/faces/";
        String eyeBase = "namekian_eye_" + character.getEyesType();
        float[] white = {1.0f, 1.0f, 1.0f};

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", white, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_3.png", skin, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
    }

    private void renderFrostFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float[] skin, float[] b2, float pt, int pl, int po, float alpha) {
        String folder = "textures/entity/races/frostdemon/faces/";
        float[] white = {1.0f, 1.0f, 1.0f};
        float[] red = {1.0f, 0.0f, 0.0f};

        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm() != null ? character.getActiveForm().toLowerCase() : "";

        boolean isFifth = faceKey.equals("frostdemon_fifth") || currentForm.contains(FrostDemonForms.FIFTH_FORM);

        float[] eyeBgColor = isFifth ? red : white;
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_eye_0.png", eyeBgColor, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_eye_1.png", eye1, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_eye_2.png", eye2, pt, pl, po, alpha);

        if (isFifth) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_fifth_mouth.png", skin, pt, pl, po, alpha);
            return;
        }

        float[] finalDetailColor;

        boolean isPrimitiveForm = !character.hasActiveForm() ||
                currentForm.equals("second") ||
                currentForm.equals("third");

        if (isPrimitiveForm && (faceKey.equals("frostdemon") || faceKey.equals("frostdemon_third"))) {
            finalDetailColor = b2;
        }
        else {
            finalDetailColor = (bodyType == 1) ? b2 : skin;
        }

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_nose_" + character.getNoseType() + ".png", finalDetailColor, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_mouth_" + character.getMouthType() + ".png", finalDetailColor, pt, pl, po, alpha);
    }

    private void renderBioFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String faceKey, boolean isModelEmpty, String race, float[] eye1, float[] eye2, float pt, int pl, int po, float alpha) {
        String folder = "textures/entity/races/bioandroid/faces/";
        String phase;
        String currentForm = character.getActiveForm() != null ? character.getActiveForm() : "";

        if (faceKey.equals("bioandroid_semi")) {
            phase = "semiperfect";
        } else if (faceKey.equals("bioandroid_perfect")) {
            phase = "perfect";
        } else if (faceKey.equals("bioandroid") && !isModelEmpty) {
            phase = "base";
        }
        else if (race.equals("bioandroid")) {
            if (character.hasActiveForm()) {
                if (currentForm.equals(BioAndroidForms.SEMI_PERFECT)) {
                    phase = "semiperfect";
                } else {
                    phase = "perfect";
                }
            } else {
                phase = "base";
            }
        } else {
            phase = "base";
        }

        float[] color0;

        if (phase.equals("base")) {
            color0 = hexToRGB("#FF6B6B");
        }
        else {
            color0 = hexToRGB("#FFFFFF");
        }

        String textureBase = folder + phase + "_eye_layer";

        renderColoredLayer(model, poseStack, animatable, bufferSource, textureBase + "0.png", color0, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, animatable, bufferSource, textureBase + "1.png", eye1, pt, pl, po, alpha);
    }

    private void renderMajinFace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, float[] eye1, float[] skin, float pt, int pl, int po, float alpha) {
        String folder = "textures/entity/races/majin/faces/";
        int eyeType = character.getEyesType();
        float[] darkGray = hexToRGB("#383838");

        float[] bgColor;
        float[] layer1Color;

        if (eyeType == 0) {
            bgColor = skin;
            layer1Color = skin;
        } else {
            bgColor = darkGray;
            layer1Color = eye1;
        }

        String eyePath = folder + "majin_eye_" + eyeType + "_";

        renderColoredLayer(model, poseStack, animatable, bufferSource, eyePath + "0.png", bgColor, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, eyePath + "1.png", layer1Color, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, eyePath + "2.png", skin, pt, pl, po, alpha);

        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_nose_" + character.getNoseType() + ".png", skin, pt, pl, po, alpha);
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_mouth_" + character.getMouthType() + ".png", skin, pt, pl, po, alpha);
    }

    private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay, float alpha) {

        if (this.currentKaiokenPhase > 0) {
            float intensity = Math.min(0.6f, this.currentKaiokenPhase * 0.1f);

            r = r * (1.0f - intensity) + (1.0f * intensity);
            g = g * (1.0f - intensity);
            b = b * (1.0f - intensity);
        }

        poseStack.pushPose();
        if (scaleInflation > 1.0f) poseStack.scale(scaleInflation, scaleInflation, scaleInflation);

        getRenderer().reRender(model, poseStack, bufferSource, animatable, renderType, bufferSource.getBuffer(renderType), partialTick, packedLight, packedOverlay, r, g, b, alpha);

        poseStack.popPose();
    }

    private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay, float alpha) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, path);
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(loc), rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay, alpha);
    }

    private void unhideParents(GeoBone bone) {
        GeoBone parent = bone.getParent();
        while (parent != null) {
            parent.setHidden(false);
            parent = parent.getParent();
        }
    }

    private float[] hexToRGB(String hexColor) {
        return ColorUtils.hexToRgb(hexColor);
    }
}