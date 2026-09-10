package com.dragonminez.client.render.layer;

import com.dragonminez.Reference;
import com.dragonminez.common.combat.logic.weapon.KiWeaponHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.client.render.util.ModRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.HumanoidArm;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DMZWeaponsLayer<T extends AbstractClientPlayer & GeoAnimatable> extends GeoRenderLayer<T> {
    private static final float OOZARU_WEAPON_SCALE = 3.8f;
    private static final float[] HUMAN_ARM_RIGHT = {-5f, 22f, 0f};
    private static final float[] HUMAN_ARM_LEFT = {5f, 22f, 0f};
    private static final float[] OOZARU_ARM_RIGHT = {-12f, 74f, 0f};
    private static final float[] OOZARU_ARM_LEFT = {21f, 74f, 0f};

    public DMZWeaponsLayer(GeoRenderer<T> entityRendererIn) {
        super(entityRendererIn);
    }

    @Override
    public void renderForBone(PoseStack poseStack, T animatable, GeoBone playerBone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (!"right_arm".equals(playerBone.getName()) && !"left_arm".equals(playerBone.getName())) return;
        if (animatable.isSpectator()) return;

        var stats = StatsProvider.get(StatsCapability.INSTANCE, animatable).orElse(null);
        if (stats == null || !stats.getSkills().isSkillActive("kimanipulation")) return;

        String weaponType = stats.getStatus().getKiWeaponType();
        if (weaponType == null || weaponType.equalsIgnoreCase("none")) return;

        if (!animatable.getMainHandItem().isEmpty()) return;

        boolean isRight = animatable.getMainArm() == HumanoidArm.RIGHT;
        boolean isRightArm = "right_arm".equals(playerBone.getName());
        if (isRight != isRightArm) return;

        String type = weaponType.toLowerCase();
        ResourceLocation modelLoc = weaponModel(type);
        if (modelLoc == null) return;
        BakedGeoModel weaponModel = getGeoModel().getBakedModel(modelLoc);
        if (weaponModel == null) return;

        ResourceLocation texture = weaponTexture(type);
        if (texture == null) return;

        weaponModel.getBone(weaponBone(type)).ifPresent(targetBone -> {
            Character character = stats.getCharacter();
            boolean isOozaru = character != null && character.isOozaruCached();

            poseStack.pushPose();

            if (type.equals("clawlance")) {
                poseStack.mulPose(Axis.YP.rotationDegrees(35.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(35.0F));
                poseStack.translate(0.0F / 16f, -0.1F, -1.0F);
            }

            if (isOozaru) {
                float[] humanPivot = isRight ? HUMAN_ARM_RIGHT : HUMAN_ARM_LEFT;
                float[] oozaruPivot = isRight ? OOZARU_ARM_RIGHT : OOZARU_ARM_LEFT;
                float k = OOZARU_WEAPON_SCALE;
                poseStack.translate(oozaruPivot[0] / 16f, oozaruPivot[1] / 16f, oozaruPivot[2] / 16f);
                poseStack.scale(k, k, k);
                poseStack.translate(-humanPivot[0] / 16f, -humanPivot[1] / 16f, -humanPivot[2] / 16f);
            }

            float[] color = KiWeaponHelper.resolveColorForType(weaponType, getKiColor(stats));
            RenderType weaponRenderType = ModRenderTypes.energy2(texture);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(weaponRenderType);

            getRenderer().renderRecursively(poseStack, animatable, targetBone, weaponRenderType, bufferSource, vertexConsumer, true,
                    partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.colorFromFloat(0.65f, color[0], color[1], color[2]));

            poseStack.popPose();
        });
    }

    private static ResourceLocation weaponModel(String type) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "geo/weapons/kiweapon_" + type + ".geo.json");
        return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : null;
    }

    private static ResourceLocation weaponTexture(String type) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "textures/entity/weapons/kiweapon_" + type + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : null;
    }

    private static String weaponBone(String type) {
        return "kiweapon_" + type;
    }

    private float[] getKiColor(StatsData stats) {
        var character = stats.getCharacter();
        float[] kiColor = character.getRgbAuraColor();
        if (character.hasActiveForm() && character.getActiveFormData() != null) {
            String formColor = character.getActiveFormData().getAuraColor();
            if (formColor != null && !formColor.isEmpty()) kiColor = character.getActiveFormData().getRgbAuraColor();
        }
        return kiColor;
    }
}