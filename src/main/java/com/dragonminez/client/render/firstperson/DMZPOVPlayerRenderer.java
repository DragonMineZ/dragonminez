package com.dragonminez.client.render.firstperson;

import com.dragonminez.client.render.DMZPlayerRenderer;
import com.dragonminez.client.render.EntityPreviewRenderContext;
import com.dragonminez.client.render.firstperson.dto.DMZCameraBuffer;
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
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class DMZPOVPlayerRenderer<T extends AbstractClientPlayer & GeoAnimatable> extends DMZPlayerRenderer<T> {
    public DMZPOVPlayerRenderer(EntityRendererProvider.Context renderManager, GeoModel model) {
        super(renderManager, model);
    }

    @Override
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
		applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, 1.0F);
	}

	// GeckoLib 4.9 calls this overload directly. DMZ 1.20.1 used GeckoLib 4.8,
	// where only the five-argument overload existed.
	@Override
	protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        final LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null || animatable != localPlayer || !FirstPersonManager.shouldRenderFirstPerson(animatable)) {
			super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
            return;
        }

        final Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final Vec3 playerPos = localPlayer.getPosition(partialTick);
        final Vector3f offset = FirstPersonManager.offsetFirstPersonView(localPlayer);
        final float BODY_PUSHBACK_Z = 0.25F;

        final Vec3 camShift = DMZCameraBuffer.getFirstPersonShift();
        final Vector3f modelScale = poseStack.last().pose().getScale(new Vector3f());

        float invX = modelScale.x() != 0F ? 1.0F / modelScale.x() : 1.0F;
        float invY = modelScale.y() != 0F ? 1.0F / modelScale.y() : 1.0F;
        float invZ = modelScale.z() != 0F ? 1.0F / modelScale.z() : 1.0F;

        poseStack.translate((playerPos.x - cameraPos.x - camShift.x) * invX, (playerPos.y - cameraPos.y - camShift.y) * invY, (playerPos.z - cameraPos.z - camShift.z) * invZ);
		super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.translate(offset.x(), 0.0D, offset.z() + BODY_PUSHBACK_Z);
    }

    @Override
    public void preRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
		int renderColour = animatable.isSpectator() ? net.minecraft.util.FastColor.ARGB32.color(38, colour) : colour;
		super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColour);
        BoneVisibilityHandler.updateVisibility(model, animatable, this.caller);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        boolean originallyHidden = bone.isHidden();
        boolean isLocalPlayer = (animatable == Minecraft.getInstance().player);

        // Hiding the head is right when the camera is inside it, but wrong for a character
        // preview. DMZHairLayer already makes that distinction; this rule needs to as well.
        if (isLocalPlayer && bone.getName().equals("head")
                && FirstPersonManager.shouldRenderFirstPerson(animatable)
                && !EntityPreviewRenderContext.isRendering()) bone.setHidden(true);
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        bone.setHidden(originallyHidden);
    }

    @Override
    public boolean shouldRender(T pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (pLivingEntity == Minecraft.getInstance().player) return !pLivingEntity.isSleeping();
        return super.shouldRender(pLivingEntity, pCamera, pCamX, pCamY, pCamZ) && !pLivingEntity.isSleeping();
    }
}
