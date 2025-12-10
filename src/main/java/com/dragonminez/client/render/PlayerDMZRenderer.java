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
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
import software.bernie.geckolib.util.RenderUtils;

import javax.annotation.Nullable;
import java.util.Optional;

public class PlayerDMZRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final ResourceLocation MAJIN_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/armor/armormajinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM_ARMOR_MODEL = new ResourceLocation(Reference.MOD_ID, "geo/armor/armormajinslim.geo.json");

    private final PlayerModel<AbstractClientPlayer> vanillaModelReference;

    private boolean isRenderingArmor = false;

    public PlayerDMZRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        this.shadowRadius = 0.4f;

        this.addRenderLayer(new PlayerItemInHandLayer<>(this));
        this.addRenderLayer(new PlayerArmorLayer<>(this));

        this.vanillaModelReference = new PlayerModel<>(renderManager.bakeLayer(ModelLayers.PLAYER), false);
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
            doRenderLogic(poseStack, animatable, model, renderType, bufferSource, buffer, packedLight,
                    packedOverlay, 1.0f, 1.0f, 1.0f, alpha, null, partialTick, isReRender);
        }

        poseStack.popPose();
    }

    private void doRenderLogic(PoseStack poseStack, T animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, @Nullable GeoBone targetBone, float partialTick, boolean isReRender) {

        if (targetBone == null) hideLayerBonesIfArmored(model, animatable);

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


                // Condición: Ocultar si es Majin (Hombre) O si es cualquier Mujer
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
            return;
        }

        if (targetBone == null) {
            super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        var statsOpt = StatsProvider.get(StatsCapability.INSTANCE, animatable);
        var stats = statsOpt.orElse(null);

        if (stats != null) {
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

        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
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

    public void renderLeftHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player) {
        renderFirstPersonArm(poseStack, buffer, packedLight, player, HumanoidArm.LEFT);
    }
    public void renderRightHand(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player) {
        renderFirstPersonArm(poseStack, buffer, packedLight, player, HumanoidArm.RIGHT);
    }

    public void renderFirstPersonArm(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T animatable, HumanoidArm arm) {

        GeoModel<T> model = this.getGeoModel();
        model.getBakedModel(model.getModelResource(animatable));
        BakedGeoModel bakedModel = (BakedGeoModel) model.getBakedModel(model.getModelResource(animatable));

        String boneName = (arm == HumanoidArm.LEFT) ? "left_arm" : "right_arm";

        model.getBone(boneName).ifPresent(bone -> {

            ModelPart vanillaArm = (arm == HumanoidArm.LEFT)
                    ? this.vanillaModelReference.leftArm
                    : this.vanillaModelReference.rightArm;

            bone.setRotX(0);
            bone.setRotY(0);
            bone.setRotZ(0);

            poseStack.pushPose();

            boolean isHoldingMap = false;
            ItemStack itemStack = ItemStack.EMPTY;
            Player player = Minecraft.getInstance().player;

            if (player != null && arm == HumanoidArm.RIGHT) { // Asumiendo diestro por ahora
                itemStack = (player.getMainArm() == HumanoidArm.RIGHT) ? player.getMainHandItem() : player.getOffhandItem();
                if (itemStack.is(net.minecraft.world.item.Items.FILLED_MAP)) {
                    isHoldingMap = true;
                }
            }
//            RenderUtils.matchModelPartRot(vanillaArm, bone);

            double xOffset = (arm == HumanoidArm.LEFT) ? -0.05D : 0.15D;
            double yOffset = -1.65D;
            double zOffset = 0.1D;

            if(arm == HumanoidArm.RIGHT){
                if (isHoldingMap) {
                    // Rotación especial para leer el mapa
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(5));
                    yOffset = -1.52D;
                    xOffset = 0.3D;
                    zOffset = 0.1D;

                    model.getBone("left_arm").ifPresent(bone2 -> {
                        bone2.setPosX(5);
                    });
                } else {
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(3));
                }
            } else {
                if (isHoldingMap) {
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    xOffset = -0.1D;
                    zOffset = 0.2D;
                    yOffset = -1.7D;
                } else {
                    poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(3));
                    xOffset = -0.1D;
                    zOffset = 0.2D;
                    yOffset = -1.7D;
                }



            }

            poseStack.scale(1.3f,1.3f,1.3f);


            vanillaArm.translateAndRotate(poseStack);


            poseStack.translate(xOffset, yOffset, zOffset);

            // Renderizado
            float partialTick = Minecraft.getInstance().getFrameTime();
            ResourceLocation texture = this.getTextureLocation(animatable);
            RenderType renderType = this.getRenderType(animatable, texture, bufferSource, partialTick);
            VertexConsumer buffer = bufferSource.getBuffer(renderType);

            this.doRenderLogic(
                    poseStack,
                    animatable,
                    bakedModel,
                    renderType,
                    bufferSource,
                    buffer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, 1.0f,
                    bone,
                    partialTick,
                    false
            );

            poseStack.popPose();
        });
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


    @Override
    protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.renderNameTag(pEntity, pDisplayName, pPoseStack, pBuffer, pPackedLight);
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
}
