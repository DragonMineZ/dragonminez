package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.render.util.RenderUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.lists.SaiyanForms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZPlayerModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final ResourceLocation BASE_DEFAULT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final ResourceLocation BASE_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human_slim.geo.json");
    private static final ResourceLocation MAJIN_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin.geo.json");
    private static final ResourceLocation MAJIN_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin_slim.geo.json");
    private static final ResourceLocation FROST_DEMON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json");
    private static final ResourceLocation FROST_DEMON_SECOND = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_second.geo.json");
    private static final ResourceLocation FROST_DEMON_THIRD = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_third.geo.json");
    private static final ResourceLocation FROST_DEMON_FIFTH = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fifth.geo.json");
    private static final ResourceLocation FROSTDEMON_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fp.geo.json");
    private static final ResourceLocation BIO_ANDROID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json");
    private static final ResourceLocation BIO_ANDROID_SEMI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_semi.geo.json");
    private static final ResourceLocation BIO_ANDROID_PERFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_perfect.geo.json");
    private static final ResourceLocation BIO_ANDROID_ULTRA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_ultra.geo.json");
    private static final ResourceLocation OOZARU = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/oozaru.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_FEMALE_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_fem_fp.geo.json");

    private static final Map<ResourceLocation, Boolean> FILE_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, ResourceLocation> MODEL_RESOLUTION_CACHE = new ConcurrentHashMap<>();
    private final ResourceLocation textureLocation;
    private final ResourceLocation animationLocation;
    private final String customModel;

    public DMZPlayerModel(String raceName, String customModel) {
        this.customModel = customModel;
        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
        this.animationLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            Character character = data.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String currentForm = character.getActiveForm();
            int bodyType = character.getBodyType();
            String playerModelName = player.getModelName();

            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
            var activeFormData = character.getActiveFormData();
            String activeCustomModel = (activeFormData != null && activeFormData.hasCustomModel() && !activeFormData.getCustomModel().isEmpty())
                    ? activeFormData.getCustomModel().toLowerCase()
                    : "";
            String raceCustomModel = (raceConfig != null && raceConfig.hasCustomModel()) ? raceConfig.getCustomModel().toLowerCase() : "";
            String fallbackCustomModel = this.customModel != null ? this.customModel.toLowerCase() : "";
            String formKey = currentForm != null ? currentForm.toLowerCase() : "";
            String stateKey = String.join("|",
                    race,
                    gender,
                    formKey,
                    Integer.toString(bodyType),
                    playerModelName,
                    activeCustomModel,
                    raceCustomModel,
                    fallbackCustomModel,
                    Boolean.toString(raceConfig != null && raceConfig.getHasGender())
            );

            return MODEL_RESOLUTION_CACHE.computeIfAbsent(stateKey, ignored -> {
                boolean isMale = gender.equals("male") || gender.equals("hombre");
                boolean isSlimSkin = playerModelName.equals("slim");
                boolean isBaseForm = currentForm == null || currentForm.isEmpty() || currentForm.equalsIgnoreCase("base");

                if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
                    return OOZARU;
                }

                String modelKey = "";
                if (!activeCustomModel.isEmpty()) {
                    modelKey = activeCustomModel;
                } else if (!raceCustomModel.isEmpty()) {
                    modelKey = raceCustomModel;
                } else if (!fallbackCustomModel.isEmpty()) {
                    modelKey = fallbackCustomModel;
                }

                if (!modelKey.isEmpty()) {
                    String customRaceGender = (raceConfig != null && raceConfig.getHasGender()) ? gender : "";
                    return resolveCustomModel(modelKey, isSlimSkin, isMale, bodyType, customRaceGender);
                }

                if (race.equals("bioandroid")) return isBaseForm ? BIO_ANDROID : BIO_ANDROID_PERFECT;
                if (race.equals("frostdemon")) return FROST_DEMON;
                if (race.equals("namekian")) return BASE_DEFAULT;

                if (race.equals("majin")) {
                    if (isBaseForm) return isMale ? MAJIN_FAT : MAJIN_SLIM;
                    return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                }

                if (race.equals("human") || race.equals("saiyan")) {
                    if (!isMale) return MAJIN_SLIM;
                    if (bodyType == 0) return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                    return BASE_DEFAULT;
                }

                if (!isMale) return MAJIN_SLIM;
                return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
            });
        }).orElse(BASE_DEFAULT);
    }

    private ResourceLocation resolveCustomModel(String modelName, boolean isSlimSkin, boolean isMale, int bodyType, String customRaceGender) {
        String key = modelName.toLowerCase();

        switch (key) {
            case "human": case "saiyan": case "saiyan_ssj4":
                if (bodyType == 0) return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                if (!isMale) return MAJIN_SLIM;
                return BASE_DEFAULT;
            case "buffed":
                if (bodyType == 0) return isSlimSkin ? HUMAN_SAIYAN_FEMALE_BUFFED : HUMAN_SAIYAN_BUFFED;
                if (!isMale) return HUMAN_SAIYAN_FEMALE_BUFFED;
                return HUMAN_SAIYAN_BUFFED;
            case "namekian": return BASE_DEFAULT;
            case "namekian_orange": return HUMAN_SAIYAN_BUFFED;
            case "majin": return isMale ? MAJIN_FAT : MAJIN_SLIM;
            case "majin_super": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            case "majin_ultra": return isMale ? HUMAN_SAIYAN_BUFFED : HUMAN_SAIYAN_FEMALE_BUFFED;
            case "majin_evil": case "majin_kid": return isMale ? BASE_SLIM : MAJIN_SLIM;
            case "frostdemon": case "frostdemon_final": return FROST_DEMON;
            case "frostdemon_second": return FROST_DEMON_SECOND;
            case "frostdemon_fifth": return FROST_DEMON_FIFTH;
            case "frostdemon_fp": return FROSTDEMON_BUFFED;
            case "frostdemon_third": return FROST_DEMON_THIRD;
            case "bioandroid_base": return BIO_ANDROID;
            case "bioandroid_semi": return BIO_ANDROID_SEMI;
            case "bioandroid_perfect": return BIO_ANDROID_PERFECT;
            case "bioandroid_ultra": return BIO_ANDROID_ULTRA;
            case "oozaru": return OOZARU;
        }

        String suffix = (customRaceGender != null && !customRaceGender.isEmpty()) ? "_" + customRaceGender : "";
        ResourceLocation customLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + modelName + suffix + ".geo.json");

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

        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float headPitch = entityData.headPitch() * Mth.DEG_TO_RAD;
        float headYaw = entityData.netHeadYaw() * Mth.DEG_TO_RAD;

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        if (head != null) {
            if (FlySkillEvent.isFlyingFast()) {
                head.setRotX(0.7854F);
                head.setRotY(0);
            } else {
                head.setRotX(headPitch);
                head.setRotY(headYaw);
            }
        }

        if (animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isShootingKi()) {
            CoreGeoBone rightArm = this.getAnimationProcessor().getBone("right_arm");
            if (rightArm != null) {
                rightArm.setRotX(headPitch + 1.5708F);
                rightArm.setRotY(headYaw);
            }
        }

        float partialTick = animationState.getPartialTick();
        float ageInTicks = (float) animatable.getTick(animatable);
        CoreGeoBone rightArm = this.getAnimationProcessor().getBone("right_arm");
        CoreGeoBone leftArm = this.getAnimationProcessor().getBone("left_arm");

        try {
            if (rightArm != null) RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            if (leftArm != null) RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
        } catch (Exception ignored) {
        }
    }

    private boolean fileExists(ResourceLocation location) {
        return FILE_EXISTS_CACHE.computeIfAbsent(location, loc ->
                Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()
        );
    }
}