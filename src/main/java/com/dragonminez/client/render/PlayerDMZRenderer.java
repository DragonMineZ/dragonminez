package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.PlayerArmorLayer;
import com.dragonminez.client.render.layer.PlayerItemInHandLayer;
import com.dragonminez.client.util.ColorUtils;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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

import java.util.Optional;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/armor/armormajinslim.geo.json");

    private boolean isRenderingArmor = false;

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        this.shadowRadius = 0.4f;

        this.addRenderLayer(new PlayerItemInHandLayer<>(this));
        this.addRenderLayer(new PlayerArmorLayer<>(this));

    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }



    @Override
    public void actuallyRender(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();

        LivingEntity livingEntity = (LivingEntity) animatable;
        boolean shouldSit = animatable.isPassenger() && (animatable.getVehicle() != null && animatable.getVehicle().shouldRiderSit());
        float lerpBodyRot = livingEntity == null ? 0 : Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);
        float lerpHeadRot = livingEntity == null ? 0 : Mth.rotLerp(partialTick, livingEntity.yHeadRotO, livingEntity.yHeadRot);
        float netHeadYaw = lerpHeadRot - lerpBodyRot;

        if (shouldSit && animatable.getVehicle() instanceof LivingEntity livingentity) {
            lerpBodyRot = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;

            if (clampedHeadYaw * clampedHeadYaw > 2500f)
                lerpBodyRot += clampedHeadYaw * 0.2f;

            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }

        if (animatable.getPose() == Pose.SLEEPING && livingEntity != null) {
            Direction bedDirection = livingEntity.getBedOrientation();

            if (bedDirection != null) {
                float eyePosOffset = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;

                poseStack.translate(-bedDirection.getStepX() * eyePosOffset, 0, -bedDirection.getStepZ() * eyePosOffset);
            }
        }

        float ageInTicks = animatable.tickCount + partialTick;
        float limbSwingAmount = 0;
        float limbSwing = 0;

        applyRotations(animatable, poseStack, ageInTicks, lerpBodyRot, partialTick);

        if (!shouldSit && animatable.isAlive() && livingEntity != null) {
            limbSwingAmount = livingEntity.walkAnimation.speed(partialTick);
            limbSwing = livingEntity.walkAnimation.position(partialTick);

            if (livingEntity.isBaby())
                limbSwing *= 3f;

            if (limbSwingAmount > 1f)
                limbSwingAmount = 1f;
        }

        if (!isReRender) {
            float headPitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
            float motionThreshold = getMotionAnimThreshold(animatable);
            Vec3 velocity = animatable.getDeltaMovement();
            float avgVelocity = (float)((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2f);
            AnimationState<T> animationState = new AnimationState<T>(animatable, limbSwing, limbSwingAmount, partialTick, avgVelocity >= motionThreshold && limbSwingAmount != 0);
            long instanceId = getInstanceId(animatable);
            GeoModel<T> currentModel = getGeoModel();

            animationState.setData(DataTickets.TICK, animatable.getTick(animatable));
            animationState.setData(DataTickets.ENTITY, animatable);
            animationState.setData(DataTickets.ENTITY_MODEL_DATA, new EntityModelData(shouldSit, livingEntity != null && livingEntity.isBaby(), -netHeadYaw, -headPitch));
            currentModel.addAdditionalStateData(animatable, instanceId, animationState::setData);
            currentModel.handleAnimations(animatable, instanceId, animationState);
        }

        poseStack.translate(0, 0.01f, 0);

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        if (animatable.isInvisibleTo(Minecraft.getInstance().player)) {
            if (Minecraft.getInstance().shouldEntityAppearGlowing(animatable)) {
                buffer = bufferSource.getBuffer(renderType = RenderType.outline(getTextureLocation(animatable)));
            }
            else {
                renderType = null;
            }
        }

        if (renderType != null){
            renderAll(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }

        poseStack.popPose();
    }

    private void renderAll(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        var statsCap = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsCap.orElse(null);

        if (stats != null) {
            var character = stats.getCharacter();
            String raceName = character.getRace().toLowerCase();
            String gender = character.getGender().toLowerCase();
            int bodyType = character.getBodyType();

            String currentForm = character.getCurrentForm();
            boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(raceName);

            int[] bodyTint = ColorUtils.hexToRgb(character.getBodyColor());
            int[] bodyTint2 = ColorUtils.hexToRgb(character.getBodyColor2());
            int[] bodyTint3 = ColorUtils.hexToRgb(character.getBodyColor3());
            int[] hairTint = ColorUtils.hexToRgb(character.getHairColor());

            boolean forceVanilla = raceConfig.useVanillaSkin();
            boolean isStandardHumanoid = (raceName.equals("human") || raceName.equals("saiyan"));
            boolean isDefaultBody = (bodyType == 0);

            //Por si carga las otras piezas del modelo aca las esconde
            model.getBone("armorBody").ifPresent(bone -> bone.setHidden(raceName.equals("majin") && gender.equals("male")));
            model.getBone("armorBody2").ifPresent(bone -> bone.setHidden(raceName.equals("majin") && gender.equals("male")));
            model.getBone("armorLeggingsBody").ifPresent(bone -> bone.setHidden(raceName.equals("majin") && gender.equals("male")));
            model.getBone("armorRightArm").ifPresent(bone -> bone.setHidden(raceName.equals("majin") && gender.equals("male")));
            model.getBone("armorLeftArm").ifPresent(bone -> bone.setHidden(raceName.equals("majin") && gender.equals("male")));

            if (forceVanilla || (isStandardHumanoid && isDefaultBody && !hasForm)) {
                ResourceLocation playerSkin = animatable.getSkinTextureLocation();
                RenderType globalRenderType = RenderType.entityTranslucent(playerSkin);
                VertexConsumer globalBuffer = bufferSource.getBuffer(globalRenderType);

                for (GeoBone group : model.topLevelBones()) {
                    renderRecursively(poseStack, animatable, group, globalRenderType, bufferSource, globalBuffer, isReRender, partialTick, packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, alpha);
                }
                return;
            }

            boolean isNamek = raceName.equals("namekian");
            boolean isFrost = raceName.equals("frostdemon");
            boolean isBio = raceName.equals("bioandroid");

            if (isNamek || isFrost || isBio) {

                String filePrefix;

                if (hasForm) {
                    String transformTexture = "";
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

                renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                        filePrefix + "layer1.png", bodyTint);

                renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                        filePrefix + "layer2.png", bodyTint2);

                renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                        filePrefix + "layer3.png", bodyTint3);

                if (isFrost) {
                    if(bodyType==0){
                        renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                                filePrefix + "layer4.png", hairTint);
                        renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                                filePrefix + "layer5.png", ColorUtils.hexToRgb("#e67d40"));
                    } else {
                        renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                                filePrefix + "layer4.png", hairTint);
                    }

                }
                if (isBio) {
                    renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                            filePrefix + "layer4.png", hairTint);
                    renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                            filePrefix + "layer5.png", ColorUtils.hexToRgb("#dbcb9e"));
                }
                return;
            }

            String textureBaseName = raceName;
            if (isStandardHumanoid) {
                textureBaseName = "humansaiyan";
            }

            String genderPart = "";
            if (raceConfig.hasGender()) {
                genderPart = "_" + gender;
            }

            String formPart = "";
            if (hasForm) {
                // forma especifica
                // if (currentForm.equals("ssj4")) formPart = "_ssj4";

                //todas las formas busquen una textura de la forma:
                // formPart = "_" + currentForm;
            }

            String customPath = "textures/entity/races/" + textureBaseName + "/bodytype" + genderPart + "_" + bodyType + formPart + ".png";

            ResourceLocation customLoc = new ResourceLocation(Reference.MOD_ID, customPath);
            RenderType globalRenderType = RenderType.entityCutoutNoCull(customLoc);
            VertexConsumer globalBuffer = bufferSource.getBuffer(globalRenderType);

            for (GeoBone group : model.topLevelBones()) {
                renderRecursively(poseStack, animatable, group, globalRenderType, bufferSource, globalBuffer, isReRender, partialTick, packedLight, packedOverlay, bodyTint[0], bodyTint[1], bodyTint[2], alpha);
            }
        }

    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats != null) {
            var character = stats.getCharacter();

            boolean isMajinFat = character.getRace().equals("majin") &&
                    character.getGender().equals("male");

            if (isMajinFat && !isRenderingArmor) {
                if (renderCustomArmor(poseStack, animatable, bone, bufferSource, packedLight, packedOverlay, isReRender, partialTick)) {
                    return;
                }
            }

        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);



    }

    private void renderColoredPass(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, int packedLight, int packedOverlay, String texturePath, int[] rgb) {

        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, texturePath);
        RenderType type = RenderType.entityCutoutNoCull(loc);
        VertexConsumer buffer = bufferSource.getBuffer(type);

        for (GeoBone group : model.topLevelBones()) {
            renderRecursively(poseStack, animatable, group, type, bufferSource, buffer, false, 0, packedLight, packedOverlay, rgb[0], rgb[1], rgb[2], 1.0f);
        }
    }


    private boolean renderCustomArmor(PoseStack poseStack, T animatable, GeoBone mainBone, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean isReRender, float partialTick) {
        String boneName = mainBone.getName();

        // 1. FILTRO DE HUESOS
        if (boneName.equals("armorBody") || boneName.equals("armorBody2")) {

            ItemStack chestStack = animatable.getItemBySlot(EquipmentSlot.CHEST);

            if (!chestStack.isEmpty() && chestStack.getItem() instanceof ArmorItem armorItem) {

                ResourceLocation texture = getArmorTexture(animatable, chestStack, EquipmentSlot.CHEST, null);

                boolean isVanillaArmor = texture.getNamespace().equals("minecraft");

                if (isVanillaArmor) {

                    BakedGeoModel armorGeoModel = GeckoLibCache.getBakedModels().get(MAJIN_ARMOR_MODEL);
                    if (armorGeoModel == null) return false;

                    Optional<GeoBone> armorBoneOpt = armorGeoModel.getBone(boneName);

                    if (armorBoneOpt.isPresent()) {
                        GeoBone armorBone = armorBoneOpt.get();

                        armorBone.setRotX(mainBone.getRotX());
                        armorBone.setRotY(mainBone.getRotY());
                        armorBone.setRotZ(mainBone.getRotZ());

                        armorBone.setPosX(mainBone.getPosX());
                        armorBone.setPosY(mainBone.getPosY());
                        armorBone.setPosZ(mainBone.getPosZ());

                        float inflation = 1.07f;

                        armorBone.setScaleX(mainBone.getScaleX() * inflation);
                        armorBone.setScaleY(mainBone.getScaleY() * inflation + 0.05f);
                        armorBone.setScaleZ(mainBone.getScaleZ() * inflation);

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
                        mainBone.setHidden(true);

                        this.renderRecursively(poseStack, animatable, armorBone, armorType, bufferSource, armorBuffer, isReRender, partialTick, packedLight, packedOverlay, r, g, b, 1.0f);

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

    @Override
    protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.renderNameTag(pEntity, pDisplayName, pPoseStack, pBuffer, pPackedLight);
    }

}
