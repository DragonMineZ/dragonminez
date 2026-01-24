package com.dragonminez.client.render.firstperson;

import com.dragonminez.client.render.PlayerDMZRenderer;
import com.dragonminez.client.render.firstperson.dto.FirstPersonManager;
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
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class PlayerDMZPOVRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends PlayerDMZRenderer<T> {
    public PlayerDMZPOVRenderer(EntityRendererProvider.Context renderManager, GeoModel model) {
        super(renderManager, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final Vec3 playerPos = player.getPosition(partialTick);
        final Vector3f offset = FirstPersonManager.offsetFirstPersonView(player);

        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);

        poseStack.translate(
                (playerPos.x + offset.x) - cameraPos.x,
                (playerPos.y + offset.y) - cameraPos.y,
                (playerPos.z + offset.z) - cameraPos.z
        );
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, float red, float green,
                                  float blue, float alpha) {
        if (bone.getName().equals("head")) {
            bone.setHidden(true);
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public boolean shouldRender(@NonNull T pLivingEntity, @NonNull Frustum pCamera, double pCamX, double pCamY,
                                double pCamZ) {
        return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ) && !pLivingEntity.isSleeping();
    }
}
