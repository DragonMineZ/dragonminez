package com.dragonminez.client.render;

import com.dragonminez.Reference;
import com.dragonminez.client.render.layer.PlayerItemInHandLayer;
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtils;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        this.shadowRadius = 0.4f;

        this.addRenderLayer(new PlayerItemInHandLayer<>(this));
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

            float[] bodyTint = hexToRGB("#badaff");
            float[] bodyTint2 = hexToRGB("#f29eff");
            float[] bodyTint3 = hexToRGB("#ff54da");
            float[] hairTint = hexToRGB(character.getHairColor());

            boolean forceVanilla = raceConfig.useVanillaSkin();
            boolean isStandardHumanoid = (raceName.equals("human") || raceName.equals("saiyan"));
            boolean isDefaultBody = (bodyType == 0);

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

                String textureBase = raceName;

                if (hasForm) {
                    switch (raceName) {
                        case "bioandroid" -> {
                            if (currentForm.equals("semi_perfect")) textureBase = "bioandroid_semi";
                            else if (currentForm.equals("perfect")) textureBase = "bioandroid_perfect";
                        }
                        case "frostdemon" -> {
                            if (currentForm.equals("form2")) textureBase = "frostdemon_2";
                            else if (currentForm.equals("form3")) textureBase = "frostdemon_3";
                            else if (currentForm.equals("golden")) textureBase = "frostdemon_golden";
                        }

                    }
                }

                // ejmplo "bioandroid_semi_0_"
                String filePrefix = "textures/entity/races/" + textureBase + "_" + bodyType + "_";

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
                                filePrefix + "layer5.png", hexToRGB("#e67d40"));
                    } else {
                        renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                                filePrefix + "layer4.png", hairTint);
                    }

                }
                if (isBio) {
                    renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                            filePrefix + "layer4.png", hairTint);
                    renderColoredPass(model, poseStack, animatable, bufferSource, packedLight, packedOverlay,
                            filePrefix + "layer5.png", hexToRGB("#dbcb9e"));
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

            // "humansaiyan_male_1.png" o "humansaiyan_male_1_ssj4.png"
            String customPath = "textures/entity/races/" + textureBaseName + genderPart + "_" + bodyType + formPart + ".png";

            ResourceLocation customLoc = new ResourceLocation(Reference.MOD_ID, customPath);
            RenderType globalRenderType = RenderType.entityCutoutNoCull(customLoc);
            VertexConsumer globalBuffer = bufferSource.getBuffer(globalRenderType);

            for (GeoBone group : model.topLevelBones()) {
                renderRecursively(poseStack, animatable, group, globalRenderType, bufferSource, globalBuffer, isReRender, partialTick, packedLight, packedOverlay, bodyTint[0], bodyTint[1], bodyTint[2], alpha);
            }
        }

//        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void renderColoredPass(BakedGeoModel model, PoseStack poseStack, T animatable, MultiBufferSource bufferSource, int packedLight, int packedOverlay, String texturePath, float[] rgb) {

        ResourceLocation loc = new ResourceLocation(Reference.MOD_ID, texturePath);
        RenderType type = RenderType.entityCutoutNoCull(loc);
        VertexConsumer buffer = bufferSource.getBuffer(type);

        for (GeoBone group : model.topLevelBones()) {
            renderRecursively(poseStack, animatable, group, type, bufferSource, buffer, false, 0, packedLight, packedOverlay, rgb[0], rgb[1], rgb[2], 1.0f);
        }
    }

    private float[] hexToRGB(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) return new float[]{1.0f, 1.0f, 1.0f};
        try {
            if (hexColor.startsWith("#")) hexColor = hexColor.substring(1);
            long color = Long.parseLong(hexColor, 16);
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            return new float[]{r, g, b};
        } catch (Exception e) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
    }

    private void renderPass(BakedGeoModel model, PoseStack stack, T animatable, RenderType type, MultiBufferSource source, int light, int overlay, float r, float g, float b, float a) {
        VertexConsumer buffer = source.getBuffer(type);
        for (GeoBone group : model.topLevelBones()) {
            renderRecursively(stack, animatable, group, type, source, buffer, false, 0, light, overlay, r, g, b, a);
        }
    }

    //RENDER UN SOLO BONE ("head", "body", "left_arm")
    private void renderBonePass(GeoBone bone, PoseStack stack, T animatable, RenderType type, MultiBufferSource source, int light, int overlay, float r, float g, float b, float a) {
        if (bone != null) {
            VertexConsumer buffer = source.getBuffer(type);
            renderRecursively(stack, animatable, bone, type, source, buffer, false, 0, light, overlay, r, g, b, a);
        }
    }

    @Override
    protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.renderNameTag(pEntity, pDisplayName, pPoseStack, pBuffer, pPackedLight);
    }

}
