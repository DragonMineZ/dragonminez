package com.dragonminez.client.render.layer;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.Character;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.BioAndroidForms;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.*;

public class DMZSkinLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {

    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();

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

        this.currentKaiokenPhase = stats.getStatus().getActiveKaiokenPhase();

        renderBody(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        renderHair(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        renderTattoos(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

    }

    private void renderBody(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        float[] bodyTint = hexToRGB(character.getBodyColor());
        float[] bodyTint2 = hexToRGB(character.getBodyColor2());
        float[] bodyTint3 = hexToRGB(character.getBodyColor3());
        float[] hairTint = hexToRGB(character.getHairColor());

        if (hasForm && character.getActiveFormData() != null) {
            var activeForm = character.getActiveFormData();
            if (!activeForm.getBodyColor1().isEmpty()) bodyTint = hexToRGB(activeForm.getBodyColor1());
            if (!activeForm.getBodyColor2().isEmpty()) bodyTint2 = hexToRGB(activeForm.getBodyColor2());
            if (!activeForm.getBodyColor3().isEmpty()) bodyTint3 = hexToRGB(activeForm.getBodyColor3());
            if (!activeForm.getHairColor().isEmpty()) hairTint = hexToRGB(activeForm.getHairColor());
        }

        if (raceName.equals("bioandroid")) {
            if (Objects.equals(currentForm, BioAndroidForms.PERFECT)
                    || Objects.equals(currentForm, BioAndroidForms.SUPER_PERFECT)) {
                bodyTint2 = new float[]{1.0f, 1.0f, 1.0f};
            }
        }

        boolean isSaiyanTail = raceName.equals("saiyan") && stats.getStatus().isTailVisible();
        if (isSaiyanTail) {
            model.getBone("tail1").ifPresent(bone -> bone.setHidden(false));
            model.getBone("tail2").ifPresent(bone -> bone.setHidden(false));
            model.getBone("tail3").ifPresent(bone -> bone.setHidden(false));
            model.getBone("tail4").ifPresent(bone -> bone.setHidden(false));

            float[] tailColor = ColorUtils.hexToRgb("#572117");

            if (hasForm && character.getActiveFormData() != null) {
                String formHairInfo = character.getActiveFormData().getHairColor();

                if (formHairInfo != null && !formHairInfo.isEmpty()) {
                    tailColor = hexToRGB(formHairInfo);
                }
            }

            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/tail1.png", tailColor, partialTick, packedLight, packedOverlay);
        }

        if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
            String oozaruPath = "textures/entity/races/humansaiyan/oozaru_";

            float[] furColor = Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU) ? hexToRGB("#FFD700") : hexToRGB("#6B1E0E");
            float[] skinColor = hexToRGB("#CC978D");

            if (hasForm && character.getActiveFormData() != null) {
                var form = character.getActiveFormData();

                if (form.getHairColor() != null && !form.getHairColor().isEmpty()) {
                    furColor = ColorUtils.hexToRgb(form.getHairColor());
                }

                if (!form.getBodyColor1().isEmpty()) skinColor = hexToRGB(form.getBodyColor1());
            }

            renderColoredLayer(model, poseStack, animatable, bufferSource, oozaruPath + "layer1.png", furColor, partialTick, packedLight, packedOverlay);
            renderColoredLayer(model, poseStack, animatable, bufferSource, oozaruPath + "layer2.png", skinColor, partialTick, packedLight, packedOverlay);
            return;
        }

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        boolean isStandard = raceName.equals("human") || raceName.equals("saiyan");
        boolean forceVanilla = (raceConfig != null && raceConfig.useVanillaSkin());

        if (forceVanilla || (isStandard && bodyType == 0)) {

            if (isSaiyanTail) {
                model.getBone("tail1").ifPresent(bone -> bone.setHidden(true));
                model.getBone("tail2").ifPresent(bone -> bone.setHidden(true));
                model.getBone("tail3").ifPresent(bone -> bone.setHidden(true));
                model.getBone("tail4").ifPresent(bone -> bone.setHidden(true));
            }

            ResourceLocation playerSkin = player.getSkinTextureLocation();
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(playerSkin), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);

            return;
        }

