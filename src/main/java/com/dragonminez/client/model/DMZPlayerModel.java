package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.data.DMZAnimatable;
import com.dragonminez.client.util.RenderUtil;
import com.dragonminez.client.util.TextureCounter;
import com.dragonminez.client.util.ResourceType;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.util.lists.FrostDemonForms;
import com.dragonminez.common.util.lists.MajinForms;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Objects;

public class DMZPlayerModel extends GeoModel<DMZAnimatable> {

    private static final ResourceLocation BASE_MODEL = TextureCounter.cache(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final ResourceLocation BASE_TEXTURE = TextureCounter.cache(Reference.MOD_ID, "textures/entity/races/human.png");
    private static final ResourceLocation NULL_TEXTURE = TextureCounter.cache(Reference.MOD_ID, "textures/entity/races/null.png");

    private static final ResourceLocation BASE_ANIMATION = TextureCounter.cache(Reference.MOD_ID, "animations/entity/races/base.animation.json");

    @Override
    public ResourceLocation getModelResource(DMZAnimatable animatable) {
        final AbstractClientPlayer player = DMZPlayerRenderer.INSTANCE.getCurrentEntity();
        if (player == null) {
            return BASE_MODEL;
        }
        return StatsProvider.get(StatsCapability.INSTANCE, player)
                .map(data -> racePath(player, ResourceType.GEO, data)).orElse(BASE_MODEL);
    }

//    @Override
//    public ResourceLocation getTextureResource(DMZAnimatable animatable) {
//        final AbstractClientPlayer player = DMZPlayerRenderer.INSTANCE.getCurrentEntity();
//        if (player == null) {
//            return BASE_TEXTURE;
//        }
//        return StatsProvider.get(StatsCapability.INSTANCE, player)
//                .map(data -> racePath(player, ResourceType.TEXTURES, data)).orElse(BASE_TEXTURE);
//    }


    @Override
    public ResourceLocation getTextureResource(DMZAnimatable animatable) {
        return NULL_TEXTURE;
    }

//    @Override
//    public ResourceLocation getAnimationResource(DMZAnimatable animatable) {
//        final AbstractClientPlayer player = DMZPlayerRenderer.INSTANCE.getCurrentEntity();
//        if (player == null) {
//            return BASE_ANIMATION;
//        }
//        return StatsProvider.get(StatsCapability.INSTANCE, player)
//                .map(data -> racePath(player, ResourceType.ANIMATIONS, data)).orElse(BASE_ANIMATION);
//    }

    @Override
    public ResourceLocation getAnimationResource(DMZAnimatable animatable) {
        return BASE_ANIMATION;
    }

    @Override
    public void setCustomAnimations(DMZAnimatable animatable, long instanceId,
                                    AnimationState<DMZAnimatable> animationState) {
        final AbstractClientPlayer player = DMZPlayerRenderer.INSTANCE.getCurrentEntity();
        if (player == null) {
            return;
        }

        super.setCustomAnimations(animatable, instanceId, animationState);

        var head = this.getAnimationProcessor().getBone("head");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

        if (head != null) {
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

        try {
            float partialTick = animationState.getPartialTick();
            float ageInTicks = (float) animatable.getTick(animatable);

            var rightArm = this.getAnimationProcessor().getBone("right_arm");
            var leftArm = this.getAnimationProcessor().getBone("left_arm");

            if (rightArm != null) {
                RenderUtil.animateHand(player, rightArm, partialTick, ageInTicks);
            }
            if (leftArm != null) {
                RenderUtil.animateHand(player, leftArm, partialTick, ageInTicks);
            }
        } catch (Exception ignored) {
        }
    }

    private ResourceLocation racePath(AbstractClientPlayer player, ResourceType resource, StatsData data) {
        final var rawRaceId = data.getCharacter().getRaceName().isEmpty() ? "base" : data.getCharacter().getRaceName();
        final RaceCharacterConfig config = ConfigManager.getRaceCharacter(rawRaceId);

        if (config == null) {
            return resource == ResourceType.GEO ? BASE_MODEL : BASE_ANIMATION;
        }

        String customModel = config.getCustomModel();
        final var character = data.getCharacter();
        String currentForm = character.getActiveForm();
        final var gender = character.getGender();
        boolean isMale = gender.equalsIgnoreCase("male") || gender.equalsIgnoreCase("hombre");

        if (character.hasActiveForm()) {
            FormConfig.FormData activeFormData = character.getActiveFormData();

            if (rawRaceId.equals("frostdemon")) {
                if (FrostDemonForms.SECOND_FORM.equals(currentForm)) customModel = "frostdemon";
                else if (FrostDemonForms.THIRD_FORM.equals(currentForm)) customModel = "frostdemon_third";
                else if (FrostDemonForms.FINAL_FORM.equals(currentForm)) customModel = "frostdemon";
            }

            else if (rawRaceId.equals("majin")) {
                boolean isKidOrEvil = Objects.equals(currentForm, MajinForms.KID) || Objects.equals(currentForm, MajinForms.EVIL);
                if (isKidOrEvil) {
                    customModel = isMale ? "human_slim" : "majin_slim";
                }
                boolean isSuperOrUltra = Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA);

                if (isSuperOrUltra) {
                    customModel = isMale ? "human" : "majin_slim";
                }
            }

            if (customModel == null && activeFormData != null && activeFormData.hasCustomModel()) {
                customModel = activeFormData.getCustomModel();
            }
        }

        final boolean hasCustomModel = customModel != null && !customModel.isEmpty();
        int bodyType = character.getBodyType();

        var raceId = hasCustomModel ? customModel : rawRaceId;

        if (!hasCustomModel && resource == ResourceType.ANIMATIONS) {
            raceId = "base";
        }

        if (raceId.equals("majin") && !isMale) {
            raceId = "majin_slim";
        }

        if (resource == ResourceType.TEXTURES && (raceId.isEmpty() || raceId.equals("saiyan") || raceId.equals("human"))) {
            return player.getSkinTextureLocation();
        }

        boolean isSlimSkin = !isMale || isSlim(player);

        if (resource == ResourceType.GEO) {
            boolean isHumanOrSaiyan = rawRaceId.equals("human") || rawRaceId.equals("saiyan");

            if (isHumanOrSaiyan && !isMale) {
                raceId = "majin_slim";
            }
            else if (raceId.equals("saiyan") || raceId.equals("human") || raceId.equals("namekian")) {
                raceId = "human";
            }
        }

        String raceName = raceId;

        boolean isBuffHumanoid = (rawRaceId.equals("human") || rawRaceId.equals("saiyan")) && bodyType > 0;

        if (isSlimSkin && raceId.equals("human") && !rawRaceId.equals("namekian") && !isBuffHumanoid && !raceId.equals("majin_slim")) {
            raceName = raceId + "_" + "slim";
        }

        return TextureCounter.cache(Reference.MOD_ID, resource.name().toLowerCase()
                + "/entity/races/" + raceName + resource.extension());
    }


    private boolean isSlim(AbstractClientPlayer player) {
        return !player.getModelName().equals("default");
    }
}
