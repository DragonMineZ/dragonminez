package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.util.RenderUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.MajinForms;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Objects;

public class DMZPlayerModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final ResourceLocation BASE_DEFAULT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final ResourceLocation BASE_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human_slim.geo.json");
    private static final ResourceLocation MAJIN_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin.geo.json");
    private static final ResourceLocation MAJIN_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin_slim.geo.json");
    private static final ResourceLocation FROST_DEMON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json");
    private static final ResourceLocation FROST_DEMON_THIRD = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_third.geo.json");
    private static final ResourceLocation FROST_DEMON_FIFTH = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fifth.geo.json");
    private static final ResourceLocation BIO_ANDROID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json");
    private static final ResourceLocation BIO_ANDROID_SEMI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_semi.geo.json");
    private static final ResourceLocation BIO_ANDROID_PERFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_perfect.geo.json");
    private static final ResourceLocation OOZARU = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/oozaru.geo.json");

    private final ResourceLocation textureLocation;
    private final ResourceLocation animationLocation;
    private final String raceName;
    private final String customModel;

    public DMZPlayerModel(String raceName, String customModel) {
        this.raceName = raceName.toLowerCase();
        this.customModel = customModel;

        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
        this.animationLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            var character = data.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String customRaceGender = ConfigManager.getRaceCharacter(race).hasGender() ? gender : "";
            String currentForm = character.getActiveForm();
            int bodyType = character.getBodyType();

            boolean isMale = gender.equals("male") || gender.equals("hombre");
            boolean isSlimSkin = player.getModelName().equals("slim");
            boolean isBaseForm = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");

            if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
                return OOZARU;
            }

            String modelKey = "";
            var activeFormData = character.getActiveFormData();
            if (activeFormData != null && activeFormData.hasCustomModel() && !activeFormData.getCustomModel().isEmpty()) {
                modelKey = activeFormData.getCustomModel();
            } else if (this.customModel != null && !this.customModel.isEmpty()) {
                modelKey = this.customModel;
            }

            if (!modelKey.isEmpty()) {
                return resolveCustomModel(modelKey, isSlimSkin, isMale, bodyType, customRaceGender);
            }

            if (race.equals("bioandroid")) {
                return isBaseForm ? BIO_ANDROID : BIO_ANDROID_PERFECT;
            }

            if (race.equals("frostdemon")) return FROST_DEMON;

            if (race.equals("namekian")) return BASE_DEFAULT;

            if (race.equals("majin")) {
                if (isBaseForm) {
                    return isMale ? MAJIN_FAT : MAJIN_SLIM;
                }
                return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            }

            if (race.equals("human") || race.equals("saiyan")) {
                if (!isMale) return MAJIN_SLIM;

                if (bodyType == 0) {
                    return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                }
                return BASE_DEFAULT;
            }

            if (!isMale) return MAJIN_SLIM;
            return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;

        }).orElse(BASE_DEFAULT);
    }

    private ResourceLocation resolveCustomModel(String modelName, boolean isSlimSkin, boolean isMale, int bodyType, String customRaceGender) {
        String key = modelName.toLowerCase();

        switch (key) {
            case "human", "saiyan", "saiyan_ssj4":
                if (bodyType == 0) return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                if (!isMale) return MAJIN_SLIM;
                return BASE_DEFAULT;
            case "namekian", "namekian_orange":
                return BASE_DEFAULT;
            case "majin":
                return isMale ? MAJIN_FAT : MAJIN_SLIM;
            case "majin_super","majin_ultra":
                return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            case "majin_evil","majin_kid":
                return isMale ? BASE_SLIM : MAJIN_SLIM;
            case "frostdemon","frostdemon_final":
                return FROST_DEMON;
            case "frostdemon_fifth":
                return FROST_DEMON_FIFTH;
            case "frostdemon_third":
                return FROST_DEMON_THIRD;
            case "bioandroid":
                return BIO_ANDROID;
            case "bioandroid_semi":
                return BIO_ANDROID_SEMI;
            case "bioandroid_perfect":
                return BIO_ANDROID_PERFECT;
            case "oozaru":
                return OOZARU;
        }

        if (customRaceGender != null && !customRaceGender.isEmpty()) customRaceGender = "_" + customRaceGender;
        ResourceLocation customLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + modelName + customRaceGender + ".geo.json");
        if (fileExists(customLoc)) return customLoc;

        return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        return textureLocation;
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return animationLocation;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        var head = this.getAnimationProcessor().getBone("head");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if (head != null) {
            float headPitch = entityData.headPitch() * Mth.DEG_TO_RAD;
            float headYaw = entityData.netHeadYaw() * Mth.DEG_TO_RAD;

            if (FlySkillEvent.isFlyingFast()) {
                head.setRotX(45); head.setRotY(0);
            } else {
                head.setRotX(headPitch); head.setRotY(headYaw);
            }
        }

        if (animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isShootingKi()) {
            var rightArm = this.getAnimationProcessor().getBone("right_arm");
            if (rightArm != null) {
                float headPitch = entityData.headPitch() * Mth.DEG_TO_RAD;
                float headYaw = entityData.netHeadYaw() * Mth.DEG_TO_RAD;
                rightArm.setRotX(headPitch + (float)(Math.PI / 2));
                rightArm.setRotY(headYaw);
            }
        }

        try {
            float partialTick = animationState.getPartialTick();
            float ageInTicks = (float) animatable.getTick(animatable);
            var rightArm = this.getAnimationProcessor().getBone("right_arm");
            var leftArm = this.getAnimationProcessor().getBone("left_arm");
            if (rightArm != null) RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            if (leftArm != null) RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
        } catch (Exception ignore) {}
    }

    private boolean fileExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

}