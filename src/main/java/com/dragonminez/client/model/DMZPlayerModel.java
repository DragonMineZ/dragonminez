package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.animation.IPlayerAnimatable;
import com.dragonminez.client.events.FlySkillEvent;
import com.dragonminez.client.render.util.RenderUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.init.MainEffects;
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
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DMZPlayerModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final List<String> SKIP_HEAD_ANIMATIONS = List.of("transf.ssj", "transf.ssj2");

    private static final ResourceLocation BASE_DEFAULT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final ResourceLocation BASE_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human_slim.geo.json");
    private static final ResourceLocation MAJIN_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin.geo.json");
    private static final ResourceLocation MAJIN_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin_slim.geo.json");
    private static final ResourceLocation JANEMBA_SUPER = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/janemba_super.geo.json");
    private static final ResourceLocation JANEMBA_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/janemba_fat.geo.json");

    private static final ResourceLocation FROST_DEMON = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json");
    private static final ResourceLocation FROST_DEMON_SECOND = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_second.geo.json");
    private static final ResourceLocation FROST_DEMON_THIRD = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_third.geo.json");
    private static final ResourceLocation FROST_DEMON_FIFTH = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fifth.geo.json");
    private static final ResourceLocation FROSTDEMON_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_fp.geo.json");
    private static final ResourceLocation FROSTDEMON_METALCORE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon_metalcore.geo.json");

    private static final ResourceLocation BIO_ANDROID = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json");
    private static final ResourceLocation BIO_ANDROID_SEMI = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_semi.geo.json");
    private static final ResourceLocation BIO_ANDROID_PERFECT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_perfect.geo.json");
    private static final ResourceLocation BIO_ANDROID_ULTRA = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_ultra.geo.json");
    private static final ResourceLocation BIO_ANDROID_XENO = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_xeno.geo.json");

    private static final ResourceLocation OOZARU = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/oozaru.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_SLIM_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_slim.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_FEMALE_BUFFED = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/hbuffed_fem.geo.json");

    private static final ResourceLocation HUMAN_SAIYAN_4ARMS = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4arms.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_4ARMS_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4armsslim.geo.json");
    private static final ResourceLocation HUMAN_SAIYAN_4ARMS_FEM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/h4armsfem.geo.json");

    private static final ResourceLocation CANDY_MODEL = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/candy.geo.json");
    private static final ResourceLocation CANDY_TEXTURE = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/candy.png");

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
        if (player.hasEffect(MainEffects.CANDY.get())) {
            return CANDY_MODEL;
        }

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
            // HUMAN & SAIYAN
            case "human":
            case "saiyan":
            case "oozaru": return OOZARU;
            case "ssj4gt":
                if (bodyType == 0) return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                if (!isMale) return MAJIN_SLIM;
                return BASE_DEFAULT;
            case "ssj4d":
                if (bodyType == 0) return isSlimSkin ? HUMAN_SAIYAN_SLIM_BUFFED : HUMAN_SAIYAN_BUFFED;
                if (!isMale) return HUMAN_SAIYAN_FEMALE_BUFFED;
                return HUMAN_SAIYAN_BUFFED;
            case "buffed":
                if (bodyType == 0) return isSlimSkin ? HUMAN_SAIYAN_SLIM_BUFFED : HUMAN_SAIYAN_BUFFED;
                if (!isMale) return HUMAN_SAIYAN_FEMALE_BUFFED;
                return HUMAN_SAIYAN_BUFFED;
            case "4arms":
                if (bodyType == 0) return isSlimSkin ? HUMAN_SAIYAN_4ARMS_SLIM : HUMAN_SAIYAN_4ARMS;
                if (!isMale) return HUMAN_SAIYAN_4ARMS_FEM;
                return HUMAN_SAIYAN_4ARMS;

            // NAMEKIAN
            case "namekian": return BASE_DEFAULT;
            case "namekian_orange": case "namekian_buffed": return HUMAN_SAIYAN_BUFFED;

            // MAJIN
            case "majin": return isMale ? MAJIN_FAT : MAJIN_SLIM;
            case "majin_super": return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            case "majin_ultra": return isMale ? HUMAN_SAIYAN_BUFFED : HUMAN_SAIYAN_SLIM_BUFFED;
            case "majin_evil": case "majin_kid": return isMale ? BASE_SLIM : MAJIN_SLIM;
            case "janemba_fat": return JANEMBA_FAT;
            case "janemba_super": return JANEMBA_SUPER;

            // FROSTDEMON
            case "frostdemon": case "frostdemon_final": case "frostdemon_mecha":return FROST_DEMON;
            case "frostdemon_second": return FROST_DEMON_SECOND;
            case "frostdemon_fifth": return FROST_DEMON_FIFTH;
            case "frostdemon_fp": return FROSTDEMON_BUFFED;
            case "frostdemon_third": return FROST_DEMON_THIRD;
            case "frostdemon_metalcore": return FROSTDEMON_METALCORE;

            // BIOANDROID
            case "bioandroid_base": return BIO_ANDROID;
            case "bioandroid_semi": return BIO_ANDROID_SEMI;
            case "bioandroid_perfect": return BIO_ANDROID_PERFECT;
            case "bioandroid_ultra": return BIO_ANDROID_ULTRA;
            case "bioandroid_xeno": return BIO_ANDROID_XENO;

        }

        String suffix = (customRaceGender != null && !customRaceGender.isEmpty()) ? "_" + customRaceGender : "";
        ResourceLocation customLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + modelName + suffix + ".geo.json");

        if (fileExists(customLoc)) return customLoc;
        return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
    }

    @Override
    public ResourceLocation getTextureResource(T t) {
        if (t.hasEffect(MainEffects.CANDY.get())) {
            return CANDY_TEXTURE;
        }
        return textureLocation;
    }

    @Override
    public ResourceLocation getAnimationResource(T t) {
        return animationLocation;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        boolean skipHead = animatable instanceof IPlayerAnimatable pa && SKIP_HEAD_ANIMATIONS.contains(pa.dragonminez$getCurrentPlayingAnimation());

        float partialTick = animationState.getPartialTick();
        float bodyYaw = Mth.lerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        float headYawDeg = Mth.wrapDegrees(Mth.lerp(partialTick, animatable.yHeadRotO, animatable.yHeadRot) - bodyYaw);
        float lookYaw = -headYawDeg * Mth.DEG_TO_RAD;
        float lookPitch = -Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD;

        CoreGeoBone head = this.getAnimationProcessor().getBone("head");
        CoreGeoBone waist = this.getAnimationProcessor().getBone("waist");
        CoreGeoBone rightArm = this.getAnimationProcessor().getBone("right_arm");
        CoreGeoBone leftArm = this.getAnimationProcessor().getBone("left_arm");

        if (head != null && !skipHead) {
            EntityModelData entityModelData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            float lookPitchRad = entityModelData.headPitch() * Mth.DEG_TO_RAD;
            float lookYawRad = entityModelData.netHeadYaw() * Mth.DEG_TO_RAD;

            float waistRotX = (waist != null) ? waist.getRotX() : 0.0F;
            float waistRotY = (waist != null) ? waist.getRotY() : 0.0F;

            head.setRotX(lookPitchRad - waistRotX);
            head.setRotY(lookYawRad - waistRotY);
        }

        if (animatable instanceof IPlayerAnimatable playerAnim && playerAnim.dragonminez$isShootingKi()) {
            if (rightArm != null) {
                rightArm.setRotX(lookPitch + 1.5708F);
                rightArm.setRotY(lookYaw);
            }
        }

        boolean isInCombat = animatable instanceof IPlayerAnimatable pa && pa.dragonminez$isPlayingCombatAnimation();

        if (isInCombat) {
            float walkSpeed = animatable.walkAnimation.speed(partialTick);
            float walkPos   = animatable.walkAnimation.position(partialTick);

            if (walkSpeed > 0.01f) {
                float amplitude = Mth.clamp(walkSpeed, 0f, 3f) * 0.15f;

                if (rightArm != null) rightArm.setRotX(rightArm.getRotX() + Mth.sin(walkPos + Mth.PI) * amplitude);
                if (leftArm != null) leftArm.setRotX(leftArm.getRotX() + Mth.sin(walkPos) * amplitude);
            }
        }

        float ageInTicks = (float) animatable.getTick(animatable);

        try {
            if (rightArm != null) RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            if (leftArm != null) RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
        } catch (Exception ignored) {}

        applyBoobScale(animatable);
    }

    private void applyBoobScale(T animatable) {
        CoreGeoBone boobas = this.getAnimationProcessor().getBone("boobas");
        if (boobas == null) return;

        float factor = StatsProvider.get(StatsCapability.INSTANCE, animatable).map(data -> {
            Character c = data.getCharacter();
            String gender = c.getGender() != null ? c.getGender().toLowerCase() : "";
            boolean isFemale = gender.equals("female") || gender.equals("mujer") || c.getBodyType() == 1;
            return isFemale ? Mth.clamp(c.getBoobScale(), 0.75f, 1.25f) : 1.0f;
        }).orElse(1.0f);

        float[] axis = computeBoobAxisScale(factor);
        boobas.setScaleX(axis[0]);
        boobas.setScaleY(axis[1]);
        boobas.setScaleZ(axis[2]);
    }

    public static float[] computeBoobAxisScale(float factor) {
        float delta = factor - 1.0f;
        return new float[]{
                1.0f + delta * 0.15f,
                1.0f + delta * 0.3f,
                1.0f + delta * 1.4f
        };
    }


    private boolean fileExists(ResourceLocation location) {
        return FILE_EXISTS_CACHE.computeIfAbsent(location, loc ->
                Minecraft.getInstance().getResourceManager().getResource(loc).isPresent()
        );
    }

    @Override
    public void applyMolangQueries(T animatable, double animTime) {
        super.applyMolangQueries(animatable, animTime);
        boolean skipHead = animatable instanceof IPlayerAnimatable pa && SKIP_HEAD_ANIMATIONS.contains(pa.dragonminez$getCurrentPlayingAnimation());

        MolangParser parser = MolangParser.INSTANCE;

        float clampedPitch = skipHead ? 0F : -Mth.clamp(animatable.getXRot(), -85F, 85F);

        float relativeYaw = -Mth.wrapDegrees(animatable.getYHeadRot() - animatable.yBodyRot);
        float clampedYaw = skipHead ? 0F : Mth.clamp(relativeYaw, -90F, 90F);

        parser.setValue("query.head_x_rotation", () -> (double) clampedPitch);
        parser.setValue("query.head_y_rotation", () -> (double) clampedYaw);

        parser.setValue("query.head_pitch", () -> (double) clampedPitch);
        parser.setValue("query.head_yaw", () -> (double) clampedYaw);
    }
}