		boolean defaultRace = ConfigManager.isDefaultRace(raceName);
		if (!defaultRace) {
			String customModel = (raceConfig != null) ? raceConfig.getCustomModel() : "";
			if (hasForm && character.getActiveFormData() != null && character.getActiveFormData().hasCustomModel() && !character.getActiveFormData().getCustomModel().isEmpty()) {
				ResourceLocation formSkinLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/" + character.getActiveFormData().getCustomModel() + ".png");
				renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(formSkinLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
				return;
			} else if (customModel != null && !customModel.isEmpty()) {
				ResourceLocation customSkinLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/" + customModel + ".png");
				renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(customSkinLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
				return;
			}
		}

        boolean isMajin = raceName.equals("majin");
        boolean isFemale = gender.equals("female") || gender.equals("mujer");
        if (isMajin && isFemale && (Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA))) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/tail1.png", bodyTint, partialTick, packedLight, packedOverlay);
        }

        if (raceName.equals("namekian") || raceName.equals("frostdemon") || raceName.equals("bioandroid") || raceName.equals("majin")) {
            renderSpecializedRace(model, poseStack, animatable, bufferSource, raceName, currentForm, bodyType, hasForm, bodyTint, bodyTint2, bodyTint3, hairTint, partialTick, packedLight, packedOverlay);
            return;
        }

