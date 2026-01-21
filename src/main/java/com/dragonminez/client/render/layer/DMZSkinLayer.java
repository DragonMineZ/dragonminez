package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
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

        // PASO 1: Renderizar CUERPO (Skin base)
        renderBody(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        renderTattoos(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

        // PASO 2: Renderizar CARA (Ojos, boca, etc)
        renderFace(poseStack, animatable, model, bufferSource, player, stats, partialTick, packedLight, packedOverlay);

    }


    private void renderBody(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {
        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);

        float[] bodyTint = hexToRGB(character.getBodyColor());
        float[] bodyTint2 = hexToRGB(character.getBodyColor2());
        float[] bodyTint3 = hexToRGB(character.getBodyColor3());
        float[] hairTint = hexToRGB(character.getHairColor());

        boolean forceVanilla = raceConfig.useVanillaSkin();
        boolean isStandardHumanoid = (raceName.equals("human") || raceName.equals("saiyan"));
        boolean isDefaultBody = (bodyType == 0);


        if (raceName.equals("saiyan") && stats.getStatus().isTailVisible() && !player.isSpectator() && !player.isInvisible()) {
            var tailColor = ColorUtils.rgbIntToFloat(0x6B1E0E);

            String racePartsPath = "textures/entity/races/tail1.png";

            renderColoredLayer(model, poseStack, animatable, bufferSource, racePartsPath, tailColor, partialTick, packedLight, packedOverlay);
        }

        if (forceVanilla || (isStandardHumanoid && isDefaultBody && !hasForm)) {
            ResourceLocation playerSkin = player.getSkinTextureLocation();
            RenderType type = RenderType.entityTranslucent(playerSkin);
            renderLayerWholeModel(model, poseStack, bufferSource, animatable, type, 1.0f, 1.0f, 1.0f, 1.0f, partialTick, packedLight, packedOverlay);
            return;
        }

        boolean isNamek = raceName.equals("namekian");
        boolean isFrost = raceName.equals("frostdemon");
        boolean isBio = raceName.equals("bioandroid");

        if (isNamek || isFrost || isBio) {
            String filePrefix;
            if (hasForm) {
                String transformTexture = raceName;
                switch (raceName) {
                    case "bioandroid" -> {
                        if (currentForm.equals("semi_perfect")) transformTexture = "bioandroid_semi";
                        else if (currentForm.equals("perfect")) transformTexture = "bioandroid_perfect";
                    }
                    case "frostdemon" -> {
                        if (currentForm.equals("form2")) transformTexture = "frostdemon_form2";
                        else if (currentForm.equals("form3")) transformTexture = "frostdemon_form3";
                        else if (currentForm.equals("golden")) transformTexture = "frostdemon_golden";
                    }
                    default -> filePrefix = "";
                }
                filePrefix = "textures/entity/races/" + raceName + "/" + transformTexture + "_";
            } else {
                filePrefix = "textures/entity/races/" + raceName + "/bodytype_" + bodyType + "_";
            }

            renderColoredLayer(model, poseStack, animatable, bufferSource, filePrefix + "layer1.png", bodyTint, partialTick, packedLight, packedOverlay);
            renderColoredLayer(model, poseStack, animatable, bufferSource, filePrefix + "layer2.png", bodyTint2, partialTick, packedLight, packedOverlay);
            renderColoredLayer(model, poseStack, animatable, bufferSource, filePrefix + "layer3.png", bodyTint3, partialTick, packedLight, packedOverlay);

            if (isFrost || isBio) {
                renderColoredLayer(model, poseStack, animatable, bufferSource, filePrefix + "layer4.png", hairTint, partialTick, packedLight, packedOverlay);
                if (isBio || (isFrost && bodyType == 0)) {
                    renderColoredLayer(model, poseStack, animatable, bufferSource, filePrefix + "layer5.png", hexToRGB("#e67d40"), partialTick, packedLight, packedOverlay);
                }
            }
            return;
        }

        String textureBaseName = isStandardHumanoid ? "humansaiyan" : raceName;
        String genderPart = raceConfig.hasGender() ? "_" + gender : "";
        String formPart = "";
        String customPath = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + formPart + ".png";

        renderColoredLayer(model, poseStack, animatable, bufferSource, customPath, bodyTint, partialTick, packedLight, packedOverlay);


    }

    private void renderTattoos(PoseStack poseStack, DMZAnimatable animatable, BakedGeoModel model, MultiBufferSource bufferSource, AbstractClientPlayer player, StatsData stats, float partialTick, int packedLight, int packedOverlay) {

        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType == 0) return;

        String fileName = "tattoo_" + tattooType + ".png";
        ResourceLocation tattooLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/tattoos/" + fileName);

        if (!textureExists(tattooLoc)) return;

        RenderType type = RenderType.entityTranslucent(tattooLoc);

        // MAPA PARA GUARDAR ESCALAS ORIGINALES
        // Usamos esto para restaurar el modelo después de pintar el tatuaje
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
        int bodyType = character.getBodyType();

        boolean isVanillaPlayer = raceName.equals("human") || raceName.equals("saiyan");
        if (isVanillaPlayer && bodyType == 0) {
            return;
        }

        Optional<GeoBone> headBoneOpt = model.getBone("head");
        if (headBoneOpt.isEmpty()) return;
        GeoBone headBone = headBoneOpt.get();

        // 1. GUARDAR ESTADO
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

        // 4. PREPARAR COLORES
        float[] eye1Tint = hexToRGB(character.getEye1Color());
        float[] eye2Tint = hexToRGB(character.getEye2Color());
        float[] hairTint = hexToRGB(character.getHairColor());
        float[] skinTint = hexToRGB(character.getBodyColor());

        // Para Bioandroid/Frost a veces la piel es el color secundario, ajusta si es necesario
        if (raceName.equals("bioandroid") || raceName.equals("frostdemon")) {
            skinTint = hexToRGB(character.getBodyColor2());
        }

        float[] whiteTint = {1.0f, 1.0f, 1.0f};
        float[] blackTint = {0.0f, 0.0f, 0.0f};

        int eyesType = character.getEyesType();
        int noseType = character.getNoseType();
        int mouthType = character.getMouthType();

        String folder = "textures/entity/races/" + raceName + "/faces/";

        // Si es humano/saiyan, la carpeta es humansaiyan
        if (isVanillaPlayer) folder = "textures/entity/races/humansaiyan/faces/";

        try {
            if (raceName.equals("bioandroid")) {
                String formPrefix = "imperfect";
                String form = character.getActiveForm();
                if (form != null) {
                    if (form.equals("semi_perfect")) formPrefix = "semi";
                    else if (form.equals("perfect")) formPrefix = "perfect";
                }

                //bioandroid_[form]_eye_[type]_[layer]
                String eyeBase = "bioandroid_" + formPrefix + "_eye";

                // Layer 0: Coloreable (Eye 1) - O White si prefieres
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", eye1Tint, partialTick, packedLight, packedOverlay);
                // Layer 1: Coloreable (Eye 2)
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye2Tint, partialTick, packedLight, packedOverlay);
            }
            // === CASO FROSTDEMON ===
            else if (raceName.equals("frostdemon")) {
                String eyeBase = "frostdemon_eye";

                // Layer 0: Blanco
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_0.png", whiteTint, partialTick, packedLight, packedOverlay);
                // Layer 1: Eye 1
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_1.png", eye1Tint, partialTick, packedLight, packedOverlay);
                // Layer 2: Eye 2
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + eyeBase + "_2.png", eye2Tint, partialTick, packedLight, packedOverlay);

                // Boca y Nariz
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_nose_" + noseType + ".png", skinTint, partialTick, packedLight, packedOverlay);
                renderColoredLayer(model, poseStack, animatable, bufferSource, folder + "frostdemon_mouth_" + mouthType + ".png", skinTint, partialTick, packedLight, packedOverlay);
            }
            // === CASO NAMEKIAN ===
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

        // 6. RESTAURAR VISIBILIDAD DE HUESOS
        for (GeoBone bone : model.topLevelBones()) {
            if (hiddenBones.contains(bone.getName())) {
                bone.setHidden(false);
            }
        }

        // 7. RESTAURAR TRANSFORMACIÓN
        headBone.setPosZ(originalZ);
        headBone.setScaleX(originalSX);
        headBone.setScaleY(originalSY);
        headBone.setScaleZ(originalSZ);
    }

    private void renderLayerWholeModel(BakedGeoModel model, PoseStack poseStack, MultiBufferSource bufferSource, DMZAnimatable animatable, RenderType renderType, float r, float g, float b, float scaleInflation, float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        float baseScale = 1.0f / 0.9375f;

        float finalScale = baseScale * scaleInflation;

        poseStack.scale(finalScale, finalScale, finalScale);

        poseStack.translate(0, -0.001f, 0);

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