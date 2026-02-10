package com.dragonminez.client.render.firstperson;

import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
import com.dragonminez.client.util.BoneVisibilityHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class DMZPOVPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerRenderer<T> {
    public DMZPOVPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel model) {
        super(renderManager, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        final LocalPlayer localPlayer = Minecraft.getInstance().player;

        if (localPlayer == null || animatable != localPlayer) {
            super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
            return;
        }

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final Vec3 playerPos = localPlayer.getPosition(partialTick);
        final Vector3f offset = FirstPersonManager.offsetFirstPersonView(localPlayer);

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);

        poseStack.translate(
                (playerPos.x + offset.x) - cameraPos.x,
                (playerPos.y + offset.y) - cameraPos.y,
                (playerPos.z + offset.z) - cameraPos.z
        );
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        BoneVisibilityHandler.updateVisibility(model, animatable);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean originallyHidden = bone.isHidden();
        boolean isLocalPlayer = (animatable == Minecraft.getInstance().player);

        if (isLocalPlayer && bone.getName().equals("head") && FirstPersonManager.shouldRenderFirstPerson(animatable)) {
            bone.setHidden(true);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        bone.setHidden(originallyHidden);
    }

    @Override
    public boolean shouldRender(@NonNull T pLivingEntity, @NonNull Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (pLivingEntity == Minecraft.getInstance().player) {
            return !pLivingEntity.isSleeping();
        }
        return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ) && !pLivingEntity.isSleeping();
    }
}
