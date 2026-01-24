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

public class PlayerDMZModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final ResourceLocation BASE_DEFAULT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human.geo.json");
    private static final ResourceLocation BASE_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/human_slim.geo.json");
    private static final ResourceLocation MAJIN_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin.geo.json");
    private static final ResourceLocation MAJIN_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majin_slim.geo.json");

    private final ResourceLocation textureLocation;
    private final ResourceLocation animationLocation;
    private final String raceName;
    private final String customModel;

    public PlayerDMZModel(String raceName, String customModel) {
        this.raceName = raceName.toLowerCase();
        this.customModel = customModel;

        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/null.png");
        this.animationLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    public PlayerDMZModel() {
        this("human", "");
    }

    @Override
    public ResourceLocation getModelResource(T player) {
        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {
            var character = data.getCharacter();
            String race = character.getRaceName().toLowerCase();
            String gender = character.getGender().toLowerCase();
            String currentForm = character.getActiveForm();
            int bodyType = character.getBodyType();

            boolean hasForm = (currentForm != null && !currentForm.isEmpty() && !currentForm.equals("base"));

            boolean isMale = gender.equals("male") || gender.equals("hombre");
            boolean isSlimSkin = player.getModelName().equals("slim");

            if (race.equals("saiyan") && (Objects.equals(currentForm, SaiyanForms.OOZARU) || Objects.equals(currentForm, SaiyanForms.GOLDEN_OOZARU))) {
                return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/oozaru.geo.json");
            }

            if (race.equals("majin")) {
                boolean isSuperOrUltra = Objects.equals(currentForm, MajinForms.SUPER) || Objects.equals(currentForm, MajinForms.ULTRA);
                boolean isEvil = Objects.equals(currentForm, MajinForms.EVIL);

                if (isSuperOrUltra) {
                    return isMale ? BASE_DEFAULT : MAJIN_SLIM;
                }

                if (isEvil) {
                    return isMale ? BASE_SLIM : MAJIN_SLIM;
                }
            }

            var activeFormData = character.getActiveFormData();
            if (activeFormData != null && activeFormData.hasCustomModel() && !activeFormData.getCustomModel().isEmpty()) {
                ResourceLocation formLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + activeFormData.getCustomModel() + ".geo.json");
                if (modelExists(formLoc)) return formLoc;
            }

            if (race.equals("majin") && hasForm) {
                return isMale ? BASE_DEFAULT : MAJIN_SLIM;
            }

            if (race.equals("bioandroid") && hasForm) {
                return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid_perfect.geo.json");
            }

            if (race.equals("human") || race.equals("saiyan")) {
                if (!isMale) {
                    return MAJIN_SLIM;
                }
                if (bodyType == 0) {
                    return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;
                }
                return BASE_DEFAULT;
            }

            switch (race) {
                case "namekian" -> { return BASE_DEFAULT; }
                case "bioandroid" -> { return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/bioandroid.geo.json"); }
                case "frostdemon" -> { return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/frostdemon.geo.json"); }
                case "majin" -> { return isMale ? MAJIN_FAT : MAJIN_SLIM; }
            }

            if (this.customModel != null && !this.customModel.isEmpty()) {
                ResourceLocation customLoc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + this.customModel + ".geo.json");
                if (modelExists(customLoc)) return customLoc;
            }

            return isSlimSkin ? BASE_SLIM : BASE_DEFAULT;

        }).orElse(BASE_DEFAULT);
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
                float flySpeedFactor = Math.min(1.0F, FlySkillEvent.getFlightSpeed() / 2.0F);
                float pitchCorrection = 70F * Mth.DEG_TO_RAD * flySpeedFactor;
                headPitch += pitchCorrection;
            }

			head.setRotX(headPitch);
			head.setRotY(headYaw);
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

            if (rightArm != null) {
                RenderUtil.animateHand(animatable, rightArm, partialTick, ageInTicks);
            }
            if (leftArm != null) {
                RenderUtil.animateHand(animatable, leftArm, partialTick, ageInTicks);
            }
        } catch (Exception ignore) {}

    }

    private boolean modelExists(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

}
