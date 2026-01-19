package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import java.util.*;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID,
            "geo/armor/armormajinslim.geo.json");

    private boolean isRenderingArmor = false;
    private boolean isRenderingTattoo = false;

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }


    private void renderBodyAll(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, @Nullable GeoBone targetBone, float partialTick, boolean isReRender) {

        if (targetBone == null) hideLayerBonesIfArmored(model, animatable);

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));

        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        String gender = character.getGender().toLowerCase();
        int bodyType = character.getBodyType();
        String currentForm = character.getActiveForm();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);

        boolean hideArmorBones = (raceName.equals("majin") && gender.equals("male")) || gender.equals("female");

        model.getBone("armorBody").ifPresent(bone -> bone.setHidden(hideArmorBones));
        model.getBone("armorBody2").ifPresent(bone -> bone.setHidden(hideArmorBones));
        model.getBone("armorLeggingsBody").ifPresent(bone -> bone.setHidden(hideArmorBones));
        model.getBone("armorRightArm").ifPresent(bone -> bone.setHidden(true));
        model.getBone("armorLeftArm").ifPresent(bone -> bone.setHidden(true));
        model.getBone("boobas").ifPresent(bone -> bone.setHidden(gender.equals("female")));


        float[] bodyTint = hexToRGB(character.getBodyColor());
        float[] bodyTint2 = hexToRGB(character.getBodyColor2());
        float[] bodyTint3 = hexToRGB(character.getBodyColor3());
        float[] hairTint = hexToRGB(character.getHairColor());

        boolean forceVanilla = raceConfig.useVanillaSkin();
        boolean isStandardHumanoid = (raceName.equals("human") || raceName.equals("saiyan"));
        boolean isDefaultBody = (bodyType == 0);

        if (forceVanilla || (isStandardHumanoid && isDefaultBody && !hasForm)) {
            ResourceLocation playerSkin = animatable.getSkinTextureLocation();
            RenderType type = RenderType.entityTranslucent(playerSkin);
            VertexConsumer buff = bufferSource.getBuffer(type);
            renderTarget(poseStack, animatable, model, type, bufferSource, buff, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, alpha, targetBone, partialTick, isReRender);
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
                }
                filePrefix = "textures/entity/races/" + raceName + "/" + transformTexture + "_";
            } else {
                filePrefix = "textures/entity/races/" + raceName + "/bodytype_" + bodyType + "_";
            }

            renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay, filePrefix + "layer1.png", bodyTint, targetBone, partialTick, isReRender);
            renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay, filePrefix + "layer2.png", bodyTint2, targetBone, partialTick, isReRender);
            renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay, filePrefix + "layer3.png", bodyTint3, targetBone, partialTick, isReRender);

            if (isFrost || isBio) {
                renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay, filePrefix + "layer4.png", hairTint, targetBone, partialTick, isReRender);
                if (isBio || (isFrost && bodyType == 0)) {
                    renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay, filePrefix + "layer5.png", hexToRGB("#e67d40"), targetBone, partialTick, isReRender);
                }
            }
            return;
        }

        String textureBaseName = isStandardHumanoid ? "humansaiyan" : raceName;
        String genderPart = raceConfig.hasGender() ? "_" + gender : "";
        String formPart = "";

        String customPath = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + formPart + ".png";
        ResourceLocation customLoc = new ResourceLocation(Reference.MOD_ID, customPath);
        RenderType type = RenderType.entityCutoutNoCull(customLoc);
        VertexConsumer buff = bufferSource.getBuffer(type);

        renderTarget(poseStack, animatable, model, type, bufferSource, buff, packedLight, packedOverlay, bodyTint[0], bodyTint[1], bodyTint[2], alpha, targetBone, partialTick, isReRender);
    }

 /**   @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsOpt.orElse(new StatsData(animatable));

        var character = stats.getCharacter();
        String gender = character.getGender().toLowerCase();
        String raceName = character.getRace().toLowerCase();

        boolean shouldReplaceArmor = (raceName.equals("majin") && gender.equals("male")) || gender.equals("female");

        boolean isArmorBone = boneName.equals("armorBody") || boneName.equals("armorBody2") ||
                boneName.equals("armorRightArm") || boneName.equals("armorLeftArm") ||
                boneName.equals("boobas");

        if (shouldReplaceArmor && isArmorBone && !isRenderingArmor) {
            if (renderCustomArmor(poseStack, animatable, bone, bufferSource, packedLight, packedOverlay, false, partialTick, gender, raceName)) {
                return;
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        if (bone.getName().equals("head")
                && !this.isRenderingFace
                && !this.isRenderingArmor
                && !this.isRenderingTattoo) {

            this.headPose = new Matrix4f(poseStack.last().pose());
            this.headBone = bone;
        }

    } **/

    private void renderFacesPlayer(PoseStack poseStack, T animatable, GeoBone headBone, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay, float partialTick, boolean isReRender) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));

        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        int bodyType = character.getBodyType();

        boolean isVanillaPlayer = raceName.equals("human") || raceName.equals("saiyan");

        if (isVanillaPlayer && bodyType == 0) {
            return;
        }

        String raceFolder = isVanillaPlayer ? "humansaiyan" : raceName;
        String filePrefix = isVanillaPlayer ? "humansaiyan" : raceName;

        float originalScaleX = headBone.getScaleX();
        float originalScaleY = headBone.getScaleY();
        float originalScaleZ = headBone.getScaleZ();

        float inflation = 0.003f;
        headBone.setScaleX(originalScaleX + inflation);
        headBone.setScaleY(originalScaleY + inflation);
        headBone.setScaleZ(originalScaleZ + inflation);

        float[] eye1Tint = hexToRGB(character.getEye1Color());
        float[] eye2Tint = hexToRGB(character.getEye2Color());
        float[] hairTint = hexToRGB(character.getHairColor());
        float[] whiteTint = new float[]{1.0f, 1.0f, 1.0f};

        float[] skinTint;
        if (raceName.equals("bioandroid") || raceName.equals("frostdemon")) {
            skinTint = hexToRGB(character.getBodyColor2());
        } else {
            skinTint = hexToRGB(character.getBodyColor());
        }

        int eyesType = character.getEyesType();
        String eyeBaseName = filePrefix + "_eye_" + eyesType;

        renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder,
                eyeBaseName + "_0.png", whiteTint, partialTick, isReRender);
        renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder,
                eyeBaseName + "_1.png", eye1Tint, partialTick, isReRender);
        renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder,
                eyeBaseName + "_2.png", eye2Tint, partialTick, isReRender);

        if (isVanillaPlayer || raceName.equals("namekian")) {
            renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder,
                    eyeBaseName + "_3.png", hairTint, partialTick, isReRender);
        }

        int noseType = character.getNoseType();
        String noseName = filePrefix + "_nose_" + noseType + ".png";
        renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder, noseName,
                skinTint, partialTick, isReRender);

        int mouthType = character.getMouthType();
        String mouthName = filePrefix + "_mouth_" + mouthType + ".png";
        renderFacePart(poseStack, animatable, headBone, bufferSource, packedLight, packedOverlay, raceFolder, mouthName,
                skinTint, partialTick, isReRender);

        headBone.setScaleX(originalScaleX);
        headBone.setScaleY(originalScaleY);
        headBone.setScaleZ(originalScaleZ);
    }

    private void renderTattoos(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                               int packedLight, int packedOverlay, float partialTick) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));
        int tattooType = stats.getCharacter().getTattooType();
        if (tattooType == 0) return;

        String fileName = "tattoo_" + tattooType + ".png";
        ResourceLocation tattooLoc = new ResourceLocation(Reference.MOD_ID, "textures/entity/races/tattoos/" + fileName);

        RenderType type = RenderType.entityTranslucent(tattooLoc);
        VertexConsumer buffer = bufferSource.getBuffer(type);

        this.isRenderingTattoo = true;

        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(-1.0f, -1.0f);

        renderTarget(poseStack, animatable, model, type, bufferSource, buffer, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f, null, partialTick, false);

        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.disablePolygonOffset();

        this.isRenderingTattoo = false;
    }


    private static final Set<String> BONES_TO_IGNORE = Set.of(
            "armorHead",
            "armorBody", "armorBody2", "boobas",
            "armorLeggingsBody",
            "hat_layer",
            "body_layer",
            "right_arm_layer", "armorRightArm",
            "left_arm_layer", "armorLeftArm",
            "armorLeftLeg", "armorLeftBoot", "left_leg_layer",
            "armorRightLeg", "armorRightBoot", "right_leg_layer"
    );

    private void renderEffects(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, int packedLight, int packedOverlay, float partialTick) {
        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(new StatsData(animatable));
        if (stats.getSkills().getSkill("kaioken") == null || !stats.getSkills().getSkill("kaioken").isActive()) return;

        var character = stats.getCharacter();
        String raceName = character.getRace().toLowerCase();
        int bodyType = character.getBodyType();
        String gender = character.getGender().toLowerCase();
        String currentForm = character.getActiveForm();

        List<ResourceLocation> textures = getKaiokenTexturesInternal(raceName, bodyType, gender, currentForm, animatable);
        if (textures.isEmpty()) return;

        poseStack.pushPose();

        Map<String, Boolean> originalVisibility = new HashMap<>();

        for (GeoBone bone : model.topLevelBones()) {
            filterBonesForRenderEffects(bone, originalVisibility);
        }

        float r = 1.0f, g = 0.1f, b = 0.1f, a = 0.6f;
        for (ResourceLocation tex : textures) {
            RenderType type = RenderType.entityTranslucent(tex);
            VertexConsumer buffer = bufferSource.getBuffer(type);
            renderTarget(poseStack, animatable, model, type, bufferSource, buffer, packedLight, packedOverlay, r, g, b, a, null, partialTick, false);
        }

        for (GeoBone bone : model.topLevelBones()) {
            restoreBones(bone, originalVisibility);
        }

        poseStack.popPose();
    }

    private void filterBonesForRenderEffects(GeoBone bone, Map<String, Boolean> originalVisibility) {
        originalVisibility.put(bone.getName(), !bone.isHidden());

        if (BONES_TO_IGNORE.contains(bone.getName())) {
            bone.setHidden(true);
            saveChildrenVisibility(bone, originalVisibility);
        } else {
            bone.setHidden(false);
            for (GeoBone child : bone.getChildBones()) {
                filterBonesForRenderEffects(child, originalVisibility);
            }
        }
    }

    private void saveChildrenVisibility(GeoBone bone, Map<String, Boolean> originalVisibility) {
        for (GeoBone child : bone.getChildBones()) {
            originalVisibility.put(child.getName(), !child.isHidden());
            saveChildrenVisibility(child, originalVisibility);
        }
    }

    private void restoreBones(GeoBone bone, Map<String, Boolean> originalVisibility) {
        if (originalVisibility.containsKey(bone.getName())) {
            bone.setHidden(!originalVisibility.get(bone.getName()));
        }
        for (GeoBone child : bone.getChildBones()) {
            restoreBones(child, originalVisibility);
        }
    }

    private List<ResourceLocation> getKaiokenTexturesInternal(String raceName, int bodyType, String gender, String currentForm, T animatable) {
        List<ResourceLocation> textures = new ArrayList<>();
        boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

        if (raceName.equals("namekian") || raceName.equals("frostdemon") || raceName.equals("bioandroid")) {
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
                }
                filePrefix = "textures/entity/races/" + raceName + "/" + transformTexture + "_";
            } else {
                filePrefix = "textures/entity/races/" + raceName + "/bodytype_" + bodyType + "_";
            }
            textures.add(new ResourceLocation(Reference.MOD_ID, filePrefix + "layer1.png"));
            textures.add(new ResourceLocation(Reference.MOD_ID, filePrefix + "layer2.png"));
            textures.add(new ResourceLocation(Reference.MOD_ID, filePrefix + "layer3.png"));
        } else {
            boolean isVanillaLike = (raceName.equals("human") || raceName.equals("saiyan")) && bodyType == 0;
            if (isVanillaLike && !hasForm) {
                textures.add(animatable.getSkinTextureLocation());
            } else {
                String textureBaseName = (raceName.equals("human") || raceName.equals("saiyan")) ? "humansaiyan" : raceName;
                String genderPart = (!raceName.equals("namekian") && !raceName.equals("frostdemon")) ? "_" + gender : "";
                String path = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + ".png";
                textures.add(new ResourceLocation(Reference.MOD_ID, path));
            }
        }
        return textures;
    }


    private void renderFacePart(PoseStack poseStack, T animatable, GeoBone bone, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay, String raceFolder, String fileName,
                                float[] rgb, float partialTick, boolean isReRender) {

        String path = "textures/entity/races/" + raceFolder + "/faces/" + fileName;
        ResourceLocation location = new ResourceLocation(Reference.MOD_ID, path);

        RenderType type = RenderType.entityCutoutNoCull(location);
        VertexConsumer buffer = bufferSource.getBuffer(type);

        renderRecursively(poseStack, animatable, bone, type, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, rgb[0], rgb[1], rgb[2], 1.0f);
    }

    private void renderTarget(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType type, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlay, float r, float g, float b, float a, @Nullable GeoBone targetBone, float partialTick, boolean isReRender) {
        if (targetBone != null) {
            renderRecursively(poseStack, animatable, targetBone, type, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, a);
        } else {
            for (GeoBone group : model.topLevelBones()) {
                renderRecursively(poseStack, animatable, group, type, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, a);
            }
        }

    }

    private void renderColoredPass(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, int packedLight, int packedOverlay, String texturePath, float[] rgb, @Nullable GeoBone targetBone, float partialTick, boolean isReRender) {
        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, texturePath);
        RenderType type = RenderType.entityCutoutNoCull(loc);
        VertexConsumer buffer = bufferSource.getBuffer(type);
        renderTarget(poseStack, animatable, model, type, bufferSource, buffer, packedLight, packedOverlay, rgb[0], rgb[1], rgb[2], 1.0f, targetBone, partialTick, isReRender);
    }

    private boolean renderCustomArmor(PoseStack poseStack, T animatable, GeoBone mainBone, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean isReRender, float partialTick, String gender, String raceName) {
        String boneName = mainBone.getName();

        if (boneName.equals("armorBody") || boneName.equals("armorBody2") || boneName.equals("boobas")) {
            ItemStack chestStack = animatable.getItemBySlot(EquipmentSlot.CHEST);

            if (!chestStack.isEmpty() && chestStack.getItem() instanceof ArmorItem armorItem) {

                ResourceLocation texture = getArmorTexture(animatable, chestStack, EquipmentSlot.CHEST, null);
                boolean isVanillaArmor = texture.getNamespace().equals("minecraft");

                if (isVanillaArmor) {

                    // Selección de Modelo
                    ResourceLocation modelToLoad = null;
                    if (gender.equals("female")) {
                        modelToLoad = MAJIN_SLIM_ARMOR_MODEL;
                    } else if (raceName.equals("majin")) {
                        modelToLoad = MAJIN_ARMOR_MODEL;
                    }

                    if (modelToLoad == null) modelToLoad = MAJIN_ARMOR_MODEL;

                    BakedGeoModel armorGeoModel = GeckoLibCache.getBakedModels().get(modelToLoad);
                    if (armorGeoModel == null) return false;

                    Optional<GeoBone> armorBoneOpt = armorGeoModel.getBone(boneName);

                    if (armorBoneOpt.isPresent()) {
                        GeoBone armorBone = armorBoneOpt.get();

                        // Copiar Transformaciones
                        armorBone.setRotX(mainBone.getRotX());
                        armorBone.setRotY(mainBone.getRotY());
                        armorBone.setRotZ(mainBone.getRotZ());
                        armorBone.setPosX(mainBone.getPosX());
                        armorBone.setPosY(mainBone.getPosY());
                        armorBone.setPosZ(mainBone.getPosZ());

                        float inflation = 1.05f;
                        armorBone.setScaleX(mainBone.getScaleX() * inflation);
                        armorBone.setScaleY(mainBone.getScaleY() * inflation);
                        armorBone.setScaleZ(mainBone.getScaleZ() * inflation);

                        // Color Base
                        float r = 1.0F, g = 1.0F, b = 1.0F;
                        if (armorItem instanceof DyeableArmorItem dyeable) {
                            int color = dyeable.getColor(chestStack);
                            r = (float)(color >> 16 & 255) / 255.0F;
                            g = (float)(color >> 8 & 255) / 255.0F;
                            b = (float)(color & 255) / 255.0F;
                        }

                        RenderType armorType = RenderType.entityCutoutNoCull(texture);
                        VertexConsumer armorBuffer = ItemRenderer.getArmorFoilBuffer(bufferSource, armorType, false, chestStack.hasFoil());

                        this.isRenderingArmor = true;
                        // Forzamos ocultar el original por si acaso
                        mainBone.setHidden(true);

                        // Renderizar el hueso del modelo EXTERNO (64x32)
                        this.renderRecursively(poseStack, animatable, armorBone, armorType, bufferSource, armorBuffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, 1.0f);

                        // Overlay
                        if (armorItem instanceof DyeableArmorItem) {
                            ResourceLocation overlayTex = getArmorTexture(animatable, chestStack, EquipmentSlot.CHEST, "overlay");
                            RenderType overlayType = RenderType.entityCutoutNoCull(overlayTex);
                            VertexConsumer overlayBuffer = ItemRenderer.getArmorFoilBuffer(bufferSource, overlayType, false, false);
                            this.renderRecursively(poseStack, animatable, armorBone, overlayType, bufferSource, overlayBuffer, isReRender, partialTick, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
                        }

                        this.isRenderingArmor = false;
                        return true;
                    }
                }
                else {
                    float r = 1.0F, g = 1.0F, b = 1.0F;
                    RenderType armorType = RenderType.entityCutoutNoCull(texture);
                    VertexConsumer armorBuffer = ItemRenderer.getArmorFoilBuffer(bufferSource, armorType, false, chestStack.hasFoil());

                    this.isRenderingArmor = true;

                    mainBone.setHidden(false);

                    this.renderRecursively(poseStack, animatable, mainBone, armorType, bufferSource, armorBuffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, 1.0f);

                    mainBone.setHidden(true);

                    this.isRenderingArmor = false;
                    return true;
                }

            } else {
                mainBone.setHidden(true);
                return true;
            }

        }

        return false;
    }

    private ResourceLocation getArmorTexture(LivingEntity entity, ItemStack stack, EquipmentSlot slot, String type) {
        String domain = "minecraft";
        String path = ((ArmorItem) stack.getItem()).getMaterial().getName();
        String[] split = path.split(":", 2);

        if (split.length > 1) {
            domain = split[0];
            path = split[1];
        }

        String typeSuffix = (type == null || type.isEmpty()) ? "" : "_" + type;

        String layer = (slot == EquipmentSlot.LEGS) ? "layer_2" : "layer_1";

        String textureString = String.format("%s:textures/models/armor/%s_%s%s.png", domain, path, layer, typeSuffix);

        textureString = ForgeHooksClient.getArmorTexture(entity, stack, textureString, slot, type);

        return new ResourceLocation(textureString);
    }

    private void hideLayerBonesIfArmored(BakedGeoModel model, T animatable) {
        ItemStack chestStack = animatable.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.isEmpty()) {
            model.getBone("body_layer").ifPresent(bone -> bone.setHidden(true));
            model.getBone("right_arm_layer").ifPresent(bone -> bone.setHidden(true));
            model.getBone("left_arm_layer").ifPresent(bone -> bone.setHidden(true));
        } else {
            model.getBone("body_layer").ifPresent(bone -> bone.setHidden(false));
            model.getBone("right_arm_layer").ifPresent(bone -> bone.setHidden(false));
            model.getBone("left_arm_layer").ifPresent(bone -> bone.setHidden(false));
        }
        ItemStack legStack = animatable.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack bootStack = animatable.getItemBySlot(EquipmentSlot.FEET);
        if (!legStack.isEmpty() || !bootStack.isEmpty()) {
            model.getBone("right_leg_layer").ifPresent(bone -> bone.setHidden(true));
            model.getBone("left_leg_layer").ifPresent(bone -> bone.setHidden(true));
        } else {
            model.getBone("right_leg_layer").ifPresent(bone -> bone.setHidden(false));
            model.getBone("left_leg_layer").ifPresent(bone -> bone.setHidden(false));
        }
    }

    private float[] hexToRGB(String hexColor) {
        return ColorUtils.hexToRgb(hexColor);
    }
}
