package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
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
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.*;

public class DMZSkinLayer<T extends DMZAnimatable> extends GeoRenderLayer<T> {

    private static final Map<ResourceLocation, Boolean> TEXTURE_CACHE = new HashMap<>();

    public DMZSkinLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void render(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        var player = this.player();
        if (player == null) return;

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, player);
        var stats = statsCap.orElse(new StatsData(player));

        //Renderizar CUERPO (Skin base)
        renderBody(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        renderTattoos(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        // Renderizar CARA (Ojos, boca, etc)
        renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

    }

    private void renderBody(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        if (raceName.equals("saiyan") && Objects.equals(currentForm, SaiyanForms.OOZARU)) {
            String oozaruPath = "textures/entity/races/humansaiyan/oozaru_";

            float[] furColor = ColorUtils.rgbIntToFloat(0x6B1E0E);
            float[] skinColor = ColorUtils.rgbIntToFloat(0xCC978D);

            if (hasForm && character.getActiveFormData() != null) {
                var form = character.getActiveFormData();
                if (!form.getHairColor().isEmpty()) furColor = hexToRGB(form.getHairColor());
                if (!form.getBodyColor1().isEmpty()) skinColor = hexToRGB(form.getBodyColor1());
            }

            renderColoredLayer(model, poseStack, animatable, bufferSource, oozaruPath + "layer1.png", furColor, partialTick, packedLight, packedOverlay);
            renderColoredLayer(model, poseStack, animatable, bufferSource, oozaruPath + "layer2.png", skinColor, partialTick, packedLight, packedOverlay);
            return;
        }

        float[] bodyTint = hexToRGB(character.getBodyColor());
        float[] bodyTint2 = hexToRGB(character.getBodyColor2());
        float[] bodyTint3 = hexToRGB(character.getBodyColor3());
        float[] hairTint = hexToRGB(character.getHairColor());

        boolean isStandard = raceName.equals("human") || raceName.equals("saiyan");

        if (!isStandard || bodyType > 0) {
            if (hasForm) {
                var activeForm = character.getActiveFormData();
                if (activeForm != null) {
                    if (!activeForm.getBodyColor1().isEmpty()) bodyTint = hexToRGB(activeForm.getBodyColor1());
                    if (!activeForm.getBodyColor2().isEmpty()) bodyTint2 = hexToRGB(activeForm.getBodyColor2());
                    if (!activeForm.getBodyColor3().isEmpty()) bodyTint3 = hexToRGB(activeForm.getBodyColor3());
                    if (!activeForm.getHairColor().isEmpty()) hairTint = hexToRGB(activeForm.getHairColor());
                }
            }
        }

        if (raceName.equals("saiyan") && stats.getStatus().isTailVisible()) {
            float[] tailColor = ColorUtils.hexToRgb("#572117"); // Marronazo obligatorio en base
            if (hasForm && character.getActiveFormData() != null) {
                String formHair = character.getActiveFormData().getHairColor();
                if (formHair != null && !formHair.isEmpty()) {
                    tailColor = hairTint;
                }
            }
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/tail1.png", tailColor, partialTick, packedLight, packedOverlay);
        }

        boolean isMajin = raceName.equals("majin");
        boolean isFemale = gender.equals("female") || gender.equals("mujer");
        boolean isSuperOrUltraMajin = Objects.equals(currentForm, MajinForms.SUPER) ||
                Objects.equals(currentForm, MajinForms.ULTRA);

        if (isMajin && isFemale && isSuperOrUltraMajin) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, "textures/entity/races/tail1.png", bodyTint, partialTick, packedLight, packedOverlay);
        }

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);
        boolean forceVanilla = raceConfig.useVanillaSkin();
        boolean isDefaultBody = (bodyType == 0);

        if (forceVanilla || (isStandard && isDefaultBody && !hasForm)) {
            ResourceLocation playerSkin = player.getSkinTextureLocation();
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, RenderType.entityTranslucent(playerSkin), 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
            return;
        }

        //Namek, Frost, Bio
        boolean isNamek = raceName.equals("namekian");
        boolean isFrost = raceName.equals("frostdemon");
        boolean isBio = raceName.equals("bioandroid");

        if (isNamek || isFrost || isBio) {
            String filePrefix;
            boolean isFinalForm = isFrost && (Objects.equals(currentForm, FrostDemonForms.FINAL_FORM) || Objects.equals(currentForm, FrostDemonForms.FULLPOWER));
            boolean useBodyTypeTexture = !hasForm || (isFrost && Objects.equals(currentForm, FrostDemonForms.SECOND_FORM));

            if (isFinalForm) {
                filePrefix = "textures/entity/races/" + raceName + "/finalform_bodytype_" + bodyType + "_";
                renderFrostDemonFinalForm(model, poseStack, animatable, bufferSource, filePrefix, bodyType, bodyTint, bodyTint2, bodyTint3, hairTint, partialTick, packedLight, packedOverlay);
            } else {
                if (!useBodyTypeTexture) {
                    String transformTexture = raceName;
                    switch (raceName) {
                        case "bioandroid" -> {
                            if (Objects.equals(currentForm, BioAndroidForms.SEMI_PERFECT)) transformTexture = "bioandroid_semi";
                            else if (Objects.equals(currentForm, BioAndroidForms.PERFECT)) transformTexture = "bioandroid_perfect";
                        }
                        case "frostdemon" -> {
                            if (Objects.equals(currentForm, FrostDemonForms.THIRD_FORM)) transformTexture = "thirdform_bodytype_" + bodyType;
                            else if (Objects.equals(currentForm, "golden")) transformTexture = "frostdemon_golden";
                        }
                    }
                    filePrefix = "textures/entity/races/" + raceName + "/" + transformTexture + "_";
                } else {
                    filePrefix = "textures/entity/races/" + raceName + "/bodytype_" + bodyType + "_";
                }
                renderStandardLayers(model, poseStack, animatable, bufferSource, filePrefix, isFrost, isBio, bodyType, bodyTint, bodyTint2, bodyTint3, hairTint, partialTick, packedLight, packedOverlay);
            }
            return;
        }

        //Humanos/Saiyans Custom
        String textureBaseName = isStandard ? "humansaiyan" : raceName;
        String genderPart = raceConfig.hasGender() ? "_" + gender : "";
        String customPath = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + ".png";
        renderColoredLayer(model, poseStack, animatable, bufferSource, customPath, bodyTint, partialTick, packedLight, packedOverlay);
    }

    private void renderTattoos(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType == 0) return;

        String fileName = "tattoo_" + tattooType + ".png";
        ResourceLocation tattooLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/tattoos/" + fileName);

        if (!textureExists(tattooLoc)) return;

        RenderType type = RenderType.entityTranslucent(tattooLoc);

        Map<String, float[]> originalScales = new HashMap<>();

        float inflation = 0;

        for (GeoBone bone : model.topLevelBones()) {
            originalScales.put(bone.getName(), new float[]{bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()});

            bone.setScaleX(bone.getScaleX() + inflation);
            bone.setScaleY(bone.getScaleY() + inflation);
            bone.setScaleZ(bone.getScaleZ() + inflation);
        }

        try {
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, type,
                    1.0f, 1.0f, 1.0f, // Color blanco
                    1.0f,             // Escala global NORMAL (1.0)
                    partialTick, packedLight, packedOverlay);
        } finally {
            for (GeoBone bone : model.topLevelBones()) {
                if (originalScales.containsKey(bone.getName())) {
                    float[] scales = originalScales.get(bone.getName());
                    bone.setScaleX(scales[0]);
                    bone.setScaleY(scales[1]);
                    bone.setScaleZ(scales[2]);
                }
            }
        }
    }

    private void renderFace(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String currentForm = character.getActiveForm();
        int bodyType = character.getBodyType();

        if (raceName.equals("saiyan") && Objects.equals(currentForm, SaiyanForms.OOZARU)) {
            String oozaruFacePath = "textures/entity/races/humansaiyan/oozaru_layer3.png";
            float[] whiteTint = {1.0f, 1.0f, 1.0f};
            renderColoredLayer(model, poseStack, animatable, bufferSource, oozaruFacePath, whiteTint, partialTick, packedLight, packedOverlay);
            return;
        }

        boolean isVanillaPlayer = raceName.equals("human") || raceName.equals("saiyan");
        if (isVanillaPlayer && bodyType == 0) {
            return;
        }

        Optional<GeoBone> headBoneOpt = model.getBone("head");
        if (headBoneOpt.isEmpty()) return;
        GeoBone headBone = headBoneOpt.get();

        float originalZ = headBone.getPosZ();
        float originalSX = headBone.getScaleX();
        float originalSY = headBone.getScaleY();
        float originalSZ = headBone.getScaleZ();

        headBone.setPosZ(originalZ - 0.002f);
        float inflation = 0.002f;
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

        float[] eye1Tint = hexToRGB(character.getEye1Color());
        float[] eye2Tint = hexToRGB(character.getEye2Color());
        float[] hairTint = hexToRGB(character.getHairColor());
        float[] bodyTint = hexToRGB(character.getBodyColor());
        float[] bodyTint2 = hexToRGB(character.getBodyColor2());
        float[] bodyTint3 = hexToRGB(character.getBodyColor3());

        boolean isStandard = raceName.equals("human") || raceName.equals("saiyan");

        if (!isStandard || bodyType > 0) {
            if (character.hasActiveForm()) {
                var activeForm = character.getActiveFormData();
                if (activeForm != null) {
                    if (!activeForm.getEye1Color().isEmpty()) eye1Tint = hexToRGB(activeForm.getEye1Color());
                    if (!activeForm.getEye2Color().isEmpty()) eye2Tint = hexToRGB(activeForm.getEye2Color());
                    if (!activeForm.getHairColor().isEmpty()) hairTint = hexToRGB(activeForm.getHairColor());

                    if (!activeForm.getBodyColor1().isEmpty()) bodyTint = hexToRGB(activeForm.getBodyColor1());
                    if (!activeForm.getBodyColor2().isEmpty()) bodyTint2 = hexToRGB(activeForm.getBodyColor2());
                    if (!activeForm.getBodyColor3().isEmpty()) bodyTint3 = hexToRGB(activeForm.getBodyColor3());
                }
            }
        }

        float [] skinTint = bodyTint2;

        if (raceName.equals("namekian") || raceName.equals("majin")) {
            skinTint = bodyTint;
        } else {
            // Humanos, Saiyans y Bio-Androids
            skinTint = bodyTint2;
        }
        if (raceName.equals("frostdemon")) {
            skinTint = bodyTint2;

            boolean isFinalOrFP = Objects.equals(currentForm, FrostDemonForms.FINAL_FORM) || Objects.equals(currentForm, FrostDemonForms.FULLPOWER);
            if (isFinalOrFP && (bodyType == 0 || bodyType == 2)) {
                skinTint = bodyTint;
            }
        }

        float[] whiteTint = {1.0f, 1.0f, 1.0f};
        float[] blackTint = {0.0f, 0.0f, 0.0f};

        int eyesType = character.getEyesType();
        int noseType = character.getNoseType();
        int mouthType = character.getMouthType();

        String folder = "textures/entity/races/" + raceName + "/faces/";

        if (isVanillaPlayer) folder = "textures/entity/races/humansaiyan/faces/";

        try {
            if (raceName.equals("bioandroid")) {
                String formPrefix = "imperfect";
                String form = character.getActiveForm();
                if (form != null) {
                    if (form.equals("semi_perfect")) formPrefix = "semi";
                    else if (form.equals("perfect")) formPrefix = "perfect";
                }

                String eyeBase = "bioandroid_" + formPrefix + "_eye";

                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", eye1Tint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye2Tint, partialTick, packedLight, packedOverlay);
            }
            else if (raceName.equals("frostdemon")) {
                String eyeBase = "frostdemon_eye";

                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", whiteTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1Tint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2Tint, partialTick, packedLight, packedOverlay);

                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_nose_" + noseType + ".png", skinTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_mouth_" + mouthType + ".png", skinTint, partialTick, packedLight, packedOverlay);
            }
            else if (raceName.equals("namekian")) {
                String eyeBase = "namekian_eye_" + eyesType;

                // Layer 0: Blanco
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", whiteTint, partialTick, packedLight, packedOverlay);
                // Layer 1: Eye 1
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1Tint, partialTick, packedLight, packedOverlay);
                // Layer 2: Eye 2
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2Tint, partialTick, packedLight, packedOverlay);
                // Layer 3: Piel
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_3.png", skinTint, partialTick, packedLight, packedOverlay);

                // Boca y Nariz
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_nose_" + noseType + ".png", skinTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "namekian_mouth_" + mouthType + ".png", skinTint, partialTick, packedLight, packedOverlay);
            }
            // === CASO MAJIN ===
            else if (raceName.equals("majin")) {
                String eyeBase = "majin_eye_" + eyesType;

                if(eyesType == 0){
                    // Layer 0: El fondo (Negro por defecto, Piel si eyeType es 0)
                    renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", skinTint, partialTick, packedLight, packedOverlay);

                    // Layer 1: El iris/pupila (Eye 1)
                    renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", skinTint, partialTick, packedLight, packedOverlay);

                    // Layer 2: Detalles de la piel/contorno
                    renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", skinTint, partialTick, packedLight, packedOverlay);

                } else {
                // Layer 0: El fondo (Negro por defecto, Piel si eyeType es 0)
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", blackTint, partialTick, packedLight, packedOverlay);

                // Layer 1: El iris/pupila (Eye 1)
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1Tint, partialTick, packedLight, packedOverlay);

                // Layer 2: Detalles de la piel/contorno
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", skinTint, partialTick, packedLight, packedOverlay);

                // Boca y Nariz
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_nose_" + noseType + ".png", skinTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "majin_mouth_" + mouthType + ".png", skinTint, partialTick, packedLight, packedOverlay);
            }}
            else {
                String prefix = isVanillaPlayer ? "humansaiyan" : raceName;
                String eyeBase = prefix + "_eye_" + eyesType;

                // Layer 0: Blanco
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", whiteTint, partialTick, packedLight, packedOverlay);
                // Layer 1: Eye 1
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1Tint, partialTick, packedLight, packedOverlay);
                // Layer 2: Eye 2
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2Tint, partialTick, packedLight, packedOverlay);
                // Layer 3: Pelo/Cejas
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_3.png", hairTint, partialTick, packedLight, packedOverlay);

                // Boca y Nariz
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "_nose_" + noseType + ".png", skinTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + prefix + "_mouth_" + mouthType + ".png", skinTint, partialTick, packedLight, packedOverlay);
            }

        } catch (Exception ignored) {}

        for (GeoBone bone : model.topLevelBones()) {
            if (hiddenBones.contains(bone.getName())) {
                bone.setHidden(false);
            }
        }

        headBone.setPosZ(originalZ);
        headBone.setScaleX(originalSX);
        headBone.setScaleY(originalSY);
        headBone.setScaleZ(originalSZ);
    }

    private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, DMZAnimatable animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        if (scaleInflation > 1.0f) {
            poseStack.scale(scaleInflation, scaleInflation, scaleInflation);
        }

        poseStack.translate(0, 0, 0);

        getRenderer().reRender(model, poseStack, bufferSource, (T)animatable, renderType,
                bufferSource.getBuffer(renderType), partialTick, packedLight, packedOverlay,
                r, g, b, 1.0f);

        poseStack.popPose();
    }

    private void renderColoredLayer(BakedGeoModel model, PoseStack poseStack, DMZAnimatable animatable, MultiBufferSource bufferSource, String path, float[] rgb, float partialTick, int packedLight, int packedOverlay) {
        ResourceLocation loc = path.contains(":") ? new ResourceLocation(path) : new ResourceLocation(Reference.MOD_ID, path);

        if (!textureExists(loc)) return;

        RenderType type = RenderType.entityCutoutNoCull(loc);
        // Render normal con inflación 1.0f
        renderLayerWholeModel(model, poseStack, bufferSource, animatable, type, rgb[0], rgb[1], rgb[2], 1.0f, partialTick, packedLight, packedOverlay);
    }

    private void renderStandardLayers(BakedGeoModel model, PoseStack poseStack, DMZAnimatable animatable, MultiBufferSource bufferSource, String prefix, boolean isFrost, boolean isBio, int bodyType, float[] b1, float[] b2, float[] b3, float[] h, float pt, int pl, int po) {
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer1.png", b1, pt, pl, po);
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer2.png", b2, pt, pl, po);
        renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer3.png", b3, pt, pl, po);

        if (isFrost || isBio) {
            renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer4.png", h, pt, pl, po);
            if (isBio || (isFrost && bodyType == 0)) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, prefix + "layer5.png", hexToRGB("#e67d40"), pt, pl, po);
            }
        }
    }

    private void renderFrostDemonFinalForm(BakedGeoModel model, PoseStack poseStack, DMZAnimatable animatable, MultiBufferSource bufferSource, String prefix, int bodyType, float[] b1, float[] b2, float[] b3, float[] h, float pt, int pl, int po) {
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
        if (TEXTURE_CACHE.containsKey(location)) return TEXTURE_CACHE.get(location);
        boolean exists = Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        TEXTURE_CACHE.put(location, exists);
        return exists;
    }

    private float[] hexToRGB(String hexColor) {
        return ColorUtils.hexToRgb(hexColor);
    }

    public AbstractClientPlayer player() {
        if (this.renderer instanceof GeoReplacedEntityRenderer<?, ?> geoRenderer) {
            return (AbstractClientPlayer) geoRenderer.getCurrentEntity();
        }
        return null;
    }
}