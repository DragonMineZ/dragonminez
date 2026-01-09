package com.dragonminez.client.model;

import com.dragonminez.Reference;
import com.dragonminez.client.util.RenderUtil;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class PlayerDMZModel<T extends AbstractClientPlayer & GeoAnimatable> extends GeoModel<T> {

    private static final ResourceLocation BASE_DEFAULT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/dmzbase.geo.json");
    private static final ResourceLocation BASE_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/dmzbaseslim.geo.json");
    private static final ResourceLocation MAJIN_FAT = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majinfat.geo.json");
    private static final ResourceLocation MAJIN_SLIM = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/majinslim.geo.json");

    private final ResourceLocation textureLocation;
    private final ResourceLocation animationLocation;
    private final String raceName;
    private final String customModel;

    public PlayerDMZModel(String raceName, String customModel) {
        this.raceName = raceName.toLowerCase();
        this.customModel = customModel;

        this.textureLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/races/krillin.png");
        this.animationLocation = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "animations/entity/races/base.animation.json");
    }

    public PlayerDMZModel() {
        this("human", "");
    }

    @Override
    public ResourceLocation getModelResource(T player) {

        if (this.customModel != null && !this.customModel.isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + this.customModel);
        }

        return StatsProvider.get(StatsCapability.INSTANCE, player).map(data -> {

            int bodyType = data.getCharacter().getBodyType();
            var gender = data.getCharacter().getGender();

            boolean isStandardRace = this.raceName.equals("human") ||
                    this.raceName.equals("saiyan");

            if (bodyType == 0 && isStandardRace) {

                if (player.getModelName().equals("slim")) {
                    return BASE_SLIM;
                } else {
                    return BASE_DEFAULT;
                }
            }
            else {
                return switch (this.raceName) {
                    case "human", "saiyan" -> {
                        if ("female".equals(gender)) {
                            yield MAJIN_SLIM;
                        }
                        yield BASE_DEFAULT;
                    }
                    case "namekian" -> BASE_DEFAULT;
                    case "majin" -> {
                        if("female".equals(gender)){
                            yield MAJIN_SLIM;
                        }
                        yield MAJIN_FAT;

                    }
                    default -> ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/entity/races/" + this.raceName + ".geo.json");
                };
            }

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
		var waist = this.getAnimationProcessor().getBone("waist");
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

		float waistContribution = 0.3f;
		float globalReduction = 0.75f;

		if (head != null) {
			float rawPitch = entityData.headPitch();
			float rawYaw = entityData.netHeadYaw();
			float pitch = Mth.clamp(rawPitch, -90f, 90f);
			float yaw = Mth.clamp(rawYaw, -90f, 90f);
			float waistPitch = pitch * waistContribution;
			float waistYaw = yaw * waistContribution;
			if (waist != null) {
				waist.setRotX(waist.getRotX() + (waistPitch * Mth.DEG_TO_RAD));
				waist.setRotY(waist.getRotY() + (waistYaw * Mth.DEG_TO_RAD));
			}
			head.setRotX(rawPitch * Mth.DEG_TO_RAD * globalReduction);
			head.setRotY(rawYaw * Mth.DEG_TO_RAD * globalReduction);
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
        } catch (Exception e) {
        }
    }
}