        String textureBaseName = isStandard ? "humansaiyan" : raceName;
        String genderPart = (raceConfig != null && raceConfig.hasGender()) ? "_" + gender : "";
        String customPath = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + ".png";
        renderColoredLayer(model, poseStack, animatable, bufferSource, customPath, bodyTint, partialTick, packedLight, packedOverlay);
    }

    private void renderHair(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String currentForm = character.getActiveForm();
        int hairId = character.getHairId();

        if (!raceName.equals("human") && !raceName.equals("saiyan")) return;
        if (raceName.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) return;
        if (hairId == 5) return;
		if (hairId == 0 && character.getHairBase().getVisibleStrandCount() == 0) return;

        float[] tempTint = hexToRGB(character.getHairColor());
        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            var activeForm = character.getActiveFormData();
            if (!activeForm.getHairColor().isEmpty()) {
                tempTint = hexToRGB(activeForm.getHairColor());
            }
        }

        final float[] hairTint = tempTint;

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

            renderColoredLayer(model, poseStack, animatable, bufferSource, hairPath, hairTint, partialTick, packedLight, packedOverlay);

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


    private void renderSpecializedRace(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String race, String form, int bodyType, boolean hasForm, float[] b1, float[] b2, float[] b3, float[] h, float pt, int pl, int po) {
        String filePrefix;
        boolean isFrost = race.equals("frostdemon");
        boolean isBio = race.equals("bioandroid");
        boolean isMajin = race.equals("majin");
        boolean isNamek = race.equals("namekian");

        var stats = StatsProvider.get(StatsCapability.INSTANCE, (AbstractClientPlayer)animatable).orElse(null);
        String gender = (stats != null) ? stats.getCharacter().getGender().toLowerCase() : "male";

        if (isFrost && (Objects.equals(form, FrostDemonForms.FINAL_FORM) || Objects.equals(form, FrostDemonForms.FULLPOWER))) {
            filePrefix = "textures/entity/races/" + race + "/finalform_bodytype_" + bodyType + "_";
            renderFrostDemonFinalForm(model, poseStack, animatable, bufferSource, filePrefix, bodyType, b1, b2, b3, h, pt, pl, po);
        } else {
            if (isBio) {
                String textureFormName = "base";
                if (form != null && !form.isEmpty()) {
                    String f = form.toLowerCase();

                    if (f.equals(BioAndroidForms.SEMI_PERFECT)) {
                        textureFormName = "semiperfect";
                    }
                    else if (f.equals(BioAndroidForms.BASE)) {
                        textureFormName = "base";
                    }
                    else {
                        textureFormName = "perfect";
                    }
                }
                filePrefix = "textures/entity/races/bioandroid/" + textureFormName + "_" + bodyType + "_";
            }
            else if (isMajin) {
                String f = (form == null || form.isEmpty()) ? "base" : form.toLowerCase();

                boolean isStandardForm = f.equals("base") || f.equals("pure") || f.equals("kid") ||
                        f.equals("ultra") || f.equals("evil");

                if (!isStandardForm) {
                    f = "super";
                    bodyType = 0;
                }

                filePrefix = "textures/entity/races/majin/" + f + "_" + bodyType + "_" + gender + "_";
            }

            else if (isNamek || !hasForm || (isFrost && Objects.equals(form, FrostDemonForms.SECOND_FORM))) {
                filePrefix = "textures/entity/races/" + race + "/bodytype_" + bodyType + "_";
            }
            else {
                String transformTexture = race;
                if (isFrost) {
                    if (Objects.equals(form, FrostDemonForms.THIRD_FORM)) {
                        transformTexture = "thirdform_bodytype_" + bodyType;
                    }
                    else if (Objects.equals(form, FrostDemonForms.FIFTH_FORM)) {
                        transformTexture = "fifth_bodytype_" + bodyType;
                    }
                    else if (Objects.equals(form, "golden")) {
                        transformTexture = "frostdemon_golden";
                    }
                }
                filePrefix = "textures/entity/races/" + race + "/" + transformTexture + "_";
            }

            float[] colorForLayer2 = b2;
            float[] colorForLayer3 = b3;


            if (isFrost && Objects.equals(form, FrostDemonForms.FIFTH_FORM)) {

                if (bodyType == 0) {
                    colorForLayer2 = h;
                }
                else if (bodyType == 2) {
                    colorForLayer3 = h;
                }
            }

            renderStandardLayers(model, poseStack, animatable, bufferSource, filePrefix, isFrost, isBio, bodyType, b1, colorForLayer2, colorForLayer3, h, pt, pl, po);
        }
    }


    private void renderTattoos(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {

        if (stats.getEffects() != null && stats.getEffects().hasEffect("majin")) {
            ResourceLocation majinMarkLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/majinm.png");
            if (textureExists(majinMarkLoc)) {
                renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(majinMarkLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
            }
        }

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType == 0) return;

        ResourceLocation tattooLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/tattoos/tattoo_" + tattooType + ".png");
        if (textureExists(tattooLoc)) {
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(tattooLoc), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
        }
    }


    private void renderFace(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String currentForm = character.getActiveForm();
        int bodyType = character.getBodyType();

        if (raceName.equals("saiyan") && Objects.equals(currentForm, SaiyanForms.OOZARU)
                || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU)) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/humansaiyan/oozaru_layer3.png", new float[]{1.0f, 1.0f, 1.0f}, partialTick, packedLight, packedOverlay);
            return;
        }

        if ((raceName.equals("human") || raceName.equals("saiyan")) && bodyType == 0) {
            return;
        }



        model.getBone("head").ifPresent(headBone -> {
            float originalZ = headBone.getPosZ();
            float originalSX = headBone.getScaleX();
            float originalSY = headBone.getScaleY();
            float originalSZ = headBone.getScaleZ();

            headBone.setPosZ(originalZ - 0.002f);
            float inflation = 0.002f;
            headBone.setScaleX(originalSX + inflation);
            headBone.setScaleY(originalSY + inflation);
            headBone.setScaleZ(originalSZ + inflation);

            renderFaceLayers(model, poseStack, animatable, bufferSource, character, raceName, currentForm, bodyType, partialTick, packedLight, packedOverlay);

            headBone.setPosZ(originalZ);
            headBone.setScaleX(originalSX);
            headBone.setScaleY(originalSY);
            headBone.setScaleZ(originalSZ);
        });
    }

    private void renderFaceLayers(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, Character character, String raceName, String currentForm, int bodyType, float pt, int pl, int po) {
        float[] eye1 = hexToRGB(character.getEye1Color());
        float[] eye2 = hexToRGB(character.getEye2Color());
        float[] hair = hexToRGB(character.getHairColor());
        float[] b1 = hexToRGB(character.getBodyColor());
        float[] b2 = hexToRGB(character.getBodyColor2());

        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            var f = character.getActiveFormData();
            if (!f.getEye1Color().isEmpty()) eye1 = hexToRGB(f.getEye1Color());
            if (!f.getEye2Color().isEmpty()) eye2 = hexToRGB(f.getEye2Color());
            if (!f.getHairColor().isEmpty()) hair = hexToRGB(f.getHairColor());
            if (!f.getBodyColor1().isEmpty()) b1 = hexToRGB(f.getBodyColor1());
            if (!f.getBodyColor2().isEmpty()) b2 = hexToRGB(f.getBodyColor2());
        }

        float[] skinTint = (raceName.equals("namekian") || raceName.equals("majin") ||
                raceName.equals("human") || raceName.equals("saiyan")) ? b1 : b2;

        if (raceName.equals("frostdemon")) {
            boolean isFinal = Objects.equals(currentForm, FrostDemonForms.FINAL_FORM) || Objects.equals(currentForm, FrostDemonForms.FULLPOWER);
            if (isFinal && (bodyType == 0 || bodyType == 2)) skinTint = b1;
        }

        String folder = "textures/entity/races/" + ((raceName.equals("human") || raceName.equals("saiyan")) ? "humansaiyan" : raceName) + "/faces/";
        float[] white = {1.0f, 1.0f, 1.0f}, black = {0.0f, 0.0f, 0.0f};

        if (raceName.equals("bioandroid")) {
            String fP = "base";

            if (currentForm != null && !currentForm.isEmpty()) {
                String f = currentForm.toLowerCase();

                if (f.equals(BioAndroidForms.SEMI_PERFECT)) {
                    fP = "semiperfect";
                }
                else if (f.equals(BioAndroidForms.BASE) || f.equals("imperfect")) {
                    fP = "base";
                }
                else {
                    fP = "perfect";
                }
            }

            float[] colorLayer0 = fP.equals("base") ? eye1 : white;

            float[] colorLayer1 = eye2;

            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "bioandroid_" + fP + "_eye_layer0.png", colorLayer0, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "bioandroid_" + fP + "_eye_layer1.png", colorLayer1, pt, pl, po);
        }

        else if (raceName.equals("frostdemon")) {
            String eyeBase = "frostdemon_eye";

            float[] eyeScleraColor = white;

            if (Objects.equals(currentForm, FrostDemonForms.FIFTH_FORM)) {
                eyeScleraColor = hexToRGB("#D91E1E");
            }

            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", eyeScleraColor, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2, pt, pl, po);

        }
        else if (raceName.equals("majin")) {
            String mEye = "majin_eye_" + character.getEyesType();
            if (character.getEyesType() == 0) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_0.png", skinTint, pt, pl, po);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_1.png", skinTint, pt, pl, po);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_2.png", skinTint, pt, pl, po);
            } else {
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_0.png", black, pt, pl, po);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_1.png", eye1, pt, pl, po);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + mEye + "_2.png", skinTint, pt, pl, po);
            }
        }
        else {
            String eyeBase = (raceName.equals("human") || raceName.equals("saiyan") ? "humansaiyan" : raceName) + "_eye_" + character.getEyesType();
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", white, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2, pt, pl, po);

            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_3.png", hair, pt, pl, po);
        }

        String prefix = (raceName.equals("human") || raceName.equals("saiyan")) ? "humansaiyan" : raceName;
        renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "_nose_" + character.getNoseType() + ".png", skinTint, pt, pl, po);

        if (raceName.equals("frostdemon") && Objects.equals(currentForm, FrostDemonForms.FIFTH_FORM)) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/frostdemon/faces/frostdemon_fifth_mouth.png", b1, pt, pl, po);
        } else {
            renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "_mouth_" + character.getMouthType() + ".png", skinTint, pt, pl, po);
        }

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

    private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, T animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay) {
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, renderType, r, g, b, scaleInflation, partialTick, packedLight, packedOverlay, 1.0F);
    }

    private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay) {
        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, path);
        if (textureExists(loc)) {
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityCutoutNoCull(loc), rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay);
        }
    }

    private void renderStandardLayers(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String prefix, boolean isFrost, boolean isBio, int bodyType, float[] b1, float[] b2, float[] b3, float[] h, float pt, int pl, int po) {
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, pt, pl, po);
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, pt, pl, po);
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, pt, pl, po);

        if (isFrost || isBio) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", h, pt, pl, po);
        }

        if (isBio || (isFrost && bodyType == 0)) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer5.png", hexToRGB("#e67d40"), pt, pl, po);
        }
    }

    private void renderFrostDemonFinalForm(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, String prefix, int bodyType, float[] b1, float[] b2, float[] b3, float[] h, float pt, int pl, int po) {
        if (bodyType == 0) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", h, pt, pl, po);
        } else if (bodyType == 1) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", h, pt, pl, po);
        } else {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, pt, pl, po);
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", h, pt, pl, po);
        }
    }

    private void unhideParents(GeoBone bone) {
        GeoBone parent = bone.getParent();
        while (parent != null) {
            parent.setHidden(false);
            parent = parent.getParent();
        }
    }

    private boolean textureExists(ResourceLocation location) {
        return TEXTURE_CACHE.computeIfAbsent(location, loc -> Minecraft.getInstance().getResourceManager().getResource(loc).isPresent());
    }

    private float[] hexToRGB(String hexColor) {
        return ColorUtils.hexToRgb(hexColor);
    }
}